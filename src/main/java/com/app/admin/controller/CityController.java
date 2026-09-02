package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.City;
import com.app.admin.service.CityService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/cities")
public class CityController {
    private final CityService service;
    public CityController(CityService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<City>> findAll(@RequestParam(name = "district_id", required = false) Long districtId) { return ResponseEntity.ok(service.findAll(districtId)); }
    @GetMapping("/{id}") public ResponseEntity<City> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<City> create(@Valid @RequestBody City request) { City created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<City> update(@PathVariable Long id, @Valid @RequestBody City request) { return ResponseEntity.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
