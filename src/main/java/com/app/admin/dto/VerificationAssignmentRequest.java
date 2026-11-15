package com.app.admin.dto;

import jakarta.validation.constraints.NotNull;

public record VerificationAssignmentRequest(@NotNull Long reviewerUserId) {}
