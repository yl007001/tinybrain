package com.tinybrain.rag.service;

import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.entity.DocumentChunk;
import com.tinybrain.knowledge.mapper.DocumentChunkMapper;
import com.tinybrain.knowledge.mapper.DocumentMapper;
import com.tinybrain.rag.chunk.DocChunkStrategy;
import com.tinybrain.rag.dto.RAGResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 核心服务 — 关键词检索版
 * <p>
 * RAG = Retrieval Augmented Generation（检索增强生成）
 * <p>
 * 创新点：使用 DeepSeek 提取关键词 + 数据库全文检索，无需外部 Embedding 服务
 * <p>
 * 流程：
 * 1. 文档分块 → DeepSeek 提取关键词 → 存储到数据库
 * 2. 用户提问 → DeepSeek 提取查询关键词 → 关键词匹配检索
 * 3. 检索结果 → DeepSeek 生成回答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final LLMApiClient llmClient;
    private final DocChunkStrategy chunkStrategy;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;

    // ==================== 文档索引 ====================

    /**
     * 文档索引：分块 → 提取关键词 → 存储到数据库
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

        // 2. 删除旧分块
        documentChunkMapper.deleteByDocumentId(documentId);

        // 3. 逐块提取关键词并保存
        int successCount = 0;
        for (DocChunkStrategy.Chunk chunk : chunks) {
            try {
                // 用 DeepSeek 提取关键词
                String keywords = extractKeywords(chunk.getContent());

                DocumentChunk entity = new DocumentChunk();
                entity.setDocumentId(documentId);
                entity.setChunkIndex(chunk.getIndex());
                entity.setContent(chunk.getContent());
                entity.setKeywords(keywords);
                documentChunkMapper.insert(entity);
                successCount++;

                log.debug("分块 {} 关键词: {}", chunk.getIndex(), keywords);
            } catch (Exception e) {
                log.warn("分块 {} 关键词提取失败: {}", chunk.getIndex(), e.getMessage());
                // 即使关键词提取失败，也保存分块（只是没有关键词）
                DocumentChunk entity = new DocumentChunk();
                entity.setDocumentId(documentId);
                entity.setChunkIndex(chunk.getIndex());
                entity.setContent(chunk.getContent());
                documentChunkMapper.insert(entity);
            }
        }

        log.info("文档 {} 索引完成: {}/{} 块成功 (耗时: {}ms)",
                documentId, successCount, chunks.size(), System.currentTimeMillis() - start);
    }

    /**
     * 使用 DeepSeek 提取关键词
     */
    private String extractKeywords(String content) {
        // 截取前1000字符避免 token 过多
        String truncated = content.length() > 1000 ? content.substring(0, 1000) : content;

        String prompt = """
                请从以下文本中提取 5-10 个核心关键词，用逗号分隔返回。
                关键词应该是名词或名词短语，能概括文本核心内容。
                只返回关键词，不要任何解释。

                文本：
                %s

                关键词：""".formatted(truncated);

        String result = llmClient.chat("你是一个关键词提取专家，只返回逗号分隔的关键词。", prompt);
        return result != null ? result.trim() : "";
    }

    // ==================== RAG 问答 ====================

    /**
     * RAG 问答
     * <p>
     * 流程：
     * 1. DeepSeek 从问题中提取查询关键词
     * 2. 关键词匹配检索相关分块
     * 3. DeepSeek 基于检索结果生成回答
     */
    @Timed(value = "rag.ask.time", description = "RAG 问答耗时", percentiles = {0.5, 0.95, 0.99})
    public RAGResult ask(String question, int topK) {
        long start = System.currentTimeMillis();
        RAGResult result = new RAGResult();
        result.setQuestion(question);

        // Step 1: 从问题中提取查询关键词
        String queryKeywords = extractQueryKeywords(question);
        log.info("问题: {} → 关键词: {}", question, queryKeywords);

        // Step 2: 关键词匹配检索
        List<DocumentChunk> matchedChunks = searchByKeywords(queryKeywords, topK);
        log.info("检索到 {} 条相关结果 (耗时: {}ms)", matchedChunks.size(), System.currentTimeMillis() - start);

        if (matchedChunks.isEmpty()) {
            result.setChunks(new ArrayList<>());
            result.setAnswer("未在知识库中检索到相关信息。请先确保：1) 已在「知识库」上传文档；" +
                    "2) 对该文档执行了「索引到 RAG」操作。");
            result.setTotalTokens(0);
            return result;
        }

        // Step 3: 构建检索结果
        List<RAGResult.ChunkResult> chunkResults = new ArrayList<>();
        for (DocumentChunk chunk : matchedChunks) {
            Document doc = documentMapper.selectById(chunk.getDocumentId());
            RAGResult.ChunkResult cr = new RAGResult.ChunkResult();
            cr.setChunkId(chunk.getId());
            cr.setDocumentId(chunk.getDocumentId());
            cr.setDocumentTitle(doc != null ? doc.getTitle() : "未知");
            cr.setContent(chunk.getContent());
            cr.setScore(1.0); // 关键词匹配不计算相似度分数
            chunkResults.add(cr);
        }
        result.setChunks(chunkResults);

        // Step 4: 拼接上下文
        StringBuilder contextBuilder = new StringBuilder();
        for (RAGResult.ChunkResult cr : chunkResults) {
            contextBuilder.append("【来源：").append(cr.getDocumentTitle()).append("】\n");
            contextBuilder.append(cr.getContent()).append("\n\n");
        }

        // Step 5: 构建 Prompt + 调用 DeepSeek 生成回答
        String prompt = buildRAGPrompt(question, contextBuilder.toString());
        String systemMsg = "你是一个知识库助手，请基于提供的上下文回答问题。" +
                "如果上下文不足以回答，如实说明不清楚。请注明信息来源。" +
                "回答应当详尽、准确、条理清晰。";
        String answer = llmClient.chat(systemMsg, prompt);

        result.setAnswer(answer != null ? answer : "LLM 回答生成失败，请检查 LLM API 配置");

        // Token 估算
        int estimatedTokens = question.length() / 2
                + contextBuilder.length() / 2
                + (answer != null ? answer.length() / 2 : 0);
        result.setTotalTokens(estimatedTokens);

        long elapsed = System.currentTimeMillis() - start;
        log.info("RAG 问答完成: 问题='{}', 检索={}块, 耗时={}ms", question, matchedChunks.size(), elapsed);
        return result;
    }

    /**
     * 从问题中提取查询关键词
     */
    private String extractQueryKeywords(String question) {
        if (question == null || question.length() < 3) {
            return question;
        }

        try {
            String prompt = """
                    请从以下问题中提取 3-5 个核心检索关键词，用逗号分隔返回。
                    关键词应该是能匹配知识库文档的术语。
                    只返回关键词，不要任何解释。

                    问题：%s

                    关键词：""".formatted(question);

            String result = llmClient.chat("你是一个关键词提取专家，只返回逗号分隔的关键词。", prompt);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        } catch (Exception e) {
            log.warn("查询关键词提取失败，使用原始问题: {}", e.getMessage());
        }

        // 回退：直接用原始问题
        return question;
    }

    /**
     * 关键词匹配检索
     * <p>
     * 使用 SQL LIKE 进行模糊匹配，支持多个关键词（OR 逻辑）
     * 任一关键词在 content 或 keywords 中出现即匹配
     */
    private List<DocumentChunk> searchByKeywords(String keywords, int topK) {
        if (keywords == null || keywords.isBlank()) {
            return Collections.emptyList();
        }

        // 分割关键词
        String[] keywordArray = keywords.split("[,，、\\s]+");

        // 构建查询：任一关键词匹配即可（OR 逻辑）
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();

        // 使用 nested 条件实现 OR 逻辑
        wrapper.and(w -> {
            boolean first = true;
            for (String kw : keywordArray) {
                String keyword = kw.trim();
                if (keyword.isEmpty()) continue;

                if (!first) {
                    w.or();
                }
                first = false;

                // 在 content 或 keywords 中搜索
                final String k = keyword;
                w.nested(inner -> inner
                        .like(DocumentChunk::getContent, k)
                        .or()
                        .like(DocumentChunk::getKeywords, k)
                );
            }
        });

        wrapper.last("LIMIT " + topK);

        List<DocumentChunk> results = documentChunkMapper.selectList(wrapper);
        return results;
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
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        // 统计已索引的分块数
        Long chunkCount = documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>().isNotNull(DocumentChunk::getKeywords)
        );
        stats.put("indexedChunks", chunkCount);
        stats.put("totalChunks", documentChunkMapper.selectCount(null));
        // 兼容前端：totalVectors 显示已索引分块数
        stats.put("totalVectors", chunkCount);
        stats.put("searchMode", "keyword-based (LLM extraction)");
        return stats;
    }
}
