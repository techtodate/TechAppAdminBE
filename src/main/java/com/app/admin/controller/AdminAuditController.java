package com.app.admin.controller;

import com.app.admin.service.AdminAuditService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {
    private final AdminAuditService audit;
    public AdminAuditController(AdminAuditService audit) { this.audit = audit; }
    @GetMapping public List<Map<String, Object>> list(@RequestParam(required = false) String module,
            @RequestParam(required = false) String action, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) { return audit.search(module, action, page, size); }
}
