package com.example.jhapcham.security;

import com.example.jhapcham.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().isBlank()
                || jwtProperties.getSecret().startsWith("change-me-in-production")) {
            throw new IllegalStateException("app.security.jwt.secret must be configured with a strong non-default value");
        }
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 bytes long");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getEmail())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("userId", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .claim("username", user.getUsername())
                .signWith(signingKey)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    public String extractSubject(String token) {
        return parse(token).getPayload().getSubject();
    }

    public Long extractUserId(String token) {
        return parse(token).getPayload().get("userId", Long.class);
    }

    public long getAccessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenTtlMinutes() * 60L;
    }
}
