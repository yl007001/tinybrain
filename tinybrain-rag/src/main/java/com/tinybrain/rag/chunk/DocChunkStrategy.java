package com.tinybrain.rag.chunk;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块策略
 * <p>
 * 将长文档切割为语义完整的块，用于后续向量化 + RAG 检索。
 * <p>
 * 分块策略权衡：
 * - 块太小：丢失上下文语义，检索结果碎片化
 * - 块太大：超出 Token 限制，包含无关噪声
 * - 最佳实践：256-512 tokens，根据文档类型调整
 * <p>
 * 面试重点：
 * 1. 固定大小分块 vs 语义分块 vs 递归分块
 * 2. 重叠(overlap)的作用：防止边界断句造成信息丢失
 * 3. 结构化分块：按 Markdown 标题 / 代码函数 / 段落分割
 */
@Component
public class DocChunkStrategy {

    /** 默认块大小（字符数，约 200-300 tokens） */
    private static final int DEFAULT_CHUNK_SIZE = 500;

    /** 相邻块重叠大小 */
    private static final int DEFAULT_OVERLAP = 100;

    /**
     * 分块
     *
     * @param content 文档内容
     * @return 分块列表
     */
    public List<Chunk> split(String content) {
        return split(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 自定义参数分块
     */
    public List<Chunk> split(String content, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int index = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 尝试在段落边界分割（提高语义完整性）
            if (end < content.length()) {
                int paragraphBreak = findParagraphBreak(content, end, chunkSize / 2);
                if (paragraphBreak > 0) {
                    end = paragraphBreak;
                }
            }

            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(Chunk.builder()
                        .index(index++)
                        .content(chunkText)
                        .startPos(start)
                        .endPos(end)
                        .build());
            }

            // 移动起点（带重叠）
            start = end - overlap;
            if (start >= content.length()) break;
            if (start < 0) start = 0;
        }

        return chunks;
    }

    /**
     * 向上查找最近的段落分隔符
     */
    private int findParagraphBreak(String content, int fromPos, int searchBack) {
        int searchStart = Math.max(0, fromPos - searchBack);
        for (int i = fromPos - 1; i >= searchStart; i--) {
            char c = content.charAt(i);
            if (c == '\n' || c == '\r') {
                // 找到段落边界，分割点包含换行符
                return i + 1;
            }
        }
        return -1; // 没找到合适的分割点
    }

    @Data
    @Builder
    public static class Chunk {
        private int index;
        private String content;
        private int startPos;
        private int endPos;
    }
}
