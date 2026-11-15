package com.app.admin.controller;

import com.app.admin.service.AdminRoleService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/permissions")
public class AdminPermissionController {
    private final AdminRoleService roles;
    public AdminPermissionController(AdminRoleService roles) { this.roles = roles; }
    @GetMapping public List<Map<String, Object>> list(@RequestParam(required = false) String module) { return roles.permissions(module); }
}
