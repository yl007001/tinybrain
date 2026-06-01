package com.tinybrain.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;    // system / user / assistant / tool
        private String content;

        /** tool_calls 字段（assistant 消息调用工具时使用） */
        @JsonProperty("tool_calls")
        private List<Map<String, Object>> toolCalls;

        /** tool_call_id 字段（tool 消息关联工具调用时使用） */
        @JsonProperty("tool_call_id")
        private String toolCallId;

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
