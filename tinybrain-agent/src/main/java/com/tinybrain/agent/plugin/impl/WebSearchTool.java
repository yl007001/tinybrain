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
 * 返回结构化的搜索结果（标题 + 摘要 + 链接），
 * 而非原始 JSON，让 Agent 能更好地理解和引用搜索结果。
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

            // 解析 SerpAPI 返回的 JSON，提取结构化结果
            return parseSearchResults(response, query);
        } catch (Exception e) {
            log.warn("网络搜索失败: {}", e.getMessage());
            return "Web search failed: " + e.getMessage() + ". " +
                   "The search service may be unavailable. Try using knowledge base search instead.";
        }
    }

    /**
     * 解析搜索引擎返回的 JSON，提取标题、摘要、链接
     */
    private String parseSearchResults(String jsonResponse, String query) {
        try {
            JsonNode root = new ObjectMapper().readTree(jsonResponse);
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for \"").append(query).append("\":\n\n");

            // 尝试解析 SerpAPI 格式的 organic_results
            JsonNode organicResults = root.path("organic_results");
            if (organicResults.isArray() && organicResults.size() > 0) {
                int count = Math.min(organicResults.size(), 5);
                for (int i = 0; i < count; i++) {
                    JsonNode result = organicResults.get(i);
                    String title = result.path("title").asText("No title");
                    String snippet = result.path("snippet").asText("No description");
                    String link = result.path("link").asText("");

                    sb.append(i + 1).append(". ").append(title).append("\n");
                    sb.append("   ").append(snippet).append("\n");
                    if (!link.isBlank()) {
                        sb.append("   Source: ").append(link).append("\n");
                    }
                    sb.append("\n");
                }
                return sb.toString().trim();
            }

            // 尝试解析 answer_box（即时回答）
            JsonNode answerBox = root.path("answer_box");
            if (!answerBox.isMissingNode()) {
                String answer = answerBox.path("answer").asText(
                        answerBox.path("snippet").asText(""));
                if (!answer.isBlank()) {
                    return "Direct answer: " + answer;
                }
            }

            // 回退：返回原始 JSON 的前 500 字符
            String raw = jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse;
            return "Search results (raw): " + raw;

        } catch (Exception e) {
            log.debug("搜索结果解析失败，返回原始数据: {}", e.getMessage());
            String raw = jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse;
            return "Search results: " + raw;
        }
    }
}
