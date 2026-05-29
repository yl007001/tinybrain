package com.tinybrain.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

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
@ComponentScan(basePackages = {
        "com.tinybrain.app",
        "com.tinybrain.common",
        "com.tinybrain.user",
        "com.tinybrain.knowledge"
})
public class TinyBrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyBrainApplication.class, args);
    }
}
