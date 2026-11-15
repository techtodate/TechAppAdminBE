package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.dto.CommissionRuleRequest;
import com.app.admin.model.PlatformCommissionRule;
import com.app.admin.model.PlatformCommissionRuleVersion;
import com.app.admin.service.TrainingCommissionRuleService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/commission-rules")
public class TrainingCommissionRuleController {
    private final TrainingCommissionRuleService service;
    public TrainingCommissionRuleController(TrainingCommissionRuleService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<PlatformCommissionRule>> findAll() { return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}") public ResponseEntity<PlatformCommissionRule> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @GetMapping("/{id}/versions") public ResponseEntity<List<PlatformCommissionRuleVersion>> versions(@PathVariable Long id) { return ResponseEntity.ok(service.versions(id)); }
    @PostMapping public ResponseEntity<PlatformCommissionRule> create(@Valid @RequestBody CommissionRuleRequest request) { PlatformCommissionRule created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<PlatformCommissionRule> update(@PathVariable Long id, @Valid @RequestBody CommissionRuleRequest request) { return ResponseEntity.ok(service.update(id, request)); }
}
