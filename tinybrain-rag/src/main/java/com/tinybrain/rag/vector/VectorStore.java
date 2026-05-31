package com.tinybrain.rag.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 持久化向量存储 — 支持 IVF ANN 索引 + 暴力搜索回退
 * <p>
 * 使用 ConcurrentHashMap 存储文档块向量，支持：
 * - 插入向量
 * - 余弦相似度检索（Top-K）
 * - 按文档ID删除
 * - 文件持久化（JSON），重启后自动恢复
 * - IVF 倒排索引加速（当向量数 > 1000 时自动启用）
 * - 延时写入，避免频繁 I/O
 * <p>
 * 设计考量：
 * - Phase 2：内存存储 + JSON 文件持久化，快速开发验证
 * - Phase 3+：可替换为 ChromaDB / Milvus / FAISS 生产方案
 * <p>
 * 面试重点：
 * 1. 余弦相似度 vs 欧氏距离 vs 点积
 * 2. 全量扫描 vs ANN (Approximate Nearest Neighbor) 索引
 * 3. HNSW / IVF 等向量索引原理
 * 4. 读写锁保证并发安全
 * 5. IVF 参数调优：K 和 nprobe 对速度/召回率的影响
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

    /** IVF 倒排索引（加速近似最近邻搜索） */
    private IvfIndex ivfIndex;

    /** 触发 IVF 重建的最小向量数 */
    private static final int IVF_MIN_VECTORS = 1000;

    /** 数据持久化路径 */
    private final String dataDir;

    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /** 延时写入调度器 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "vector-store-writer");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean dirty = false;
    private static final long FLUSH_DELAY_MS = 5000;

    public VectorStore(@Value("${tinybrain.vectorstore.data-dir:./data/vectorstore}") String dataDir) {
        this.dataDir = dataDir;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        loadFromDisk();
        initIvfIndex();
        scheduler.scheduleWithFixedDelay(this::flushIfDirty, FLUSH_DELAY_MS, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
        // 定期检查是否需要重建 IVF 索引（当有新向量加入后）
        scheduler.scheduleWithFixedDelay(this::maybeRebuildIvf, 30, 30, TimeUnit.SECONDS);
        log.info("VectorStore 初始化完成，维度: {}, 已恢复 {} 条向量, IVF: {}",
                dimension, store.size(), ivfIndex != null && ivfIndex.getStatus().built() ? "已启用" : "未启用");
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        flushToDisk();
        log.info("VectorStore 已关闭，持久化 {} 条向量, IVF状态: {}", store.size(),
                ivfIndex != null ? ivfIndex.getStatus().built() : "N/A");
    }

    // ========== IVF 索引管理 ==========

    /**
     * 初始化 IVF 索引（在恢复数据后调用）
     */
    private void initIvfIndex() {
        // K ≈ √N 是常用经验值，但至少 10
        int N = store.size();
        int K = Math.max(10, (int) Math.sqrt(N * 10));
        K = Math.min(K, Math.max(N / 10, 10));
        // 限制最大值，避免过多空簇
        K = Math.min(K, 512);
        int nprobe = Math.max(1, K / 50); // 搜索约 2% 的簇

        ivfIndex = new IvfIndex(K, nprobe, dimension);

        if (N > 0) {
            Map<Long, float[]> snapshot = new HashMap<>(store);
            ivfIndex.buildIndex(snapshot, false);
            if (ivfIndex.getStatus().built()) {
                log.info("IVF 索引已构建: K={}, nprobe={}, 向量数={}", K, nprobe, N);
            }
        }
    }

    /**
     * 定期检查是否需要重建 IVF 索引
     */
    private void maybeRebuildIvf() {
        if (store.size() < IVF_MIN_VECTORS) return;
        if (ivfIndex == null || !ivfIndex.getStatus().built()) {
            rwLock.readLock().lock();
            try {
                ivfIndex.buildIndex(new HashMap<>(store), false);
                if (ivfIndex.getStatus().built()) {
                    log.info("IVF 索引已自动重建: {}", ivfIndex.getStatus());
                }
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    /**
     * 设置向量维度（在首次插入前调用）
     */
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    /**
     * 插入或更新向量
     */
    public void upsert(Long chunkId, Long documentId, List<Double> vector) {
        rwLock.writeLock().lock();
        try {
            float[] arr = listToArray(vector);
            store.put(chunkId, arr);
            chunkDocMap.put(chunkId, documentId);
            if (ivfIndex != null) {
                ivfIndex.addVector(chunkId, arr);
            }
            markDirty();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 批量插入向量
     */
    public void upsertBatch(Map<Long, VectorEntry> entries) {
        rwLock.writeLock().lock();
        try {
            entries.forEach((chunkId, entry) -> {
                float[] arr = listToArray(entry.getVector());
                store.put(chunkId, arr);
                chunkDocMap.put(chunkId, entry.getDocumentId());
                if (ivfIndex != null) {
                    ivfIndex.addVector(chunkId, arr);
                }
            });
            markDirty();
            log.debug("批量插入 {} 条向量", entries.size());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 余弦相似度 Top-K 检索
     * <p>
     * 使用两级搜索策略：
     * 1. IVF ANN 搜索（当索引可用且向量数 > 阈值时）
     * 2. 暴力搜索回退（当 IVF 索引未构建或搜索无结果时）
     * <p>
     * 对两种结果进行融合去重，保证召回率。
     */
    public List<SearchResult> search(List<Double> queryVector, int topK) {
        float[] query = listToArray(queryVector);
        rwLock.readLock().lock();
        try {
            // 尝试 IVF ANN 搜索
            List<IvfIndex.SearchCandidate> annCandidates = Collections.emptyList();
            if (ivfIndex != null && ivfIndex.getStatus().built()) {
                annCandidates = ivfIndex.search(query, topK, store);
            }

            // 暴力搜索（作为回退或补充）
            List<SearchResult> bruteForceResults = bruteForceSearch(query, topK);

            // 如果 IVF 有效，融合结果
            if (!annCandidates.isEmpty()) {
                Set<Long> seen = bruteForceResults.stream()
                        .map(SearchResult::getChunkId)
                        .collect(Collectors.toSet());

                // 将 IVF 中但暴力搜索没覆盖到的结果添加进来
                for (IvfIndex.SearchCandidate ann : annCandidates) {
                    if (!seen.contains(ann.chunkId())) {
                        Long docId = chunkDocMap.get(ann.chunkId());
                        if (docId != null) {
                            bruteForceResults.add(new SearchResult(ann.chunkId(), docId, ann.score()));
                        }
                    }
                }

                // 重新排序
                bruteForceResults.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
                if (bruteForceResults.size() > topK * 2) {
                    return bruteForceResults.subList(0, topK * 2);
                }
            }

            return bruteForceResults;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 暴力全量搜索（兜底）
     */
    private List<SearchResult> bruteForceSearch(float[] query, int topK) {
        return store.entrySet().parallelStream()
                .map(entry -> {
                    float similarity = IvfIndex.cosineSimilarity(query, entry.getValue());
                    Long docId = chunkDocMap.get(entry.getKey());
                    return new SearchResult(entry.getKey(), docId, similarity);
                })
                .filter(r -> r.getScore() > 0.3)
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(topK * 2) // 多取一些，便于与 IVF 结果融合
                .collect(Collectors.toList());
    }

    /**
     * 获取搜索策略统计信息
     */
    public SearchStats getSearchStats() {
        boolean ivfActive = ivfIndex != null && ivfIndex.getStatus().built();
        return new SearchStats(
                store.size(),
                ivfActive,
                ivfActive ? ivfIndex.getStatus().numClusters() : 0,
                ivfActive ? ivfIndex.getStatus().numProbes() : 0
        );
    }

    @Data
    public static class SearchStats {
        private final int totalVectors;
        private final boolean ivfActive;
        private final int ivfClusters;
        private final int ivfProbes;
    }

    /**
     * 删除文档的所有向量
     */
    public void deleteByDocumentId(Long documentId) {
        rwLock.writeLock().lock();
        try {
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
            if (!toRemove.isEmpty()) {
                markDirty();
                log.debug("删除文档 {} 的 {} 条向量", documentId, toRemove.size());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 删除单个向量
     */
    public void delete(Long chunkId) {
        rwLock.writeLock().lock();
        try {
            store.remove(chunkId);
            chunkDocMap.remove(chunkId);
            markDirty();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 获取向量总数
     */
    public int size() {
        rwLock.readLock().lock();
        try {
            return store.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // ========== 持久化 ==========

    private void markDirty() {
        dirty = true;
    }

    private void flushIfDirty() {
        if (dirty) {
            flushToDisk();
        }
    }

    private void flushToDisk() {
        rwLock.readLock().lock();
        try {
            PersistedState state = new PersistedState();
            state.setDimension(dimension);
            state.setVersion(2);

            // 序列化向量（float[] → List<Double>）
            Map<Long, List<Double>> serializedStore = new HashMap<>();
            store.forEach((id, vec) -> {
                List<Double> list = new ArrayList<>(vec.length);
                for (float v : vec) {
                    list.add((double) v);
                }
                serializedStore.put(id, list);
            });
            state.setStore(serializedStore);

            // chunk → doc 映射
            Map<Long, Long> docMap = new HashMap<>(chunkDocMap);
            state.setChunkDocMap(docMap);

            // 写文件
            File dir = new File(dataDir);
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("无法创建向量存储目录: {}", dataDir);
                return;
            }
            File file = new File(dir, "vectors.json");
            mapper.writeValue(file, state);
            dirty = false;
            log.debug("向量存储已持久化到: {} ({} 条)", file.getAbsolutePath(), store.size());
        } catch (IOException e) {
            log.error("向量存储持久化失败: {}", e.getMessage());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void loadFromDisk() {
        try {
            File file = Paths.get(dataDir, "vectors.json").toFile();
            if (!file.exists()) {
                log.info("向量持久化文件不存在，使用空存储启动");
                return;
            }
            PersistedState state = mapper.readValue(file, PersistedState.class);
            if (state == null) return;

            this.dimension = state.getDimension();

            // 反序列化向量
            if (state.getStore() != null) {
                state.getStore().forEach((id, vec) -> {
                    store.put(id, listToArray(vec));
                });
            }
            if (state.getChunkDocMap() != null) {
                chunkDocMap.putAll(state.getChunkDocMap());
            }
            log.info("从磁盘恢复向量存储: {} 条向量, 维度={}", store.size(), dimension);
        } catch (IOException e) {
            log.warn("向量存储恢复失败，使用空存储启动: {}", e.getMessage());
        }
    }

    // ========== 工具方法 ==========

    private float[] listToArray(List<Double> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).floatValue();
        }
        return arr;
    }

    // ========== 持久化状态模型 ==========

    @Data
    private static class PersistedState {
        private int version = 2;
        private int dimension;
        private Map<Long, List<Double>> store = new HashMap<>();
        private Map<Long, Long> chunkDocMap = new HashMap<>();
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
