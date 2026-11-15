package com.app.admin.controller;

import com.app.admin.dto.TrainingEnrollmentCancellationRequest;
import com.app.admin.service.TrainingEnrollmentFinancialService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/training/enrollments")
public class TrainingEnrollmentFinancialController {
    private final TrainingEnrollmentFinancialService service;

    public TrainingEnrollmentFinancialController(TrainingEnrollmentFinancialService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> searchEnrollments(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int limit) {
        return service.searchEnrollments(search, limit);
    }
    @GetMapping("/{enrollmentId}/payment")
    public Map<String, Object> payment(@PathVariable Integer enrollmentId) {
        return service.paymentStatus(enrollmentId);
    }

    @GetMapping("/{enrollmentId}/cancellation")
    public Map<String, Object> cancellation(@PathVariable Integer enrollmentId) {
        return service.cancellation(enrollmentId);
    }

    @GetMapping("/{enrollmentId}/refunds")
    public List<Map<String, Object>> refunds(@PathVariable Integer enrollmentId) {
        return service.refunds(enrollmentId);
    }

    @GetMapping("/{enrollmentId}/cancellation/preview")
    public Map<String, Object> cancellationPreview(@PathVariable Integer enrollmentId) {
        return service.cancellationPreview(enrollmentId);
    }

    @PostMapping("/{enrollmentId}/cancel")
    public Map<String, Object> requestCancellation(@PathVariable Integer enrollmentId,
            @Valid @RequestBody TrainingEnrollmentCancellationRequest request) {
        return service.requestCancellation(enrollmentId, request.reason());
    }
}
