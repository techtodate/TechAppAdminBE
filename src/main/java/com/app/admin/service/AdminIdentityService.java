package com.app.admin.service;

import com.app.admin.dto.AdminCreateUserRequest;
import com.app.admin.dto.AdminLoginRequest;
import com.app.admin.dto.AdminRefreshRequest;
import com.app.admin.dto.AdminUpdateUserRequest;
import com.app.admin.model.AdminRefreshToken;
import com.app.admin.model.AdminRole;
import com.app.admin.model.AdminUser;
import com.app.admin.repository.AdminRefreshTokenRepository;
import com.app.admin.repository.AdminRoleRepository;
import com.app.admin.repository.AdminUserRepository;
import com.app.admin.security.AdminJwtService;
import com.app.admin.security.AdminPasswordHasher;
import com.app.admin.security.AdminPrincipal;
import com.app.admin.security.AdminSecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminIdentityService {
    private final AdminUserRepository users;
    private final AdminRoleRepository roles;
    private final AdminRefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final AdminPasswordHasher legacyPasswords;
    private final AdminJwtService jwt;
    private final AdminSecurityContext security;
    private final AdminAuditService audit;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;
    @Value("${app.admin.refresh-token-days:14}") private long refreshDays;

    public AdminIdentityService(AdminUserRepository users, AdminRoleRepository roles,
            AdminRefreshTokenRepository refreshTokens,
            PasswordEncoder passwords, AdminPasswordHasher legacyPasswords, AdminJwtService jwt,
            AdminSecurityContext security, AdminAuditService audit) {
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.legacyPasswords = legacyPasswords;
        this.jwt = jwt;
        this.security = security;
        this.audit = audit;
        this.dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Map<String, Object> login(AdminLoginRequest request) {
        String identifier = (!blank(request.email()) ? request.email() : request.username()).trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            passwords.matches(request.password().substring(0, Math.min(request.password().length(), 72)), dummyHash);
            audit.recordLogin("LOGIN_FAILURE", identifier, "Invalid Admin credentials.");
            throw invalidCredentials();
        }
        List<AdminUser> matches = users.findForLogin(identifier);
        if (matches.size() != 1) {
            passwords.matches(request.password(), dummyHash);
            audit.recordLogin("LOGIN_FAILURE", identifier, "Invalid Admin credentials.");
            throw invalidCredentials();
        }
        AdminUser user = matches.get(0);
        String encoded = user.getPasswordHash();
        boolean matchesPassword = passwords.matches(request.password(), encoded);
        boolean legacyMatch = !matchesPassword && legacyPasswords.matches(request.password(), encoded);
        if (!user.isActive() || (!matchesPassword && !legacyMatch)) {
            passwords.matches(request.password(), dummyHash);
            audit.recordLogin("LOGIN_FAILURE", identifier, "Invalid Admin credentials.");
            throw invalidCredentials();
        }
        if (legacyMatch) user.setPasswordHash(passwords.encode(request.password()));
        user.setLastLoginAt(LocalDateTime.now());
        users.save(user);
        audit.recordLoginForUser(user.getId(), "LOGIN_SUCCESS", "AUTH", "ADMIN_USER", user.getId(), "Admin login succeeded.");
        return tokenPair(user);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Map<String, Object> refresh(AdminRefreshRequest request) {
        AdminRefreshToken token = refreshTokens.findByTokenHash(tokenHash(request.refreshToken())).orElse(null);
        if (token == null) {
            audit.recordLogin("TOKEN_REFRESH_FAILURE", null, "Unknown refresh token rejected.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        AdminUser user = token.getAdminUser();
        Long userId = user.getId();
        LocalDateTime now = LocalDateTime.now();
        if (token.getRevokedAt() != null) {
            refreshTokens.revokeAllActiveForUser(userId, now);
            audit.recordLoginForUser(userId, "REFRESH_TOKEN_REUSE", "AUTH", "ADMIN_USER", userId, "A previously used refresh token was presented.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (!token.getExpiresAt().isAfter(now) || !users.existsByIdAndActiveTrue(userId)) {
            token.setRevokedAt(now);
            refreshTokens.save(token);
            audit.recordLoginForUser(userId, "TOKEN_REFRESH_FAILURE", "AUTH", "ADMIN_USER", userId, "Expired or inactive refresh token rejected.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        token.setRevokedAt(now);
        refreshTokens.save(token);
        audit.recordLoginForUser(userId, "TOKEN_REFRESH", "AUTH", "ADMIN_USER", userId, "Refresh token rotated.");
        return tokenPair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        AdminPrincipal principal = requirePrincipal();
        int updated = refreshTokens.revokeActiveToken(tokenHash(rawRefreshToken), principal.id(), LocalDateTime.now());
        if (updated == 0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        audit.record("LOGOUT", "AUTH", "ADMIN_USER", principal.id(), "Admin logged out.");
    }

    @Transactional(readOnly = true)
    public AdminPrincipal authenticateAccessToken(String rawToken) {
        AdminJwtService.Claims claims = jwt.validate(rawToken);
        if (claims == null) return null;
        AdminUser user = users.findById(claims.adminId()).orElse(null);
        if (user == null || !user.isActive()) return null;
        Set<String> roleCodes = new LinkedHashSet<>(users.findActiveRoleCodes(user.getId()));
        Set<String> permissionCodes = new LinkedHashSet<>(users.findActivePermissionCodes(user.getId()));
        return new AdminPrincipal(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), true,
                user.isMustChangePassword(), Set.copyOf(roleCodes), Set.copyOf(permissionCodes), claims.tokenId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> users() {
        security.requirePermission("USER_VIEW");
        return users.findAllWithRoles().stream().map(this::userMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> userDetails(long id) {
        security.requirePermission("USER_VIEW");
        return userMap(findUser(id));
    }

    @Transactional
    public Map<String, Object> createUser(AdminCreateUserRequest request) {
        security.requirePermission("USER_CREATE");
        security.requirePermission("ROLE_ASSIGN");
        validateBcryptPassword(request.password());
        List<AdminRole> assignedRoles = findRolesByCode(request.roles());
        if (assignedRoles.stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getCode())) && !security.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can grant the SUPER_ADMIN role");
        }
        if (assignedRoles.stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()))) lockSuperAdminRole();
        AdminUser user = new AdminUser();
        user.setUsername(request.username().trim().toLowerCase(Locale.ROOT));
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwords.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName());
        user.setMobileNumber(request.mobileNumber());
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setRoles(new LinkedHashSet<>(assignedRoles));
        user = users.save(user);
        audit.record("ADMIN_USER_CREATED", "USER", "ADMIN_USER", user.getId(), "Admin account created.");
        return userMap(user);
    }

    @Transactional
    public Map<String, Object> updateUser(long id, AdminUpdateUserRequest request) {
        security.requirePermission("USER_UPDATE");
        AdminUser user = findUser(id);
        requireSuperAdminForTarget(user);
        user.setUsername(request.username().trim().toLowerCase(Locale.ROOT));
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName());
        user.setMobileNumber(request.mobileNumber());
        user = users.save(user);
        audit.record("ADMIN_USER_UPDATED", "USER", "ADMIN_USER", id, "Admin account details updated.");
        return userMap(user);
    }

    @Transactional
    public Map<String, Object> setEnabled(long id, boolean enabled) {
        security.requirePermission(enabled ? "USER_ENABLE" : "USER_DISABLE");
        lockSuperAdminRole();
        AdminUser target = findUser(id);
        requireSuperAdminForTarget(target);
        AdminPrincipal actor = requirePrincipal();
        if (!enabled && actor.id().equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot disable your own Admin account");
        if (target.isActive() && !enabled && hasRole(target, "SUPER_ADMIN") && users.countActiveSuperAdmins() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active Super Admin must remain");
        }
        target.setActive(enabled);
        target = users.save(target);
        if (!enabled) refreshTokens.revokeAllActiveForUser(id, LocalDateTime.now());
        audit.record(enabled ? "ADMIN_USER_ENABLED" : "ADMIN_USER_DISABLED", "USER", "ADMIN_USER", id,
                enabled ? "Admin account enabled." : "Admin account disabled.");
        return userMap(target);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> userRoles(long id) {
        security.requirePermission("USER_VIEW");
        return findUser(id).getRoles().stream().filter(AdminRole::isActive).map(this::roleMap).toList();
    }

    @Transactional
    public Map<String, Object> assignRole(long id, long roleId) {
        security.requirePermission("ROLE_ASSIGN");
        AdminUser user = findUser(id);
        requireSuperAdminForTarget(user);
        AdminRole role = roles.findById(roleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin role not found"));
        if (!role.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Inactive roles cannot be assigned");
        if ("SUPER_ADMIN".equals(role.getCode()) && !security.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can grant the SUPER_ADMIN role");
        }
        if ("SUPER_ADMIN".equals(role.getCode())) lockSuperAdminRole();
        boolean changed = user.getRoles().add(role);
        if (changed) {
            users.save(user);
            audit.record("ADMIN_ROLE_ASSIGNED", "ROLE", "ADMIN_USER", id, "Role assigned: " + role.getCode());
        }
        return userMap(user);
    }

    @Transactional
    public Map<String, Object> removeRole(long id, long roleId) {
        security.requirePermission("ROLE_ASSIGN");
        AdminUser user = findUser(id);
        AdminRole role = roles.findById(roleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin role not found"));
        requireSuperAdminForTarget(user);
        if ("SUPER_ADMIN".equals(role.getCode())) lockSuperAdminRole();
        if ("SUPER_ADMIN".equals(role.getCode()) && user.isActive() && users.countActiveSuperAdmins() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active Super Admin must remain");
        }
        boolean removed = user.getRoles().removeIf(existing -> existing.getId().equals(roleId));
        if (!removed) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user does not have this role");
        if (user.getRoles().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Every Admin account must retain at least one role");
        users.save(user);
        audit.record("ADMIN_ROLE_REMOVED", "ROLE", "ADMIN_USER", id, "Role removed: " + role.getCode());
        return userMap(user);
    }

    @Transactional
    public Map<String, Object> changePassword(String currentPassword, String newPassword) {
        AdminPrincipal principal = requirePrincipal();
        validateBcryptPassword(newPassword);
        AdminUser user = users.findByIdForUpdate(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin authentication is required"));
        boolean valid = passwords.matches(currentPassword, user.getPasswordHash()) || legacyPasswords.matches(currentPassword, user.getPasswordHash());
        if (!valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        user.setPasswordHash(passwords.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);
        refreshTokens.revokeAllActiveForUser(principal.id(), LocalDateTime.now());
        audit.record("PASSWORD_CHANGED", "AUTH", "ADMIN_USER", principal.id(), "Admin password changed.");
        return Map.of("changed", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentUser() {
        AdminPrincipal principal = requirePrincipal();
        AdminUser user = findUser(principal.id());
        Map<String, Object> result = userMap(user);
        String displayName = String.join(" ", List.of(
                user.getFirstName() == null ? "" : user.getFirstName().trim(),
                user.getLastName() == null ? "" : user.getLastName().trim())).trim().replaceAll("\\s+", " ");
        result.put("display_name", displayName.isBlank() ? user.getUsername() : displayName);
        result.put("roles", users.findActiveRoleCodes(user.getId()));
        result.put("permissions", users.findActivePermissionCodes(user.getId()));
        return result;
    }

    private Map<String, Object> tokenPair(AdminUser user) {
        AdminJwtService.IssuedToken access = jwt.issue(user.getId());
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String refreshValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        int days = (int) Math.max(7, Math.min(refreshDays, 30));
        LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(days);
        AdminRefreshToken refresh = new AdminRefreshToken();
        refresh.setAdminUser(user);
        refresh.setTokenHash(tokenHash(refreshValue));
        refresh.setExpiresAt(refreshExpiry);
        refreshTokens.save(refresh);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("access_token", access.value());
        result.put("token_type", "Bearer");
        result.put("access_expires_at", access.expiresAt());
        result.put("refresh_token", refreshValue);
        result.put("refresh_expires_at", refreshExpiry);
        result.put("user", userMap(user));
        return result;
    }

    private Map<String, Object> userMap(AdminUser user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId()); result.put("username", user.getUsername()); result.put("email", user.getEmail());
        result.put("first_name", user.getFirstName()); result.put("last_name", user.getLastName());
        result.put("mobile_number", user.getMobileNumber()); result.put("is_active", user.isActive());
        result.put("must_change_password", user.isMustChangePassword()); result.put("last_login_at", user.getLastLoginAt());
        result.put("created_at", user.getCreatedAt()); result.put("updated_at", user.getUpdatedAt());
        result.put("roles", user.getRoles().stream().filter(AdminRole::isActive).map(AdminRole::getCode).sorted().toList());
        return result;
    }

    private Map<String, Object> roleMap(AdminRole role) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", role.getId()); result.put("code", role.getCode()); result.put("name", role.getName());
        result.put("description", role.getDescription()); result.put("is_system_role", role.isSystemRole());
        result.put("is_active", role.isActive());
        return result;
    }

    private AdminUser findUser(long id) {
        return users.findWithRolesById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
    }

    private List<AdminRole> findRolesByCode(List<String> codes) {
        List<AdminRole> found = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String value : codes) {
            String code = value.trim().toUpperCase(Locale.ROOT);
            if (!seen.add(code)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate Admin role: " + value);
            found.add(roles.findByCodeAndActiveTrue(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive Admin role: " + value)));
        }
        return found;
    }

    private boolean hasRole(AdminUser user, String code) {
        return user.getRoles().stream().anyMatch(role -> code.equals(role.getCode()));
    }

    private void requireSuperAdminForTarget(AdminUser user) {
        if (hasRole(user, "SUPER_ADMIN") && !security.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SUPER_ADMIN can manage a SUPER_ADMIN account");
        }
    }

    private void lockSuperAdminRole() {
        roles.findSuperAdminRoleForUpdate().orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role is missing"));
    }

    private AdminPrincipal requirePrincipal() {
        AdminPrincipal principal = security.currentPrincipal();
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin authentication is required");
        return principal;
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
    }

    private void validateBcryptPassword(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
    }

    private String tokenHash(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 is unavailable", ex); }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
