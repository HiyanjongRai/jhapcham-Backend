package com.example.jhapcham.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenCookieService {

    private final JwtProperties jwtProperties;

    public RefreshTokenCookieService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ofDays(jwtProperties.getRefreshTokenTtlDays()))
                .build()
                .toString();
    }

    public String createClearedRefreshTokenCookie() {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    public String extractRefreshToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (jwtProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void ensureTrustedOrigin(HttpServletRequest request) {
        if (request == null || jwtProperties.getAllowedOrigins() == null || jwtProperties.getAllowedOrigins().isEmpty()) {
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }

        boolean trusted = jwtProperties.getAllowedOrigins().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(origin));

        if (!trusted) {
            throw new IllegalArgumentException("Untrusted origin");
        }
    }
}
