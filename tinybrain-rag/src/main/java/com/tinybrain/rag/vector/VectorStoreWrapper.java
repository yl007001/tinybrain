package com.tinybrain.rag.vector;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量存储 — v2 Spring AI Alibaba 版
 * <p>
 * 基于 Spring AI 抽象的向量存储封装。
 * 当前使用内存存储（ConcurrentHashMap），保持与 v1 兼容的 API。
 * <p>
 * 可升级方案（面试重点）：
 * 1. 替换为 Spring AI VectorStore 实现（SimpleVectorStore / RedisVectorStore / PGVectorStore）
 * 2. 使用 VectorStore 接口 + @Qualifier 注入不同实现
 * 3. 配置中心动态切换向量库类型
 * <p>
 * 面试重点：
 * 1. 向量的余弦相似度计算原理
 * 2. 全量扫描 vs ANN (IVF, HNSW) 索引
 * 3. 元数据过滤（Metadata Filter）提升检索精度
 */
@Slf4j
@Component
public class VectorStoreWrapper {

    /** 存储结构：chunkId → 向量 */
    private final ConcurrentHashMap<Long, float[]> store = new ConcurrentHashMap<>();

    /** chunkId → 文档ID 映射 */
    private final ConcurrentHashMap<Long, Long> chunkDocMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("VectorStoreWrapper 初始化完成 (v2 Spring AI Alibaba 版)");
    }

    /**
     * 插入或更新向量
     */
    public void upsert(Long chunkId, Long documentId, List<Double> vector) {
        store.put(chunkId, listToArray(vector));
        chunkDocMap.put(chunkId, documentId);
    }

    /**
     * 批量插入向量
     */
    public void upsertBatch(Map<Long, VectorEntry> entries) {
        entries.forEach((chunkId, entry) -> {
            store.put(chunkId, listToArray(entry.getVector()));
            chunkDocMap.put(chunkId, entry.getDocumentId());
        });
        log.debug("批量插入 {} 条向量", entries.size());
    }

    /**
     * 余弦相似度 Top-K 检索
     */
    public List<SearchResult> search(List<Double> queryVector, int topK) {
        float[] query = listToArray(queryVector);

        return store.entrySet().parallelStream()
                .map(entry -> {
                    float similarity = cosineSimilarity(query, entry.getValue());
                    return new SearchResult(entry.getKey(), chunkDocMap.get(entry.getKey()), similarity);
                })
                .filter(r -> r.getScore() > 0.3)
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 删除文档的所有向量
     */
    public void deleteByDocumentId(Long documentId) {
        List<Long> toRemove = new ArrayList<>();
        chunkDocMap.forEach((chunkId, docId) -> {
            if (docId.equals(documentId)) {
                toRemove.add(chunkId);
            }
        });
        toRemove.forEach(chunkId -> {
            store.remove(chunkId);
            chunkDocMap.remove(chunkId);
        });
        log.debug("删除文档 {} 的 {} 条向量", documentId, toRemove.size());
    }

    /**
     * 删除单个向量
     */
    public void delete(Long chunkId) {
        store.remove(chunkId);
        chunkDocMap.remove(chunkId);
    }

    /**
     * 获取向量总数
     */
    public int size() {
        return store.size();
    }

    // ========== 数学计算 ==========

    /**
     * 余弦相似度
     * cos(θ) = (A·B) / (||A|| × ||B||)
     */
    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        float denominator = (float) (Math.sqrt(normA) * Math.sqrt(normB));
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    private float[] listToArray(List<Double> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).floatValue();
        }
        return arr;
    }

    // ========== 内部数据结构 ==========

    @Data
    public static class VectorEntry {
        private final Long documentId;
        private final List<Double> vector;
    }

    @Data
    public static class SearchResult {
        private final Long chunkId;
        private final Long documentId;
        private final float score;
    }
}
