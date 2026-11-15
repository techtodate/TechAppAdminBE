package com.app.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RazorpayTestOrderRequest(
        @NotNull @Min(100) Long amount,
        @NotBlank @Size(max = 10) String currency) {}
