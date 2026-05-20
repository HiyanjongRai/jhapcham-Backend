package com.example.jhapcham.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

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
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (jwtProperties.getAllowedOrigins() == null || jwtProperties.getAllowedOrigins().isEmpty()) {
            throw new IllegalArgumentException("Trusted origins are not configured");
        }

        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if (fetchSite != null) {
            String normalizedFetchSite = fetchSite.trim().toLowerCase(Locale.ROOT);
            if ("cross-site".equals(normalizedFetchSite)) {
                throw new IllegalArgumentException("Cross-site auth request rejected");
            }
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin header is required");
        }

        boolean trusted = jwtProperties.getAllowedOrigins().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(origin));

        if (!trusted) {
            throw new IllegalArgumentException("Untrusted origin");
        }
    }
}
