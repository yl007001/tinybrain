package com.tinybrain.rag.service;

import com.tinybrain.common.exception.BusinessException;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM API 客户端 — v2 Spring AI Alibaba 版
 * <p>
 * 基于 Spring AI Alibaba 的 ChatClient + EmbeddingModel 实现，
 * 替代 v1 的 WebClient 直接调用方式。
 * <p>
 * 底层可切换多种大模型：
 * - 阿里云 DashScope (通义千问) — 默认
 * - OpenAI / DeepSeek — 通过配置切换
 * <p>
 * 面试重点：
 * 1. Spring AI 统一抽象：ChatClient / ChatModel / EmbeddingModel
 * 2. 自动配置原理：spring.factories → DashScopeAutoConfiguration
 * 3. 回退策略：配置多组 model 实现高可用
 */
@Slf4j
@Service
public class LLMApiClient {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    public LLMApiClient(ChatClient.Builder chatClientBuilder,
                        EmbeddingModel embeddingModel) {
        // 构建默认 ChatClient：带 system prompt 和基础配置
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个智能知识库助手 TinyBrain，请基于上下文提供准确、详细的回答。")
                .build();
        this.embeddingModel = embeddingModel;
        log.info("LLMApiClient 初始化完成 (Spring AI Alibaba)");
    }

    /**
     * Chat 对话：返回结构化响应
     */
    @Timed(value = "llm.chat.time", description = "LLM Chat 耗时", percentiles = {0.5, 0.95, 0.99})
    public String chat(String systemPrompt, String userMessage) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM Chat 调用失败: {}", e.getMessage());
            throw BusinessException.badRequest("LLM 调用失败: " + e.getMessage());
        }
    }

    /**
     * Chat 对话：全参数版（可传自定义 system prompt）
     */
    @Timed(value = "llm.chat.full.time", description = "LLM Chat 全参数耗时")
    public String chat(List<ChatMessage> messages, double temperature, int maxTokens) {
        try {
            var builder = chatClient.prompt();

            for (ChatMessage msg : messages) {
                switch (msg.role()) {
                    case "system" -> builder.system(msg.content());
                    case "user" -> builder.user(msg.content());
                    case "assistant" -> builder.user(u -> u.text(msg.content())); // Spring AI 没有直接 add assistant 的方法
                }
            }

            var response = builder.call().chatResponse();
            return response != null ? response.getResult().getOutput().getContent() : null;
        } catch (Exception e) {
            log.error("LLM Chat 全参数调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 文本向量化
     */
    public List<Double> embed(String text) {
        try {
            float[] vector = embeddingModel.embed(text);
            return vector != null ? floatToList(vector) : null;
        } catch (Exception e) {
            log.error("Embedding 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 批量文本向量化
     */
    public List<List<Double>> embed(List<String> texts) {
        try {
            var result = embeddingModel.embed(new EmbeddingRequest(texts, org.springframework.ai.embedding.EmbeddingOptions.EMPTY));
            return result.getResults().stream()
                    .map(r -> floatToList(r.getOutput()))
                    .toList();
        } catch (Exception e) {
            log.error("批量 Embedding 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 创建带工具的 ChatClient（用于 Agent 模块）
     */
    public ChatClient createToolEnabledClient(Object... tools) {
        return ChatClient.builder(chatClient)
                .defaultTools(tools)
                .build();
    }

    private List<Double> floatToList(float[] arr) {
        return java.util.stream.IntStream.range(0, arr.length)
                .mapToObj(i -> (double) arr[i])
                .toList();
    }

    /**
     * Chat 消息记录
     */
    public record ChatMessage(String role, String content) {
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }

        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
    }
}
