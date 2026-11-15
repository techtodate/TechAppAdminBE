package com.app.admin.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.app.admin.dto.CreateTechUpdateRequest;
import com.app.admin.dto.TechUpdateResponse;
import com.app.admin.service.TechUpdateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/technology-updates")
public class AdminTechUpdateController {
    private final TechUpdateService service;

    public AdminTechUpdateController(TechUpdateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TechUpdateResponse> create(@Valid @RequestBody CreateTechUpdateRequest request) {
        TechUpdateResponse created = service.createManual(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/tech-updates/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
