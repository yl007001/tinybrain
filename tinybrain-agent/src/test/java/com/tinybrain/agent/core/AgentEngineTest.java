package com.tinybrain.agent.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 引擎单元测试
 */
class AgentEngineTest {

    private AgentEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AgentEngine();
    }

    @Test
    void registerTool_shouldAddToolToRegistry() {
        AgentTool tool = new TestTool("test-tool", "测试工具");
        engine.registerTool(tool);

        assertEquals(1, engine.getTools().size());
        assertTrue(engine.getTools().containsKey("test-tool"));
    }

    @Test
    void registerTools_shouldAddAllTools() {
        engine.registerTools(List.of(
                new TestTool("tool1", "工具1"),
                new TestTool("tool2", "工具2")
        ));

        assertEquals(2, engine.getTools().size());
    }

    @Test
    void executeTool_shouldReturnResult() {
        engine.registerTool(new TestTool("hello", "打招呼工具"));

        String result = engine.executeTool("hello", "{\"name\": \"TinyBrain\"}");

        assertEquals("你好, TinyBrain!", result);
    }

    @Test
    void executeTool_shouldReturnErrorForUnknownTool() {
        String result = engine.executeTool("unknown", "{}");

        assertTrue(result.contains("未找到工具"));
    }

    @Test
    void executeTool_shouldHandleExceptions() {
        AgentTool failingTool = new AgentTool() {
            @Override
            public String getName() { return "fail"; }
            @Override
            public String getDescription() { return "总是失败"; }
            @Override
            public ObjectNode getParametersSchema() { return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(); }
            @Override
            public String execute(com.fasterxml.jackson.databind.JsonNode args, com.fasterxml.jackson.databind.ObjectMapper mapper) {
                throw new RuntimeException("模拟失败");
            }
        };
        engine.registerTool(failingTool);

        String result = engine.executeTool("fail", "{}");

        assertTrue(result.contains("工具执行出错"));
    }

    @Test
    void getToolDefinitions_shouldReturnDefinitions() {
        engine.registerTool(new TestTool("test", "测试工具"));

        List<ObjectNode> defs = engine.getToolDefinitions();

        assertEquals(1, defs.size());
        assertEquals("function", defs.get(0).get("type").asText());
        assertEquals("test", defs.get(0).get("function").get("name").asText());
    }

    @Test
    void buildSystemPrompt_shouldIncludeAllTools() {
        engine.registerTool(new TestTool("tool-a", "工具A"));
        engine.registerTool(new TestTool("tool-b", "工具B"));

        String prompt = engine.buildSystemPrompt();

        assertTrue(prompt.contains("tool-a"));
        assertTrue(prompt.contains("tool-b"));
        assertTrue(prompt.contains("工具A"));
        assertTrue(prompt.contains("工具B"));
    }

    @Test
    void getTools_shouldReturnUnmodifiableMap() {
        engine.registerTool(new TestTool("immutable", "不可变测试"));

        Map<String, AgentTool> tools = engine.getTools();
        assertThrows(UnsupportedOperationException.class, () -> tools.put("new", null));
    }

    /** 测试用工具类 */
    private static class TestTool implements AgentTool {
        private final String name;
        private final String description;
        private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        TestTool(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getDescription() { return description; }

        @Override
        public ObjectNode getParametersSchema() {
            ObjectNode schema = mapper.createObjectNode();
            schema.put("type", "object");
            schema.putObject("properties").putObject("name").put("type", "string");
            return schema;
        }

        @Override
        public String execute(com.fasterxml.jackson.databind.JsonNode args, com.fasterxml.jackson.databind.ObjectMapper m) {
            String name = args.has("name") ? args.get("name").asText() : "World";
            return "你好, " + name + "!";
        }
    }
}
