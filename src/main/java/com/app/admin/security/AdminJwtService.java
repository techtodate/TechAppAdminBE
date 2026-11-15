package com.app.admin.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Issues short-lived Admin-audience JWTs and rejects tokens for any other application. */
@Component
public class AdminJwtService {
    public static final String ISSUER = "zeto9-admin";
    public static final String AUDIENCE = "zeto9-admin-api";
    private static final String TOKEN_USE = "admin_access";
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();
    private final byte[] secret;
    private final long accessMinutes;

    public AdminJwtService(ObjectMapper mapper,
            @Value("${app.admin.jwt-secret:${ADMIN_JWT_SECRET:}}") String secret,
            @Value("${app.admin.access-token-minutes:15}") long accessMinutes) {
        this.mapper = mapper;
        this.secret = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        this.accessMinutes = Math.max(1, Math.min(accessMinutes, 30));
        requireSecret();
    }

    public IssuedToken issue(long adminId) {
        requireSecret();
        Instant now = clock.instant();
        Instant expiry = now.plusSeconds(accessMinutes * 60);
        String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER); claims.put("aud", AUDIENCE);
        claims.put("sub", Long.toString(adminId)); claims.put("token_use", TOKEN_USE);
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", now.getEpochSecond()); claims.put("exp", expiry.getEpochSecond());
        String unsigned = header + "." + encode(claims);
        String token = unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(unsigned));
        return new IssuedToken(token, expiry);
    }

    public Claims validate(String token) {
        try {
            requireSecret();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;
            Map<?, ?> header = mapper.readValue(Base64.getUrlDecoder().decode(parts[0]), Map.class);
            if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) return null;
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned), Base64.getUrlDecoder().decode(parts[2]))) return null;
            Map<?, ?> body = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            long now = clock.instant().getEpochSecond();
            long expiry = number(body.get("exp"));
            long issuedAt = number(body.get("iat"));
            long subject = Long.parseLong(String.valueOf(body.get("sub")));
            if (!ISSUER.equals(body.get("iss")) || !AUDIENCE.equals(body.get("aud"))
                    || !TOKEN_USE.equals(body.get("token_use")) || expiry <= now
                    || issuedAt > now + 60 || subject <= 0 || body.get("jti") == null) return null;
            return new Claims(subject, String.valueOf(body.get("jti")), Instant.ofEpochSecond(expiry));
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ex) {
            throw new IllegalStateException("Admin token signing is unavailable", ex);
        }
    }

    private String encode(Object value) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(value)); }
        catch (Exception ex) { throw new IllegalStateException("Could not encode Admin token", ex); }
    }
    private long number(Object value) {
        if (!(value instanceof Number n)) throw new IllegalArgumentException("Invalid JWT date claim");
        return n.longValue();
    }
    private void requireSecret() {
        if (secret.length < 32) throw new IllegalStateException("ADMIN_JWT_SECRET must contain at least 32 bytes");
    }

    public record Claims(long adminId, String tokenId, Instant expiresAt) {}
    public record IssuedToken(String value, Instant expiresAt) {}
}
