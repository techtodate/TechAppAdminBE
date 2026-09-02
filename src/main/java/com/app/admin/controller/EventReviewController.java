package com.app.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.admin.dto.EventReviewRequest;
import com.app.admin.dto.BulkEventReviewRequest;
import com.app.admin.dto.StartEventReviewRequest;
import com.app.admin.dto.EventDetailsResponse;
import com.app.admin.dto.UpdateEventStatusRequest;
import com.app.admin.model.Event;
import com.app.admin.service.EventReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/events")
public class EventReviewController {
    private final EventReviewService service;

    public EventReviewController(EventReviewService service) {
        this.service = service;
    }

    @GetMapping("/under-review")
    public ResponseEntity<List<Event>> findUnderReview() {
        return ResponseEntity.ok(service.findUnderReview());
    }

    @GetMapping("/review-queue")
    public ResponseEntity<List<Event>> findReviewQueue() {
        return ResponseEntity.ok(service.findReviewQueue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailsResponse> findDetails(@PathVariable Long id) {
        return ResponseEntity.ok(service.findDetails(id));
    }

    @PostMapping("/{id}/start-review")
    public ResponseEntity<Event> startReview(
            @PathVariable Long id, @Valid @RequestBody StartEventReviewRequest request) {
        return ResponseEntity.ok(service.startReview(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Event> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateEventStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Event> approve(
            @PathVariable Long id, @Valid @RequestBody EventReviewRequest request) {
        return ResponseEntity.ok(service.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Event> reject(
            @PathVariable Long id, @Valid @RequestBody EventReviewRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }

    @PostMapping("/bulk/approve")
    public ResponseEntity<List<Event>> approveBulk(
            @Valid @RequestBody BulkEventReviewRequest request) {
        return ResponseEntity.ok(service.approveBulk(request));
    }

    @PostMapping("/bulk/reject")
    public ResponseEntity<List<Event>> rejectBulk(
            @Valid @RequestBody BulkEventReviewRequest request) {
        return ResponseEntity.ok(service.rejectBulk(request));
    }
}
