package com.app.admin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.app.admin.model.MedicalUpdate;
import com.app.admin.service.MedicalUpdateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/medical-updates")
public class MedicalUpdateController {
    private final MedicalUpdateService service;

    public MedicalUpdateController(MedicalUpdateService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MedicalUpdate>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalUpdate> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<MedicalUpdate> create(@Valid @RequestBody MedicalUpdate request) {
        MedicalUpdate created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalUpdate> update(
            @PathVariable Long id, @Valid @RequestBody MedicalUpdate request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
