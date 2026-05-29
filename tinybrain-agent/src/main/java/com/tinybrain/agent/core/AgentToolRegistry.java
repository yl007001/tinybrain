package com.tinybrain.agent.core;

import jakarta.annotation.PostConstruct;
import com.tinybrain.agent.plugin.AgentTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Agent 工具注册器
 * <p>
 * 启动时自动扫描所有 AgentTool 实现并注册到引擎。
 * 使用 Spring IoC 容器自动注入，无需手动注册。
 * <p>
 * 扩展方式：新增 AgentTool 实现类 + @Component 即可自动注册。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentToolRegistry {

    private final ApplicationContext applicationContext;
    private final AgentEngine agentEngine;

    @PostConstruct
    public void init() {
        // 从 Spring 容器获取所有 AgentTool 实现
        Map<String, AgentTool> tools = applicationContext.getBeansOfType(AgentTool.class);
        tools.values().forEach(agentEngine::registerTool);
        log.info("Agent 工具注册完成，共 {} 个工具", tools.size());
    }
}
