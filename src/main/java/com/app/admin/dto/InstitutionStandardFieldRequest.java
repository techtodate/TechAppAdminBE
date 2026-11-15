package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InstitutionStandardFieldRequest(
        @NotBlank @Size(max = 100) String field_code,
        @NotBlank @Size(max = 200) String field_name,
        @NotBlank @Size(max = 50) String data_type,
        @NotNull Boolean required,
        @Size(max = 500) String description,
        @NotNull Boolean active,
        @NotNull @PositiveOrZero Integer display_order) {
}
