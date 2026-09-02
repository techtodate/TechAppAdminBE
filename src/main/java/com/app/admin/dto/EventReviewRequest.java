package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventReviewRequest(
        @NotNull(message = "reviewer_user_id is required") Long reviewerUserId,
        @NotBlank(message = "reason is required")
        @Size(min = 10, max = 2000, message = "reason must contain between 10 and 2000 characters")
        String reason) {
}
