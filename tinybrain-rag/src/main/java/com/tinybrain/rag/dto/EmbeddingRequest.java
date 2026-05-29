package com.tinybrain.rag.dto;

import lombok.Data;

import java.util.List;

/**
 * Embedding API 请求体
 */
@Data
public class EmbeddingRequest {

    /** 模型名称 */
    private String model = "deepseek-embedding";

    /** 输入文本 */
    private List<String> input;

    public static EmbeddingRequest of(String text) {
        EmbeddingRequest req = new EmbeddingRequest();
        req.setInput(List.of(text));
        return req;
    }

    public static EmbeddingRequest of(List<String> texts) {
        EmbeddingRequest req = new EmbeddingRequest();
        req.setInput(texts);
        return req;
    }
}
