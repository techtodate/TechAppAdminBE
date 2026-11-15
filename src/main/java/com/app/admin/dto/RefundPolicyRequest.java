package com.app.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefundPolicyRequest(@NotBlank String code, @NotBlank String name, String description,
        @NotNull BigDecimal refundPercentage, Boolean active, Integer displayOrder) {}
