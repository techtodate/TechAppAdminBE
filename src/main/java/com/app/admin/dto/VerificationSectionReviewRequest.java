package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationSectionReviewRequest(
        @NotBlank @Pattern(regexp = "APPROVE|REJECT|REQUEST_CHANGES") String action,
        String comments) {}
