package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.RefundPolicyRule;
import com.app.admin.service.RefundPolicyRuleService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/refund-policies")
public class RefundPolicyRuleController {
    private final RefundPolicyRuleService service;
    public RefundPolicyRuleController(RefundPolicyRuleService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<RefundPolicyRule>> findAll() { return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}") public ResponseEntity<RefundPolicyRule> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<RefundPolicyRule> create(@Valid @RequestBody RefundPolicyRule request) { RefundPolicyRule created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<RefundPolicyRule> update(@PathVariable Long id, @Valid @RequestBody RefundPolicyRule request) { return ResponseEntity.ok(service.update(id, request)); }
}
