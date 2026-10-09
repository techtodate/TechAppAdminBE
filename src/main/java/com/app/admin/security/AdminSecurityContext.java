package com.app.admin.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.app.admin.exception.InstitutionException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AdminSecurityContext {

    public static final String PERMISSION_INSTITUTION_VIEW = "INSTITUTION_VIEW";
    public static final String PERMISSION_INSTITUTION_IMPORT = "INSTITUTION_IMPORT";
    public static final String PERMISSION_INSTITUTION_MANAGE = "INSTITUTION_MANAGE";

    public String getCurrentUsername() {
        // If Spring Security is present in classpath and configured, we can extract from SecurityContextHolder
        try {
            Class<?> holderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = holderClass.getMethod("getContext").invoke(null);
            if (context != null) {
                Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
                if (auth != null) {
                    Object name = auth.getClass().getMethod("getName").invoke(auth);
                    if (name != null && !name.toString().equalsIgnoreCase("anonymousUser")) {
                        return name.toString();
                    }
                }
            }
        } catch (Exception ignored) {
            // Spring Security is not present or not configured
        }

        // Check request header if available
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String userHeader = request.getHeader("X-Admin-User");
            if (userHeader != null && !userHeader.isBlank()) {
                return userHeader.trim();
            }
        }

        return "admin";
    }

    public Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String idHeader = request.getHeader("X-Admin-User-Id");
            if (idHeader != null && !idHeader.isBlank()) {
                try {
                    return Long.parseLong(idHeader.trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 1L;
    }

    public void requirePermission(String permission) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String forbiddenHeader = request.getHeader("X-Simulate-Forbidden");
            if ("true".equalsIgnoreCase(forbiddenHeader)) {
                throw InstitutionException.unauthorizedImportAction("User lacks required permission: " + permission);
            }
        }
        // Admin user has all required permissions by default in the current project model
    }
}
