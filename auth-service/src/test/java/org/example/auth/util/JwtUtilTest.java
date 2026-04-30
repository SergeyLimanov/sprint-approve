package org.example.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour
    private static final Long TEST_REFRESH_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", TEST_REFRESH_EXPIRATION);
    }

    @Test
    void testGenerateToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "DEVELOPER";

        // When
        String token = jwtUtil.generateToken(userId, email, role);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractEmail() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "DEVELOPER";
        String token = jwtUtil.generateToken(userId, email, role);

        // When
        String extractedEmail = jwtUtil.extractEmail(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    void testExtractUserId() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "DEVELOPER";
        String token = jwtUtil.generateToken(userId, email, role);

        // When
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testExtractRole() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "TEAM_LEAD";
        String token = jwtUtil.generateToken(userId, email, role);

        // When
        String extractedRole = jwtUtil.extractRole(token);

        // Then
        assertEquals(role, extractedRole);
    }

    @Test
    void testValidateToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "DEVELOPER";
        String token = jwtUtil.generateToken(userId, email, role);

        // When
        Boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGenerateRefreshToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // When
        String refreshToken = jwtUtil.generateRefreshToken(userId, email);

        // Then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertEquals(email, jwtUtil.extractEmail(refreshToken));
        assertEquals(userId, jwtUtil.extractUserId(refreshToken));
    }

    @Test
    void testTokenExpiration() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "DEVELOPER";
        String token = jwtUtil.generateToken(userId, email, role);

        // When
        java.util.Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }
}
