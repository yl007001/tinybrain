package com.tinybrain.agent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent 对话请求
 */
@Data
public class AgentRequest {

    /** 用户输入 */
    private String message;

    /** 会话ID（用于多轮对话记忆） */
    private String sessionId;

    /** Agent 配置 */
    private Config config = new Config();

    @Data
    public static class Config {
        /** 最大工具调用轮数 */
        private int maxIterations = 5;

        /** 系统提示词后缀 */
        private String systemPromptSuffix = "";
    }
}
