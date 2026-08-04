package com.wrenchlog.wrenchlog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKeyString;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, List<String> roles) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("roles", roles);

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateFileDownloadToken(Long fileId, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "file-download");
        claims.put("fileId", fileId);
        claims.put("userId", userId);

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject("download")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60_000))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateAndExtractDownloadToken(String token, Long expectedFileId) {
        Claims claims = parseClaims(token);

        if (!"file-download".equals(claims.get("scope"))
                || !expectedFileId.equals(((Number) claims.get("fileId")).longValue())) {
            throw new SecurityException("Invalid download token");
        }
        return claims;
    }
}