package com.tinybrain.common.util;

import com.tinybrain.common.constant.CommonConstant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具类单元测试
 */
class JwtUtilTest {

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = JwtUtil.generateToken(1L, "ROLE_USER");

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT 由三部分组成
    }

    @Test
    void parseToken_shouldExtractUserId() {
        String token = JwtUtil.generateToken(42L, "ROLE_ADMIN");

        Long userId = JwtUtil.getUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    void parseToken_shouldExtractRole() {
        String token = JwtUtil.generateToken(1L, "ROLE_ADMIN");

        String role = JwtUtil.getRole(token);
        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = JwtUtil.generateToken(1L, "ROLE_USER");

        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(JwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_shouldReturnFalseForNull() {
        assertFalse(JwtUtil.validateToken(null));
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyString() {
        assertFalse(JwtUtil.validateToken(""));
    }

    @Test
    void extractToken_shouldStripBearerPrefix() {
        String result = JwtUtil.extractToken("Bearer my-token-value");

        assertEquals("my-token-value", result);
    }

    @Test
    void extractToken_shouldReturnNullIfNoBearer() {
        assertNull(JwtUtil.extractToken("plain-token"));
    }

    @Test
    void extractToken_shouldReturnNullIfNull() {
        assertNull(JwtUtil.extractToken(null));
    }

    @Test
    void generateAndParse_shouldBeConsistent() {
        String token = JwtUtil.generateToken(99L, "ROLE_USER");

        assertEquals(99L, JwtUtil.getUserId(token));
        assertEquals("ROLE_USER", JwtUtil.getRole(token));
        assertTrue(JwtUtil.validateToken(token));
    }
}
