package com.tinybrain.agent.controller;

import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.agent.service.AgentService;
import com.tinybrain.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 控制器
 * <p>
 * 提供智能体对话、工具查询、会话管理接口。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentEngine agentEngine;
    private final AgentService agentService;

    /**
     * Agent 对话
     */
    @PostMapping("/chat")
    public R<AgentResponse> chat(@Valid @RequestBody AgentRequest request) {
        AgentResponse response = agentService.process(request);
        return R.ok(response);
    }

    /**
     * 获取所有已注册的工具
     */
    @GetMapping("/tools")
    public R<Map<String, String>> listTools() {
        Map<String, String> tools = agentEngine.getTools().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getDescription()
                ));
        return R.ok(tools);
    }

    /**
     * 清除会话历史
     */
    @DeleteMapping("/session/{sessionId}")
    public R<Void> clearSession(@PathVariable String sessionId) {
        agentService.clearSession(sessionId);
        return R.ok("会话已清除");
    }
}
