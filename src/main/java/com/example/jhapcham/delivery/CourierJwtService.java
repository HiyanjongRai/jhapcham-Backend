package com.example.jhapcham.delivery;

import com.example.jhapcham.security.JwtProperties;
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
public class CourierJwtService {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public CourierJwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateCourierToken(Courier courier) {
        Instant now = Instant.now();
        Instant expiry = now.plus(7, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(courier.getEmail())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("scope", "COURIER")
                .claim("courierId", courier.getId())
                .claim("roles", List.of("COURIER"))
                .claim("fullName", courier.getFullName())
                .signWith(signingKey)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    public boolean isCourierToken(String token) {
        Object scope = parse(token).getPayload().get("scope");
        return scope != null && "COURIER".equalsIgnoreCase(scope.toString());
    }

    public String extractSubject(String token) {
        return parse(token).getPayload().getSubject();
    }
}
