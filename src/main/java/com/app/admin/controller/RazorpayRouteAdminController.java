package com.app.admin.controller;

import com.app.admin.security.AdminSecurityContext;
import com.app.admin.service.RazorpayRouteAdminService;
import com.app.admin.dto.RazorpayRouteLinkedAccountTestRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/razorpay-route")
public class RazorpayRouteAdminController {
    private final RazorpayRouteAdminService service;
    private final AdminSecurityContext security;

    public RazorpayRouteAdminController(RazorpayRouteAdminService service, AdminSecurityContext security) {
        this.service = service;
        this.security = security;
    }

    @GetMapping("/health")
    public Object health() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.health();
    }

    @PostMapping("/check-connection")
    public Object checkConnection() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.checkConnection();
    }

    @PostMapping("/check-access")
    public Object checkAccess() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.checkAccess();
    }

    @PostMapping("/check-linked-account")
    public Object checkLinkedAccount(@RequestBody Map<String, String> request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.checkLinkedAccount(request == null ? null : request.get("accountId"));
    }

    @PostMapping("/test-linked-accounts")
    public Object createTestLinkedAccount(@Valid @RequestBody RazorpayRouteLinkedAccountTestRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        return service.createTestLinkedAccount(request);
    }

    @GetMapping("/linked-accounts")
    public Object linkedAccounts() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.linkedAccounts();
    }

    @PostMapping("/test-transfer")
    public Object createTestTransfer(@RequestBody Map<String, String> request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_PROCESS);
        return service.createTestTransfer(request);
    }

    @PostMapping("/settlements/{id}/readiness")
    public Object readiness(@PathVariable Long id) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.readiness(id);
    }

    @PostMapping("/verify-payment")
    public Object verifyPayment(@RequestBody Map<String, String> request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return service.verifyPayment(request);
    }
}
