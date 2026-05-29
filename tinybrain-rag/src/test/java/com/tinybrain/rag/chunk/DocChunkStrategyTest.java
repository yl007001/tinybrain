package com.tinybrain.rag.chunk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Document chunking strategy unit tests.
 */
class DocChunkStrategyTest {

    private DocChunkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DocChunkStrategy();
    }

    @Test
    void split_shouldReturnSingleChunkForShortText() {
        // Text shorter than chunkSize (500) returns 1 chunk
        String text = "Paragraph A.\n\nParagraph B.\n\nParagraph C.";
        List<DocChunkStrategy.Chunk> chunks = strategy.split(text);

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).getIndex());
        assertTrue(chunks.get(0).getContent().contains("Paragraph A"));
    }

    @Test
    void split_shouldHandleSingleParagraph() {
        String text = "Single paragraph of text.";
        List<DocChunkStrategy.Chunk> chunks = strategy.split(text);

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).getIndex());
    }

    @Test
    void split_shouldHandleEmptyText() {
        List<DocChunkStrategy.Chunk> chunks = strategy.split("");

        assertTrue(chunks.isEmpty());
    }

    @Test
    void split_shouldHandleNullText() {
        List<DocChunkStrategy.Chunk> chunks = strategy.split(null);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void split_shouldChunkLongText() {
        // Generate text long enough to need multiple chunks
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is a long paragraph designed to fill up space and exceed the default chunk size limit. ");
        }
        String text = sb.toString();

        List<DocChunkStrategy.Chunk> chunks = strategy.split(text);

        // Should produce at least 2 chunks
        assertTrue(chunks.size() >= 2);
        for (DocChunkStrategy.Chunk chunk : chunks) {
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isEmpty());
        }
    }

    @Test
    void split_shouldIndexSequentially() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Long paragraph content for sequential indexing. ");
        }
        String text = sb.toString();

        List<DocChunkStrategy.Chunk> chunks = strategy.split(text);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getIndex());
        }
    }

    @Test
    void split_withCustomChunkSize_shouldRespectLimit() {
        String text = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z";
        // Use chunk size 10 to force splitting
        List<DocChunkStrategy.Chunk> chunks = strategy.split(text, 10, 0);

        assertTrue(chunks.size() >= 2);
        for (DocChunkStrategy.Chunk chunk : chunks) {
            assertTrue(chunk.getContent().length() <= 10,
                    "Chunk content '" + chunk.getContent() + "' exceeds limit");
        }
    }
}
