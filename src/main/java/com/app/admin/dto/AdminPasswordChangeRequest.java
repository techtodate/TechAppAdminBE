package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordChangeRequest(@NotBlank String currentPassword,
        @NotBlank @Size(min=12,max=128) String newPassword) {}
