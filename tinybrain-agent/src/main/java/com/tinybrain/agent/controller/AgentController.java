package com.tinybrain.agent.controller;

import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.agent.service.AgentService;
import com.tinybrain.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 控制器
 * <p>
 * 提供智能体对话、工具查询、会话管理接口。
 */
@Tag(name = "04-Agent 智能体", description = "AI Agent 自主对话、Function Calling 工具调用、会话管理")
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentEngine agentEngine;
    private final AgentService agentService;

    @Operation(summary = "Agent 对话", description = "向 Agent 发送消息，Agent 自动判断是否需要调用工具（知识检索、获取时间等）")
    @PostMapping("/chat")
    public R<AgentResponse> chat(@Valid @RequestBody AgentRequest request) {
        AgentResponse response = agentService.process(request);
        return R.ok(response);
    }

    @Operation(summary = "获取已注册工具列表", description = "查看当前 Agent 可用的所有工具及其描述")
    @GetMapping("/tools")
    public R<Map<String, String>> listTools() {
        Map<String, String> tools = agentEngine.getTools().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getDescription()
                ));
        return R.ok(tools);
    }

    @Operation(summary = "清除会话历史", description = "删除指定 sessionId 的对话历史记录")
    @DeleteMapping("/session/{sessionId}")
    public R<Void> clearSession(@PathVariable String sessionId) {
        agentService.clearSession(sessionId);
        return R.okMsg("会话已清除");
    }
}
