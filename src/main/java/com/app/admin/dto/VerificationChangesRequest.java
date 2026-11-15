package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationChangesRequest(@NotBlank String section, @NotBlank String comments) {}
