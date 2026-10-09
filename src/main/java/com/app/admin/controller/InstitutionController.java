package com.app.admin.controller;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import com.app.admin.service.institution.InstitutionApiService;
@RestController
@RequestMapping("/api/admin/institutions")
public class InstitutionController {
    private final InstitutionApiService api;
    public InstitutionController(InstitutionApiService api) { this.api = api; }
    @GetMapping({"", "/search"})
    public Map<String, Object> list(@RequestParam(required = false) Long countryId,
            @RequestParam(name = "country_id", required = false) Long countryIdSnake,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) String institutionType,
            @RequestParam(required = false) String verificationStatus, @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return api.list(countryId != null ? countryId : countryIdSnake, q, institutionType, verificationStatus, active, pageable);
    }
    @GetMapping("/{id}") public Map<String, Object> details(@PathVariable Long id) { return api.institutionDetails(id); }
    @GetMapping("/summary") public Map<String, Object> summary() { return api.dashboard(); }
    @GetMapping("/metadata") public Map<String, Object> metadata() { return api.metadata(); }
}
