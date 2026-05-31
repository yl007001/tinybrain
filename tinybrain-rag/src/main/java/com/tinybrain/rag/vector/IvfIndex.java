package com.tinybrain.rag.vector;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * IVF (Inverted File) 倒排索引 — ANN 近似最近邻搜索
 * <p>
 * 核心思想：将向量空间划分为 K 个聚类（Voronoi 单元），
 * 搜索时只在与查询最接近的 nprobe 个聚类中搜索。
 * <p>
 * 复杂度分析：
 * - 暴力搜索: O(N × D)  —— N=向量数, D=维度
 * - IVF 搜索: O(K×D + nprobe × (N/K) × D)  —— K=聚类数
 * - 当 N >> K 时，加速比 ≈ K / nprobe
 * <p>
 * 示例：N=100,000, D=256, K=256, nprobe=4
 * - 暴力搜索: 100,000 × 256 = 25.6M FLOPS
 * - IVF 搜索: 256×256 + 4 × (100,000/256) × 256 ≈ 0.46M FLOPS
 * - 加速比: ~55x（代价：召回率从 100% 降为 ~90%）
 * <p>
 * ：
 * 1. IVF 为什么有效：数据聚集性（天然簇结构）
 * 2. K 的选择：K ≈ √N 是常用启发式
 * 3. nprobe 权衡：越大召回越高但越慢
 * 4. K-means++ 初始化：比随机初始化更稳定
 * 5. 边界问题：查询点接近簇边界时，需要搜索多个簇
 */
@Slf4j
public class IvfIndex {

    /** 聚类数量 K */
    private final int numClusters;

    /** 搜索时探测的聚类数 nprobe */
    private final int numProbes;

    /** 向量维度 */
    private final int dimension;

    /** 聚类中心 (K × D) */
    private float[][] centroids;

    /** 聚类 → 向量ID列表 */
    private final List<List<Long>> clusters;

    /** 向量ID → 所属聚类索引 */
    private final Map<Long, Integer> vectorToCluster;

    /** 是否已构建索引 */
    private volatile boolean built = false;

    /** 上次 K-means 的迭代次数 */
    private int lastIterations = 0;

    public IvfIndex(int numClusters, int numProbes, int dimension) {
        this.numClusters = numClusters;
        this.numProbes = Math.min(numProbes, numClusters);
        this.dimension = dimension;
        this.centroids = new float[numClusters][dimension];
        this.clusters = new ArrayList<>(numClusters);
        for (int i = 0; i < numClusters; i++) {
            clusters.add(new ArrayList<>());
        }
        this.vectorToCluster = new HashMap<>();
    }

    /**
     * 构建/重建索引（K-means 聚类）
     * <p>
     * 当向量数量远大于 K 时调用此方法。
     * 增量添加少量向量时不会触发重建。
     *
     * @param vectors 当前所有向量 (chunkId → vector)
     * @param force   是否强制重建（即使索引已存在）
     */
    public synchronized void buildIndex(Map<Long, float[]> vectors, boolean force) {
        if (vectors.size() < numClusters && !force) {
            log.warn("IVF: 向量数量({}) < 聚类数({})，跳过索引构建，回退暴力搜索", vectors.size(), numClusters);
            built = false;
            return;
        }

        int effectiveK = Math.min(numClusters, vectors.size());
        List<Map.Entry<Long, float[]>> entries = new ArrayList<>(vectors.entrySet());

        // 1. K-means++ 初始化聚类中心
        centroids = kmeansPlusPlusInit(entries, effectiveK);
        log.debug("IVF: K-means++ 初始化完成，K={}", effectiveK);

        // 2. K-means 迭代
        lastIterations = kmeansIterate(entries, effectiveK, 50);

        // 3. 分配向量到最近的簇
        assignToClusters(entries, effectiveK);

        built = true;
        log.info("IVF 索引构建完成: K={}, nprobe={}, 向量数={}, 迭代次数={}",
                effectiveK, numProbes, vectors.size(), lastIterations);
    }

    /**
     * 增量添加向量（不重建索引，只分配到最近的簇）
     */
    public synchronized void addVector(Long chunkId, float[] vector) {
        if (built && centroids != null) {
            int nearest = findNearestCentroid(vector);
            clusters.get(nearest).add(chunkId);
            vectorToCluster.put(chunkId, nearest);
        }
        // 若索引未构建，向量会在下次 buildIndex 时被分配
    }

