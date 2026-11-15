package com.app.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;
import com.app.admin.dto.FinancialListResponse;
import com.app.admin.dto.FinancialRecordResponse;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.*;
import com.app.admin.repository.*;
import com.app.admin.security.AdminSecurityContext;
import jakarta.persistence.criteria.Predicate;

@Service
public class TrainingFinancialAdminService {
    private static final int MAX_LIMIT = 500;
    private static final String SETTLEMENT_ELIGIBILITY_SQL = """
        SELECT s.status,s.amount,s.currency,s.trainer_user_id,s.provider_transfer_id,
                (s.status='ELIGIBLE' AND s.provider_transfer_id IS NULL AND s.amount>0 AND s.currency='INR'
                AND tpa.id IS NOT NULL AND tpa.trainer_user_id=s.trainer_user_id AND tpa.provider=pp.code
                AND tp.payout_status='VERIFIED' AND tp.payout_enabled=TRUE AND tpa.is_primary=TRUE AND tpa.account_status='VERIFIED'
                AND NULLIF(BTRIM(tpa.provider_account_id),'') IS NOT NULL AND pp.active=TRUE AND pp.code='RAZORPAY'
                AND EXISTS (SELECT 1 FROM training_settlement_items i WHERE i.settlement_id=s.id)
                AND s.amount=(SELECT COALESCE(SUM(i.amount),0) FROM training_settlement_items i WHERE i.settlement_id=s.id)
                AND (SELECT COUNT(DISTINCT p.id) FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id WHERE i.settlement_id=s.id)=1
                AND s.amount <= (SELECT MIN(p.amount-COALESCE((SELECT SUM(rf.refund_amount) FROM training_refunds rf WHERE rf.payment_id=p.id AND rf.status='PROCESSED'),0)) FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id WHERE i.settlement_id=s.id)
                AND NOT EXISTS (SELECT 1 FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id JOIN training_enrollments e ON e.id=i.enrollment_id JOIN training_offerings o ON o.id=e.training_offering_id WHERE i.settlement_id=s.id AND (c.status<>'CALCULATED' OR c.trainer_amount<>i.amount OR c.currency<>s.currency OR p.provider_payment_id IS NULL OR p.payment_provider_id<>s.payment_provider_id OR p.currency<>s.currency OR p.status NOT IN ('CAPTURED','PARTIALLY_REFUNDED') OR o.status<>'COMPLETED' OR c.gross_amount>p.amount-COALESCE((SELECT SUM(rf.refund_amount) FROM training_refunds rf WHERE rf.payment_id=p.id AND rf.status='PROCESSED'),0) OR NOT EXISTS (SELECT 1 FROM training_offering_sessions ss WHERE ss.training_offering_id=o.id) OR EXISTS (SELECT 1 FROM training_offering_sessions ss WHERE ss.training_offering_id=o.id AND ss.status<>'COMPLETED' AND ss.actual_end_at IS NULL) OR EXISTS (SELECT 1 FROM training_cancellations ca WHERE ca.enrollment_id=e.id AND ca.status IN ('REQUESTED','APPROVED')) OR EXISTS (SELECT 1 FROM training_refunds r WHERE (r.enrollment_id=e.id OR r.payment_id=p.id) AND r.status IN ('CREATED','PROCESSING'))))) AS eligible
          FROM training_settlements s
          LEFT JOIN trainer_payout_accounts tpa ON tpa.id=s.payment_account_id
          LEFT JOIN trainer_profiles tp ON tp.user_id=s.trainer_user_id
          LEFT JOIN payment_providers pp ON pp.id=s.payment_provider_id
         WHERE s.id=?
        """;
    private final TrainingPaymentRepository payments; private final TrainingCancellationRepository cancellations; private final TrainingRefundRepository refunds;
    private final TrainingCommissionRepository commissions; private final TrainerPayoutAccountRepository accounts; private final TrainingSettlementRepository settlements;
    private final TrainingSettlementItemRepository settlementItems; private final PaymentTransactionsLedgerRepository ledger; private final PaymentWebhookEventRepository webhooks;
    private final JdbcTemplate db;
    private final AdminAuditService audit;
    private final AdminSecurityContext security;
    private final boolean routeEnabled;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final RestClient razorpay = RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();

