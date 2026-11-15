package com.app.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record AdminUpdateUserRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,100}") String username,
        @NotBlank @Email @Size(max=255) String email,
        @NotBlank @Size(max=100) String firstName, @Size(max=100) String lastName,
        @Size(max=30) String mobileNumber) {}
