package com.app.admin.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import com.app.admin.service.TrainingFinancialAdminService;
import com.app.admin.service.AdminAuditService;
import com.app.admin.security.AdminSecurityContext;

@RestController
@RequestMapping("/api/admin")
public class TrainingFinancialAdminController {
    private final TrainingFinancialAdminService service;
    private final AdminAuditService audit;
    private final AdminSecurityContext security;
    public TrainingFinancialAdminController(TrainingFinancialAdminService service, AdminAuditService audit, AdminSecurityContext security) { this.service = service; this.audit = audit; this.security = security; }

    @GetMapping("/training/payments") public Object payments(@RequestParam Map<String,String> filters) { return service.list("payments", filters); }
    @GetMapping("/training/payments/{id}") public Object payment(@PathVariable Long id) { return service.get("payments", id); }
    @GetMapping("/training/cancellations") public Object cancellations(@RequestParam Map<String,String> filters) { return service.list("cancellations", filters); }
    @GetMapping("/training/cancellations/{id}") public Object cancellation(@PathVariable Long id) { return service.get("cancellations", id); }
    @GetMapping("/training/refunds") public Object refunds(@RequestParam Map<String,String> filters) { return service.list("refunds", filters); }
    @GetMapping("/training/refunds/{id}") public Object refund(@PathVariable Long id) { return service.get("refunds", id); }
    @GetMapping("/training/commissions") public Object commissions(@RequestParam Map<String,String> filters) { return service.list("commissions", filters); }
    @GetMapping("/training/commissions/{id}") public Object commission(@PathVariable Long id) { return service.get("commissions", id); }
    @GetMapping("/training/settlements") public Object settlements(@RequestParam Map<String,String> filters) { security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW); return service.list("settlements", filters); }
    @GetMapping("/training/settlements/{id}") public Object settlement(@PathVariable Long id) { security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW); return service.get("settlements", id); }
    @GetMapping("/training/financial-ledger") public Object ledger(@RequestParam Map<String,String> filters) { return service.list("ledger", filters); }
    @GetMapping("/training/financial-ledger/{id}") public Object ledgerEntry(@PathVariable Long id) { return service.get("ledger", id); }
    @GetMapping("/payment-webhooks") public Object webhooks(@RequestParam Map<String,String> filters) { return service.list("webhooks", filters); }
    @GetMapping("/payment-webhooks/{id}") public Object webhook(@PathVariable Long id) { return service.get("webhooks", id); }
    @GetMapping("/trainer-payment-accounts") public Object accounts(@RequestParam Map<String,String> filters) { return service.list("accounts", filters); }
    @GetMapping("/trainer-payment-accounts/{id}") public Object account(@PathVariable Long id) { return service.get("accounts", id); }
    @PostMapping({"/training/settlements/{id}/pay", "/training/settlements/{id}/process"}) public Object processSettlement(@PathVariable Long id) {
        security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_PROCESS);
        Object result;
        try {
            result = service.requestSettlementPayment(id);
        } catch (RuntimeException e) {
            if (!audit.hasSettlementEvent(id, "TRAINER_PAYOUT_FAILED")) audit.record("TRAINER_PAYOUT_FAILED", "SETTLEMENT", "SETTLEMENT", id, "Trainer payout request was rejected before provider confirmation.");
            throw e;
        }
        String status = String.valueOf(service.settlementStatus(result));
        if ("PROCESSING".equalsIgnoreCase(status) && !audit.hasSettlementEvent(id, "TRAINER_PAYOUT_INITIATED")) audit.record("TRAINER_PAYOUT_INITIATED", "SETTLEMENT", "SETTLEMENT", id, "Trainer payout initiated directly from the Admin application through Razorpay.");
        else if ("COMPLETED".equalsIgnoreCase(status)) audit.record("TRAINER_PAYOUT_SUCCESS", "SETTLEMENT", "SETTLEMENT", id, "Trainer payout completed.");
        else if ("FAILED".equalsIgnoreCase(status) && !audit.hasSettlementEvent(id, "TRAINER_PAYOUT_FAILED")) audit.record("TRAINER_PAYOUT_FAILED", "SETTLEMENT", "SETTLEMENT", id, "Trainer payout failed according to the provider.");
        return result;
    }
    @PostMapping("/training/settlements/{id}/retry") public Object retrySettlement(@PathVariable Long id) { security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_PROCESS); return service.retrySettlementPayment(id); }
    @PostMapping("/training/settlements/{id}/refresh-payment-status") public Object refreshSettlement(@PathVariable Long id) { security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_RECONCILE); return service.refreshSettlementPayment(id); }
    @PostMapping("/training/refunds/{id}/retry") public ResponseEntity<Object> retryRefund(@PathVariable Long id) { return ResponseEntity.status(501).body(Map.of("message",service.operationalActionUnavailable("refund",id,"retry"))); }
    @PostMapping("/payment-webhooks/{id}/retry") public ResponseEntity<Object> retryWebhook(@PathVariable Long id) { return ResponseEntity.status(501).body(Map.of("message",service.operationalActionUnavailable("webhook",id,"retry"))); }

}
