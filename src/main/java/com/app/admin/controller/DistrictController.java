package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.District;
import com.app.admin.service.DistrictService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/districts")
public class DistrictController {
    private final DistrictService service;
    public DistrictController(DistrictService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<District>> findAll(@RequestParam(name = "state_id", required = false) Long stateId) { return ResponseEntity.ok(service.findAll(stateId)); }
    @GetMapping("/{id}") public ResponseEntity<District> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<District> create(@Valid @RequestBody District request) { District created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<District> update(@PathVariable Long id, @Valid @RequestBody District request) { return ResponseEntity.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
