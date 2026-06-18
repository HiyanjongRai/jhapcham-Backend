package com.example.jhapcham.security;

import com.example.jhapcham.Error.AuthenticationException;
import com.example.jhapcham.user.dto.AuthResponseDTO;
import com.example.jhapcham.user.domain.User;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final SecureRandom REFRESH_RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    public IssuedTokens issueAuthResponse(User user, String message) {
        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken refreshToken = createRefreshToken(user, RefreshToken.newFamilyId());

        return new IssuedTokens(buildResponse(user, message, accessToken), refreshToken.rawToken());
    }

    @Transactional
    public RotatedTokens rotateRefreshToken(String rawRefreshToken) {
        RefreshToken existing = requireActiveRefreshToken(rawRefreshToken);

        if (existing.isRevoked()) {
            revokeTokenFamily(existing.getTokenFamily());
            throw new AuthenticationException("Refresh token reuse detected. Please login again.");
        }

        if (existing.getExpiryDate().isBefore(LocalDateTime.now())) {
            existing.setRevoked(true);
            existing.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(existing);
            throw new AuthenticationException("Refresh token has expired. Please login again.");
        }

        User user = existing.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken replacement = createRefreshToken(user, existing.getTokenFamily());

        existing.setRevoked(true);
        existing.setRevokedAt(LocalDateTime.now());
        existing.setReplacedByTokenHash(replacement.refreshToken().getTokenHash());
        refreshTokenRepository.save(existing);

        return new RotatedTokens(buildResponse(user, "Token refreshed", accessToken), replacement.rawToken());
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.findAllByUser_IdAndRevokedFalse(userId).forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }

    private RefreshToken requireActiveRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is missing");
        }

        RefreshToken token = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new AuthenticationException("Refresh token is invalid"));

        if (token.isRevoked()) {
            revokeTokenFamily(token.getTokenFamily());
            throw new AuthenticationException("Refresh token reuse detected. Please login again.");
        }

        return token;
    }

    private void revokeTokenFamily(String tokenFamily) {
        refreshTokenRepository.findAllByTokenFamily(tokenFamily).forEach(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    private IssuedRefreshToken createRefreshToken(User user, String familyId) {
        String rawToken = generateOpaqueRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiryDate(LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenTtlDays()))
                .revoked(false)
                .tokenFamily(familyId)
                .build();
        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(refreshToken, rawToken);
    }

    private AuthResponseDTO buildResponse(User user, String message, String accessToken) {
        return new AuthResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                message,
                accessToken,
                accessToken,
                "Bearer",
                jwtService.getAccessTokenTtlSeconds());
    }

    private String generateOpaqueRefreshToken() {
        byte[] randomBytes = new byte[64];
        REFRESH_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record IssuedTokens(AuthResponseDTO authResponse, String rawRefreshToken) {
    }

    public record RotatedTokens(AuthResponseDTO authResponse, String rawRefreshToken) {
    }

    private record IssuedRefreshToken(RefreshToken refreshToken, String rawToken) {
    }
}
