package com.app.admin.service;

import com.app.admin.security.AdminSecurityContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrainingEnrollmentFinancialService {
    private final JdbcTemplate db;
    private final AdminSecurityContext security;
    private final AdminAuditService audit;

    public TrainingEnrollmentFinancialService(JdbcTemplate db, AdminSecurityContext security, AdminAuditService audit) {
        this.db = db;
        this.security = security;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchEnrollments(String search, int requestedLimit) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        String term = search == null ? "" : search.trim();
        return db.query("""
                SELECT DISTINCT ON (o.enrollment_id)
                       o.enrollment_id AS "enrollmentId",o.trainee_user_id AS "traineeUserId",
                       o.id AS "paymentOrderId",o.amount,o.currency,o.status AS "paymentOrderStatus",
                       p.status AS "paymentStatus",o.created_at AS "createdAt"
                  FROM training_payment_orders o
                  LEFT JOIN LATERAL (
                       SELECT status FROM training_payments WHERE payment_order_id=o.id ORDER BY id DESC LIMIT 1
                  ) p ON TRUE
                 WHERE (?='' OR CAST(o.enrollment_id AS TEXT) LIKE '%'||?||'%'
                                OR CAST(o.trainee_user_id AS TEXT) LIKE '%'||?||'%')
                 ORDER BY o.enrollment_id,o.id DESC
                 LIMIT ?
                """, (row, index) -> {
            Map<String, Object> enrollment = new LinkedHashMap<>();
            enrollment.put("enrollmentId", row.getInt("enrollmentId"));
            enrollment.put("traineeUserId", row.getLong("traineeUserId"));
            enrollment.put("paymentOrderId", row.getLong("paymentOrderId"));
            enrollment.put("amount", row.getBigDecimal("amount"));
            enrollment.put("currency", row.getString("currency"));
            enrollment.put("paymentOrderStatus", row.getString("paymentOrderStatus"));
            enrollment.put("paymentStatus", row.getString("paymentStatus"));
            enrollment.put("createdAt", row.getTimestamp("createdAt") == null ? null : row.getTimestamp("createdAt").toLocalDateTime());
            return enrollment;
        }, term, term, term, limit);
    }
    @Transactional(readOnly = true)
    public Map<String, Object> paymentStatus(Integer enrollmentId) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        requireEnrollment(enrollmentId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enrollmentId", enrollmentId);
        List<Map<String, Object>> orders = db.queryForList("""
                SELECT id,amount,currency,status,provider_order_id,created_at,expires_at
                  FROM training_payment_orders WHERE enrollment_id=? ORDER BY id DESC LIMIT 1
                """, enrollmentId);
        List<Map<String, Object>> payments = db.queryForList("""
                SELECT id,payment_order_id,amount,currency,status,provider_payment_id,provider_order_id,
                       payment_method,captured_at,created_at
                  FROM training_payments WHERE enrollment_id=? ORDER BY COALESCE(captured_at,created_at) DESC,id DESC
                """, enrollmentId);
        Map<String, Object> order = orders.isEmpty() ? null : orders.get(0);
        Map<String, Object> payment = payments.isEmpty() ? null : payments.get(0);
        Object currentStatus = payment != null ? payment.get("status") : order != null ? order.get("status") : "UNPAID";
        BigDecimal amount = payment != null ? decimal(payment.get("amount")) : order != null ? decimal(order.get("amount")) : BigDecimal.ZERO;
        String currency = payment != null ? String.valueOf(payment.get("currency")) : order != null ? String.valueOf(order.get("currency")) : "INR";
        BigDecimal amountDue = order != null && List.of("CREATED", "ATTEMPTED").contains(String.valueOf(order.get("status")))
                ? decimal(order.get("amount")) : BigDecimal.ZERO;
        response.put("status", currentStatus);
        response.put("amount", amount);
        response.put("amountDue", amountDue);
        response.put("currency", currency);
        response.put("paymentOrderId", order == null ? null : order.get("id"));
        response.put("providerOrderId", order == null ? null : order.get("provider_order_id"));
        response.put("payment", payment);
        response.put("paymentAttempts", payments);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> cancellation(Integer enrollmentId) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        requireEnrollment(enrollmentId);
        List<Map<String, Object>> rows = db.queryForList("""
                SELECT c.id,c.enrollment_id,c.requested_by_user_id,c.cancellation_stage,c.policy_rule_id,
                       r.code AS policy_code,r.name AS policy_name,c.refund_percentage,c.payment_amount,
                       c.expected_refund_amount,c.reason,c.status,c.requested_at,c.approved_at,c.rejected_at
                  FROM training_cancellations c LEFT JOIN refund_policy_rules r ON r.id=c.policy_rule_id
                 WHERE c.enrollment_id=? ORDER BY c.id DESC
                """, enrollmentId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enrollmentId", enrollmentId);
        response.put("cancellations", rows);
        response.put("latestCancellation", rows.isEmpty() ? null : rows.get(0));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> refunds(Integer enrollmentId) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        requireEnrollment(enrollmentId);
        return db.queryForList("""
                SELECT id,payment_id AS "paymentId",cancellation_id AS "cancellationId",enrollment_id AS "enrollmentId",
                       refund_amount AS amount,currency,provider_refund_id AS "providerRefundId",status,
                       failure_reason AS "failureReason",requested_at AS "requestedAt",processed_at AS "processedAt"
                  FROM training_refunds WHERE enrollment_id=? ORDER BY id DESC
                """, enrollmentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> cancellationPreview(Integer enrollmentId) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        requireEnrollment(enrollmentId);
        return cancellationPreviewData(enrollmentId);
    }

    private Map<String, Object> cancellationPreviewData(Integer enrollmentId) {
        long offeringId = offeringId(enrollmentId);
        Map<String, Object> sessionCounts = db.queryForMap("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE UPPER(status)='COMPLETED' OR actual_end_at IS NOT NULL) AS completed,
                       COUNT(*) FILTER (WHERE UPPER(status) NOT IN ('SCHEDULED','UPCOMING','PENDING','NOT_STARTED','CREATED','CANCELLED')) AS started
                  FROM training_offering_sessions WHERE training_offering_id=?
                """, offeringId);
        long total = ((Number) sessionCounts.get("total")).longValue();
        long completed = ((Number) sessionCounts.get("completed")).longValue();
        long started = ((Number) sessionCounts.get("started")).longValue();
        String stage = total > 0 && completed == total ? "AFTER_COMPLETION"
                : completed > 0 ? "BETWEEN_SESSIONS" : "BEFORE_START";
        List<Map<String, Object>> policy = db.queryForList("""
                SELECT r.id,r.code,r.name,r.refund_percentage
                  FROM training_cancellation_policies p
                  JOIN refund_policy_rules r ON r.id=CASE ?
                       WHEN 'BEFORE_START' THEN p.before_training_start_policy_id
                       WHEN 'BETWEEN_SESSIONS' THEN p.between_sessions_policy_id
                       ELSE p.after_training_completion_policy_id END
                 WHERE p.training_offering_id=? AND p.active=TRUE AND r.active=TRUE
                """, stage, offeringId);
        if (policy.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "No active cancellation policy is configured for this training.");
        Map<String, Object> rule = policy.get(0);
        BigDecimal percentage = decimal(rule.get("refund_percentage"));
        BigDecimal paymentAmount = latestPaymentAmount(enrollmentId);
        BigDecimal refundAmount = paymentAmount.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enrollmentId", enrollmentId);
        response.put("cancellationStage", stage);
        response.put("policyRuleId", rule.get("id"));
        response.put("policyCode", rule.get("code"));
        response.put("policyName", rule.get("name"));
        response.put("refundPercentage", percentage);
        response.put("paymentAmount", paymentAmount);
        response.put("expectedRefundAmount", refundAmount);
        response.put("currency", "INR");
        return response;
    }

    @Transactional
    public Map<String, Object> requestCancellation(Integer enrollmentId, String reason) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        requireEnrollment(enrollmentId);
        db.queryForObject("SELECT id FROM training_enrollments WHERE id=? FOR UPDATE", Integer.class, enrollmentId);
        Integer pending = db.queryForObject("""
                SELECT COUNT(*) FROM training_cancellations WHERE enrollment_id=? AND status IN ('REQUESTED','APPROVED')
                """, Integer.class, enrollmentId);
        if (pending != null && pending > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "An active cancellation request already exists.");
        Map<String, Object> preview = cancellationPreviewData(enrollmentId);
        Long id = db.queryForObject("""
                INSERT INTO training_cancellations(enrollment_id,requested_by_user_id,cancellation_stage,policy_rule_id,
                    refund_percentage,payment_amount,expected_refund_amount,reason,status)
                VALUES (?,?,?,?,?,?,?,?, 'REQUESTED') RETURNING id
                """, Long.class, enrollmentId, security.getCurrentUserId(), preview.get("cancellationStage"),
                preview.get("policyRuleId"), preview.get("refundPercentage"), preview.get("paymentAmount"),
                preview.get("expectedRefundAmount"), reason == null || reason.isBlank() ? null : reason.trim());
        audit.record("ENROLLMENT_CANCELLATION_REQUESTED", "TRAINING_FINANCE", "ENROLLMENT_CANCELLATION", id,
                "Cancellation request recorded for enrollment " + enrollmentId + ".");
        return db.queryForMap("SELECT id,enrollment_id,cancellation_stage,refund_percentage,payment_amount,expected_refund_amount,reason,status,requested_at FROM training_cancellations WHERE id=?", id);
    }

    private void requireEnrollment(Integer enrollmentId) {
        if (enrollmentId == null || enrollmentId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment ID must be a positive integer.");
        Integer exists = db.queryForObject("SELECT COUNT(*) FROM training_enrollments WHERE id=?", Integer.class, enrollmentId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment was not found.");
    }

    private long offeringId(Integer enrollmentId) {
        Long id = db.queryForObject("SELECT training_offering_id FROM training_enrollments WHERE id=?", Long.class, enrollmentId);
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Training offering was not found for this enrollment.");
        return id;
    }

    private BigDecimal latestPaymentAmount(Integer enrollmentId) {
        List<BigDecimal> captured = db.query("""
                SELECT amount FROM training_payments WHERE enrollment_id=? AND status IN ('CAPTURED','PARTIALLY_REFUNDED')
                 ORDER BY COALESCE(captured_at,created_at) DESC,id DESC LIMIT 1
                """, (row, index) -> row.getBigDecimal("amount"), enrollmentId);
        if (!captured.isEmpty()) return captured.get(0);
        List<BigDecimal> orders = db.query("""
                SELECT amount FROM training_payment_orders WHERE enrollment_id=? AND status IN ('CREATED','ATTEMPTED') ORDER BY id DESC LIMIT 1
                """, (row, index) -> row.getBigDecimal("amount"), enrollmentId);
        if (!orders.isEmpty()) return orders.get(0);
        return BigDecimal.ZERO.setScale(2);
    }

    private BigDecimal decimal(Object value) { return value instanceof BigDecimal amount ? amount : value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); }
}
