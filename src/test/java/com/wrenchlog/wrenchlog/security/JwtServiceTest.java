package com.wrenchlog.wrenchlog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "01234567890123456789012345678901234567890123456789012345678901234567890123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    void generateToken_andParseClaims_returnsCorrectSubjectAndClaims() {
        String token = jwtService.generateToken(1L, "alice", List.of("ROLE_USER"));

        Claims claims = jwtService.parseClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals(1, ((Number) claims.get("userId")).intValue());
    }

    @Test
    void parseClaims_throwsExpiredJwtException_whenTokenExpired() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(1L, "alice", List.of("ROLE_USER"));

        assertThrows(ExpiredJwtException.class, () -> jwtService.parseClaims(expiredToken));
    }

    @Test
    void parseClaims_throwsSignatureException_whenTokenTampered() {
        String token = jwtService.generateToken(1L, "alice", List.of("ROLE_USER"));
        String tamperedToken = token.substring(0, token.length() - 4) + "abcd";

        assertThrows(SignatureException.class, () -> jwtService.parseClaims(tamperedToken));
    }

    @Test
    void parseClaims_throwsMalformedJwtException_whenTokenMalformed() {
        assertThrows(MalformedJwtException.class, () -> jwtService.parseClaims("not-a-real-jwt"));
    }

    @Test
    void generateFileDownloadToken_andValidate_succeeds_whenScopeAndFileIdMatch() {
        String token = jwtService.generateFileDownloadToken(400L, 1L);

        Claims claims = jwtService.validateAndExtractDownloadToken(token, 400L);

        assertEquals("file-download", claims.get("scope"));
    }

    @Test
    void validateAndExtractDownloadToken_throwsSecurityException_whenFileIdMismatched() {
        String token = jwtService.generateFileDownloadToken(400L, 1L);

        assertThrows(SecurityException.class,
                () -> jwtService.validateAndExtractDownloadToken(token, 999L));
    }

    @Test
    void validateAndExtractDownloadToken_throwsSecurityException_whenScopeIsWrong() {
        String normalLoginToken = jwtService.generateToken(1L, "alice", List.of("ROLE_USER"));

        assertThrows(SecurityException.class,
                () -> jwtService.validateAndExtractDownloadToken(normalLoginToken, 1L));
    }
}