package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间日期工具
 * <p>
 * Agent 通过此工具获取当前时间、日期等信息。
 */
@Component
public class DateTimeTool implements AgentTool {

    @Override
    public String getName() {
        return "get_datetime";
    }

    @Override
    public String getDescription() {
        return "获取当前的日期和时间。当用户询问"今天几号"、"现在几点"时使用此工具。";
    }

    @Override
    public ObjectNode getParametersSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", mapper.createObjectNode());
        schema.set("required", mapper.createArrayNode());
        return schema;
    }

    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return String.format("当前日期: %s, 当前时间: %s", date, time);
    }
}
