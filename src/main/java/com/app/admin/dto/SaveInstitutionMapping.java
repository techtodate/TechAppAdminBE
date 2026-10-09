package com.app.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveInstitutionMapping(
        @JsonProperty("source_field_name") @JsonAlias("sourceFieldName") @NotBlank @Size(max = 200) String sourceFieldName,
        @JsonProperty("standard_field_code") @JsonAlias("standardFieldCode") @NotBlank @Size(max = 100) String standardFieldCode,
        @JsonProperty("transformation_rule") @JsonAlias("transformationRule") @Size(max = 100) String transformationRule) {}
