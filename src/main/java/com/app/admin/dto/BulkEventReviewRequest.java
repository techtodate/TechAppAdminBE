package com.app.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkEventReviewRequest(
        @NotEmpty(message = "event_ids must contain at least one event")
        @Size(max = 100, message = "a maximum of 100 events can be reviewed at once")
        List<@NotNull(message = "event_ids cannot contain null values") Long> eventIds,
        @NotNull(message = "reviewer_user_id is required") Long reviewerUserId,
        @NotBlank(message = "reason is required")
        @Size(min = 10, max = 2000, message = "reason must contain between 10 and 2000 characters")
        String reason) {
}
