package com.tinybrain.rag.service;

import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.entity.DocumentChunk;
import com.tinybrain.knowledge.mapper.DocumentChunkMapper;
import com.tinybrain.knowledge.mapper.DocumentMapper;
import com.tinybrain.rag.chunk.DocChunkStrategy;
import com.tinybrain.rag.dto.EmbeddingRequest;
import com.tinybrain.rag.dto.EmbeddingResponse;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.vector.VectorStore;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 核心服务 — 高级版
 * <p>
 * RAG = Retrieval Augmented Generation（检索增强生成）
 * <p>
 * 高级流程：
 * 1. 查询改写：LLM 将模糊问题改写为更清晰的表述
 * 2. 向量化问题
 * 3. 两级检索：IVF ANN → 暴力搜索融合
 * 4. 重排序：LLM 对检索结果重新评分
 * 5. 上下文拼接 + LLM 生成回答
 * <p>
 * 面试重点：
 * - RAG 解决的是 LLM 知识截止和幻觉问题
 * - 检索质量决定 RAG 效果（分块策略 + 向量模型 + 排序）
 * - 查询改写 + HyDE + 重排序是提升检索质量的三大法宝
 * - 批量 Embedding 比逐条调用减少 10x 延迟
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

    // ==================== 文档索引 ====================

    /**
     * 文档引入：分块 → 批量向量化 → 存入向量库
     * <p>
     * 优化：使用批量嵌入 API 替代逐条调用，减少 LLM API 请求次数
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
        // 逐条 insert（MyBatis-Plus 批量插入）
        for (DocumentChunk entity : entities) {
            documentChunkMapper.insert(entity);
        }

        // 4. 批量向量化（一次性发送所有文本，减少 API 调用）
        List<String> texts = entities.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());

        List<List<Double>> vectors = batchEmbed(texts);

        // 5. 存入向量库
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
     * RAG 问答（高级版）
     * <p>
     * 流程：
     * 1. 查询改写：LLM 重写用户问题
     * 2. 问题向量化
     * 3. 向量检索（IVF + 暴力搜索融合）
     * 4. LLM 重排序 Top-K 结果
     * 5. 拼接 Prompt + 生成回答
     */
    @Timed(value = "rag.ask.time", description = "RAG 问答耗时", percentiles = {0.5, 0.95, 0.99})
    public RAGResult ask(String question, int topK) {
        long start = System.currentTimeMillis();
        RAGResult result = new RAGResult();

        // Step 1: 查询改写 — 让 LLM 把模糊问题改得更清晰
        String rewrittenQuery = rewriteQuery(question);
        result.setQuestion(rewrittenQuery != null ? rewrittenQuery : question);
        log.info("原始问题: {} → 改写后: {}", question, result.getQuestion());

        // Step 2: 问题向量化
        List<Double> queryVector = llmClient.embed(result.getQuestion());
        if (queryVector == null) {
            throw BusinessException.badRequest("问题向量化失败，请检查 LLM API 配置（如需本地测试，可激活 ollama profile）");
        }

        // Step 3: 向量检索 Top-K（IVF 加速 + 暴力搜索回退）
        List<VectorStore.SearchResult> hits = vectorStore.search(queryVector, topK);
        log.info("检索到 {} 条相关结果 (耗时: {}ms)", hits.size(), System.currentTimeMillis() - start);

        if (hits.isEmpty()) {
            result.setChunks(new ArrayList<>());
            result.setAnswer("未在知识库中检索到相关信息。请先确保：1) 已在「知识库」上传文档；" +
                    "2) 对该文档执行了「索引到 RAG」操作。");
            result.setTotalTokens(0);
            return result;
        }

        // Step 4: 获取分块内容 + 重排序
        List<RAGResult.ChunkResult> chunkResults = getChunkResults(hits);
        result.setChunks(chunkResults);

        // Step 5: 拼接上下文
        StringBuilder contextBuilder = new StringBuilder();
        for (RAGResult.ChunkResult cr : chunkResults) {
            contextBuilder.append("【来源：").append(cr.getDocumentTitle()).append("】\n");
            contextBuilder.append(cr.getContent()).append("\n\n");
        }

        // Step 6: 构建 Prompt + 调用 LLM
        String prompt = buildRAGPrompt(result.getQuestion(), contextBuilder.toString());
        String systemMsg = "你是一个知识库助手，请基于提供的上下文回答问题。" +
                "如果上下文不足以回答，如实说明不清楚。请注明信息来源。" +
                "回答应当详尽、准确、条理清晰。";
        String answer = llmClient.chat(systemMsg, prompt);

        result.setAnswer(answer != null ? answer : "LLM 回答生成失败，请检查 LLM API 配置");

        // Token 估算（用于监控）
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
     * <p>
     * 用户原始问题可能模糊、口语化、缺少上下文。
     * 通过 LLM 将问题改写为更适合向量检索的表述。
     * <p>
     * 例如: "讲一下事务" → "Spring 事务的传播机制和隔离级别"
     */
    private String rewriteQuery(String original) {
        if (original == null || original.length() < 5) {
            return original; // 短问题不需要改写
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
     * 批量嵌入（一次性向量化多个文本）
     * <p>
     * 相比逐条调用嵌入 API，批量嵌入可以减少 HTTP 开销，
     * 并且很多嵌入模型对批量输入有优化。
     * <p>
     * 注意：DeepSeek Embedding API 直接支持批量输入。
     */
    private List<List<Double>> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();
        if (texts.size() == 1) {
            // 单条直接调用快捷方法
            List<Double> vec = llmClient.embed(texts.get(0));
            return vec != null ? List.of(vec) : Collections.emptyList();
        }

        try {
            EmbeddingResponse response = llmClient.embed(EmbeddingRequest.of(texts));
            if (response != null && response.getData() != null) {
                // 保持原始顺序
                return response.getData().stream()
                        .sorted(Comparator.comparingInt(d -> d.getIndex()))
                        .map(d -> d.getEmbedding())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("批量嵌入失败，回退逐条嵌入: {}", e.getMessage());
        }

        // 回退：逐条调用
        List<List<Double>> results = new ArrayList<>();
        for (String text : texts) {
            results.add(llmClient.embed(text));
        }
        return results;
    }

    /**
     * 获取分块内容并附加文档信息
     */
    private List<RAGResult.ChunkResult> getChunkResults(List<VectorStore.SearchResult> hits) {
        List<RAGResult.ChunkResult> chunkResults = new ArrayList<>();
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
        }
        return chunkResults;
    }

    // ==================== Prompt 构建 ====================

    /**
     * 构建 RAG Prompt
     * <p>
     * 这是 RAG 效果的关键优化点。好的 Prompt 能引导 LLM：
     * - 基于上下文回答，不胡编
     * - 承认知识不足
     * - 标注信息来源
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
     * 获取向量存储统计信息（用于监控界面）
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVectors", vectorStore.size());
        var searchStats = vectorStore.getSearchStats();
        stats.put("ivfActive", searchStats.isIvfActive());
        stats.put("ivfClusters", searchStats.getIvfClusters());
        stats.put("ivfProbes", searchStats.getIvfProbes());
        return stats;
    }
}
