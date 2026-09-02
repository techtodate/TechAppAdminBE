package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.State;
import com.app.admin.service.StateService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/states")
public class StateController {
    private final StateService service;
    public StateController(StateService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<State>> findAll(@RequestParam(name = "country_id", required = false) Long countryId) { return ResponseEntity.ok(service.findAll(countryId)); }
    @GetMapping("/{id}") public ResponseEntity<State> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<State> create(@Valid @RequestBody State request) { State created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<State> update(@PathVariable Long id, @Valid @RequestBody State request) { return ResponseEntity.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
