package com.app.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateTechUpdateRequest {
    @JsonProperty("tech_id")
    @JsonAlias("techId")
    @NotNull
    private Long techId;

    @JsonProperty("title")
    @NotBlank
    @Size(max = 255)
    private String title;

    @JsonProperty("content")
    @NotBlank
    private String content;

    @JsonProperty("source_url")
    @JsonAlias("sourceUrl")
    @Size(max = 255)
    private String sourceUrl;
}
