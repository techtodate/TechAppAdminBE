package com.app.admin.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminPasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(12);
    @Override public String encode(CharSequence rawPassword) { return delegate.encode(rawPassword); }
    @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encodedPassword != null && encodedPassword.startsWith("$2") && delegate.matches(rawPassword, encodedPassword);
    }
}
