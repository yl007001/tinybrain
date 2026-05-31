package com.tinybrain.user.config;

import com.tinybrain.common.constant.CommonConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 * <p>
 * 核心设计：
 * - 无状态（JWT），不创建 Session
 * - JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
 * - RBAC 通过 @PreAuthorize 注解实现
 * - 密码使用 BCrypt 加密
 * <p>
 * ：
 * 1. SecurityFilterChain 是 Spring Security 的核心过滤器链配置
 * 2. SessionCreationPolicy.STATELESS 表示无状态认证
 * 3. BCryptPasswordEncoder 内置加盐，无需额外处理
 * 4. @EnableMethodSecurity 替代旧版 @EnableGlobalMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离，使用 Token 认证）
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话（不创建 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 请求权限配置
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // 静态资源
                        .requestMatchers(HttpMethod.GET, "/", "/*.html", "/favicon.ico").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )

                // 添加 JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前）
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // 禁用 Spring Security 默认的/等页面
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 允许 H2 Console 的 frame
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
