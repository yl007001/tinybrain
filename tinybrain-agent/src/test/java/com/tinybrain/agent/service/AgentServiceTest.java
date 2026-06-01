package com.tinybrain.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.AgentRequest;
import com.tinybrain.agent.dto.AgentResponse;
import com.tinybrain.rag.dto.LLMChatRequest;
import com.tinybrain.rag.dto.LLMChatResponse;
import com.tinybrain.rag.service.LLMApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Agent 服务单元测试
 * <p>
 * 覆盖：正常对话、工具调用循环、最大迭代限制、会话清理
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentEngine agentEngine;

    @Mock
    private LLMApiClient llmClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private AgentService agentService;

    private AgentRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new AgentRequest();
        testRequest.setMessage("Hello");
        testRequest.setSessionId("test-session");

        AgentRequest.Config config = new AgentRequest.Config();
        config.setMaxIterations(5);
        config.setSystemPromptSuffix("");
        testRequest.setConfig(config);

        when(agentEngine.buildSystemPrompt()).thenReturn("You are an AI assistant.");
    }

    @Test
    void process_shouldReturnDirectAnswer() {
        // LLM 直接回答，不调用工具
        LLMChatResponse response = createLLMResponse("Hello! How can I help you?");
        when(llmClient.chat(any(LLMChatRequest.class))).thenReturn(response);

        AgentResponse result = agentService.process(testRequest);

        assertNotNull(result);
        assertEquals("Hello! How can I help you?", result.getReply());
        assertEquals(1, result.getIterations());
        assertTrue(result.getToolCalls().isEmpty());
    }

    @Test
    void process_shouldHandleToolCall() {
        // 第一次 LLM 返回工具调用，第二次返回最终回答
        LLMChatResponse toolCallResponse = createLLMResponse(
                "{\"tool\": \"calculator\", \"args\": {\"expression\": \"2+2\"}}");
        LLMChatResponse finalResponse = createLLMResponse("2 + 2 = 4");

        when(llmClient.chat(any(LLMChatRequest.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);
        when(agentEngine.executeTool("calculator", "{\"expression\":\"2+2\"}"))
                .thenReturn("2 + 2 = 4");

        AgentResponse result = agentService.process(testRequest);

        assertNotNull(result);
        assertEquals(2, result.getIterations());
        assertEquals(1, result.getToolCalls().size());
        assertEquals("calculator", result.getToolCalls().get(0).getToolName());
    }

    @Test
    void process_shouldRespectMaxIterations() {
        // LLM 一直返回工具调用
        LLMChatResponse toolCallResponse = createLLMResponse(
                "{\"tool\": \"calculator\", \"args\": {\"expression\": \"1+1\"}}");

        when(llmClient.chat(any(LLMChatRequest.class))).thenReturn(toolCallResponse);
        when(agentEngine.executeTool(anyString(), anyString())).thenReturn("2");

        testRequest.getConfig().setMaxIterations(3);

        AgentResponse result = agentService.process(testRequest);

        assertEquals(3, result.getIterations());
    }

    @Test
    void process_shouldHandleNullLLMResponse() {
        when(llmClient.chat(any(LLMChatRequest.class))).thenReturn(null);

        AgentResponse result = agentService.process(testRequest);

        assertNotNull(result);
        assertEquals("处理完成", result.getReply());
    }

    @Test
    void clearSession_shouldRemoveHistory() {
        // 先有一次对话
        LLMChatResponse response = createLLMResponse("Hi");
        when(llmClient.chat(any(LLMChatRequest.class))).thenReturn(response);

        agentService.process(testRequest);

        // 清除会话
        agentService.clearSession("test-session");

        // 再次对话，验证历史已清除（不会报错）
        AgentResponse result = agentService.process(testRequest);
        assertNotNull(result);
    }

    private LLMChatResponse createLLMResponse(String content) {
        LLMChatResponse response = new LLMChatResponse();
        response.setId("test-id");
        LLMChatResponse.Choice choice = new LLMChatResponse.Choice();
        LLMChatResponse.Message msg = new LLMChatResponse.Message();
        msg.setRole("assistant");
        msg.setContent(content);
        choice.setMessage(msg);
        response.setChoices(List.of(choice));
        return response;
    }
}
