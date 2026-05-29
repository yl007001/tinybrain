package com.tinybrain.user.config;

import com.tinybrain.common.constant.CommonConstant;
import com.tinybrain.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * <p>
 * 继承 OncePerRequestFilter，确保每个请求只执行一次过滤。
 * 从 Authorization 头提取 Token → 解析 → 设置 SecurityContext。
 * <p>
 * 放过路径：登录、注册、健康检查等不需要认证的接口。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 不需要认证的路径 */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/h2-console"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 白名单路径直接放行
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 从请求头提取 Token
        String authHeader = request.getHeader(CommonConstant.TOKEN_HEADER);
        String token = JwtUtil.extractToken(authHeader);

        if (StringUtils.hasText(token)) {
            Long userId = JwtUtil.getUserId(token);
            String role = JwtUtil.getRole(token);

            if (userId != null && role != null) {
                // 创建认证对象并设置到 SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority(role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
