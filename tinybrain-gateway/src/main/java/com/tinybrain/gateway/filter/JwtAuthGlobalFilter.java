package com.tinybrain.gateway.filter;

import com.tinybrain.common.constant.CommonConstant;
import com.tinybrain.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关 JWT 鉴权全局过滤器
 * <p>
 * 在请求转发到微服务之前校验 Token 有效性，
 * 校验通过后将用户ID注入请求头，下游服务直接获取。
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单路径 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login", "/api/auth/register",
            "/actuator", "/h2-console"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        if (WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(CommonConstant.TOKEN_HEADER);
        String token = JwtUtil.extractToken(authHeader);

        if (token == null || !JwtUtil.validateToken(token)) {
            log.warn("网关鉴权失败: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 将用户ID注入请求头，传递给下游微服务
        Long userId = JwtUtil.getUserId(token);
        exchange = exchange.mutate()
                .request(r -> r.header(CommonConstant.USER_ID_HEADER, String.valueOf(userId)))
                .build();

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
