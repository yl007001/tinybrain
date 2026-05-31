package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 知识库检索工具
 * <p>
 * Agent 通过此工具搜索知识库文档。
 * 内部调用 RAGService 完成向量语义检索 + LLM 增强生成。
 * <p>
 * 工作流程：
 * 1. 解析参数（query, top_k）
 * 2. 调用 RAGService.ask() 执行语义搜索
 * 3. 返回检索结果和 LLM 生成的回答
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool implements AgentTool {

    private final RAGService ragService;

    @Override
    public String getName() {
        return "knowledge_search";
    }

    @Override
    public String getDescription() {
        return "Search knowledge base documents and return relevant content. " +
               "Use when user asks about stored knowledge, documents, or specific topics from the knowledge base. " +
               "Performs semantic search and returns AI-generated answers based on the retrieved context.";
    }

    @Override
    public ObjectNode getParametersSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode queryField = mapper.createObjectNode();
        queryField.put("type", "string");
        queryField.put("description", "Search keywords or question, e.g. 'What is Spring transaction propagation?'");
        properties.set("query", queryField);

        ObjectNode topKField = mapper.createObjectNode();
        topKField.put("type", "integer");
        topKField.put("description", "Number of results to return (default 3, max 10)");
        topKField.put("default", 3);
        topKField.put("minimum", 1);
        topKField.put("maximum", 10);
        properties.set("top_k", topKField);

        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("query"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        String query = args.has("query") ? args.get("query").asText() : "";
        int topK = args.has("top_k") ? Math.min(args.get("top_k").asInt(), 10) : 3;

        if (query.isBlank()) {
            return "Please provide a search query.";
        }

        try {
            log.info("Agent 调用知识库检索: query={}, topK={}", query, topK);
            RAGResult result = ragService.ask(query, topK);

            if (result == null || result.getChunks() == null || result.getChunks().isEmpty()) {
                return "No relevant knowledge found for query: \"" + query + "\". " +
                       "Please make sure there are documents indexed in the knowledge base.";
            }

            // 构建返回结果
            StringBuilder sb = new StringBuilder();
            sb.append("Knowledge search results for \"").append(query).append("\":\n\n");

            // 列出检索到的上下文片段
            sb.append("Found ").append(result.getChunks().size()).append(" relevant document chunks:\n");
            int idx = 1;
            for (RAGResult.ChunkResult chunk : result.getChunks()) {
                sb.append(idx++).append(". [").append(chunk.getDocumentTitle())
                  .append("] (score: ").append(String.format("%.4f", chunk.getScore())).append(")\n")
                  .append("   ").append(chunk.getContent(), 0, Math.min(chunk.getContent().length(), 200))
                  .append(chunk.getContent().length() > 200 ? "..." : "").append("\n\n");
            }

            // AI 生成的回答
            if (result.getAnswer() != null && !result.getAnswer().isEmpty()) {
                sb.append("AI Answer: ").append(result.getAnswer()).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("知识库检索失败: {}", e.getMessage(), e);
            return "Knowledge search failed: " + e.getMessage() + ". " +
                   "Please check if the knowledge base is properly configured.";
        }
    }
}
