package com.siddharth.tradesim_backend.auth.service;

import com.siddharth.tradesim_backend.auth.AuthException;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.RefreshToken;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(now.plusMillis(refreshExpiration))
                .build());

        return rawToken;
    }

    @Transactional
    public RefreshTokenRotation rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.unauthorized("Refresh token is missing.");
        }

        Instant now = Instant.now();
        String currentHash = hash(rawRefreshToken);
        RefreshToken current = refreshTokenRepository.findByTokenHash(currentHash).orElseThrow(() -> AuthException.unauthorized("Invalid refresh token."));

        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeActiveTokensForUser(current.getUser().getId(), now);
            throw AuthException.unauthorized("Invalid refresh token.");
        }

        if (!current.isActive(now)) {
            throw AuthException.unauthorized("Invalid or expired refresh token.");
        }

        User user = current.getUser();
        if (user.getAccountStatus() == AccountStatus.SUSPENDED || user.getAccountStatus() == AccountStatus.BANNED) {
            current.setRevokedAt(now);
            refreshTokenRepository.save(current);
            throw AuthException.forbidden("Cannot refresh token, your account is " + user.getAccountStatus());
        }

        String newRawToken = generateRawToken();
        String newHash = hash(newRawToken);

        current.setRevokedAt(now);
        current.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(current);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .expiresAt(now.plusMillis(refreshExpiration))
                .build());

        return new RefreshTokenRotation(newRawToken, user);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(refreshToken -> {
            if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(Instant.now());
                refreshTokenRepository.save(refreshToken);
            }
        });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}