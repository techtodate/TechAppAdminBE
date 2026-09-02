package com.app.admin.dto;

import jakarta.validation.constraints.NotNull;

public record StartEventReviewRequest(
        @NotNull(message = "reviewer_user_id is required") Long reviewerUserId) {
}
