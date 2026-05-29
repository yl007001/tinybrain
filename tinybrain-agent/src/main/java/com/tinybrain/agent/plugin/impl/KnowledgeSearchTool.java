package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import org.springframework.stereotype.Component;

/**
 * 知识库检索工具
 * <p>
 * Agent 通过此工具搜索知识库文档。
 */
@Component
public class KnowledgeSearchTool implements AgentTool {

    @Override
    public String getName() {
        return "knowledge_search";
    }

    @Override
    public String getDescription() {
        return "搜索知识库中的文档，返回相关的内容片段。当用户询问知识库中的信息时使用此工具。";
    }

    @Override
    public ObjectNode getParametersSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode queryField = mapper.createObjectNode();
        queryField.put("type", "string");
        queryField.put("description", "搜索关键词，如"Spring事务"");
        properties.set("query", queryField);

        ObjectNode topKField = mapper.createObjectNode();
        topKField.put("type", "integer");
        topKField.put("description", "返回结果数量（默认5）");
        topKField.put("default", 5);
        properties.set("top_k", topKField);

        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("query"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        String query = args.has("query") ? args.get("query").asText() : "";
        int topK = args.has("top_k") ? args.get("top_k").asInt() : 5;

        // TODO: 调用 RAGService 的 search 方法（Phase 3 集成）
        return String.format("正在搜索知识库: query=%s, top_k=%d", query, topK);
    }
}
