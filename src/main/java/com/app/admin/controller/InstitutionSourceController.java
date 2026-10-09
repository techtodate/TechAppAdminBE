package com.app.admin.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.app.admin.service.institution.InstitutionApiService;
@RestController
@RequestMapping("/api/admin/institution-sources")
public class InstitutionSourceController {
    private final InstitutionApiService api;
    public InstitutionSourceController(InstitutionApiService api) { this.api = api; }
    @GetMapping public List<Map<String, Object>> list(@RequestParam(required = false) Long countryId,
            @RequestParam(name = "country_id", required = false) Long countryIdSnake) { return api.sources(countryId != null ? countryId : countryIdSnake); }
}
