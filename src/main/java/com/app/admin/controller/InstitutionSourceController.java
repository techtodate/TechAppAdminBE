package com.app.admin.controller;
import java.util.*;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.InstitutionSource;
import com.app.admin.service.InstitutionSourceMasterService;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/admin/institution-sources")
public class InstitutionSourceController {
    private final InstitutionSourceMasterService service;
    public InstitutionSourceController(InstitutionSourceMasterService service) { this.service = service; }
    @GetMapping public List<Map<String, Object>> list(@RequestParam(required = false) Long countryId,
            @RequestParam(name = "country_id", required = false) Long countryIdSnake,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) String q) {
        return service.list(countryId != null ? countryId : countryIdSnake, active, q);
    }
    @GetMapping("/countries") public List<Map<String, Object>> countries() { return service.countries(); }
    @GetMapping("/{id}") public Map<String, Object> get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody InstitutionSource request) {
        Map<String, Object> created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.get("id")).toUri();
        return ResponseEntity.created(location).body(created);
    }
    @PutMapping("/{id}") public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody InstitutionSource request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
