package com.tinybrain.rag.service;

import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.rag.dto.EmbeddingRequest;
import com.tinybrain.rag.dto.EmbeddingResponse;
import com.tinybrain.rag.dto.LLMChatRequest;
import com.tinybrain.rag.dto.LLMChatResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
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
 * - 兼容任何 OpenAI 格式的 API（DeepSeek、通义千问、Ollama 等）
 * - Resilience4j 熔断 + 重试，防止 LLM API 故障级联
 * <p>
 * ：
 * 1. 熔断器模式(Circuit Breaker)防止级联故障
 * 2. 重试策略(Retry)处理临时网络故障
 * 3. 背压(Backpressure)与超时控制
 */
@Slf4j
@Service
public class LLMApiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;

    public LLMApiClient(@Value("${tinybrain.llm.api-key:}") String apiKey,
                        @Value("${tinybrain.llm.base-url:https://api.deepseek.com}") String baseUrl,
                        @Value("${tinybrain.llm.timeout:30000}") int timeoutMs) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;

        if (apiKey == null || apiKey.isBlank() || "sk-your-api-key-here".equals(apiKey)) {
            log.warn("LLM API Key 未配置！请在 application.yml 中设置 tinybrain.llm.api-key 或环境变量 TINYBRAIN_LLM_KEY");
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Chat 对话（带熔断 + 重试）
     */
    @Retry(name = "llmApi", fallbackMethod = "chatFallback")
    @CircuitBreaker(name = "llmApi", fallbackMethod = "chatFallback")
    public LLMChatResponse chat(LLMChatRequest request) {
        validateApiKey();
        try {
            LLMChatResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LLMChatResponse.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw BusinessException.badRequest("LLM API 返回为空，请检查网络连接或 API Key 配置");
            }
            return response;
        } catch (WebClientResponseException e) {
            log.error("LLM Chat API HTTP 错误: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(502, "LLM 服务响应异常: " + e.getStatusCode());
        } catch (WebClientRequestException e) {
            log.error("LLM Chat API 连接失败: {}", e.getMessage());
            throw new BusinessException(502, "LLM 服务连接失败，请检查网络和 API 配置: " + e.getMessage());
        }
    }

    /**
     * Chat 熔断/失败降级
     */
    public LLMChatResponse chatFallback(LLMChatRequest request, Throwable t) {
        log.error("LLM Chat 调用失败（熔断降级）: {}", t.getMessage());
        // 构造一个降级响应
        LLMChatResponse fallback = new LLMChatResponse();
        fallback.setId("fallback");
        fallback.setObject("chat.completion");
        fallback.setModel("fallback");
        LLMChatResponse.Choice choice = new LLMChatResponse.Choice();
        LLMChatResponse.Message msg = new LLMChatResponse.Message();
        msg.setRole("assistant");
        msg.setContent("抱歉，AI 服务暂时不可用，请稍后再试。(" + t.getMessage() + ")");
        choice.setMessage(msg);
        fallback.setChoices(List.of(choice));
        return fallback;
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
     * 文本向量化（带熔断 + 重试）
     */
    @Retry(name = "llmApi", fallbackMethod = "embedFallback")
    @CircuitBreaker(name = "llmApi", fallbackMethod = "embedFallback")
    public EmbeddingResponse embed(EmbeddingRequest request) {
        validateApiKey();
        try {
            EmbeddingResponse response = webClient.post()
                    .uri("/v1/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw BusinessException.badRequest("Embedding API 返回为空，请检查 API Key 配置");
            }
            return response;
        } catch (WebClientResponseException e) {
            log.error("LLM Embedding API HTTP 错误: status={}", e.getStatusCode());
            throw new BusinessException(502, "Embedding 服务响应异常: " + e.getStatusCode());
        } catch (WebClientRequestException e) {
            log.error("LLM Embedding API 连接失败: {}", e.getMessage());
            throw new BusinessException(502, "Embedding 服务连接失败: " + e.getMessage());
        }
    }

    /**
     * Embedding 熔断降级
     */
    public EmbeddingResponse embedFallback(EmbeddingRequest request, Throwable t) {
        log.error("LLM Embedding 调用失败（熔断降级）: {}", t.getMessage());
        return null;
    }

    /**
     * 单文本向量化（快捷方法）
     */
    public List<Double> embed(String text) {
        EmbeddingResponse response = embed(EmbeddingRequest.of(text));
        return response != null ? response.getFirstEmbedding() : null;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank() || "sk-your-api-key-here".equals(apiKey)) {
            throw BusinessException.badRequest(
                    "LLM API Key 未配置。请在 application.yml 中设置 tinybrain.llm.api-key，或设置环境变量 TINYBRAIN_LLM_KEY。" +
                    "支持 DeepSeek / OpenAI / Ollama 等兼容 API。"
            );
        }
    }
}
