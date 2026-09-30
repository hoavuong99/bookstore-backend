package com.example.bookstore.security;

import com.example.bookstore.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;

    private final JwtProperties jwtProperties;

    public String generateToken(CustomUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claims(Map.of(
                        "userId", principal.getId(),
                        "role", principal.getAuthorities().iterator().next().getAuthority()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, CustomUserPrincipal principal) {
        String username = extractUsername(token);
        return username.equals(principal.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(resolveSecretBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(resolveSecretBytes());
    }

    private byte[] resolveSecretBytes() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must not be blank");
        }

        String normalizedSecret = secret.trim();
        if (normalizedSecret.startsWith("base64:")) {
            byte[] decoded = Decoders.BASE64.decode(normalizedSecret.substring("base64:".length()));
            validateMinLength(decoded);
            return decoded;
        }

        byte[] plainBytes = normalizedSecret.getBytes(StandardCharsets.UTF_8);
        validateMinLength(plainBytes);
        return plainBytes;
    }

    private void validateMinLength(byte[] secretBytes) {
        if (secretBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 characters for HS256");
        }
    }
}