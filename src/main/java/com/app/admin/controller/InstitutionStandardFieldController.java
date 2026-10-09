package com.app.admin.controller;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.app.admin.service.institution.InstitutionApiService;
@RestController
@RequestMapping("/api/admin/institution-standard-fields")
public class InstitutionStandardFieldController {
    private final InstitutionApiService api;
    public InstitutionStandardFieldController(InstitutionApiService api) { this.api = api; }
    @GetMapping public List<Map<String, Object>> list() { return api.fields(); }
}