    public TrainingFinancialAdminService(TrainingPaymentRepository payments, TrainingCancellationRepository cancellations, TrainingRefundRepository refunds,
            TrainingCommissionRepository commissions, TrainerPayoutAccountRepository accounts, TrainingSettlementRepository settlements,
            TrainingSettlementItemRepository settlementItems, PaymentTransactionsLedgerRepository ledger, PaymentWebhookEventRepository webhooks,
            JdbcTemplate db, AdminAuditService audit, AdminSecurityContext security, @Value("${training.settlement.route-enabled:false}") boolean routeEnabled,
            @Value("${razorpay.key-id:}") String razorpayKeyId, @Value("${razorpay.key-secret:}") String razorpayKeySecret) {
        this.payments=payments; this.cancellations=cancellations; this.refunds=refunds; this.commissions=commissions; this.accounts=accounts;
        this.settlements=settlements; this.settlementItems=settlementItems; this.ledger=ledger; this.webhooks=webhooks;
        this.db = db;
        this.audit = audit;
        this.security = security;
        this.routeEnabled = routeEnabled;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    @Transactional
    public Object requestSettlementPayment(Long id) {
        security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_PROCESS);
        Map<String,Object> state;
        try {
            state = db.queryForMap(SETTLEMENT_ELIGIBILITY_SQL + " FOR UPDATE OF s", id);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Settlement", id);
        }
        if (!routeEnabled || razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Razorpay Route is not configured.");
        if (!Boolean.TRUE.equals(state.get("eligible"))) throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Settlement is not eligible for payment.");
        Map<String,Object> payout = db.queryForMap("""
            SELECT s.id,s.amount,s.currency,p.provider_payment_id,tpa.provider_account_id
              FROM training_settlements s
              JOIN training_settlement_items i ON i.settlement_id=s.id
              JOIN training_commissions c ON c.id=i.commission_id
              JOIN training_payments p ON p.id=c.payment_id
              JOIN trainer_payout_accounts tpa ON tpa.id=s.payment_account_id
             WHERE s.id=? ORDER BY i.id LIMIT 1
            """, id);
        String sourcePayment = String.valueOf(payout.get("provider_payment_id"));
        String linkedAccount = String.valueOf(payout.get("provider_account_id"));
        String reference = "training-settlement-" + id;
        Map<String,Object> found;
        try {
            found = findTransfer(sourcePayment, reference);
        } catch (RuntimeException lookupFailure) {
            markProcessing(id, null, "Provider transfer lookup is uncertain; refresh status before retrying.");
            return Map.of("id", id, "status", "PROCESSING", "eligible", false, "message", "Provider status is uncertain. Refresh payment status before retrying.");
        }
        if (found == null) {
            long minor = ((java.math.BigDecimal)payout.get("amount")).movePointRight(2).longValueExact();
            Map<String,Object> transfer = Map.of("account", linkedAccount, "amount", minor, "currency", payout.get("currency"), "on_hold", false, "notes", Map.of("reference", reference));
            try {
                Map<String,Object> response = razorpay.post().uri("/payments/{paymentId}/transfers", sourcePayment)
                        .headers(h -> h.setBasicAuth(razorpayKeyId, razorpayKeySecret))
                        .header("X-Transfer-Idempotency", "ts-" + id)
                        .body(Map.of("transfers", java.util.List.of(transfer))).retrieve()
                        .body(new ParameterizedTypeReference<Map<String,Object>>() {});
                Object items = response == null ? null : response.get("items");
                found = items instanceof java.util.List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?,?> item ? (Map<String,Object>)item : response;
            } catch (RuntimeException providerFailure) {
                markProcessing(id, null, "Provider response is uncertain; refresh status before retrying.");
                return Map.of("id", id, "status", "PROCESSING", "eligible", false, "message", "Provider response is uncertain. Refresh payment status before retrying.");
            }
        }
        String transferId = found == null ? null : String.valueOf(found.get("id"));
        String providerStatus = found == null ? "" : String.valueOf(found.get("status"));
        if (transferId == null || "null".equals(transferId)) {
            markProcessing(id, null, "Provider response did not include a transfer ID; refresh status before retrying.");
            return Map.of("id", id, "status", "PROCESSING", "eligible", false, "message", "Provider response is uncertain. Refresh payment status before retrying.");
        }
        String storedStatus = "processed".equalsIgnoreCase(providerStatus) ? "COMPLETED" : "failed".equalsIgnoreCase(providerStatus) ? "FAILED" : "reversed".equalsIgnoreCase(providerStatus) ? "REVERSED" : "PROCESSING";
        db.update("UPDATE training_settlements SET status=?,provider_transfer_id=?,initiated_at=COALESCE(initiated_at,CURRENT_TIMESTAMP),completed_at=CASE WHEN ?='processed' THEN CURRENT_TIMESTAMP ELSE completed_at END,failure_reason=CASE WHEN ? IN ('failed','reversed') THEN 'Razorpay reported transfer ' || ? ELSE NULL END,updated_at=CURRENT_TIMESTAMP WHERE id=? AND provider_transfer_id IS NULL AND status='ELIGIBLE'", storedStatus, transferId, providerStatus, providerStatus, providerStatus, id);
        if ("COMPLETED".equals(storedStatus)) {
            recordSettlementLedger(id, "TRAINER_SETTLEMENT", "settlement-item-", true);
            db.update("UPDATE training_commissions SET status='SETTLED',updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT commission_id FROM training_settlement_items WHERE settlement_id=?)", id);
        } else if ("REVERSED".equals(storedStatus)) {
            recordSettlementLedger(id, "SETTLEMENT_REVERSAL", "settlement-reversal-", false);
        }
        return Map.of("id", id, "status", storedStatus, "eligible", false, "providerTransferId", transferId);
    }

    private void markProcessing(Long id, String transferId, String reason) {
        db.update("UPDATE training_settlements SET status='PROCESSING',provider_transfer_id=COALESCE(?,provider_transfer_id),initiated_at=COALESCE(initiated_at,CURRENT_TIMESTAMP),failure_reason=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='ELIGIBLE'", transferId, reason, id);
    }

    @Transactional
    public Object refreshSettlementPayment(Long id) {
        security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_RECONCILE);
        TrainingSettlement settlement = settlements.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Settlement", id));
        if (!"PROCESSING".equalsIgnoreCase(settlement.getStatus())) throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Only processing settlements can be reconciled.");
        if (!routeEnabled || razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Razorpay Route is not configured.");
        try {
            String paymentId = db.queryForObject("SELECT p.provider_payment_id FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id WHERE i.settlement_id=? ORDER BY i.id LIMIT 1", String.class, id);
            Map<String,Object> transfer = findTransfer(paymentId, "training-settlement-" + id);
            if (transfer == null) {
                db.update("UPDATE training_settlements SET failure_reason='Provider has no matching transfer; retry is safe after review.',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'", id);
                return Map.of("id", id, "status", "PROCESSING", "message", "No matching provider transfer was found. Verify account readiness before retrying.");
            }
            String providerStatus = String.valueOf(transfer.get("status")).toLowerCase(java.util.Locale.ROOT);
            String stored = switch (providerStatus) { case "processed" -> "COMPLETED"; case "failed" -> "FAILED"; case "reversed" -> "REVERSED"; default -> "PROCESSING"; };
            String transferId = String.valueOf(transfer.get("id"));
            String reason = "FAILED".equals(stored) ? "Razorpay reported transfer failure" : "REVERSED".equals(stored) ? "Razorpay reversed transfer" : null;
            db.update("UPDATE training_settlements SET status=?,provider_transfer_id=COALESCE(provider_transfer_id,?),completed_at=CASE WHEN ?='COMPLETED' THEN COALESCE(completed_at,CURRENT_TIMESTAMP) ELSE completed_at END,failure_reason=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'", stored, transferId, stored, reason, id);
            if ("COMPLETED".equals(stored)) {
                recordSettlementLedger(id, "TRAINER_SETTLEMENT", "settlement-item-", true);
                db.update("UPDATE training_commissions SET status='SETTLED',updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT commission_id FROM training_settlement_items WHERE settlement_id=?)", id);
            } else if ("REVERSED".equals(stored)) recordSettlementLedger(id, "SETTLEMENT_REVERSAL", "settlement-reversal-", false);
            audit.record("TRAINER_PAYOUT_STATUS_REFRESHED", "SETTLEMENT", "SETTLEMENT", id, "Trainer payout status refreshed from Razorpay: " + stored + ".");
            return Map.of("id", id, "status", stored, "providerTransferId", transferId);
        } catch (org.springframework.dao.EmptyResultDataAccessException missingSourcePayment) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "The source payment cannot be found for provider reconciliation.");
        }
    }

    @Transactional
    public Object retrySettlementPayment(Long id) {
        security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_PROCESS);
        TrainingSettlement settlement = settlements.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Settlement", id));
        if (!"FAILED".equalsIgnoreCase(settlement.getStatus())) throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Only failed settlements can be retried.");
        if (settlement.getProviderTransferId() != null) {
            Object refreshed = refreshSettlementPayment(id);
            if (!(refreshed instanceof Map<?,?> map) || !"PROCESSING".equals(String.valueOf(map.get("status")))) return refreshed;
        }
        db.update("UPDATE training_settlements SET status='ELIGIBLE',provider_transfer_id=NULL,failure_reason=NULL,completed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='FAILED'", id);
        audit.record("TRAINER_PAYOUT_RETRY", "SETTLEMENT", "SETTLEMENT", id, "Failed trainer payout reset after provider reconciliation for a safe retry.");
        return requestSettlementPayment(id);
    }

    private void recordSettlementLedger(Long settlementId, String type, String referencePrefix, boolean negate) {
        List<Map<String,Object>> items = db.queryForList("SELECT id,enrollment_id,commission_id,amount FROM training_settlement_items WHERE settlement_id=? ORDER BY id", settlementId);
        for (Map<String,Object> item : items) {
            String reference = referencePrefix + item.get("id");
            java.math.BigDecimal amount = (java.math.BigDecimal)item.get("amount");
            if (negate) amount = amount.negate();
            db.update("""
                INSERT INTO payment_transactions_ledger(enrollment_id,commission_id,settlement_id,transaction_type,amount,currency,reference_id,description)
                SELECT ?,?,?,?,?,'INR',?,?
                 WHERE NOT EXISTS (SELECT 1 FROM payment_transactions_ledger WHERE transaction_type=? AND reference_id=?)
                """, item.get("enrollment_id"), item.get("commission_id"), settlementId, type, amount,
                    reference, "TRAINER_SETTLEMENT".equals(type) ? "Trainer settlement" : "Trainer settlement reversal", type, reference);
        }
    }

    private Map<String,Object> findTransfer(String paymentId, String reference) {
        Map<String,Object> list = razorpay.get().uri("/payments/{paymentId}/transfers", paymentId)
            .headers(h -> h.setBasicAuth(razorpayKeyId, razorpayKeySecret)).retrieve()
            .body(new ParameterizedTypeReference<Map<String,Object>>() {});
        Object items = list == null ? null : list.get("items");
        if (items instanceof java.util.List<?> values) for (Object value : values) if (value instanceof Map<?,?> item) {
            Object notes = item.get("notes"); if (notes instanceof Map<?,?> n && reference.equals(n.get("reference"))) return (Map<String,Object>)item;
        }
        return null;
    }

    public Object settlementStatus(Object response) {
        return response instanceof Map<?, ?> map ? map.get("status") : null;
    }

    @Transactional public FinancialListResponse list(String resource, Map<String,String> filters) {
        int limit = parseLimit(filters.get("limit"));
        return switch(resource) {
            case "payments" -> response(payments.findAll(spec(filters, "createdAt", Map.of("status","status","trainee_user_id","traineeUserId","trainer_user_id","trainerUserId","enrollment_id","enrollmentId","payment_order_id","paymentOrder.id","provider_order_id","providerOrderId","provider_payment_id","providerPaymentId")), page(filters, limit, "createdAt")), limit);
            case "cancellations" -> response(cancellations.findAll(spec(filters, "requestedAt", Map.of("status","status","enrollment_id","enrollmentId","requested_by_user_id","requestedByUserId","trainee_user_id","requestedByUserId")), page(filters, limit, "requestedAt")), limit);
            case "refunds" -> response(refunds.findAll(spec(filters, "requestedAt", Map.of("status","status","payment_id","payment.id","enrollment_id","enrollmentId","trainee_user_id","payment.traineeUserId","trainer_user_id","payment.trainerUserId")), page(filters, limit, "requestedAt")), limit);
            case "commissions" -> response(commissions.findAll(spec(filters, "createdAt", Map.of("enrollment_id","enrollmentId","payment_id","payment.id","commission_rule_id","commissionRule.id","commission_rule_version_id","commissionRuleVersion.id","status","status")), page(filters, limit, "createdAt")), limit);
            case "accounts" -> response(accounts.findAll(spec(filters, "createdAt", Map.of("trainer_user_id","trainerUserId","account_status","accountStatus","provider","provider")), page(filters, limit, "createdAt")), limit);
            case "settlements" -> { Page<TrainingSettlement> rows=settlements.findAll(spec(filters, "createdAt", Map.of("trainer_user_id","trainerUserId","status","status")), page(filters, limit, "createdAt")); auditSettlementTransitions(rows.getContent()); yield response(rows, limit); }
            case "ledger" -> response(ledger.findAll(spec(filters, "createdAt", Map.of("transaction_type","transactionType","enrollment_id","enrollmentId","payment_id","payment.id","commission_id","commission.id","settlement_id","settlement.id")), page(filters, limit, "createdAt")), limit);
            case "webhooks" -> response(webhooks.findAll(spec(filters, "receivedAt", Map.of("event_type","eventType","processing_status","processingStatus","provider_event_id","providerEventId")), page(filters, limit, "receivedAt")), limit);
            default -> throw new IllegalArgumentException("Unknown financial resource: " + resource);
        };
    }

    @Transactional public FinancialRecordResponse get(String resource, Long id) {
        return switch(resource) {
            case "payments" -> record(payments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id)));
            case "cancellations" -> record(cancellations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cancellation", id)));
            case "refunds" -> record(refunds.findById(id).orElseThrow(() -> new ResourceNotFoundException("Refund", id)));
            case "commissions" -> record(commissions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commission", id)));
            case "accounts" -> record(accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Trainer payout account", id)));
            case "settlements" -> settlementDetail(id);
            case "ledger" -> record(ledger.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ledger entry", id)));
            case "webhooks" -> record(webhooks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Webhook event", id)));
            default -> throw new IllegalArgumentException("Unknown financial resource: " + resource);
        };
    }

    @Transactional(readOnly=true) public String operationalActionUnavailable(String resource, Long id, String action) {
        switch(resource) {
            case "settlement" -> settlements.findById(id).orElseThrow(() -> new ResourceNotFoundException("Settlement", id));
            case "refund" -> refunds.findById(id).orElseThrow(() -> new ResourceNotFoundException("Refund", id));
            case "webhook" -> webhooks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Webhook event", id));
            default -> throw new IllegalArgumentException("Unsupported financial operation");
        }
        return "Cannot " + action + " this " + resource + ": the existing provider processing service is not available in this application.";
    }

    private FinancialRecordResponse settlementDetail(Long id) {
        TrainingSettlement settlement = settlements.findById(id).orElseThrow(() -> new ResourceNotFoundException("Settlement", id));
        refreshProviderStatus(settlement);
        auditSettlementTransitions(List.of(settlement));
        Map<String,Object> fields = fields(settlement);
        fields.put("items", settlementItems.findBySettlement_Id(id, PageRequest.of(0, MAX_LIMIT, Sort.by("id"))).stream().map(this::fields).toList());
        return new FinancialRecordResponse(id, fields);
    }
    private void refreshProviderStatus(TrainingSettlement settlement) {
        if (!"PROCESSING".equalsIgnoreCase(settlement.getStatus()) || settlement.getProviderTransferId() == null
                || !routeEnabled || razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) return;
        try {
            String paymentId = db.queryForObject("""
                SELECT p.provider_payment_id FROM training_settlement_items i
                JOIN training_commissions c ON c.id=i.commission_id
                JOIN training_payments p ON p.id=c.payment_id
                WHERE i.settlement_id=? ORDER BY i.id LIMIT 1
                """, String.class, settlement.getId());
            Map<String,Object> transfer = findTransfer(paymentId, "training-settlement-" + settlement.getId());
            if (transfer == null) return;
            String status = String.valueOf(transfer.get("status")).toLowerCase(java.util.Locale.ROOT);
            String stored = switch (status) { case "processed" -> "COMPLETED"; case "failed" -> "FAILED"; case "reversed" -> "REVERSED"; default -> null; };
            if (stored == null) return;
            String reason = "FAILED".equals(stored) ? "Razorpay reported transfer failure" : "REVERSED".equals(stored) ? "Razorpay reversed transfer" : null;
            db.update("UPDATE training_settlements SET status=?,completed_at=CASE WHEN ?='COMPLETED' THEN COALESCE(completed_at,CURRENT_TIMESTAMP) ELSE completed_at END,failure_reason=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING' AND provider_transfer_id=?", stored, stored, reason, settlement.getId(), settlement.getProviderTransferId());
            settlement.setStatus(stored);
            settlement.setFailureReason(reason);
            if ("COMPLETED".equals(stored)) {
                settlement.setCompletedAt(java.time.LocalDateTime.now());
                recordSettlementLedger(settlement.getId(), "TRAINER_SETTLEMENT", "settlement-item-", true);
                db.update("UPDATE training_commissions SET status='SETTLED',updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT commission_id FROM training_settlement_items WHERE settlement_id=?)", settlement.getId());
            } else if ("REVERSED".equals(stored)) {
                recordSettlementLedger(settlement.getId(), "SETTLEMENT_REVERSAL", "settlement-reversal-", false);
            }
        } catch (RuntimeException ignored) {
            // Keep the shared status visible if Razorpay cannot be reached during this details request.
        }
    }
    private void auditSettlementTransitions(List<TrainingSettlement> rows) {
        for (TrainingSettlement row : rows) {
            String status = row.getStatus() == null ? "" : row.getStatus().toUpperCase(java.util.Locale.ROOT);
            String action = switch(status) { case "COMPLETED" -> "SETTLEMENT_PAYMENT_COMPLETED"; case "FAILED" -> "SETTLEMENT_PAYMENT_FAILED"; case "REVERSED" -> "SETTLEMENT_PAYMENT_REVERSED"; default -> null; };
            if (action == null || audit.hasSettlementEvent(row.getId(), action)) continue;
            audit.initiatedByForSettlement(row.getId()).ifPresent(adminId -> audit.recordAs(adminId, action,
                    "SETTLEMENT", "SETTLEMENT", row.getId(), "Trainer payment status confirmed by the provider: " + status + "."));
        }
    }
    private FinancialListResponse response(Page<?> rows, int limit) { return new FinancialListResponse(rows.getContent().stream().map(this::record).toList(), (int)rows.getTotalElements(), limit); }
    private FinancialRecordResponse record(Object value) {
        if(value instanceof TrainingPayment x) return new FinancialRecordResponse(x.getId(), map("paymentOrderId",x.getPaymentOrder()==null?null:x.getPaymentOrder().getId(),"applicationId",null,"enrollmentId",x.getEnrollmentId(),"traineeUserId",x.getTraineeUserId(),"trainerUserId",x.getTrainerUserId(),"amount",x.getAmount(),"currency",x.getCurrency(),"provider",provider(x.getPaymentProvider()),"providerOrderId",x.getProviderOrderId(),"providerPaymentId",x.getProviderPaymentId(),"status",x.getStatus(),"paymentMethod",x.getPaymentMethod(),"capturedAt",x.getCapturedAt(),"createdAt",x.getCreatedAt()));
        if(value instanceof TrainingCancellation x) return new FinancialRecordResponse(x.getId(),map("enrollmentId",x.getEnrollmentId(),"requestedByUserId",x.getRequestedByUserId(),"stage",x.getCancellationStage(),"policyRuleId",x.getPolicyRule()==null?null:x.getPolicyRule().getId(),"refundPercentage",x.getRefundPercentage(),"paymentAmount",x.getPaymentAmount(),"refundAmount",x.getExpectedRefundAmount(),"reason",x.getReason(),"status",x.getStatus(),"createdAt",x.getRequestedAt()));
        if(value instanceof TrainingRefund x) return new FinancialRecordResponse(x.getId(),map("paymentId",x.getPayment()==null?null:x.getPayment().getId(),"cancellationId",x.getCancellation()==null?null:x.getCancellation().getId(),"enrollmentId",x.getEnrollmentId(),"paymentProvider",provider(x.getPaymentProvider()),"refundAmount",x.getRefundAmount(),"currency",x.getCurrency(),"providerRefundId",x.getProviderRefundId(),"status",x.getStatus(),"failureReason",x.getFailureReason(),"createdAt",x.getRequestedAt(),"completedAt",x.getProcessedAt()));
        if(value instanceof TrainingCommission x) return new FinancialRecordResponse(x.getId(),map("enrollmentId",x.getEnrollmentId(),"paymentId",x.getPayment()==null?null:x.getPayment().getId(),"trainerUserId",x.getPayment()==null?null:x.getPayment().getTrainerUserId(),"grossAmount",x.getGrossAmount(),"commissionRuleId",x.getCommissionRule()==null?null:x.getCommissionRule().getId(),"commissionRuleVersionId",x.getCommissionRuleVersion()==null?null:x.getCommissionRuleVersion().getId(),"commissionAmount",x.getCommissionAmount(),"trainerPayableAmount",x.getTrainerAmount(),"currency",x.getCurrency(),"createdAt",x.getCreatedAt()));
        if(value instanceof TrainerPayoutAccount x) return new FinancialRecordResponse(x.getId(),map("trainerUserId",x.getTrainerUserId(),"provider",x.getProvider(),"accountStatus",x.getAccountStatus(),"isPrimary",x.getPrimaryAccount(),"verifiedAt",x.getVerifiedAt(),"createdAt",x.getCreatedAt(),"updatedAt",x.getUpdatedAt()));
        if(value instanceof TrainingSettlement x) {
            boolean eligible=false;
            try { eligible=Boolean.TRUE.equals(db.queryForMap(SETTLEMENT_ELIGIBILITY_SQL,x.getId()).get("eligible")); }
            catch(org.springframework.dao.EmptyResultDataAccessException ignored) { }
            String payoutStatus = db.query("SELECT payout_status FROM trainer_profiles WHERE user_id=?", rs -> rs.next() ? rs.getString(1) : null, x.getTrainerUserId());
            String paymentStatus = switch (x.getStatus().toUpperCase(java.util.Locale.ROOT)) { case "ELIGIBLE", "PENDING" -> "UNPAID"; case "COMPLETED" -> "PAID"; default -> x.getStatus().toUpperCase(java.util.Locale.ROOT); };
            Object accountStatus = x.getPayoutAccount()==null ? "NOT_CONFIGURED" : x.getPayoutAccount().getAccountStatus();
            return new FinancialRecordResponse(x.getId(),map("settlementId",x.getId(),"trainerUserId",x.getTrainerUserId(),"amount",x.getAmount(),"currency",x.getCurrency(),"settlementStatus",x.getStatus(),"status",x.getStatus(),"eligible",eligible,"paymentStatus",paymentStatus,"payoutAccountId",x.getPayoutAccount()==null?null:x.getPayoutAccount().getId(),"paymentAccountStatus",accountStatus,"payoutStatus",payoutStatus,"paymentProvider",provider(x.getPaymentProvider()),"providerTransferId",x.getProviderTransferId(),"eligibleAt",x.getEligibleAt(),"initiatedAt",x.getInitiatedAt(),"completedAt",x.getCompletedAt(),"failureReason",x.getFailureReason(),"createdAt",x.getCreatedAt(),"updatedAt",x.getUpdatedAt()));
        }
        if(value instanceof TrainingSettlementItem x) return new FinancialRecordResponse(x.getId(),map("settlementId",x.getSettlement()==null?null:x.getSettlement().getId(),"enrollmentId",x.getEnrollmentId(),"commissionId",x.getCommission()==null?null:x.getCommission().getId(),"amount",x.getAmount(),"createdAt",x.getCreatedAt()));
        if(value instanceof PaymentWebhookEvent x) return new FinancialRecordResponse(x.getId(),map("paymentProvider",provider(x.getPaymentProvider()),"eventType",x.getEventType(),"eventId",x.getProviderEventId(),"processingStatus",x.getProcessingStatus(),"receivedAt",x.getReceivedAt(),"processedAt",x.getProcessedAt(),"failureReason",x.getFailureReason()));
        if(value instanceof PaymentTransactionsLedger x) return new FinancialRecordResponse(x.getId(),map("enrollmentId",x.getEnrollmentId(),"paymentId",x.getPayment()==null?null:x.getPayment().getId(),"refundId",x.getRefundId(),"commissionId",x.getCommission()==null?null:x.getCommission().getId(),"settlementId",x.getSettlement()==null?null:x.getSettlement().getId(),"transactionType",x.getTransactionType(),"amount",x.getAmount(),"currency",x.getCurrency(),"referenceId",x.getReferenceId(),"description",x.getDescription(),"createdAt",x.getCreatedAt()));
        return new FinancialRecordResponse(null,Map.of());
    }
    private Map<String,Object> fields(Object value) { return record(value).fields(); }
    private Map<String,Object> provider(PaymentProvider provider) { return provider==null?null:map("id",provider.getId(),"code",provider.getCode(),"name",provider.getName()); }
    private Map<String,Object> map(Object... pairs) { Map<String,Object> result=new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]); return result; }
    private <T> Specification<T> spec(Map<String,String> filters, String dateProperty, Map<String,String> allowed) {
        return (root, query, cb) -> { List<Predicate> parts=new ArrayList<>(); allowed.forEach((parameter,property) -> { String v=filters.get(parameter); if(v==null) v=aliasValue(filters,parameter); if(v!=null&&!v.isBlank()) { var path=root.get(property); if(property.contains(".")) { String[] segments=property.split("\\."); path=root.get(segments[0]).get(segments[1]); } Class<?> type=path.getJavaType(); Object typed=v; try { if(type==Long.class||type==long.class) typed=Long.valueOf(v); else if(type==Integer.class||type==int.class) typed=Integer.valueOf(v); else if(type==Boolean.class||type==boolean.class) typed=Boolean.valueOf(v); } catch(NumberFormatException e) { throw new IllegalArgumentException(parameter+" must be a valid value"); } parts.add(cb.equal(path,typed)); } });
            LocalDate from=parseDate(filters.get("date_from"),"date_from"), to=parseDate(filters.get("date_to"),"date_to");
            if(from==null) from=parseDate(filters.get("dateFrom"),"dateFrom"); if(to==null) to=parseDate(filters.get("dateTo"),"dateTo");
            if(from!=null) parts.add(cb.greaterThanOrEqualTo(root.get(dateProperty),from.atStartOfDay())); if(to!=null) parts.add(cb.lessThan(root.get(dateProperty),to.plusDays(1).atStartOfDay())); return cb.and(parts.toArray(Predicate[]::new)); };
    }
    private String aliasValue(Map<String,String> filters,String key) { return switch(key) { case "trainee_user_id" -> filters.get("trainee"); case "trainer_user_id" -> filters.get("trainer"); case "enrollment_id" -> filters.get("enrollment"); case "payment_id" -> filters.get("payment"); case "training_offering_id" -> filters.get("training"); case "commission_id" -> filters.get("commission"); case "settlement_id" -> filters.get("settlement"); case "transaction_type" -> filters.get("type"); default -> null; }; }
    private PageRequest page(Map<String,String> filters,int limit,String date) { return PageRequest.of(0,limit,Sort.by(Sort.Order.desc(date),Sort.Order.desc("id"))); }
    private int parseLimit(String value) { try { return Math.max(1,Math.min(MAX_LIMIT,value==null?100:Integer.parseInt(value))); } catch(NumberFormatException e) { throw new IllegalArgumentException("limit must be an integer"); } }
    private LocalDate parseDate(String value,String name) { if(value==null||value.isBlank())return null; try{return LocalDate.parse(value);}catch(Exception e){throw new IllegalArgumentException(name+" must use YYYY-MM-DD format");} }
}
