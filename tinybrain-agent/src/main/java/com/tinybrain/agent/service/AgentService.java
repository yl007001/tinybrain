package com.tinybrain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.rag.service.LLMApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 对话服务
 * <p>
 * 将 Agent 引擎与 LLM API 串联，实现完整的 Function Calling 循环：
 * 用户输入 → LLM 判断 → 调用工具 → 结果回填 → LLM 生成 → 返回
 * <p>
 * 面试重点：
 * 1. ReAct 模式：Reason + Act 交替循环
 * 2. 循环终止条件：达到最大轮数 / LLM 不再调用工具
 * 3. 记忆管理：多轮对话上下文的拼接
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentEngine agentEngine;
    private final LLMApiClient llmClient;
    private final ObjectMapper mapper;

    /** 会话记忆存储（sessionId → 消息历史） */
    private final Map<String, List<Map<String, String>>> sessionMemory = new ConcurrentHashMap<>();

    /**
     * 处理 Agent 对话
     */
    public AgentResponse process(AgentRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        AgentResponse response = new AgentResponse();
        List<AgentResponse.ToolCall> toolCalls = new ArrayList<>();
        response.setToolCalls(toolCalls);

        // 获取或创建会话历史
        List<Map<String, String>> history = sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // 构建 System Prompt（含工具描述）
        String systemPrompt = agentEngine.buildSystemPrompt();
        if (!request.getConfig().getSystemPromptSuffix().isEmpty()) {
            systemPrompt += "\n" + request.getConfig().getSystemPromptSuffix();
        }

        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        // Function Calling 循环
        int iterations = 0;
        Map<String, String> lastAssistantMsg = null;

        while (iterations < request.getConfig().getMaxIterations()) {
            iterations++;

            // 调用 LLM
            com.tinybrain.rag.dto.LLMChatRequest llmRequest = new com.tinybrain.rag.dto.LLMChatRequest();
            llmRequest.setMessages(messages.stream()
                    .map(m -> {
                        var msg = new com.tinybrain.rag.dto.LLMChatRequest.Message();
                        msg.setRole(m.get("role"));
                        msg.setContent(m.get("content"));
                        return msg;
                    })
                    .toList());
            llmRequest.setTemperature(0.7);

            var llmResponse = llmClient.chat(llmRequest);
            if (llmResponse == null || llmResponse.getReplyText() == null) {
                break;
            }

            String reply = llmResponse.getReplyText();
            lastAssistantMsg = Map.of("role", "assistant", "content", reply);

            // 检查 LLM 是否要调用工具（检测 JSON 格式的工具调用）
            String toolCallJson = extractToolCall(reply);
            if (toolCallJson == null) {
                // LLM 直接回答了，不需要调用工具
                break;
            }

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> toolCall = mapper.readValue(toolCallJson, Map.class);
                String toolName = (String) toolCall.get("tool");
                String argsJson = mapper.writeValueAsString(toolCall.get("args"));

                // 记录工具调用
                AgentResponse.ToolCall tc = new AgentResponse.ToolCall();
                tc.setToolName(toolName);
                tc.setArgs(argsJson);
                toolCalls.add(tc);

                // 执行工具
                String result = agentEngine.executeTool(toolName, argsJson);
                tc.setResult(result);

                // 将工具调用结果加入消息列表
                messages.add(lastAssistantMsg);
                messages.add(Map.of("role", "tool", "content",
                        String.format("工具 %s 返回: %s", toolName, result)));

            } catch (Exception e) {
                log.warn("工具调用解析失败: {}", e.getMessage());
                break;
            }
        }

        // 最终回复
        String finalReply = (lastAssistantMsg != null) ? lastAssistantMsg.get("content") : "处理完成";
        response.setReply(finalReply);
        response.setIterations(iterations);

        // 保存到历史（只保存用户消息和最终回复）
        history.add(Map.of("role", "user", "content", request.getMessage()));
        history.add(Map.of("role", "assistant", "content", finalReply));
        // 限制历史长度（只保留最近 10 轮）
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        return response;
    }

    /**
     * 从 LLM 回复中提取工具调用 JSON
     * <p>
     * 简单实现：检测回复中是否包含 {"tool": ...} 格式的 JSON。
     * 生产环境建议使用 OpenAI Function Calling 的 structured output。
     */
    private String extractToolCall(String reply) {
        int start = reply.indexOf("{\"tool\"");
        if (start < 0) start = reply.indexOf("{\"tool\":");
        if (start < 0) return null;

        int end = reply.indexOf("}", start) + 1;
        // 处理嵌套 JSON
        int braceCount = 0;
        for (int i = start; i < reply.length(); i++) {
            if (reply.charAt(i) == '{') braceCount++;
            else if (reply.charAt(i) == '}') braceCount--;
            if (braceCount == 0) {
                end = i + 1;
                break;
            }
        }

        return reply.substring(start, end);
    }

    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
    }
}
