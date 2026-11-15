package com.app.admin.repository;

import com.app.admin.model.AdminRole;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {
    Optional<AdminRole> findByCodeAndActiveTrue(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AdminRole r where r.code='SUPER_ADMIN'")
    Optional<AdminRole> findSuperAdminRoleForUpdate();

    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @EntityGraph(attributePaths = "permissions")
    List<AdminRole> findAllByOrderBySystemRoleDescNameAsc();

    @EntityGraph(attributePaths = "permissions")
    Optional<AdminRole> findWithPermissionsById(Long id);
}
