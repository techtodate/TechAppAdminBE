package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateTechUpdateRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    @Size(max = 255)
    private String sourceUrl;
}
