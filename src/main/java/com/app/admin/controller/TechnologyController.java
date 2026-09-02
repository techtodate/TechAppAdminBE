package com.app.admin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.app.admin.model.Technology;
import com.app.admin.service.TechnologyService;
import com.app.admin.service.TopicImageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/technologies")
public class TechnologyController {
    private final TechnologyService service;
    private final TopicImageService imageService;

    public TechnologyController(TechnologyService service, TopicImageService imageService) {
        this.service = service;
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<Technology>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Technology> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Technology> create(@Valid @RequestBody Technology request) {
        Technology created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Technology> update(
            @PathVariable Long id, @Valid @RequestBody Technology request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @RequestMapping(value = "/{id}/image", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = "multipart/form-data")
    public ResponseEntity<Technology> uploadOrReplaceImage(
            @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.updateTechnologyImage(id, file));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Void> loadImage(@PathVariable Long id) {
        Technology topic = service.findById(id);
        String imageUrl = imageService.imageUrl(topic.getImageKey());
        if (imageUrl == null) {
            throw new com.app.admin.exception.ResourceNotFoundException("Technology image", id);
        }
        return ResponseEntity.status(302).location(URI.create(imageUrl)).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
