package com.app.admin.controller;

import com.app.admin.dto.AdminRoleRequest;
import com.app.admin.service.AdminRoleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {
    private final AdminRoleService roles;
    public AdminRoleController(AdminRoleService roles) { this.roles = roles; }

    @GetMapping public List<Map<String, Object>> list() { return roles.roles(); }
    @GetMapping("/{id}") public Map<String, Object> get(@PathVariable long id) { return roles.role(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody AdminRoleRequest request) { return roles.create(request); }
    @PutMapping("/{id}") public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody AdminRoleRequest request) { return roles.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable long id) { roles.delete(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/permissions") public List<Map<String, Object>> permissions(@PathVariable long id) { return roles.rolePermissions(id); }
    @PutMapping("/{id}/permissions") public Map<String, Object> setPermissions(@PathVariable long id, @RequestBody List<Long> permissionIds) { return roles.setRolePermissions(id, permissionIds); }
}
