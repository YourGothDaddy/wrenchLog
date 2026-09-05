package com.wrenchlog.wrenchlog.security;

import com.wrenchlog.wrenchlog.model.RefreshToken;
import com.wrenchlog.wrenchlog.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issueToken(Long userId) {
        String rawToken = generateRawToken();
        String hash = hash(rawToken);

        RefreshToken entity = new RefreshToken(
                userId,
                hash,
                Instant.now().plusMillis(refreshExpirationMs)
        );
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    public RotationResult rotate(String rawToken) {
        String hash = hash(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new SecurityException("Unknown refresh token"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(existing.getUserId());
            throw new SecurityException("Refresh token reuse detected");
        }

        if (!existing.isValid()) {
            throw new SecurityException("Refresh token expired");
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        String newRawToken = issueToken(existing.getUserId());
        return new RotationResult(existing.getUserId(), newRawToken);
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(Long userId, String rawToken) {}
}