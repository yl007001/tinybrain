package com.tinybrain.common.config;

import com.tinybrain.common.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置
 * <p>
 * 在应用启动时初始化 JWT 密钥。
 * 密钥优先从环境变量 TINYBRAIN_JWT_SECRET 读取，
 * 未设置时使用开发默认密钥（会打印警告）。
 */
@Slf4j
@Configuration
public class JwtConfig {

    @PostConstruct
    public void initJwt() {
        JwtUtil.initialize();
        log.info("JWT 工具类初始化完成");
    }
}
