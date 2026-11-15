package com.app.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(String username, @Email String email,
        @NotBlank @Size(max=128) String password) {
    @AssertTrue(message = "Provide username or email")
    public boolean hasIdentifier() {
        return (username != null && !username.isBlank()) || (email != null && !email.isBlank());
    }
}
