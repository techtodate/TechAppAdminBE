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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.app.admin.model.MedicalSubject;
import com.app.admin.service.MedicalSubjectService;
import com.app.admin.service.TopicImageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/medical-subjects")
public class MedicalSubjectController {
    private final MedicalSubjectService service;
    private final TopicImageService imageService;

    public MedicalSubjectController(MedicalSubjectService service, TopicImageService imageService) {
        this.service = service;
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<MedicalSubject>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalSubject> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<MedicalSubject> create(@Valid @RequestBody MedicalSubject request) {
        MedicalSubject created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalSubject> update(
            @PathVariable Long id, @Valid @RequestBody MedicalSubject request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @RequestMapping(value = "/{id}/image", method = {RequestMethod.POST, RequestMethod.PUT}, consumes = "multipart/form-data")
    public ResponseEntity<MedicalSubject> uploadOrReplaceImage(
            @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.updateMedicalSubjectImage(id, file));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Void> loadImage(@PathVariable Long id) {
        MedicalSubject topic = service.findById(id);
        String imageUrl = imageService.imageUrl(topic.getImageKey());
        if (imageUrl == null) {
            throw new com.app.admin.exception.ResourceNotFoundException("MedicalSubject image", id);
        }
        return ResponseEntity.status(302).location(URI.create(imageUrl)).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
