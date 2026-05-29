package com.tinybrain.rag.dto;

import lombok.Data;

import java.util.List;

/**
 * RAG 检索结果
 */
@Data
public class RAGResult {

    /** 原始问题 */
    private String question;

    /** 检索到的上下文片段 */
    private List<ChunkResult> chunks;

    /** LLM 生成的回答 */
    private String answer;

    /** Token 消耗 */
    private int totalTokens;

    @Data
    public static class ChunkResult {
        private Long chunkId;
        private Long documentId;
        private String documentTitle;
        private String content;
        private Double score;  // 相似度分数
    }
}
