package com.tinybrain.agent.tool;

import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Agent 工具集 — v2 Spring AI @Tool 注解版
 * <p>
 * 使用 Spring AI 的 {@code @Tool} 注解标记工具方法，
 * Spring AI 自动生成 Tool Schema（JSON Schema 格式），
 * ChatClient 在 Function Calling 中自动识别并调用。
 * <p>
 * 相比 v1-handcrafted 的手动 JSON Schema + AgentEngine 注册，
 * v2 无需手写参数 Schema，无需维护工具注册表。
 * <p>
 * 面试重点：
 * 1. Spring AI @Tool 注解原理：AOP + 反射生成 ToolDefinition
 * 2. Function Calling 流程：LLM → Tool Schema → 执行 → 回填
 * 3. 无状态设计：每个工具方法独立可测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    private final RAGService ragService;

    /**
     * 获取当前日期时间
     */
    @Tool(name = "get_datetime", description = "获取当前日期和时间，当用户询问时间日期时使用")
    public String getCurrentDateTime() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return String.format("当前日期: %s, 当前时间: %s", date, time);
    }

    /**
     * 知识库检索
     */
    @Tool(name = "knowledge_search", description = "搜索知识库文档并返回相关内容，当用户询问知识库中的内容时使用")
    public String searchKnowledge(String query, int topK) {
        log.info("Agent 工具调用: knowledge_search, query={}, topK={}", query, topK);
        try {
            RAGResult result = ragService.ask(query, Math.min(topK, 10));
            if (result.getChunks() == null || result.getChunks().isEmpty()) {
                return "未找到与「" + query + "」相关的知识。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(result.getChunks().size()).append(" 条相关结果：\n\n");
            for (int i = 0; i < result.getChunks().size(); i++) {
                var chunk = result.getChunks().get(i);
                sb.append(i + 1).append(". 【").append(chunk.getDocumentTitle()).append("】\n");
                sb.append(chunk.getContent()).append("\n");
                sb.append("   -- 相关度: ").append(String.format("%.2f", chunk.getScore())).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("知识库检索失败: {}", e.getMessage());
            return "知识库检索出错: " + e.getMessage();
        }
    }

    /**
     * 简单知识搜索（默认参数）
     */
    @Tool(name = "knowledge_search_simple", description = "快速搜索知识库，使用默认参数")
    public String searchKnowledgeSimple(String query) {
        return searchKnowledge(query, 5);
    }
}
