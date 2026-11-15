package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLogoutRequest(@NotBlank String refreshToken) {}
