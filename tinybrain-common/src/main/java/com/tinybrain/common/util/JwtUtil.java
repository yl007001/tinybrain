package com.tinybrain.common.util;

import com.tinybrain.common.constant.CommonConstant;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 使用 JJWT 0.12.x 实现，支持：
 * - Token 签发（含用户ID + 角色）
 * - Token 解析与校验
 * - 密钥安全生成（HMAC-SHA256）
 * - 密钥从环境变量 TINYBRAIN_JWT_SECRET 读取
 * <p>
 * 面试重点：
 * 1. JWT 结构：Header.Payload.Signature
 * 2. 对称签名 vs 非对称签名（HMAC-SHA256 vs RSA/ECDSA）
 * 3. Token 刷新与过期策略
 * 4. 密钥管理：环境变量 / 配置中心 / KMS
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** 环境变量名称 */
    private static final String ENV_SECRET_KEY = "TINYBRAIN_JWT_SECRET";

    /** 密钥（至少256位，HMAC-SHA256要求） */
    private static String SECRET;

    /** 过期时间：7天（毫秒） */
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private static SecretKey KEY;

    private static volatile boolean initialized = false;

    private JwtUtil() {}

    /**
     * 初始化密钥（由 Spring 配置类在启动时调用）
     */
    public static synchronized void initialize() {
        if (initialized) return;
        String envSecret = System.getenv(ENV_SECRET_KEY);
        if (envSecret != null && !envSecret.isBlank()) {
            SECRET = envSecret;
            log.info("JWT 密钥已从环境变量 {} 加载", ENV_SECRET_KEY);
        } else {
            SECRET = Base64.getEncoder().encodeToString(
                    "TinyBrain-Default-Secret-For-Dev-Only-Please-Set-ENV-In-Production!".getBytes(StandardCharsets.UTF_8)
            );
            log.warn("========== JWT 密钥安全警告 ==========");
            log.warn("环境变量 {} 未设置，使用开发默认密钥！", ENV_SECRET_KEY);
            log.warn("生产环境请务必设置该环境变量！");
            log.warn("======================================");
        }
        KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        initialized = true;
    }

    /**
     * 确保已初始化，若未初始化则使用默认值
     */
    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * 生成 JWT Token
     */
    public static String generateToken(Long userId, String role) {
        ensureInitialized();
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
     */
    public static Claims parseToken(String token) {
        ensureInitialized();
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
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return authHeader.substring(CommonConstant.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
