package com.app.admin.dto;

import com.app.admin.model.EventStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEventStatusRequest(
        @NotNull(message = "status is required") EventStatus status,
        Long reviewerUserId,
        @Size(max = 2000, message = "reason must not exceed 2000 characters") String reason) {
}
