package com.app.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RazorpayOrderRequest(@NotNull @Positive Integer enrollmentId) {}
