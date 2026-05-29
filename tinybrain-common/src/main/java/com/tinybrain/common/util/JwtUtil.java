package com.tinybrain.common.util;

import com.tinybrain.common.constant.CommonConstant;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 使用 JJWT 0.12.x 实现，支持：
 * - Token 签发（含用户ID + 角色）
 * - Token 解析与校验
 * - 密钥安全生成（HMAC-SHA256）
 * <p>
 * 注意：生产环境中密钥应从配置中心或环境变量读取，不硬编码。
 */
@Slf4j
public class JwtUtil {

    /** 密钥（至少256位，HMAC-SHA256要求）— TODO: 迁移到配置中心 */
    private static final String SECRET = "TinyBrain2026SecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong!";

    /** 过期时间：7天（毫秒） */
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {}

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @param role   用户角色
     * @return JWT token 字符串
     */
    public static String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 Token，获取 Claims
     *
     * @param token JWT token
     * @return Claims，解析失败返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期");
        } catch (JwtException e) {
            log.warn("Token 解析失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 Token 中提取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.parseLong(claims.getSubject()) : null;
    }

    /**
     * 从 Token 中提取角色
     */
    public static String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * 校验 Token 是否有效
     */
    public static boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return parseToken(token) != null;
    }

    /**
     * 从请求头中提取 Bearer Token
     *
     * @param authHeader Authorization 请求头值
     * @return 纯净的 token 字符串，无 "Bearer " 前缀
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return authHeader.substring(CommonConstant.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
