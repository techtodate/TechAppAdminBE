package com.app.admin.repository;

import com.app.admin.model.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    boolean existsByEntityTypeAndEntityIdAndActionIgnoreCase(String entityType, String entityId, String action);
    java.util.Optional<AdminAuditLog> findFirstByEntityTypeAndEntityIdAndActionIgnoreCaseOrderByCreatedAtDesc(String entityType, String entityId, String action);
    Page<AdminAuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<AdminAuditLog> findAllByModuleIgnoreCaseOrderByCreatedAtDescIdDesc(String module, Pageable pageable);
    Page<AdminAuditLog> findAllByActionIgnoreCaseOrderByCreatedAtDescIdDesc(String action, Pageable pageable);
    Page<AdminAuditLog> findAllByModuleIgnoreCaseAndActionIgnoreCaseOrderByCreatedAtDescIdDesc(String module, String action, Pageable pageable);
}
