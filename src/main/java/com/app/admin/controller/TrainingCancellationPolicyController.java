package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.TrainingCancellationPolicy;
import com.app.admin.service.TrainingCancellationPolicyService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/cancellation-policies")
public class TrainingCancellationPolicyController {
    private final TrainingCancellationPolicyService service;
    public TrainingCancellationPolicyController(TrainingCancellationPolicyService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<TrainingCancellationPolicy>> findAll() { return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}") public ResponseEntity<TrainingCancellationPolicy> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<TrainingCancellationPolicy> create(@Valid @RequestBody TrainingCancellationPolicy request) { TrainingCancellationPolicy created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<TrainingCancellationPolicy> update(@PathVariable Long id, @Valid @RequestBody TrainingCancellationPolicy request) { return ResponseEntity.ok(service.update(id, request)); }
}
