package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentProviderRequest(@NotBlank String code, @NotBlank String name, Boolean active) {}
