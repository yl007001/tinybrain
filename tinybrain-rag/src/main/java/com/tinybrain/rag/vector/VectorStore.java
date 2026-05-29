package com.tinybrain.rag.vector;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 内存向量存储
 * <p>
 * 使用 ConcurrentHashMap 存储文档块向量，支持：
 * - 插入向量
 * - 余弦相似度检索（Top-K）
 * - 按文档ID删除
 * <p>
 * 设计考量：
 * - Phase 2：内存存储，快速开发验证
 * - Phase 2+：替换为 ChromaDB / Milvus 持久化方案
 * <p>
 * 面试重点：
 * 1. 余弦相似度 vs 欧氏距离 vs 点积
 * 2. 全量扫描 vs ANN (Approximate Nearest Neighbor) 索引
 * 3. HNSW / IVF 等向量索引原理
 */
@Slf4j
@Component
public class VectorStore {

    /** 向量维度（由 LLM 模型决定，DeepSeek/OpenAI 通常为 1536 或 1024） */
    private int dimension = 1024;

    /** 存储结构：chunkId → 向量 */
    private final ConcurrentHashMap<Long, float[]> store = new ConcurrentHashMap<>();

    /** chunkId → 文档ID 映射 */
    private final ConcurrentHashMap<Long, Long> chunkDocMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("VectorStore 初始化完成，维度: {}", dimension);
    }

    /**
     * 设置向量维度（在首次插入前调用）
     */
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    /**
     * 插入或更新向量
     *
     * @param chunkId    分块ID
     * @param documentId 文档ID
     * @param vector     浮点数向量
     */
    public void upsert(Long chunkId, Long documentId, List<Double> vector) {
        float[] arr = listToArray(vector);
        store.put(chunkId, arr);
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
     *
     * @param queryVector 查询向量
     * @param topK        返回前 K 个结果
     * @return 相似度排序结果
     */
    public List<SearchResult> search(List<Double> queryVector, int topK) {
        float[] query = listToArray(queryVector);

        return store.entrySet().parallelStream()
                .map(entry -> {
                    float similarity = cosineSimilarity(query, entry.getValue());
                    return new SearchResult(entry.getKey(), chunkDocMap.get(entry.getKey()), similarity);
                })
                .filter(r -> r.getScore() > 0.3) // 过滤低相似度结果
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
     * 值域 [-1, 1]，越高越相似
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
