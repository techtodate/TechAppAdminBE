package com.app.admin.repository;

import com.app.admin.model.AdminUser;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AdminUser u where lower(u.username)=lower(:identifier) or lower(u.email)=lower(:identifier)")
    List<AdminUser> findForLogin(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AdminUser u where u.id=:id")
    Optional<AdminUser> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "roles")
    @Query("select distinct u from AdminUser u order by u.id")
    List<AdminUser> findAllWithRoles();

    @EntityGraph(attributePaths = "roles")
    @Query("select distinct u from AdminUser u where u.id=:id")
    Optional<AdminUser> findWithRolesById(@Param("id") Long id);

    @Query("select count(distinct u.id) from AdminUser u join u.roles r where u.active=true and r.active=true and r.code='SUPER_ADMIN'")
    long countActiveSuperAdmins();

    @Query("select count(u) from AdminUser u join u.roles r where u.id=:id and r.code=:code")
    long countByRoleCode(@Param("id") Long id, @Param("code") String code);

    @Query("select count(u) from AdminUser u join u.roles r where r.id=:roleId")
    long countUsersWithRole(@Param("roleId") Long roleId);

    @Query("select distinct r.code from AdminUser u join u.roles r where u.id=:id and r.active=true order by r.code")
    List<String> findActiveRoleCodes(@Param("id") Long id);

    @Query("select distinct p.code from AdminUser u join u.roles r join r.permissions p where u.id=:id and r.active=true and p.active=true order by p.code")
    List<String> findActivePermissionCodes(@Param("id") Long id);

    boolean existsByIdAndActiveTrue(Long id);
}
