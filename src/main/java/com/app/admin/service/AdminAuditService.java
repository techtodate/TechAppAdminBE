package com.app.admin.service;

import com.app.admin.model.AdminAuditLog;
import com.app.admin.model.AdminUser;
import com.app.admin.repository.AdminAuditLogRepository;
import com.app.admin.repository.AdminUserRepository;
import com.app.admin.security.AdminSecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AdminAuditService {
    private final AdminAuditLogRepository auditLogs;
    private final AdminUserRepository users;
    private final AdminSecurityContext security;

    public AdminAuditService(AdminAuditLogRepository auditLogs, AdminUserRepository users, AdminSecurityContext security) {
        this.auditLogs = auditLogs;
        this.users = users;
        this.security = security;
    }

    @Transactional
    public void record(String action, String module, String entityType, Object entityId, String description) {
        save(security.getAuthenticatedUserId(), action, module, entityType, entityId, description);
    }

    @Transactional
    public void recordLogin(String action, String identifier, String description) {
        save(null, action, "AUTH", "ADMIN_USER", identifier == null ? null : identifier.substring(0, Math.min(identifier.length(), 100)), description);
    }

    @Transactional
    public void recordLoginForUser(Long userId, String action, String module, String entityType, Object entityId, String description) {
        save(userId, action, module, entityType, entityId, description);
    }

    @Transactional
    public void recordAs(Long userId, String action, String module, String entityType, Object entityId, String description) {
        save(userId, action, module, entityType, entityId, description);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Long> initiatedByForSettlement(Long settlementId) {
        return auditLogs.findFirstByEntityTypeAndEntityIdAndActionIgnoreCaseOrderByCreatedAtDesc(
                "SETTLEMENT", String.valueOf(settlementId), "SETTLEMENT_PAYMENT_INITIATED")
                .map(entry -> entry.getAdminUser() == null ? null : entry.getAdminUser().getId());
    }

    @Transactional(readOnly = true)
    public boolean hasSettlementEvent(Long settlementId, String action) {
        return auditLogs.existsByEntityTypeAndEntityIdAndActionIgnoreCase("SETTLEMENT", String.valueOf(settlementId), action);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> search(String module, String action, int page, int size) {
        security.requirePermission("AUDIT_LOG_VIEW");
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        String normalizedModule = normalize(module);
        String normalizedAction = normalize(action);
        Page<AdminAuditLog> result;
        if (normalizedModule != null && normalizedAction != null) {
            result = auditLogs.findAllByModuleIgnoreCaseAndActionIgnoreCaseOrderByCreatedAtDescIdDesc(normalizedModule, normalizedAction, pageable);
        } else if (normalizedModule != null) {
            result = auditLogs.findAllByModuleIgnoreCaseOrderByCreatedAtDescIdDesc(normalizedModule, pageable);
        } else if (normalizedAction != null) {
            result = auditLogs.findAllByActionIgnoreCaseOrderByCreatedAtDescIdDesc(normalizedAction, pageable);
        } else {
            result = auditLogs.findAllByOrderByCreatedAtDescIdDesc(pageable);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        result.getContent().forEach(entry -> rows.add(toMap(entry)));
        return rows;
    }

    private void save(Long userId, String action, String module, String entityType, Object entityId, String description) {
        HttpServletRequest request = currentRequest();
        String userAgent = request == null ? null : request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 2000) userAgent = userAgent.substring(0, 2000);
        AdminAuditLog entry = new AdminAuditLog();
        if (userId != null) entry.setAdminUser(users.getReferenceById(userId));
        entry.setAction(action);
        entry.setModule(module);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId == null ? null : String.valueOf(entityId));
        entry.setDescription(description);
        entry.setIpAddress(request == null ? null : request.getRemoteAddr());
        entry.setUserAgent(userAgent);
        auditLogs.save(entry);
    }

    private Map<String, Object> toMap(AdminAuditLog entry) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entry.getId());
        row.put("admin_user_id", entry.getAdminUser() == null ? null : entry.getAdminUser().getId());
        row.put("action", entry.getAction());
        row.put("module", entry.getModule());
        row.put("entity_type", entry.getEntityType());
        row.put("entity_id", entry.getEntityId());
        row.put("description", entry.getDescription());
        row.put("ip_address", entry.getIpAddress());
        row.put("user_agent", entry.getUserAgent());
        row.put("created_at", entry.getCreatedAt());
        return row;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }
}
