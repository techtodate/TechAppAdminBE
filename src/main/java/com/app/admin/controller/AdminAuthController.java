package com.app.admin.controller;

import com.app.admin.dto.AdminLoginRequest;
import com.app.admin.dto.AdminLogoutRequest;
import com.app.admin.dto.AdminPasswordChangeRequest;
import com.app.admin.dto.AdminRefreshRequest;
import com.app.admin.service.AdminIdentityService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminIdentityService identities;
    public AdminAuthController(AdminIdentityService identities) { this.identities = identities; }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(identities.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody AdminRefreshRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(identities.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody AdminLogoutRequest request) {
        identities.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(identities.currentUser());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody AdminPasswordChangeRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(identities.changePassword(request.currentPassword(), request.newPassword()));
    }
}
