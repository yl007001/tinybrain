package com.tinybrain.app;

import com.tinybrain.rag.vector.VectorStoreWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义健康指标：TinyBrain 核心组件状态 (v2 Spring AI 版)
 *
 * <p>暴露 {@code /actuator/health} 端点中的自定义健康检查。
 * 包括：
 * <ul>
 *   <li>VectorStore — Spring AI 向量库状态和大小</li>
 *   <li>Agent 工具注册数（未来扩展）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TinyBrainHealthIndicator implements HealthIndicator {

    private final VectorStoreWrapper vectorStore;

    @Override
    public Health health() {
        int vectorCount = vectorStore.size();

        return Health.up()
                .withDetail("vectorStore", vectorCount + " vectors indexed")
                .withDetail("version", "1.0.0")
                .withDetail("profile",
                        System.getProperty("spring.profiles.active", "default"))
                .build();
    }
}
