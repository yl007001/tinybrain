package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.text.DecimalFormat;

/**
 * 计算器工具
 * <p>
 * Agent 通过此工具执行数学计算和表达式求值。
 */
@Component
public class CalculatorTool implements AgentTool {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##########");

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Perform mathematical calculations. Supports arithmetic operations (+, -, *, /), " +
               "parentheses, and math functions like sqrt, pow, sin, cos, log. " +
               "Use when user asks to calculate something or solve math problems.";
    }

    @Override
    public ObjectNode getParametersSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode exprField = mapper.createObjectNode();
        exprField.put("type", "string");
        exprField.put("description", "Mathematical expression to evaluate, e.g. '(15 + 3) * 2 / 4' or 'sqrt(144) + 2^10'");
        properties.set("expression", exprField);

        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("expression"));
        return schema;
    }

    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        String expression = args.has("expression") ? args.get("expression").asText().trim() : "";

        if (expression.isBlank()) {
            return "Please provide a mathematical expression.";
        }

        try {
            // 使用 Java 的 ScriptEngine 来执行数学表达式
            // 注意：对于生产环境，建议使用更安全的解析器（如 exp4j）
            Object result = new ScriptEngineManager().getEngineByName("JavaScript").eval(expression);
            if (result instanceof Number) {
                double val = ((Number) result).doubleValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return expression + " = " + (long) val;
                }
                return expression + " = " + FORMAT.format(val);
            }
            return expression + " = " + result;
        } catch (ScriptException e) {
            return "Failed to evaluate expression: " + expression + ". Error: " + e.getMessage();
        }
    }
}
