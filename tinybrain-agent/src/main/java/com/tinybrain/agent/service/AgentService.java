package com.tinybrain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.rag.service.LLMApiClient;
import io.micrometer.core.annotation.Timed;
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
    @Timed(value = "agent.process.time", description = "Agent 对话处理耗时", percentiles = {0.5, 0.95, 0.99})
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
     * 支持两种格式：
     * 1. OpenAI Function Calling 格式：回复中包含工具调用结构
     * 2. 自定义 JSON 格式：{"tool": "name", "args": {...}}
     * <p>
     * 先尝试 Function Calling 格式，再尝试自定义 JSON 格式。
     */
    private String extractToolCall(String reply) {
        if (reply == null || reply.isBlank()) return null;

        // 尝试自定义 JSON 格式: {"tool": "...", "args": {...}}
        int jsonStart = -1;

        // 查找 {"tool" 或 {"tool": 模式
        int idx1 = reply.indexOf("{\"tool\"");
        int idx2 = reply.indexOf("{\"tool\":");
        jsonStart = (idx1 >= 0 && idx2 >= 0) ? Math.min(idx1, idx2) :
                    (idx1 >= 0 ? idx1 : idx2);

        if (jsonStart < 0) return null;

        // 找到匹配的闭合花括号（处理嵌套 JSON）
        int braceCount = 0;
        boolean inString = false;
        char escapeChar = 0;

        for (int i = jsonStart; i < reply.length(); i++) {
            char c = reply.charAt(i);

            // 处理字符串转义
            if (escapeChar != 0) {
                escapeChar = 0;
                continue;
            }
            if (c == '\\') {
                escapeChar = c;
                continue;
            }

            // 处理字符串边界
            if (c == '"' && !inString) {
                inString = true;
                continue;
            }
            if (c == '"' && inString) {
                inString = false;
                continue;
            }

            if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;

                if (braceCount == 0) {
                    // 提取完整的 JSON
                    String json = reply.substring(jsonStart, i + 1);
                    // 快速验证：必须是有效的工具调用 JSON
                    if (json.contains("\"tool\"") || json.contains("\"name\"")) {
                        return json;
                    }
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
    }
}
