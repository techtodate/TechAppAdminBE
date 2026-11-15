package com.app.admin.security;

import com.app.admin.service.AdminIdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Default-deny Admin API authentication and coarse route authorization. */
@Component
public class AdminAuthenticationFilter extends OncePerRequestFilter {
    private final AdminIdentityService identities;
    public AdminAuthenticationFilter(AdminIdentityService identities) { this.identities = identities; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        return !path.startsWith("/api/") || HttpMethod.OPTIONS.matches(request.getMethod()) || isPublic(request.getMethod(), path);
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) { unauthorized(response); return; }
        AdminPrincipal principal = identities.authenticateAccessToken(authorization.substring(7).trim());
        if (principal == null) { unauthorized(response); return; }
        request.setAttribute(AdminSecurityContext.REQUEST_PRINCIPAL, principal);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        principal.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        principal.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
        SecurityContextHolder.setContext(context);
        try {
            String route = path(request);
            if (principal.mustChangePassword() && !route.equals("/api/admin/auth/change-password")
                    && !route.equals("/api/admin/auth/logout") && !route.equals("/api/admin/auth/me")) {
                forbidden(response, "Password change required"); return;
            }
            String required = requiredPermission(request.getMethod(), route);
            if ("UNMAPPED_ADMIN_ROUTE".equals(required)) { forbidden(response, "This route has no Admin permission mapping"); return; }
            if (required != null && !principal.roles().contains("SUPER_ADMIN") && !principal.permissions().contains(required)) {
                forbidden(response, "Missing permission: " + required); return;
            }
            response.setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String path(HttpServletRequest request) { return request.getRequestURI().substring(request.getContextPath().length()); }
    private boolean isPublic(String method, String path) {
        return HttpMethod.POST.matches(method) && (path.equals("/api/admin/auth/login") || path.equals("/api/admin/auth/refresh"));
    }

    private String requiredPermission(String method, String path) {
        String verb = method.toUpperCase(Locale.ROOT);
        if (path.equals("/api/admin/auth/me") || path.equals("/api/admin/auth/logout") || path.equals("/api/admin/auth/change-password")) return null;
        if (path.startsWith("/api/admin/auth/")) return "UNMAPPED_ADMIN_ROUTE";

        if (path.equals("/api/admin/users") || path.matches("/api/admin/users/\\d+") || path.matches("/api/admin/users/\\d+/roles")) {
            if (verb.equals("GET")) return "USER_VIEW";
            if (path.equals("/api/admin/users") && verb.equals("POST")) return "USER_CREATE";
            if (path.matches("/api/admin/users/\\d+") && verb.equals("PUT")) return "USER_UPDATE";
            if (path.matches("/api/admin/users/\\d+/roles") && verb.equals("POST")) return "ROLE_ASSIGN";
        }
        if (path.matches("/api/admin/users/\\d+/enable") && verb.equals("POST")) return "USER_ENABLE";
        if (path.matches("/api/admin/users/\\d+/disable") && verb.equals("POST")) return "USER_DISABLE";
        if (path.matches("/api/admin/users/\\d+/roles/\\d+") && verb.equals("DELETE")) return "ROLE_ASSIGN";

        if (path.equals("/api/admin/roles") || path.matches("/api/admin/roles/\\d+")) {
            if (verb.equals("GET")) return "ROLE_VIEW";
            if (path.equals("/api/admin/roles") && verb.equals("POST")) return "ROLE_CREATE";
            if (path.matches("/api/admin/roles/\\d+") && verb.equals("PUT")) return "ROLE_UPDATE";
            if (path.matches("/api/admin/roles/\\d+") && verb.equals("DELETE")) return "ROLE_DELETE";
        }
        if (path.matches("/api/admin/roles/\\d+/permissions")) {
            if (verb.equals("GET")) return "PERMISSION_VIEW";
            if (verb.equals("PUT")) return "PERMISSION_ASSIGN";
        }
        if (path.equals("/api/admin/permissions") && verb.equals("GET")) return "PERMISSION_VIEW";
        if (path.equals("/api/admin/audit-logs") && verb.equals("GET")) return "AUDIT_LOG_VIEW";

        if (path.equals("/api/admin/verifications") || path.startsWith("/api/admin/verifications/")) {
            if (verb.equals("GET") && (path.equals("/api/admin/verifications")
                    || path.matches("/api/admin/verifications/(menu|counts)"))) return "VERIFICATION_VIEW";
            if (verb.equals("GET") && (path.matches("/api/admin/verifications/\\d+")
                    || path.matches("/api/admin/verifications/\\d+/approval-readiness")
                    || path.matches("/api/admin/verifications/\\d+/history")
                    || path.matches("/api/admin/verifications/\\d+/evidence/\\d+/download-url"))) return "VERIFICATION_VIEW";
            if (path.matches("/api/admin/verifications/\\d+/assign") && verb.equals("POST")) return "VERIFICATION_ASSIGN";
            if (path.matches("/api/admin/verifications/\\d+/start-review") && verb.equals("POST")) return "VERIFICATION_REVIEW";
            if (path.matches("/api/admin/verifications/\\d+/sections/[^/]+") && verb.equals("PUT")) return "VERIFICATION_REVIEW";
            if (path.matches("/api/admin/verifications/\\d+/identity/review") && verb.equals("PUT")) return "VERIFICATION_REVIEW";
            if (path.matches("/api/admin/verifications/\\d+/evidence/\\d+/approve") && verb.equals("POST")) return "VERIFICATION_APPROVE";
            if (path.matches("/api/admin/verifications/\\d+/evidence/\\d+/reject") && verb.equals("POST")) return "VERIFICATION_REJECT";
            if ((path.matches("/api/admin/verifications/\\d+/evidence/\\d+/request-changes") || path.matches("/api/admin/verifications/\\d+/request-changes")) && verb.equals("POST")) return "VERIFICATION_REQUEST_CHANGES";
            if (path.matches("/api/admin/verifications/\\d+/approve") && verb.equals("POST")) return "VERIFICATION_APPROVE";
            if (path.matches("/api/admin/verifications/\\d+/reject") && verb.equals("POST")) return "VERIFICATION_REJECT";
            return "UNMAPPED_ADMIN_ROUTE";
        }
        // Legacy Admin modules remain protected with explicit view/manage permissions.
        if (path.equals("/api/admin/events") || path.startsWith("/api/admin/events/")) return verb.equals("GET") ? "EVENT_REVIEW_VIEW" : "EVENT_REVIEW_MANAGE";
        if (path.equals("/api/admin/institution-countries")) return "INSTITUTION_VIEW";
        if (path.startsWith("/api/admin/institution-imports") || path.startsWith("/api/admin/institution-import-history")) return verb.equals("GET") ? "INSTITUTION_VIEW" : "INSTITUTION_IMPORT";
        if (path.startsWith("/api/admin/institutions") || path.startsWith("/api/admin/institution-sources") || path.startsWith("/api/admin/institution-standard-fields") || path.startsWith("/api/admin/institution-source-mappings")) return verb.equals("GET") ? "INSTITUTION_VIEW" : "INSTITUTION_MANAGE";
        if (path.startsWith("/api/admin/technology-updates")) return "TECH_UPDATE_MANAGE";
        if (path.startsWith("/api/admin/training/") || path.startsWith("/api/admin/payment-webhooks") || path.startsWith("/api/admin/trainer-payment-accounts")) {
            if (verb.equals("GET")) return "TRAINING_FINANCIAL_VIEW";
            if (path.matches("/api/admin/training/settlements/\\d+/refresh-payment-status")) return "TRAINING_SETTLEMENT_RECONCILE";
            if (path.contains("/settlements/") && (path.endsWith("/process") || path.endsWith("/retry") || path.endsWith("/pay"))) return "TRAINING_SETTLEMENT_PROCESS";
            return "TRAINING_FINANCIAL_MANAGE";
        }
        if (path.startsWith("/api/admin/razorpay-route/")) {
            if (verb.equals("GET")) return "TRAINING_FINANCIAL_VIEW";
            if (path.endsWith("/test-linked-accounts")) return "TRAINING_FINANCIAL_MANAGE";
            if (path.endsWith("/test-transfer")) return "TRAINING_SETTLEMENT_PROCESS";
            return "TRAINING_FINANCIAL_VIEW";
        }
        if (path.startsWith("/api/admin/payments/")) return "TRAINING_FINANCIAL_MANAGE";
        if (path.startsWith("/api/admin/commission-rules") || path.startsWith("/api/admin/refund-policies") || path.startsWith("/api/admin/cancellation-policies") || path.startsWith("/api/admin/payment-providers")) return verb.equals("GET") ? "TRAINING_FINANCIAL_VIEW" : "TRAINING_FINANCIAL_MANAGE";
        if (path.startsWith("/api/admin/")) return "UNMAPPED_ADMIN_ROUTE";
        if (path.startsWith("/api/dashboard")) return "DASHBOARD_VIEW";
        return verb.equals("GET") ? "CATALOG_VIEW" : "CATALOG_MANAGE";
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Valid Admin authentication is required\"}");
    }
    private void forbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); response.setContentType("application/json");
        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }
}
