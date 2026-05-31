package com.tinybrain.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * TinyBrain 应用启动入口
 * <p>
 * 组件扫描范围：
 * - com.tinybrain.app         — 本模块（启动类）
 * - com.tinybrain.common      — 公共模块（配置、异常处理等）
 * - com.tinybrain.user        — 用户模块
 * - com.tinybrain.knowledge   — 知识库模块
 * <p>
 * Spring Boot 3.2 + Java 17
 */
@SpringBootApplication
@EnableRetry
@EnableAsync
@EnableCaching
@MapperScan(basePackages = {
        "com.tinybrain.**.mapper"
})
@ComponentScan(basePackages = {
        "com.tinybrain.app",
        "com.tinybrain.common",
        "com.tinybrain.user",
        "com.tinybrain.knowledge",
        "com.tinybrain.rag",
        "com.tinybrain.agent"
})
public class TinyBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyBrainApplication.class, args);
    }
}
