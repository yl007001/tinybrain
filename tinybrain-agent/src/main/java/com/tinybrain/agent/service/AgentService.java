package com.tinybrain.agent.service;

import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.agent.tool.AgentTools;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 对话服务 — v2 Spring AI 版
 * <p>
 * 基于 Spring AI ChatClient 的 Function Calling 能力实现 Agent 循环。
 * 相比 v1-handcrafted 的手写 JSON 解析 + 循环调度，
 * v2 利用 Spring AI 的 ToolCallback 机制自动完成：
 * 1. Tool Schema 生成（从 @Tool 注解反射）
 * 2. 工具执行结果回填
 * 3. 多轮 Function Calling 自动循环
 * <p>
 * 面试重点：
 * 1. Spring AI Function Calling vs 手写 Function Calling 的优劣
 * 2. ToolCallback 注册和自动发现机制
 * 3. 会话记忆管理：上下文窗口大小控制
 */
@Slf4j
@Service
public class AgentService {

    private final ChatClient toolEnabledClient;
    private final AgentTools agentTools;

    /** 会话记忆存储（sessionId → 消息历史） */
    private final Map<String, List<String>> sessionMemory = new ConcurrentHashMap<>();

    public AgentService(ChatClient.Builder chatClientBuilder,
                        AgentTools agentTools) {
        this.agentTools = agentTools;

        // 构建带工具的 ChatClient
        // Spring AI 自动扫描 @Tool 注解并注册为 ToolCallback
        this.toolEnabledClient = chatClientBuilder
                .defaultSystem("""
                        你是 TinyBrain Agent，一个智能知识助手。
                        你拥有以下能力：
                        1. 知识库检索 — 搜索已存储的文档内容
                        2. 日期时间查询 — 获取当前日期和时间

                        请根据用户的问题选择合适的工具，如果工具不足请如实告知。
                        回答应当详尽、准确、有条理。
                        """)
                .defaultTools(agentTools)
                .build();

        log.info("AgentService 初始化完成 (Spring AI Function Calling)");
    }

    /**
     * 处理 Agent 对话
     */
    @Timed(value = "agent.process.time", description = "Agent 对话处理耗时", percentiles = {0.5, 0.95, 0.99})
    public AgentResponse process(AgentRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        AgentResponse response = new AgentResponse();
        response.setToolCalls(new ArrayList<>());

        // 获取会话历史
        List<String> history = sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // 构建用户消息（包含历史上下文）
        StringBuilder userMessage = new StringBuilder();
        if (!history.isEmpty()) {
            userMessage.append("以下是之前的对话：\n");
            for (String hist : history) {
                userMessage.append(hist).append("\n");
            }
            userMessage.append("\n---\n\n");
        }
        userMessage.append(request.getMessage());

        // 添加自定义 system prompt 后缀
        if (request.getConfig() != null
                && request.getConfig().getSystemPromptSuffix() != null
                && !request.getConfig().getSystemPromptSuffix().isEmpty()) {
            userMessage.append("\n\n额外指示：").append(request.getConfig().getSystemPromptSuffix());
        }

        // 调用 Spring AI ChatClient（自动处理 Function Calling）
        String reply;
        try {
            reply = toolEnabledClient.prompt()
                    .user(userMessage.toString())
                    .call()
                    .content();

            response.setReply(reply != null ? reply : "Agent 处理完成，但未生成回复。");

            // 记录工具调用信息（从 ChatClient 响应中提取）
            // Spring AI 1.0.0 的 ChatClient.call().chatResponse() 可获取 tool call 详情
            var chatResponse = toolEnabledClient.prompt()
                    .user(userMessage.toString())
                    .call()
                    .chatResponse();

            if (chatResponse != null && chatResponse.getResult() != null) {
                var output = chatResponse.getResult().getOutput();
                if (output != null && output.getToolCalls() != null) {
                    output.getToolCalls().forEach(tc -> {
                        AgentResponse.ToolCall toolCall = new AgentResponse.ToolCall();
                        toolCall.setToolName(tc.getName());
                        toolCall.setArgs(tc.getArguments());
                        toolCall.setResult("已执行");
                        response.getToolCalls().add(toolCall);
                    });
                }
            }

            response.setIterations(Math.min(response.getToolCalls().size() + 1, request.getConfig().getMaxIterations()));

        } catch (Exception e) {
            log.error("Agent 对话处理失败: {}", e.getMessage());
            response.setReply("抱歉，处理您的请求时出现错误: " + e.getMessage());
            response.setIterations(1);
        }

        // 更新会话记忆
        history.add("用户: " + request.getMessage());
        history.add("助手: " + response.getReply());
        // 保留最近 10 轮对话
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        return response;
    }

    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        log.info("清除会话: {}", sessionId);
    }
}
