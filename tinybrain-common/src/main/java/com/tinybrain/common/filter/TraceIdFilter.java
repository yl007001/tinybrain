package com.tinybrain.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC 追踪 ID 过滤器
 *
 * <p>为每个 HTTP 请求注入以下 MDC 上下文：
 * <ul>
 *   <li><b>traceId</b> — 链路追踪 ID（若 Micrometer Tracing 已注入则复用，否则自动生成）</li>
 *   <li><b>requestId</b> — 单次请求唯一 ID</li>
 *   <li><b>userId</b> — 当前操作用户（由下游过滤器注入）</li>
 * </ul>
 *
 * <p>MDC 值自动出现在 Logback 日志中，配合 JSON 日志格式可在 ELK 中实现全链路检索。
 *
 * <p>注意：Micrometer Tracing 会自动注入 traceId 和 spanId 到 MDC。
 * 此过滤器作为补充，确保在非追踪场景下也有 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        // 如果 Micrometer Tracing 已注入 traceId，则复用
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put(TRACE_ID_KEY, traceId);
        }

        // 生成请求 ID
        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString().replace("-", "").substring(0, 8));

        // 从请求头获取用户 ID（网关已注入）
        if (request instanceof HttpServletRequest httpReq) {
            String userId = httpReq.getHeader("X-User-Id");
            if (userId != null) {
                MDC.put("userId", userId);
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // 请求结束清理 MDC，防止内存泄漏
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(REQUEST_ID_KEY);
            MDC.remove("userId");
        }
    }
}
