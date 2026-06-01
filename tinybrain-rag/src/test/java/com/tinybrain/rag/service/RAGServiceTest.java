package com.tinybrain.rag.service;

import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.entity.DocumentChunk;
import com.tinybrain.knowledge.mapper.DocumentChunkMapper;
import com.tinybrain.knowledge.mapper.DocumentMapper;
import com.tinybrain.rag.chunk.DocChunkStrategy;
import com.tinybrain.rag.dto.EmbeddingRequest;
import com.tinybrain.rag.dto.EmbeddingResponse;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.vector.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG 服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RAGServiceTest {

    @Mock
    private LLMApiClient llmClient;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocChunkStrategy chunkStrategy;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    @InjectMocks
    private RAGService ragService;

    private Document testDoc;

    @BeforeEach
    void setUp() {
        testDoc = new Document();
        testDoc.setId(1L);
        testDoc.setTitle("Test Document");
        testDoc.setContent("This is a test document with enough content to be split into chunks for RAG testing.");
        testDoc.setUserId(1L);
    }

    @Test
    void indexDocument_shouldSplitAndEmbed() {
        when(documentMapper.selectById(1L)).thenReturn(testDoc);
        when(chunkStrategy.split(anyString())).thenReturn(List.of(
                DocChunkStrategy.Chunk.builder().index(0).content("chunk 1").build(),
                DocChunkStrategy.Chunk.builder().index(1).content("chunk 2").build()
        ));
        when(documentChunkMapper.insert(any(DocumentChunk.class))).thenReturn(1);

        // Mock 批量嵌入返回 2 个向量（每个 3 维）
        EmbeddingResponse embedResp = new EmbeddingResponse();
        EmbeddingResponse.EmbeddingData d1 = new EmbeddingResponse.EmbeddingData();
        d1.setIndex(0);
        d1.setEmbedding(List.of(0.1, 0.2, 0.3));
        EmbeddingResponse.EmbeddingData d2 = new EmbeddingResponse.EmbeddingData();
        d2.setIndex(1);
        d2.setEmbedding(List.of(0.4, 0.5, 0.6));
        embedResp.setData(List.of(d1, d2));
        when(llmClient.embed(any(EmbeddingRequest.class))).thenReturn(embedResp);

        ragService.indexDocument(1L);

        verify(documentChunkMapper, times(2)).insert(any(DocumentChunk.class));
        // upsert 被调用 2 次（deleteByDocumentId 也会被调用）
        verify(vectorStore, atLeastOnce()).deleteByDocumentId(eq(1L));
    }

    @Test
    void indexDocument_shouldThrowWhenDocNotFound() {
        when(documentMapper.selectById(999L)).thenReturn(null);
        assertThrows(Exception.class, () -> ragService.indexDocument(999L));
    }

    @Test
    void ask_shouldReturnAnswerWithChunks() {
        // 查询改写返回 null（不改写）
        when(llmClient.chat(anyString(), anyString())).thenReturn(null);
        // 向量化
        when(llmClient.embed(anyString())).thenReturn(List.of(0.5, 0.3, 0.1));
        // 向量检索
        List<VectorStore.SearchResult> hits = new ArrayList<>();
        hits.add(new VectorStore.SearchResult(1L, 1L, 0.9f));
        when(vectorStore.search(anyList(), anyInt())).thenReturn(hits);
        // 分块查询
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(1L);
        chunk.setDocumentId(1L);
        chunk.setContent("Spring Boot is a framework...");
        when(documentChunkMapper.selectById(1L)).thenReturn(chunk);
        when(documentMapper.selectById(1L)).thenReturn(testDoc);

        // 第二次 chat 调用返回回答（第一次是查询改写返回 null，第二次是生成回答）
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn(null)  // 查询改写
                .thenReturn("Spring Boot is a Java framework.");  // 生成回答

        RAGResult result = ragService.ask("What is Spring Boot?", 5);

        assertNotNull(result);
        assertNotNull(result.getAnswer());
    }

    @Test
    void ask_shouldHandleEmptySearchResults() {
        when(llmClient.chat(anyString(), anyString())).thenReturn(null);
        when(llmClient.embed(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(vectorStore.search(anyList(), anyInt())).thenReturn(new ArrayList<>());

        RAGResult result = ragService.ask("Unknown topic", 5);

        assertNotNull(result);
        assertNotNull(result.getAnswer());
        assertTrue(result.getChunks().isEmpty());
    }

    @Test
    void ask_shouldHandleEmbeddingFailure() {
        when(llmClient.chat(anyString(), anyString())).thenReturn(null);
        when(llmClient.embed(anyString())).thenReturn(null);

        assertThrows(Exception.class, () -> ragService.ask("test question", 5));
    }
}
