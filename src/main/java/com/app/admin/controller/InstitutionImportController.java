package com.app.admin.controller;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.app.admin.dto.CreateInstitutionImportRequest;
import com.app.admin.service.institution.InstitutionApiService;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/admin")
public class InstitutionImportController {
    private final InstitutionApiService api;
    public InstitutionImportController(InstitutionApiService api) { this.api = api; }
    @GetMapping("/institution-countries") public List<Map<String, Object>> countries() { return api.countries(); }
    @PostMapping("/institution-imports") public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateInstitutionImportRequest request) {
        Map<String, Object> result = api.create(request); return ResponseEntity.created(URI.create("/api/admin/institution-imports/" + result.get("id"))).body(result);
    }
    @PostMapping("/institution-imports/{id}/upload") public Map<String, Object> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return api.upload(id, file); }
    @PostMapping("/institution-imports/{id}/validate") public Map<String, Object> validate(@PathVariable Long id) { return api.validate(id); }
    @GetMapping("/institution-imports/{id}") public Map<String, Object> get(@PathVariable Long id) { return api.getImport(id); }
    @GetMapping("/institution-imports/{id}/summary") public Map<String, Object> summary(@PathVariable Long id) { return api.preview(id); }
    @GetMapping("/institution-imports/{id}/records") public Map<String, Object> records(@PathVariable Long id,
            @RequestParam(required = false) String classification, @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = {"rowNumber", "id"}) Pageable pageable) { return api.records(id, classification, q, pageable); }
    @GetMapping("/institution-imports/{id}/records/{recordId}") public Map<String, Object> record(@PathVariable Long id, @PathVariable Long recordId) { return api.record(id, recordId); }
    @GetMapping("/institution-imports/{id}/duplicates") public List<Map<String, Object>> duplicates(@PathVariable Long id) { return api.duplicates(id); }
    @PostMapping("/institution-imports/{id}/duplicates/{matchId}/merge") public Map<String, Object> merge(@PathVariable Long id, @PathVariable Long matchId) { return api.resolve(id, matchId, true); }
    @PostMapping("/institution-imports/{id}/duplicates/{matchId}/keep-separate") public Map<String, Object> separate(@PathVariable Long id, @PathVariable Long matchId) { return api.resolve(id, matchId, false); }
    @PostMapping("/institution-imports/{id}/apply") public Map<String, Object> apply(@PathVariable Long id) { return api.apply(id); }
    @PostMapping("/institution-imports/{id}/cancel") public Map<String, Object> cancel(@PathVariable Long id) { return api.cancel(id); }
    @GetMapping("/institution-import-history") public Map<String, Object> history(@RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long sourceId, @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) { return api.history(countryId, sourceId, status, dateFrom, dateTo, pageable); }
}
