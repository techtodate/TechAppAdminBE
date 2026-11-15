package com.app.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public record CommissionRuleRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotBlank String commissionType,
        BigDecimal percentage,
        BigDecimal fixedAmount,
        String currency,
        LocalDateTime effectiveFrom) {}
