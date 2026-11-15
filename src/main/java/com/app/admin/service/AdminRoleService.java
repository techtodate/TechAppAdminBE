package com.app.admin.service;

import com.app.admin.dto.AdminRoleRequest;
import com.app.admin.model.AdminPermission;
import com.app.admin.model.AdminRole;
import com.app.admin.repository.AdminPermissionRepository;
import com.app.admin.repository.AdminRoleRepository;
import com.app.admin.repository.AdminUserRepository;
import com.app.admin.security.AdminSecurityContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminRoleService {
    private final AdminRoleRepository roles;
    private final AdminPermissionRepository permissions;
    private final AdminUserRepository users;
    private final AdminSecurityContext security;
    private final AdminAuditService audit;

    public AdminRoleService(AdminRoleRepository roles, AdminPermissionRepository permissions,
            AdminUserRepository users, AdminSecurityContext security, AdminAuditService audit) {
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.security = security;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> roles() {
        security.requirePermission("ROLE_VIEW");
        return roles.findAllByOrderBySystemRoleDescNameAsc().stream().map(this::roleMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> role(long id) {
        security.requirePermission("ROLE_VIEW");
        return roleMap(findRole(id));
    }

    @Transactional
    public Map<String, Object> create(AdminRoleRequest request) {
        security.requirePermission("ROLE_CREATE");
        String name = request.name().trim();
        ensureUniqueName(name, null);
        String code = codeFor(name);
        AdminRole role = new AdminRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(request.description());
        role.setSystemRole(false);
        role.setActive(true);
        role = roles.save(role);
        audit.record("ROLE_CREATED", "ROLE", "ADMIN_ROLE", role.getId(), "Admin role created: " + code);
        return roleMap(role);
    }

    @Transactional
    public Map<String, Object> update(long id, AdminRoleRequest request) {
        security.requirePermission("ROLE_UPDATE");
        AdminRole role = findRole(id);
        if (role.isSystemRole()) throw new ResponseStatusException(HttpStatus.CONFLICT, "System role definitions cannot be edited");
        ensureUniqueName(request.name().trim(), id);
        role.setName(request.name().trim());
        role.setDescription(request.description());
        role = roles.save(role);
        audit.record("ROLE_UPDATED", "ROLE", "ADMIN_ROLE", id, "Admin role updated: " + role.getCode());
        return roleMap(role);
    }

    @Transactional
    public void delete(long id) {
        security.requirePermission("ROLE_DELETE");
        AdminRole role = findRole(id);
        if (role.isSystemRole()) throw new ResponseStatusException(HttpStatus.CONFLICT, "System roles cannot be deleted");
        if (users.countUsersWithRole(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remove this role from Admin users before deleting it");
        }
        String code = role.getCode();
        roles.delete(role);
        audit.record("ROLE_DELETED", "ROLE", "ADMIN_ROLE", id, "Admin role deleted: " + code);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> permissions(String module) {
        security.requirePermission("PERMISSION_VIEW");
        List<AdminPermission> result = module == null || module.isBlank()
                ? permissions.findAllByActiveTrueOrderByModuleAscNameAsc()
                : permissions.findAllByActiveTrueAndModuleIgnoreCaseOrderByNameAsc(module.trim().toUpperCase(Locale.ROOT));
        return result.stream().map(this::permissionMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rolePermissions(long roleId) {
        security.requirePermission("PERMISSION_VIEW");
        return findRoleWithPermissions(roleId).getPermissions().stream().map(this::permissionMap).toList();
    }

    @Transactional
    public Map<String, Object> setRolePermissions(long roleId, List<Long> permissionIds) {
        security.requirePermission("PERMISSION_ASSIGN");
        AdminRole role = findRoleWithPermissions(roleId);
        if ("SUPER_ADMIN".equals(role.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SUPER_ADMIN permissions cannot be changed");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : permissionIds) {
            if (id == null || !uniqueIds.add(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission IDs must be non-null and unique");
            }
        }
        List<AdminPermission> activePermissions = uniqueIds.isEmpty()
                ? List.of() : permissions.findAllByIdInAndActiveTrue(uniqueIds);
        if (activePermissions.size() != uniqueIds.size()) {
            Set<Long> foundIds = activePermissions.stream().map(AdminPermission::getId).collect(java.util.stream.Collectors.toSet());
            Long missing = uniqueIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive permission ID: " + missing);
        }
        role.setPermissions(new LinkedHashSet<>(activePermissions));
        role = roles.save(role);
        audit.record("ROLE_PERMISSIONS_UPDATED", "PERMISSION", "ADMIN_ROLE", roleId, "Permissions updated for role: " + role.getCode());
        return roleMap(role);
    }

    private void ensureUniqueName(String name, Long exceptId) {
        boolean exists = exceptId == null ? roles.existsByNameIgnoreCase(name) : roles.existsByNameIgnoreCaseAndIdNot(name, exceptId);
        if (exists) throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
    }

    private String codeFor(String name) {
        String code = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (code.isBlank() || code.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name cannot be converted to a valid role code");
        }
        if (roles.existsByCode(code)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        return code;
    }

    private AdminRole findRole(long id) {
        return roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin role not found"));
    }

    private AdminRole findRoleWithPermissions(long id) {
        return roles.findWithPermissionsById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin role not found"));
    }

    private Map<String, Object> roleMap(AdminRole role) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", role.getId()); result.put("code", role.getCode()); result.put("name", role.getName());
        result.put("description", role.getDescription()); result.put("is_system_role", role.isSystemRole());
        result.put("is_active", role.isActive()); result.put("created_at", role.getCreatedAt()); result.put("updated_at", role.getUpdatedAt());
        result.put("permissions", role.getPermissions().stream().filter(AdminPermission::isActive).map(AdminPermission::getCode).sorted().toList());
        return result;
    }

    private Map<String, Object> permissionMap(AdminPermission permission) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", permission.getId()); result.put("code", permission.getCode()); result.put("name", permission.getName());
        result.put("module", permission.getModule()); result.put("description", permission.getDescription()); result.put("is_active", permission.isActive());
        return result;
    }
}
