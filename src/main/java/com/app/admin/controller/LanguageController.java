package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.Language;
import com.app.admin.service.LanguageService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/languages")
public class LanguageController {
    private final LanguageService service;
    public LanguageController(LanguageService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<Language>> findAll() { return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}") public ResponseEntity<Language> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<Language> create(@Valid @RequestBody Language request) { Language created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<Language> update(@PathVariable Long id, @Valid @RequestBody Language request) { return ResponseEntity.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
