package com.app.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateInstitutionImportRequest(
        @JsonProperty("country_id") @JsonAlias("countryId") @NotNull(message = "country_id is required") @Positive Long countryId,
        @JsonProperty("source_id") @JsonAlias("sourceId") @NotNull(message = "source_id is required") @Positive Long sourceId,
        @JsonProperty("version") @Size(max = 100) String version,
        @JsonProperty("import_type") @JsonAlias("importType") @jakarta.validation.constraints.Pattern(regexp = "FULL|INCREMENTAL", message = "import_type must be FULL or INCREMENTAL") String importType,
        @JsonProperty("file_name") @JsonAlias("fileName") @Size(max = 255) String fileName
) {
}