    /**
     * 移除向量
     */
    public synchronized void removeVector(Long chunkId) {
        Integer clusterIdx = vectorToCluster.remove(chunkId);
        if (clusterIdx != null && clusterIdx < clusters.size()) {
            clusters.get(clusterIdx).remove(chunkId);
        }
    }

    /**
     * 搜索最近邻
     * <p>
     * 1. 计算查询向量到所有聚类中心的距离
     * 2. 选择最近的 nprobe 个簇
     * 3. 在这些簇中暴力搜索 Top-K
     * <p>
     * 若索引未构建，则返回空列表（由调用方回退暴力搜索）
     *
     * @param query  查询向量
     * @param topK   返回结果数
     * @param vectors 全局向量存储（用于未索引时的回退搜索及已索引时检索向量）
     * @return 排序后的搜索结果
     */
    public List<SearchCandidate> search(float[] query, int topK, Map<Long, float[]> vectors) {
        if (!built || centroids == null) {
            return Collections.emptyList();
        }

        // 1. 计算到所有聚类中心的距离
        float[] distToCentroids = new float[numClusters];
        for (int i = 0; i < numClusters; i++) {
            distToCentroids[i] = cosineDistance(query, centroids[i]);
        }

        // 2. 选择最近的 nprobe 个簇（使用部分排序）
        int[] nearestClusters = topKIndices(distToCentroids, numProbes);

        // 3. 在选中的簇中搜索
        List<SearchCandidate> candidates = new ArrayList<>();
        for (int ci : nearestClusters) {
            for (Long chunkId : clusters.get(ci)) {
                float[] vec = vectors.get(chunkId);
                if (vec != null) {
                    float sim = cosineSimilarity(query, vec);
                    if (sim > 0.3f) { // 低分过滤
                        candidates.add(new SearchCandidate(chunkId, sim));
                    }
                }
            }
        }

        // 4. 排序取 Top-K
        candidates.sort((a, b) -> Float.compare(b.score, a.score));
        return candidates.size() > topK ? candidates.subList(0, topK) : candidates;
    }

    /**
     * 获取索引状态
     */
    public IndexStatus getStatus() {
        return new IndexStatus(built, numClusters, numProbes, dimension,
                vectorToCluster.size(), lastIterations);
    }

    // ==================== K-means 实现 ====================

    /**
     * K-means++ 初始化：选择初始聚类中心
     * 第一个中心随机选，后续每个中心以概率 D(x)²/ΣD(x)² 选取
     * 其中 D(x) 是点 x 到最近已选中心的距离
     */
    private float[][] kmeansPlusPlusInit(List<Map.Entry<Long, float[]>> entries, int K) {
        float[][] centers = new float[K][dimension];
        Random rng = ThreadLocalRandom.current();

        // 第一个中心：随机选
        int firstIdx = rng.nextInt(entries.size());
        System.arraycopy(entries.get(firstIdx).getValue(), 0, centers[0], 0, dimension);

        // 剩余 K-1 个中心
        for (int c = 1; c < K; c++) {
            double[] minDists = new double[entries.size()];
            double totalDist = 0;

            for (int i = 0; i < entries.size(); i++) {
                float[] vec = entries.get(i).getValue();
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < c; j++) {
                    double dist = cosineDistance(vec, centers[j]);
                    if (dist < minDist) minDist = dist;
                }
                // D(x)²
                minDists[i] = minDist * minDist;
                totalDist += minDists[i];
            }

            // 按 D(x)²/ΣD(x)² 概率选择下一个中心
            double threshold = rng.nextDouble() * totalDist;
            double cumSum = 0;
            for (int i = 0; i < entries.size(); i++) {
                cumSum += minDists[i];
                if (cumSum >= threshold) {
                    System.arraycopy(entries.get(i).getValue(), 0, centers[c], 0, dimension);
                    break;
                }
            }
        }

