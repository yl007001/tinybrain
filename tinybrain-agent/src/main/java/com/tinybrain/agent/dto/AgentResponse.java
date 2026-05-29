package com.tinybrain.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * Agent 对话响应
 */
@Data
public class AgentResponse {

    /** 最终回复 */
    private String reply;

    /** 工具调用历史 */
    private List<ToolCall> toolCalls;

    /** 总轮数 */
    private int iterations;

    @Data
    public static class ToolCall {
        private String toolName;
        private String args;
        private String result;
    }
}
