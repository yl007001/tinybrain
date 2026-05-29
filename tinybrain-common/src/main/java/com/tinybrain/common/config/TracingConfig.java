package com.tinybrain.common.config;

import brave.sampler.Sampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式链路追踪配置
 *
 * <p>基于 Micrometer Tracing + Brave（Zipkin 客户端）实现。
 * 自动注入 traceId 到 MDC，由 Logback 输出到日志。
 *
 * <p>配置项（application.yml）：
 * <ul>
 *   <li>{@code management.tracing.sampling.probability} — 采样率（默认 1.0）</li>
 *   <li>{@code management.tracing.propagation.type} — 传播类型（w3c / b3）</li>
 * </ul>
 *
 * <p>可视化：Zipkin UI (http://localhost:9411) 或 Grafana Tempo
 */
@Configuration(proxyBeanMethods = false)
public class TracingConfig {

    /**
     * 自定义采样器：全量采样，便于开发和调试。
     * 生产环境建议通过 {@code management.tracing.sampling.probability} 配置。
     */
    @Bean
    public Sampler customSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }
}
