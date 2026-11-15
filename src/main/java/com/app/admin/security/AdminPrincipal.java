package com.app.admin.security;

import java.util.Set;

public record AdminPrincipal(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean active,
        boolean mustChangePassword,
        Set<String> roles,
        Set<String> permissions,
        String tokenId) {
    public String displayName() {
        return String.join(" ", java.util.stream.Stream.of(firstName, lastName)
                .filter(value -> value != null && !value.isBlank()).toList());
    }
}
