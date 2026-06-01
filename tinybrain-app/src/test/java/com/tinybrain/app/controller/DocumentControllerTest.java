package com.tinybrain.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文档管理接口集成测试
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
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // 登录获取 Token
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"password\"}"))
                .andReturn();
        authToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    // ==================== 创建文档 ====================

    @Test
    void create_withValidData_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Doc\",\"content\":\"Hello World\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test Doc"));
    }

    @Test
    void create_withoutToken_shouldReturnError() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Hello\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400);
                });
    }

    @Test
    void create_withEmptyTitle_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 查询文档列表 ====================

    @Test
    void queryPage_shouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void queryPage_withKeyword_shouldFilter() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 获取文档详情 ====================

    @Test
    void getDetail_withValidId_shouldReturnDoc() throws Exception {
        // 先创建一个文档
        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Detail Test\",\"content\":\"Content for detail\"}"))
                .andReturn();

        long docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Detail Test"));
    }

    @Test
    void getDetail_withInvalidId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/documents/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 删除文档 ====================

    @Test
    void delete_ownDocument_shouldSucceed() throws Exception {
        // 创建
        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"To Delete\",\"content\":\"Delete me\"}"))
                .andReturn();

        long docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 删除
        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_nonExistentDoc_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/documents/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 更新文档 ====================

    @Test
    void update_ownDocument_shouldSucceed() throws Exception {
        // 创建
        MvcResult createResult = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Original\",\"content\":\"Original content\"}"))
                .andReturn();

        long docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 更新
        mockMvc.perform(put("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"content\":\"Updated content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    // ==================== 上传文件 ====================

    @Test
    void uploadFile_withValidFile_shouldSucceed() throws Exception {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "test.md", "text/markdown",
                        "# Test\nHello World".getBytes());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken)
                        .param("contentType", "markdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("test"));
    }

    @Test
    void uploadFile_withEmptyFile_shouldReturn400() throws Exception {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "empty.md", "text/markdown", new byte[0]);

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
