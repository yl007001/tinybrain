package com.tinybrain.rag.dto;

import lombok.Data;

import java.util.List;

/**
 * Embedding API 响应体
 */
@Data
public class EmbeddingResponse {

    private String model;
    private List<EmbeddingData> data;
    private Usage usage;

    @Data
    public static class EmbeddingData {
        private int index;
        private List<Double> embedding;
    }

    @Data
    public static class Usage {
        private int promptTokens;
        private int totalTokens;
    }

    /**
     * 获取第一个向量
     */
    public List<Double> getFirstEmbedding() {
        if (data != null && !data.isEmpty()) {
            return data.get(0).getEmbedding();
        }
        return null;
    }
}
