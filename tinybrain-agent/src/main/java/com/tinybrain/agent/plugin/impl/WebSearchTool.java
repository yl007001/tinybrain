package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 网络搜索工具
 * <p>
 * Agent 通过此工具执行网络搜索，获取实时信息。
 * 使用 SerpAPI / Bing Search 等兼容搜索引擎 API。
 * <p>
 * 配置：
 * - tinybrain.search.api-key: 搜索引擎 API Key
 * - tinybrain.search.api-url: 搜索引擎 API 地址
 */
@Slf4j
@Component
public class WebSearchTool implements AgentTool {

    private final WebClient webClient;
    private final String apiKey;
    private final String apiUrl;

    public WebSearchTool(
            @Value("${tinybrain.search.api-key:}") String apiKey,
            @Value("${tinybrain.search.api-url:https://serpapi.com}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "Search the internet for real-time information. Use when user asks about " +
               "current events, latest news, or information that may not be in the knowledge base.";
    }

    @Override
    public ObjectNode getParametersSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode queryField = mapper.createObjectNode();
        queryField.put("type", "string");
        queryField.put("description", "Search query, e.g. 'latest AI news 2026'");
        properties.set("query", queryField);

        ObjectNode numField = mapper.createObjectNode();
        numField.put("type", "integer");
        numField.put("description", "Number of results (default 5)");
        numField.put("default", 5);
        properties.set("num_results", numField);

        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("query"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        String query = args.has("query") ? args.get("query").asText() : "";
        int numResults = args.has("num_results") ? args.get("num_results").asInt() : 5;

        if (query.isBlank()) {
            return "Please provide a search query.";
        }

        // 检查 API Key 是否配置
        if (apiKey == null || apiKey.isBlank()) {
            return "Web search is not configured. Set tinybrain.search.api-key in configuration to enable web search. " +
                   "Currently using knowledge base search as fallback — try asking about stored documents instead.";
        }

        try {
            log.info("Agent 调用网络搜索: query={}", query);

            // 简单实现：调用搜索引擎 API
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("api_key", apiKey)
                            .queryParam("num", numResults)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));

            if (response == null || response.isBlank()) {
                return "Web search returned no results for: " + query;
            }

            return "Web search results for \"" + query + "\":\n" + response;
        } catch (Exception e) {
            log.warn("网络搜索失败: {}", e.getMessage());
            return "Web search failed: " + e.getMessage() + ". " +
                   "The search service may be unavailable. Try using knowledge base search instead.";
        }
    }
}
