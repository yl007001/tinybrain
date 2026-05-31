package com.tinybrain.rag.service;

import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.entity.DocumentChunk;
import com.tinybrain.knowledge.mapper.DocumentChunkMapper;
import com.tinybrain.knowledge.mapper.DocumentMapper;
import com.tinybrain.rag.chunk.DocChunkStrategy;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.vector.VectorStoreWrapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAG 核心服务 — v2 Spring AI 版
 * <p>
 * 基于 Spring AI Alibaba 的 ChatClient + EmbeddingModel + VectorStore 实现。
 * 相比 v1-handcrafted 的手写 WebClient 方案，v2 利用 Spring AI 统一抽象，
 * 可轻松切换不同的 LLM 供应商（DashScope / OpenAI / DeepSeek）和
 * 不同的向量库（内存 / Redis / PGVector）。
 * <p>
 * 流程：
 * 1. 文档引入：分块 → EmbeddingModel 向量化 → VectorStore 存储
 * 2. RAG 问答：EmbeddingModel 向量化问题 → VectorStore 检索 → LLM 生成
 * <p>
 * ：
 * - Spring AI 的 ChatClient / EmbeddingModel / VectorStore SPI 抽象
 * - 依赖倒置：平台无关的 AI 接口设计
 * - 自动配置：通过 spring.factories 加载不同 Provider
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final LLMApiClient llmClient;
    private final EmbeddingModel embeddingModel;
    private final VectorStoreWrapper vectorStore;
    private final DocChunkStrategy chunkStrategy;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;

    // ==================== 文档索引 ====================

    /**
     * 文档引入：分块 → Spring AI EmbeddingModel 向量化 → VectorStore 存储
     */
    @Timed(value = "rag.index.time", description = "文档索引耗时", percentiles = {0.5, 0.95, 0.99})
    @Transactional(rollbackFor = Exception.class)
    public void indexDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在");
        }

        long start = System.currentTimeMillis();

        // 1. 分块
        List<DocChunkStrategy.Chunk> chunks = chunkStrategy.split(doc.getContent());
        log.info("文档 {} 分块完成: {} 块", documentId, chunks.size());

        // 2. 删除旧分块和向量
        documentChunkMapper.deleteByDocumentId(documentId);
        vectorStore.deleteByDocumentId(documentId);

        // 3. 批量保存分块到数据库
        List<DocumentChunk> entities = new ArrayList<>();
        for (DocChunkStrategy.Chunk chunk : chunks) {
            DocumentChunk entity = new DocumentChunk();
            entity.setDocumentId(documentId);
            entity.setChunkIndex(chunk.getIndex());
            entity.setContent(chunk.getContent());
            entities.add(entity);
        }
        entities.forEach(e -> documentChunkMapper.insert(e));

        // 4. 批量向量化（使用 Spring AI EmbeddingModel）
        List<String> texts = entities.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());

        List<List<Double>> vectors = batchEmbed(texts);

        // 5. 存入向量库（使用 Spring AI VectorStore）
        if (vectors != null && vectors.size() == entities.size()) {
            for (int i = 0; i < entities.size(); i++) {
                List<Double> vector = vectors.get(i);
                if (vector != null) {
                    vectorStore.upsert(entities.get(i).getId(), documentId, vector);
                }
            }
            log.info("文档 {} 索引完成: {} 块向量化 (耗时: {}ms)",
                    documentId, entities.size(), System.currentTimeMillis() - start);
        } else {
            log.warn("文档 {} 部分向量化失败: 文本数={}, 向量数={}",
                    documentId, texts.size(), vectors != null ? vectors.size() : 0);
        }
    }

    // ==================== RAG 问答 ====================

    /**
     * RAG 问答（支持查询改写 + 重排序）
     * <p>
     * v2 使用 Spring AI EmbeddingModel 替代手写向量化，
     * 使用 Spring AI VectorStore 替代手写内存向量库。
     */
    @Timed(value = "rag.ask.time", description = "RAG 问答耗时", percentiles = {0.5, 0.95, 0.99})
    public RAGResult ask(String question, int topK) {
        long start = System.currentTimeMillis();
        RAGResult result = new RAGResult();

        // Step 1: 查询改写 — LLM 把模糊问题改得更清晰
        String rewrittenQuery = rewriteQuery(question);
        result.setQuestion(rewrittenQuery != null ? rewrittenQuery : question);
        log.info("原始问题: {} → 改写后: {}", question, result.getQuestion());

        // Step 2: 问题向量化（Spring AI EmbeddingModel — 返回 float[]）
        float[] queryEmbedding = embeddingModel.embed(result.getQuestion());
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw BusinessException.badRequest("问题向量化失败，请检查 EmbeddingModel 配置");
        }
        List<Double> queryVector = floatToList(queryEmbedding);

        // Step 3: 向量检索 Top-K（Spring AI VectorStore）
        List<VectorStoreWrapper.SearchResult> hits = vectorStore.search(queryVector, topK);
        log.info("检索到 {} 条相关结果 (耗时: {}ms)", hits.size(), System.currentTimeMillis() - start);

        if (hits.isEmpty()) {
            result.setChunks(new ArrayList<>());
            result.setAnswer("未在知识库中检索到相关信息。请先确保：1) 已在「知识库」上传文档；" +
                    "2) 对该文档执行了「索引到 RAG」操作。");
            result.setTotalTokens(0);
            return result;
        }

        // Step 4: 获取分块内容
        List<RAGResult.ChunkResult> chunkResults = getChunkResults(hits);
        result.setChunks(chunkResults);

        // Step 5: 拼接上下文
        StringBuilder contextBuilder = new StringBuilder();
        for (RAGResult.ChunkResult cr : chunkResults) {
            contextBuilder.append("【来源：").append(cr.getDocumentTitle()).append("】\n");
            contextBuilder.append(cr.getContent()).append("\n\n");
        }

        // Step 6: 构建 Prompt + 调用 LLM（Spring AI ChatClient）
        String prompt = buildRAGPrompt(result.getQuestion(), contextBuilder.toString());
        String systemMsg = "你是一个知识库助手，请基于提供的上下文回答问题。" +
                "如果上下文不足以回答，如实说明不清楚。请注明信息来源。" +
                "回答应当详尽、准确、条理清晰。";
        String answer = llmClient.chat(systemMsg, prompt);

        result.setAnswer(answer != null ? answer : "LLM 回答生成失败，请检查 LLM API 配置");

        // Token 估算
        int estimatedTokens = result.getQuestion().length() / 2
                + contextBuilder.length() / 2
                + (answer != null ? answer.length() / 2 : 0);
        result.setTotalTokens(estimatedTokens);

        long elapsed = System.currentTimeMillis() - start;
        log.info("RAG 问答完成: 问题='{}', 检索={}块, 耗时={}ms, 预估tokens={}",
                question, hits.size(), elapsed, estimatedTokens);
        return result;
    }

    // ==================== 高级 RAG 技术 ====================

    /**
     * 查询改写 (Query Rewriting)
     */
    private String rewriteQuery(String original) {
        if (original == null || original.length() < 5) {
            return original;
        }
        try {
            String prompt = """
                    你是一个查询改写助手。用户的问题可能模糊或口语化，
                    请将其改写为适合知识库检索的、清晰完整的查询语句。
                    只返回改写后的查询，不要加任何解释。

                    原始问题：%s
                    改写后：""".formatted(original);
            String rewritten = llmClient.chat("你是一个查询改写专家，请简洁改写。", prompt);
            if (rewritten != null && !rewritten.isBlank() && rewritten.length() < original.length() * 3) {
                return rewritten.trim();
            }
        } catch (Exception e) {
            log.warn("查询改写失败，使用原始问题: {}", e.getMessage());
        }
        return original;
    }

    /**
     * 批量嵌入（Spring AI EmbeddingModel — 返回 List<Embedding>）
     */
    private List<List<Double>> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();
        if (texts.size() == 1) {
            List<Double> vec = floatToList(embeddingModel.embed(texts.get(0)));
            return vec != null ? List.of(vec) : Collections.emptyList();
        }

        try {
            var results = embeddingModel.embed(texts);
            return results.stream()
                    .map(this::floatToList)
                    .toList();
        } catch (Exception e) {
            log.warn("批量嵌入失败: {}", e.getMessage());
        }

        // 回退：逐条调用
        return texts.stream()
                .map(t -> floatToList(embeddingModel.embed(t)))
                .toList();
    }

    /**
     * float[] → List<Double> 工具方法
     */
    private List<Double> floatToList(float[] arr) {
        if (arr == null) return null;
        return IntStream.range(0, arr.length)
                .mapToObj(i -> (double) arr[i])
                .toList();
    }

    /**
     * 获取分块内容并附加文档信息
     */
    private List<RAGResult.ChunkResult> getChunkResults(List<VectorStoreWrapper.SearchResult> hits) {
        List<RAGResult.ChunkResult> chunkResults = new ArrayList<>();
        for (VectorStoreWrapper.SearchResult hit : hits) {
            DocumentChunk chunk = documentChunkMapper.selectById(hit.getChunkId());
            if (chunk == null) continue;

            Document doc = documentMapper.selectById(hit.getDocumentId());

            RAGResult.ChunkResult cr = new RAGResult.ChunkResult();
            cr.setChunkId(hit.getChunkId());
            cr.setDocumentId(hit.getDocumentId());
            cr.setDocumentTitle(doc != null ? doc.getTitle() : "未知");
            cr.setContent(chunk.getContent());
            cr.setScore((double) hit.getScore());
            chunkResults.add(cr);
        }
        return chunkResults;
    }

    // ==================== Prompt 构建 ====================

    /**
     * 构建 RAG Prompt
     */
    private String buildRAGPrompt(String question, String context) {
        return """
                请根据以下上下文回答问题。请严格遵守：
                1. 只基于提供的上下文回答，不要使用自己的知识
                2. 如果上下文信息不足，请如实说"根据提供的知识无法完整回答这个问题"
                3. 在回答末尾列出信息来源
                4. 如涉及技术概念，请给出详细解释

                === 上下文 ===
                %s

                === 问题 ===
                %s

                请用中文回答：
                """.formatted(context, question).strip();
    }

    /**
     * 获取向量存储统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVectors", vectorStore.size());
        stats.put("vectorStoreType", "Spring AI VectorStore");
        return stats;
    }
}
