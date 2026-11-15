package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationDecisionRequest(@NotBlank String reason) {}
