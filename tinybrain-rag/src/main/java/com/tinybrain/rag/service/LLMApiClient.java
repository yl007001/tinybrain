package com.tinybrain.rag.service;

import com.tinybrain.rag.dto.EmbeddingRequest;
import com.tinybrain.rag.dto.EmbeddingResponse;
import com.tinybrain.rag.dto.LLMChatRequest;
import com.tinybrain.rag.dto.LLMChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * LLM API 客户端
 * <p>
 * 封装对 OpenAI/DeepSeek 兼容 API 的调用。
 * 支持 Chat 对话和 Embedding 向量化。
 * <p>
 * 设计要点：
 * - 通过 WebClient 发起非阻塞 HTTP 请求
 * - API Key 从配置注入，不硬编码
 * - 兼容任何 OpenAI 格式的 API（DeepSeek、通义千问等）
 */
@Slf4j
@Service
public class LLMApiClient {

    private final WebClient webClient;

    public LLMApiClient(@Value("${tinybrain.llm.api-key:}") String apiKey,
                        @Value("${tinybrain.llm.base-url:https://api.deepseek.com}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Chat 对话
     */
    public LLMChatResponse chat(LLMChatRequest request) {
        try {
            return webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LLMChatResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("LLM Chat API 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 简单对话（一行调用）
     */
    public String chat(String systemPrompt, String userMessage) {
        LLMChatRequest request = new LLMChatRequest();
        request.setMessages(List.of(
                LLMChatRequest.Message.system(systemPrompt),
                LLMChatRequest.Message.user(userMessage)
        ));

        LLMChatResponse response = chat(request);
        return response != null ? response.getReplyText() : null;
    }

    /**
     * 文本向量化
     */
    public EmbeddingResponse embed(EmbeddingRequest request) {
        try {
            return webClient.post()
                    .uri("/v1/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("LLM Embedding API 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 单文本向量化（快捷方法）
     */
    public List<Double> embed(String text) {
        EmbeddingResponse response = embed(EmbeddingRequest.of(text));
        return response != null ? response.getFirstEmbedding() : null;
    }
}
