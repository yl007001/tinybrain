package com.tinybrain.agent.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Agent 工具接口
 * <p>
 * 所有可被 Agent 调用的工具都要实现此接口。
 * 通过 SPI 机制动态加载，支持热插拔。
 * <p>
 * ：
 * 1. 策略模式 — 每个 Tool 封装一种能力
 * 2. SPI 机制 — 运行时动态发现和加载
 * 3. JSON Schema — 描述工具参数，LLM 按 Schema 生成调用
 */
public interface AgentTool {

    /**
     * 工具名称（唯一标识）
     */
    String getName();

    /**
     * 工具描述（LLM 理解何时调用此工具）
     */
    String getDescription();

    /**
     * 参数 JSON Schema（OpenAI Tool Calling 格式）
     * <p>
     * 例如：
     * {
     *   "type": "object",
     *   "properties": {
     *     "query": { "type": "string", "description": "搜索关键词" }
     *   },
     *   "required": ["query"]
     * }
     */
    ObjectNode getParametersSchema();

    /**
     * 执行工具
     *
     * @param args   JSON 格式的参数
     * @param mapper ObjectMapper
     * @return 执行结果字符串
     */
    String execute(JsonNode args, ObjectMapper mapper) throws Exception;
}
