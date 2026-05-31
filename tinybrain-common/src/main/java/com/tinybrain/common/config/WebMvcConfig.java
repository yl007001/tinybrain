package com.tinybrain.common.config;

import com.tinybrain.common.constant.CommonConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户ID注入拦截器
 * <p>
 * 从 Spring SecurityContext 获取当前用户ID，注入到请求属性中，
 * 方便 Controller 直接通过 @RequestAttribute 获取。
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 全局 CORS 配置，允许前端跨域访问
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); // 开发环境允许所有来源
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                // 从 SecurityContext 获取用户ID（由 JwtAuthFilter 设置）
                Object principal = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication() != null ?
                        org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication().getPrincipal() : null;

                if (principal instanceof Long userId) {
                    request.setAttribute(CommonConstant.CURRENT_USER, userId);
                }
                return true;
            }
        });
    }
}
