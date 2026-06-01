package com.tinybrain.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RAG 接口集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "tinybrain.llm.api-key=test-key"
})
class RAGControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"demo\",\"password\":\"password\"}"))
                .andReturn();
        authToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    void stats_shouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/rag/stats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalVectors").exists());
    }

    @Test
    void ask_withBlankQuestion_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/rag/ask")
                        .header("Authorization", "Bearer " + authToken)
                        .param("question", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void ask_withoutToken_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/rag/ask")
                        .param("question", "test"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400);
                });
    }

    @Test
    void indexDocument_withValidId_shouldSubmitTask() throws Exception {
        // 先创建一个文档
        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType("application/json")
                        .content("{\"title\":\"RAG Test\",\"content\":\"Spring Boot is a framework.\"}"))
                .andReturn();

        long docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 索引文档（异步执行，立即返回）
        mockMvc.perform(post("/api/rag/index/" + docId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