        return centers;
    }

    /**
     * K-means 迭代：分配 → 更新，直到收敛
     */
    private int kmeansIterate(List<Map.Entry<Long, float[]>> entries, int K, int maxIter) {
        int[] assignments = new int[entries.size()];
        float[][] newCentroids = new float[K][dimension];
        int[] counts = new int[K];
        int iter;
        boolean changed;

        for (iter = 0; iter < maxIter; iter++) {
            // E 步：分配每个点到最近的中心
            changed = false;
            for (int i = 0; i < entries.size(); i++) {
                float[] vec = entries.get(i).getValue();
                int nearest = 0;
                float minDist = Float.MAX_VALUE;
                for (int j = 0; j < K; j++) {
                    float dist = cosineDistance(vec, centroids[j]);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = j;
                    }
                }
                if (assignments[i] != nearest) {
                    assignments[i] = nearest;
                    changed = true;
                }
            }

            if (!changed) break; // 收敛

            // M 步：重新计算聚类中心
            for (int j = 0; j < K; j++) {
                Arrays.fill(newCentroids[j], 0);
                counts[j] = 0;
            }
            for (int i = 0; i < entries.size(); i++) {
                int c = assignments[i];
                float[] vec = entries.get(i).getValue();
                for (int d = 0; d < dimension; d++) {
                    newCentroids[c][d] += vec[d];
                }
                counts[c]++;
            }
            for (int j = 0; j < K; j++) {
                if (counts[j] > 0) {
                    for (int d = 0; d < dimension; d++) {
                        newCentroids[j][d] /= counts[j];
                    }
                    // 归一化中心向量（余弦相似度仅关心方向，不关心模长）
                    float norm = 0;
                    for (int d = 0; d < dimension; d++) {
                        norm += newCentroids[j][d] * newCentroids[j][d];
                    }
                    norm = (float) Math.sqrt(norm);
                    if (norm > 1e-10f) {
                        for (int d = 0; d < dimension; d++) {
                            newCentroids[j][d] /= norm;
                        }
                    }
                    System.arraycopy(newCentroids[j], 0, centroids[j], 0, dimension);
                }
            }
        }

        return iter + 1;
    }

    /**
     * 将所有向量分配到最近的簇
     */
    private void assignToClusters(List<Map.Entry<Long, float[]>> entries, int K) {
        // 清空旧分配
        for (int i = 0; i < K; i++) {
            clusters.get(i).clear();
        }
        vectorToCluster.clear();

        for (Map.Entry<Long, float[]> entry : entries) {
            int nearest = findNearestCentroid(entry.getValue());
            clusters.get(nearest).add(entry.getKey());
            vectorToCluster.put(entry.getKey(), nearest);
        }
    }

    /**
     * 找到最近的聚类中心
     */
    private int findNearestCentroid(float[] vector) {
        int nearest = 0;
        float minDist = Float.MAX_VALUE;
        for (int i = 0; i < centroids.length; i++) {
            float dist = cosineDistance(vector, centroids[i]);
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    // ==================== 数学计算 ====================

    /**
     * 余弦相似度 cos(θ) = (A·B) / (||A|| × ||B||)
     * 值域 [-1, 1]，越高越相似
     */
    public static float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        float denominator = (float) (Math.sqrt(normA) * Math.sqrt(normB));
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    /**
     * 余弦距离 = 1 - cos(θ)，值域 [0, 2]
     */
    private static float cosineDistance(float[] a, float[] b) {
        return 1 - cosineSimilarity(a, b);
    }

    /**
     * 获取数组中前 K 个最小值的索引（部分排序）
     */
    private static int[] topKIndices(float[] array, int K) {
        int n = array.length;
        int k = Math.min(K, n);
        // 使用最大堆找前 K 个最小值
        PriorityQueue<Integer> heap = new PriorityQueue<>(
                (a, b) -> Float.compare(array[b], array[a])); // 最大堆
        for (int i = 0; i < n; i++) {
            if (heap.size() < k) {
                heap.offer(i);
            } else if (array[i] < array[heap.peek()]) {
                heap.poll();
                heap.offer(i);
            }
        }
        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            result[i] = heap.poll();
        }
        return result;
    }

    // ==================== 内部类型 ====================

    public record SearchCandidate(long chunkId, float score) {}

    public record IndexStatus(boolean built, int numClusters, int numProbes, int dimension,
                              int totalVectors, int lastIterations) {}
}
