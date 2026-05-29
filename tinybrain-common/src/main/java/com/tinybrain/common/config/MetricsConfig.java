package com.tinybrain.common.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 指标配置
 *
 * <p>启用 {@link io.micrometer.core.annotation.Timed} 和 {@link io.micrometer.core.annotation.Counted}
 * 注解的 AOP 支持。
 *
 * <p>用法示例：
 * <pre>{@code
 * @Timed(value = "rag.ask.time", description = "RAG 问答耗时")
 * public RAGResult ask(String question, int topK) { ... }
 *
 * @Counted(value = "rag.index.count", description = "文档索引次数")
 * public void indexDocument(Long documentId) { ... }
 * }</pre>
 *
 * <p>指标自动暴露到 {@code /actuator/prometheus} 端点，
 * 可被 Prometheus 采集并在 Grafana 中可视化。
 */
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}
