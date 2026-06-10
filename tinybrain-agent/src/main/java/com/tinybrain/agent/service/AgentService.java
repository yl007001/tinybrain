package com.tinybrain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.agent.dto.SkillInfo;
import com.tinybrain.common.entity.Message;
import com.tinybrain.common.entity.Session;
import com.tinybrain.rag.service.LLMApiClient;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent 对话服务
 * <p>
 * 将 Agent 引擎与 LLM API 串联，实现完整的 Function Calling 循环：
 * 用户输入 → LLM 判断 → 调用工具 → 结果回填 → LLM 生成 → 返回
 * <p>
 * ：
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
    private final SkillService skillService;
    private final SessionService sessionService;

    /**
     * 处理 Agent 对话
     */
    @Timed(value = "agent.process.time", description = "Agent 对话处理耗时", percentiles = {0.5, 0.95, 0.99})
    public AgentResponse process(AgentRequest request) {
        // 获取当前用户ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 如果 sessionId 为空，创建新会话
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            Session session = sessionService.createSession(userId, request.getMessage());
            sessionId = session.getSessionId();
        }

        AgentResponse response = new AgentResponse();
        List<AgentResponse.ToolCall> toolCalls = new ArrayList<>();
        response.setToolCalls(toolCalls);
        response.setSessionId(sessionId);

        // 从数据库加载历史消息（最近20条）构建上下文
        List<Message> recentMessages = sessionService.getRecentMessages(sessionId, 20);
        List<Map<String, Object>> history = new ArrayList<>();
        for (Message msg : recentMessages) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            if (msg.getToolCalls() != null && !msg.getToolCalls().isBlank()) {
                try {
                    m.put("tool_calls", mapper.readValue(msg.getToolCalls(), List.class));
                } catch (Exception ignored) {
                }
            }
            history.add(m);
        }

        // 构建 System Prompt（含工具描述）
        String systemPrompt = agentEngine.buildSystemPrompt();
        if (!request.getConfig().getSystemPromptSuffix().isEmpty()) {
            systemPrompt += "\n" + request.getConfig().getSystemPromptSuffix();
        }

        // === Skill 集成：手动 /skill 命令 + 自动触发匹配 ===
        String userMessage = request.getMessage();
        SkillInfo matchedSkill = null;
        String triggerType = null;

        // 1. 检查 /skill-name 手动调用
        if (userMessage.startsWith("/")) {
            matchedSkill = matchSkillByCommand(userMessage);
            if (matchedSkill != null) {
                triggerType = "manual";
                // 移除 /skill-name 前缀，保留实际问题
                String actualMessage = stripSkillCommand(userMessage);
                if (!actualMessage.isBlank()) {
                    request.setMessage(actualMessage);
                    userMessage = actualMessage;
                }
                log.info("用户手动调用 Skill: {} (/{})", matchedSkill.getName(), matchedSkill.getName());
            }
        }

        // 2. 自动触发匹配（仅在未手动指定时）
        if (matchedSkill == null) {
            matchedSkill = matchSkillByTriggers(userMessage);
            if (matchedSkill != null) {
                triggerType = "auto";
                log.info("Skill 自动触发: {} (匹配消息: {})", matchedSkill.getName(), userMessage);
            }
        }

        // 3. 注入 Skill 上下文到 System Prompt
        if (matchedSkill != null) {
            systemPrompt += "\n" + buildSkillContext(matchedSkill);
        }

        // 4. 设置响应中的 Skill 匹配信息
        if (matchedSkill != null) {
            AgentResponse.SkillMatch sm = new AgentResponse.SkillMatch();
            sm.setId(matchedSkill.getId());
            sm.setName(matchedSkill.getName());
            sm.setDescription(matchedSkill.getDescription());
            sm.setTriggerType(triggerType);
            response.setMatchedSkill(sm);
        }

        // 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        // Function Calling 循环
        int iterations = 0;
        Map<String, Object> lastAssistantMsg = null;

        while (iterations < request.getConfig().getMaxIterations()) {
            iterations++;

            // 调用 LLM
            com.tinybrain.rag.dto.LLMChatRequest llmRequest = new com.tinybrain.rag.dto.LLMChatRequest();
            llmRequest.setMessages(messages.stream()
                    .map(m -> {
                        var msg = new com.tinybrain.rag.dto.LLMChatRequest.Message();
                        msg.setRole((String) m.get("role"));
                        msg.setContent((String) m.get("content"));
                        // 处理 tool_calls 字段
                        if (m.containsKey("tool_calls")) {
                            msg.setToolCalls((List<Map<String, Object>>) m.get("tool_calls"));
                        }
                        // 处理 tool_call_id 字段
                        if (m.containsKey("tool_call_id")) {
                            msg.setToolCallId((String) m.get("tool_call_id"));
                        }
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

                // 生成 tool_call_id（用于关联工具调用和结果）
                String toolCallId = "call_" + System.currentTimeMillis();

                // 将 assistant 消息（含 tool_calls）加入消息列表
                Map<String, Object> assistantMsg = new java.util.HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", reply);
                assistantMsg.put("tool_calls", List.of(Map.of(
                        "id", toolCallId,
                        "type", "function",
                        "function", Map.of("name", toolName, "arguments", argsJson)
                )));
                messages.add(assistantMsg);

                // 将工具结果加入消息列表（需要 tool_call_id 关联）
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCallId,
                        "content", result
                ));

            } catch (Exception e) {
                log.warn("工具调用解析失败: {}", e.getMessage());
                break;
            }
        }

        // 最终回复
        String finalReply = (lastAssistantMsg != null) ? (String) lastAssistantMsg.get("content") : "处理完成";
        response.setReply(finalReply);
        response.setIterations(iterations);

        // 保存用户消息到数据库
        sessionService.saveMessage(sessionId, userId, "user", request.getMessage(), null);

        // 保存 assistant 回复到数据库（含 tool_calls JSON）
        String toolCallsJson = null;
        if (!toolCalls.isEmpty()) {
            try {
                toolCallsJson = mapper.writeValueAsString(toolCalls);
            } catch (Exception ignored) {
            }
        }
        sessionService.saveMessage(sessionId, userId, "assistant", finalReply, toolCallsJson);

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
     * 清除会话历史（同时清除数据库记录）
     */
    public void clearSession(String sessionId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        sessionService.deleteSession(sessionId, userId);
    }

    // ========== Skill 集成方法 ==========

    /**
     * 通过 /skill-name 命令匹配 Skill
     * 支持格式：/skill-name 或 /skill-name 实际问题内容
     */
    private SkillInfo matchSkillByCommand(String message) {
        // 提取 / 后面的第一个词作为 skill name
        String trimmed = message.substring(1).trim(); // 去掉 /
        String skillName;
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx > 0) {
            skillName = trimmed.substring(0, spaceIdx);
        } else {
            skillName = trimmed;
        }

        if (skillName.isBlank()) return null;

        // 精确匹配 skill name（不区分大小写）
        for (SkillInfo skill : skillService.listSkills()) {
            if (skill.isEnabled() && skill.getName().equalsIgnoreCase(skillName)) {
                return skill;
            }
        }
        return null;
    }

    /**
     * 移除消息中的 /skill-name 前缀，返回实际问题
     */
    private String stripSkillCommand(String message) {
        String trimmed = message.substring(1).trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx > 0) {
            return trimmed.substring(spaceIdx + 1).trim();
        }
        // 只有 /skill-name 没有后续内容，返回空（让 LLM 根据 skill 上下文自行发挥）
        return "";
    }

    /**
     * 通过触发条件自动匹配 Skill
     * 遍历所有已启用的 Skill，检查 triggers 中的关键词是否出现在用户消息中
     */
    private SkillInfo matchSkillByTriggers(String message) {
        String lowerMessage = message.toLowerCase();
        SkillInfo bestMatch = null;
        int bestPriority = Integer.MIN_VALUE;

        for (SkillInfo skill : skillService.listSkills()) {
            if (!skill.isEnabled() || skill.getTriggers() == null || skill.getTriggers().isEmpty()) {
                continue;
            }

            for (String trigger : skill.getTriggers()) {
                if (trigger != null && lowerMessage.contains(trigger.toLowerCase().trim())) {
                    // 匹配到触发条件，取优先级最高的
                    if (skill.getPriority() > bestPriority) {
                        bestPriority = skill.getPriority();
                        bestMatch = skill;
                    }
                    break; // 一个 skill 只要有一个 trigger 匹配就够了
                }
            }
        }

        return bestMatch;
    }

    /**
     * 构建 Skill 上下文注入到 System Prompt
     * 告知 LLM 当前激活了哪个 Skill，以及应该使用哪个工具、如何使用
     */
    private String buildSkillContext(SkillInfo skill) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## [Active Skill: ").append(skill.getName()).append("]\n");
        sb.append("Description: ").append(skill.getDescription()).append("\n");

        if (skill.getToolName() != null && !skill.getToolName().isBlank()) {
            sb.append("Primary Tool: ").append(skill.getToolName()).append("\n");
        }

        if (skill.getToolDescription() != null && !skill.getToolDescription().isBlank()) {
            sb.append("Tool Usage: ").append(skill.getToolDescription()).append("\n");
        }

        if (skill.getParametersSchema() != null && !skill.getParametersSchema().isBlank()) {
            sb.append("Parameters: ").append(skill.getParametersSchema()).append("\n");
        }

        if (skill.getTags() != null && !skill.getTags().isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", skill.getTags())).append("\n");
        }

        sb.append("\nInstruction: You are currently operating under the \"")
          .append(skill.getName()).append("\" skill. ")
          .append("Focus your response on this skill's domain. ");

        if (skill.getToolName() != null && !skill.getToolName().isBlank()) {
            sb.append("Use the \"").append(skill.getToolName())
              .append("\" tool when appropriate to fulfill the user's request within this skill's scope.\n");
        }

        return sb.toString();
    }
}
