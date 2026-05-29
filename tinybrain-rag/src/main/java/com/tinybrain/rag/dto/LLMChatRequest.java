package com.tinybrain.rag.dto;

import lombok.Data;

import java.util.List;

/**
 * LLM API 统一请求体
 * <p>
 * 兼容 OpenAI / DeepSeek 格式的 Chat API。
 */
@Data
public class LLMChatRequest {

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 消息列表 */
    private List<Message> messages;

    /** 温度 (0-2)，越低越确定 */
    private Double temperature = 0.7;

    /** 最大输出 Token 数 */
    private Integer maxTokens = 2048;

    @Data
    public static class Message {
        private String role;    // system / user / assistant
        private String content;

        public static Message system(String content) {
            Message msg = new Message();
            msg.role = "system";
            msg.content = content;
            return msg;
        }

        public static Message user(String content) {
            Message msg = new Message();
            msg.role = "user";
            msg.content = content;
            return msg;
        }

        public static Message assistant(String content) {
            Message msg = new Message();
            msg.role = "assistant";
            msg.content = content;
            return msg;
        }
    }
}
