package com.app.admin.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.app.admin.service.institution.InstitutionApiService;
import com.app.admin.dto.SaveInstitutionMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
@RestController
@RequestMapping("/api/admin/institution-source-mappings")
public class InstitutionSourceMappingController {
    private final InstitutionApiService api;
    public InstitutionSourceMappingController(InstitutionApiService api) { this.api = api; }
    @GetMapping public List<Map<String, Object>> list(@RequestParam(required = false) Long sourceId,
            @RequestParam(name = "source_id", required = false) Long sourceIdSnake) { return api.mappings(sourceId != null ? sourceId : sourceIdSnake); }
    @PutMapping public List<Map<String, Object>> save(@RequestParam Long sourceId, @RequestBody @NotEmpty List<@Valid SaveInstitutionMapping> mappings) { return api.saveMappings(sourceId, mappings); }
}
