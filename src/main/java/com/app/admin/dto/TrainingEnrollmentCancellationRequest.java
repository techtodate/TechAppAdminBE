package com.app.admin.dto;

import jakarta.validation.constraints.Size;

public record TrainingEnrollmentCancellationRequest(@Size(max = 500) String reason) {}
