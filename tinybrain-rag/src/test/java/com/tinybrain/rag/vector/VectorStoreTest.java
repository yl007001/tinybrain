package com.tinybrain.rag.vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存向量存储单元测试
 */
class VectorStoreTest {

    private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new VectorStore("./target/test-data/vectorstore");
    }

    @Test
    void upsertAndSearch_shouldReturnTopK() {
        // 插入三条向量
        vectorStore.upsert(1L, 10L, List.of(1.0, 0.0, 0.0, 0.0));
        vectorStore.upsert(2L, 10L, List.of(0.0, 1.0, 0.0, 0.0));
        vectorStore.upsert(3L, 20L, List.of(0.0, 0.0, 1.0, 0.0));

        // 检索与第一条最相似的向量（v2 相似度 ~0.099，v3 相似度 0，均被 score>0.3 过滤）
        List<VectorStore.SearchResult> results = vectorStore.search(List.of(1.0, 0.1, 0.0, 0.0), 2);

        assertEquals(1, results.size()); // 只有 v1 相似度 > 0.3
        assertEquals(1L, results.get(0).getChunkId()); // 最相似的应该是 chunk 1
        assertTrue(results.get(0).getScore() > 0.9);
    }

    @Test
    void search_shouldFilterLowSimilarity() {
        vectorStore.upsert(1L, 10L, List.of(1.0, 0.0, 0.0));

        // 查询一个完全不相似的向量
        List<VectorStore.SearchResult> results = vectorStore.search(List.of(0.0, 1.0, 0.0), 5);

        // 相似度 > 0.3 才返回，不同向量的余弦相似度为 0
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteByDocumentId_shouldRemoveAllRelatedVectors() {
        vectorStore.upsert(1L, 10L, List.of(1.0, 0.0));
        vectorStore.upsert(2L, 10L, List.of(0.0, 1.0));
        vectorStore.upsert(3L, 20L, List.of(0.5, 0.5));

        vectorStore.deleteByDocumentId(10L);

        assertEquals(1, vectorStore.size());
    }

    @Test
    void delete_shouldRemoveSingleVector() {
        vectorStore.upsert(1L, 10L, List.of(1.0, 0.0));
        vectorStore.upsert(2L, 10L, List.of(0.0, 1.0));

        vectorStore.delete(1L);

        assertEquals(1, vectorStore.size());
    }

    @Test
    void upsertBatch_shouldInsertAllVectors() {
        var entries = new java.util.HashMap<Long, VectorStore.VectorEntry>();
        entries.put(1L, new VectorStore.VectorEntry(10L, List.of(1.0, 0.0)));
        entries.put(2L, new VectorStore.VectorEntry(10L, List.of(0.0, 1.0)));

        vectorStore.upsertBatch(entries);

        assertEquals(2, vectorStore.size());
    }

    @Test
    void size_shouldReturnCorrectCount() {
        assertEquals(0, vectorStore.size());

        vectorStore.upsert(1L, 10L, List.of(1.0, 0.0));
        assertEquals(1, vectorStore.size());

        vectorStore.upsert(2L, 10L, List.of(0.0, 1.0));
        assertEquals(2, vectorStore.size());
    }
}
