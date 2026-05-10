package com.example.jhapcham.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secret;
    private String issuer = "jhapcham";
    private long accessTokenTtlMinutes = 15;
    private long refreshTokenTtlDays = 14;
    private String refreshCookieName = "refresh_token";
    private String refreshCookiePath = "/api/auth";
    private boolean refreshCookieSecure = true;
    private String refreshCookieSameSite = "Strict";
    private List<String> allowedOrigins = new ArrayList<>();
}
