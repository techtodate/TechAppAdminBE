package com.app.admin.controller;

import com.app.admin.dto.AdminCreateUserRequest;
import com.app.admin.dto.AdminUpdateUserRequest;
import com.app.admin.dto.AdminUserRoleAssignmentRequest;
import com.app.admin.service.AdminIdentityService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminIdentityService identities;
    public AdminUserController(AdminIdentityService identities) { this.identities = identities; }

    @GetMapping public List<Map<String, Object>> list() { return identities.users(); }
    @GetMapping("/{id}") public Map<String, Object> get(@PathVariable long id) { return identities.userDetails(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody AdminCreateUserRequest request) { return identities.createUser(request); }
    @PutMapping("/{id}") public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody AdminUpdateUserRequest request) { return identities.updateUser(id, request); }
    @PostMapping("/{id}/enable") public Map<String, Object> enable(@PathVariable long id) { return identities.setEnabled(id, true); }
    @PostMapping("/{id}/disable") public Map<String, Object> disable(@PathVariable long id) { return identities.setEnabled(id, false); }
    @GetMapping("/{id}/roles") public List<Map<String, Object>> roles(@PathVariable long id) { return identities.userRoles(id); }
    @PostMapping("/{id}/roles") public Map<String, Object> assignRole(@PathVariable long id, @Valid @RequestBody AdminUserRoleAssignmentRequest request) { return identities.assignRole(id, request.roleId()); }
    @DeleteMapping("/{id}/roles/{roleId}") public Map<String, Object> removeRole(@PathVariable long id, @PathVariable long roleId) { return identities.removeRole(id, roleId); }
}
