package com.app.admin.security;

import com.app.admin.exception.InstitutionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AdminSecurityContext {
    public static final String REQUEST_PRINCIPAL = AdminPrincipal.class.getName();

    public static final String PERMISSION_ADMIN_APP_ACCESS = "ADMIN_APP_ACCESS";
    public static final String PERMISSION_ADMIN_USER_VIEW = "USER_VIEW";
    public static final String PERMISSION_ADMIN_USER_MANAGE = "USER_UPDATE";
    public static final String PERMISSION_ADMIN_ROLE_VIEW = "ROLE_VIEW";
    public static final String PERMISSION_ADMIN_ROLE_MANAGE = "ROLE_UPDATE";
    public static final String PERMISSION_DASHBOARD_VIEW = "DASHBOARD_VIEW";
    public static final String PERMISSION_CATALOG_VIEW = "CATALOG_VIEW";
    public static final String PERMISSION_CATALOG_MANAGE = "CATALOG_MANAGE";
    public static final String PERMISSION_EVENT_REVIEW_VIEW = "EVENT_REVIEW_VIEW";
    public static final String PERMISSION_EVENT_REVIEW_MANAGE = "EVENT_REVIEW_MANAGE";
    public static final String PERMISSION_INSTITUTION_VIEW = "INSTITUTION_VIEW";
    public static final String PERMISSION_INSTITUTION_IMPORT = "INSTITUTION_IMPORT";
    public static final String PERMISSION_INSTITUTION_MANAGE = "INSTITUTION_MANAGE";
    public static final String PERMISSION_TECH_UPDATE_MANAGE = "TECH_UPDATE_MANAGE";
    public static final String PERMISSION_FINANCIAL_VIEW = "TRAINING_FINANCIAL_VIEW";
    public static final String PERMISSION_FINANCIAL_MANAGE = "TRAINING_FINANCIAL_MANAGE";
    public static final String PERMISSION_SETTLEMENT_PROCESS = "TRAINING_SETTLEMENT_PROCESS";
    public static final String PERMISSION_SETTLEMENT_RECONCILE = "TRAINING_SETTLEMENT_RECONCILE";
    public static final String PERMISSION_VERIFICATION_VIEW = "VERIFICATION_VIEW";
    public static final String PERMISSION_VERIFICATION_MANAGE = "VERIFICATION_REVIEW";

    public AdminPrincipal currentPrincipal() {
        ServletRequestAttributes attrs=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if(attrs==null) return null;
        Object principal=attrs.getRequest().getAttribute(REQUEST_PRINCIPAL);
        return principal instanceof AdminPrincipal p?p:null;
    }

    public String getCurrentUsername() {
        AdminPrincipal principal=currentPrincipal();
        return principal==null?"anonymous":principal.email();
    }

    public String getAuthenticatedUsername() {
        AdminPrincipal principal=currentPrincipal();
        return principal==null?null:principal.email();
    }

    public Long getCurrentUserId() {
        AdminPrincipal principal=currentPrincipal();
        return principal==null?null:principal.id();
    }

    public Long getAuthenticatedUserId() { return getCurrentUserId(); }

    public boolean isSuperAdmin() {
        AdminPrincipal principal=currentPrincipal();
        return principal!=null&&principal.roles().contains("SUPER_ADMIN");
    }

    public boolean hasPermission(String permission) {
        AdminPrincipal principal=currentPrincipal();
        return principal!=null&&(principal.roles().contains("SUPER_ADMIN")||principal.permissions().contains(permission));
    }

    public void requirePermission(String permission) {
        AdminPrincipal principal=currentPrincipal();
        if(principal==null) throw new InstitutionException(HttpStatus.UNAUTHORIZED,"ADMIN_AUTHENTICATION_REQUIRED","Admin authentication is required");
        if(!principal.roles().contains("SUPER_ADMIN")&&!principal.permissions().contains(permission))
            throw InstitutionException.unauthorizedImportAction("User lacks required permission: "+permission);
    }

    public HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        return attrs==null?null:attrs.getRequest();
    }
}
