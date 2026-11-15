package com.app.admin.repository;

import com.app.admin.model.AdminPermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    List<AdminPermission> findAllByActiveTrueOrderByModuleAscNameAsc();
    List<AdminPermission> findAllByActiveTrueAndModuleIgnoreCaseOrderByNameAsc(String module);
    List<AdminPermission> findAllByIdInAndActiveTrue(Iterable<Long> ids);
}
