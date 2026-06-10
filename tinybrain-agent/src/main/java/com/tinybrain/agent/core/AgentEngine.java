package com.tinybrain.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import com.tinybrain.agent.plugin.impl.MCPTool;
import com.tinybrain.mcp.MCPClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 引擎
 * <p>
 * 核心编排器，管理工具注册和 Function Calling 调度。
 * <p>
 * Agent 工作流程：
 * 1. 接收用户输入
 * 2. 构建包含工具描述的 System Prompt
 * 3. LLM 判断是否需要调用工具 → 返回 Function Call
 * 4. 执行工具 → 将结果回填给 LLM
 * 5. LLM 生成最终回复
 * <p>
 * ：
 * - Tool Use 模式 vs 传统 API 调用
 * - Function Calling 本质：LLM 输出结构化 JSON，系统执行
 * - 循环次数控制：防止 Agent 陷入无限循环
 */
@Slf4j
@Component
public class AgentEngine {

    private final Map<String, AgentTool> toolRegistry = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private MCPClient mcpClient;

    /**
     * 设置 MCP 客户端
     */
    @Autowired(required = false)
    public void setMcpClient(MCPClient mcpClient) {
        this.mcpClient = mcpClient;
        log.info("MCP 客户端已注入");
    }

    /**
     * 注册工具
     */
    public void registerTool(AgentTool tool) {
        toolRegistry.put(tool.getName(), tool);
        log.info("Agent 工具注册: {} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * 批量注册工具
     */
    public void registerTools(List<AgentTool> tools) {
        tools.forEach(this::registerTool);
    }

    /**
     * 注册 MCP 工具
     * <p>
     * 从 MCP 客户端获取可用工具并注册到 Agent 引擎
     */
    public void registerMCPTools() {
        if (mcpClient == null || !mcpClient.isAlive()) {
            log.warn("MCP 客户端未初始化或不可用，跳过 MCP 工具注册");
            return;
        }

        try {
            mcpClient.getAvailableTools().forEach(toolDefinition -> {
                MCPTool mcpTool = new MCPTool(mcpClient, toolDefinition);
                registerTool(mcpTool);
            });
            log.info("MCP 工具注册完成");
        } catch (Exception e) {
            log.error("MCP 工具注册失败", e);
        }
    }

    /**
     * 获取已注册的所有工具
     */
    public Map<String, AgentTool> getTools() {
        return Collections.unmodifiableMap(toolRegistry);
    }

    /**
     * 获取工具定义（OpenAI Tool Calling 格式）
     * <p>
     * 用于构建 LLM 的 tools 参数，让 LLM 知道有哪些工具可用。
     */
    public List<ObjectNode> getToolDefinitions() {
        List<ObjectNode> definitions = new ArrayList<>();
        for (AgentTool tool : toolRegistry.values()) {
            ObjectNode toolDef = mapper.createObjectNode();
            toolDef.put("type", "function");

            ObjectNode function = mapper.createObjectNode();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            function.set("parameters", tool.getParametersSchema());

            toolDef.set("function", function);
            definitions.add(toolDef);
        }
        return definitions;
    }

    /**
     * 执行工具调用
     *
     * @param toolName 工具名称
     * @param argsJson 参数 JSON
     * @return 工具执行结果
     */
    public String executeTool(String toolName, String argsJson) {
        AgentTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            return "错误：未找到工具 '" + toolName + "'";
        }

        try {
            JsonNode args = mapper.readTree(argsJson);
            String result = tool.execute(args, mapper);
            log.debug("工具执行完成: {} → {}", toolName, result);
            return result;
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return "工具执行出错: " + e.getMessage();
        }
    }

    /**
     * 构建 System Prompt（包含工具描述信息）
     */
    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个 AI 助手。你可以使用以下工具：\n\n");

        for (AgentTool tool : toolRegistry.values()) {
            sb.append("## ").append(tool.getName()).append("\n");
            sb.append("描述: ").append(tool.getDescription()).append("\n");
            sb.append("参数: ").append(tool.getParametersSchema().toPrettyString()).append("\n\n");
        }

        sb.append("重要：当你需要调用工具时，必须且只能使用以下 JSON 格式，不要使用任何其他格式：\n");
        sb.append("{\"tool\": \"工具名称\", \"args\": {\"参数名\": \"参数值\"}}\n\n");
        sb.append("示例：\n");
        sb.append("- 调用知识搜索: {\"tool\": \"knowledge_search\", \"args\": {\"query\": \"Java线程\"}}\n");
        sb.append("- 调用网络搜索: {\"tool\": \"web_search\", \"args\": {\"query\": \"今日新闻\"}}\n");
        sb.append("- 调用计算器: {\"tool\": \"calculator\", \"args\": {\"expression\": \"2+3*4\"}}\n");
        sb.append("- 调用获取时间: {\"tool\": \"get_datetime\", \"args\": {}}\n\n");
        sb.append("注意：\n");
        sb.append("1. 只输出一个 JSON 对象，不要输出多个\n");
        sb.append("2. 不要使用 XML 或其他格式，只用 JSON\n");
        sb.append("3. 工具执行后，将结果整合到你的回答中\n");

        return sb.toString();
    }
}
