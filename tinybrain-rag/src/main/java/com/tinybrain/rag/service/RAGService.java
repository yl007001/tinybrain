package com.tinybrain.rag.service;

import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.entity.DocumentChunk;
import com.tinybrain.knowledge.mapper.DocumentChunkMapper;
import com.tinybrain.knowledge.mapper.DocumentMapper;
import com.tinybrain.rag.chunk.DocChunkStrategy;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.vector.VectorStore;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 核心服务
 * <p>
 * RAG = Retrieval Augmented Generation（检索增强生成）
 * <p>
 * 流程：
 * 1. 文档引入：文档创建时自动分块 → 向量化 → 存储
 * 2. 检索：用户提问 → 向量化 → 向量库 Top-K 检索
 * 3. 增强：检索结果 + 原始问题 → 拼接 Prompt
 * 4. 生成：调用 LLM → 产生带上下文的回答
 * <p>
 * 面试重点：
 * - RAG 解决的是 LLM 知识截止和幻觉问题
 * - 检索质量决定 RAG 效果（分块策略 + 向量模型 + 排序）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final LLMApiClient llmClient;
    private final VectorStore vectorStore;
    private final DocChunkStrategy chunkStrategy;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;

    /**
     * 文档引入：分块 → 向量化 → 存入向量库
     */
    @Timed(value = "rag.index.time", description = "文档索引耗时", percentiles = {0.5, 0.95, 0.99})
    @Transactional(rollbackFor = Exception.class)
    public void indexDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在");
        }

        // 1. 分块
        List<DocChunkStrategy.Chunk> chunks = chunkStrategy.split(doc.getContent());
        log.info("文档 {} 分块完成: {} 块", documentId, chunks.size());

        // 2. 删除旧分块和向量
        documentChunkMapper.deleteByDocumentId(documentId);
        vectorStore.deleteByDocumentId(documentId);

        // 3. 保存分块到数据库
        List<DocumentChunk> entities = new ArrayList<>();
        for (DocChunkStrategy.Chunk chunk : chunks) {
            DocumentChunk entity = new DocumentChunk();
            entity.setDocumentId(documentId);
            entity.setChunkIndex(chunk.getIndex());
            entity.setContent(chunk.getContent());
            entities.add(entity);
        }

        // 批量插入
        entities.forEach(e -> documentChunkMapper.insert(e));

        // 4. 向量化并存入向量库
        for (int i = 0; i < entities.size(); i++) {
            DocumentChunk entity = entities.get(i);
            List<Double> vector = llmClient.embed(entity.getContent());
            if (vector != null) {
                vectorStore.upsert(entity.getId(), documentId, vector);
            }
        }

        log.info("文档 {} 索引完成: {} 块向量化", documentId, entities.size());
    }

    /**
     * RAG 问答
     *
     * @param question 用户问题
     * @param topK     检索 Top-K 相关块
     * @return RAG 结果（上下文 + 回答）
     */
    @Timed(value = "rag.ask.time", description = "RAG 问答耗时", percentiles = {0.5, 0.95, 0.99})
    public RAGResult ask(String question, int topK) {
        RAGResult result = new RAGResult();
        result.setQuestion(question);

        // 1. 问题向量化
        List<Double> queryVector = llmClient.embed(question);
        if (queryVector == null) {
            throw BusinessException.badRequest("问题向量化失败，请检查 LLM API 配置");
        }

        // 2. 向量检索 Top-K
        List<VectorStore.SearchResult> hits = vectorStore.search(queryVector, topK);
        log.info("检索到 {} 条相关结果", hits.size());

        if (hits.isEmpty()) {
            result.setChunks(new ArrayList<>());
            result.setAnswer("未找到相关知识，请先导入相关文档。");
            return result;
        }

        // 3. 获取分块内容
        List<RAGResult.ChunkResult> chunkResults = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        for (VectorStore.SearchResult hit : hits) {
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

            contextBuilder.append("【来源：").append(cr.getDocumentTitle()).append("】\n");
            contextBuilder.append(chunk.getContent()).append("\n\n");
        }

        result.setChunks(chunkResults);

        // 4. 拼接 Prompt + 调用 LLM 生成回答
        String prompt = buildRAGPrompt(question, contextBuilder.toString());
        String answer = llmClient.chat("你是一个知识库助手，请基于提供的上下文回答问题。" +
                        "如果上下文不足以回答，如实说明。请注明信息来源。", prompt);

        result.setAnswer(answer != null ? answer : "LLM 回答生成失败");

        // 5. Token 统计（估算）
        int estimatedTokens = question.length() / 2 + contextBuilder.length() / 2
                + (answer != null ? answer.length() / 2 : 0);
        result.setTotalTokens(estimatedTokens);

        return result;
    }

    /**
     * 构建 RAG Prompt
     * <p>
     * 将检索到的上下文与用户问题拼接到一起。
     * 这是 RAG 效果的关键优化点。
     */
    private String buildRAGPrompt(String question, String context) {
        return """
                请根据以下上下文回答问题。如果上下文信息不足，请如实说"根据提供的知识无法回答这个问题"。

                === 上下文 ===
                %s

                === 问题 ===
                %s

                请用中文回答，并在回答末尾列出信息来源。
                """.formatted(context, question).strip();
    }
}
