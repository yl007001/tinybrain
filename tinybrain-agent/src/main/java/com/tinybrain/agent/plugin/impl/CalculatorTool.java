package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

/**
 * 计算器工具 — 安全的递归下降表达式解析器
 * <p>
 * 不使用 ScriptEngine（有任意代码执行风险），而是实现一个
 * 只支持数学运算的递归下降解析器，保证安全性。
 * <p>
 * 支持：
 * - 四则运算: +, -, *, /
 * - 幂运算: ^
 * - 括号: ()
 * - 函数: sqrt, sin, cos, tan, log, abs, ceil, floor
 * - 常量: pi, e
 * <p>
 * ：
 * 1. 递归下降解析器的优先级处理
 * 2. 为什么不用 eval/ScriptEngine — 安全沙箱问题
 * 3. 表达式解析的文法设计：Expr → Term (('+'|'-') Term)*
 */
@Slf4j
@Component
public class CalculatorTool implements AgentTool {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##########");

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Perform mathematical calculations. Supports arithmetic (+, -, *, /), " +
               "power (^), parentheses, and functions: sqrt, sin, cos, tan, log, abs, ceil, floor. " +
               "Constants: pi, e. Example: '(15 + 3) * 2 / 4' or 'sqrt(144) + 2^10'";
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
            ExprParser parser = new ExprParser(expression);
            double result = parser.parse();
            if (parser.pos < parser.input.length()) {
                return "Failed to parse: unexpected character at position " + parser.pos
                       + " ('" + parser.input.charAt(parser.pos) + "')";
            }
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return expression + " = " + result;
            }
            if (result == Math.floor(result) && !Double.isInfinite(result) && Math.abs(result) < Long.MAX_VALUE) {
                return expression + " = " + (long) result;
            }
            return expression + " = " + FORMAT.format(result);
        } catch (Exception e) {
            return "Failed to evaluate expression: " + expression + ". Error: " + e.getMessage();
        }
    }

    /**
     * 递归下降表达式解析器
     * <p>
     * 文法（优先级从低到高）：
     * expr   → term (('+' | '-') term)*
     * term   → power (('*' | '/') power)*
     * power  → unary ('^' unary)*
     * unary  → ('-' | '+') unary | atom
     * atom   → NUMBER | '(' expr ')' | FUNC '(' expr ')'
     */
    static class ExprParser {
        final String input;
        int pos = 0;

        ExprParser(String input) {
            this.input = input.replaceAll("\\s+", ""); // 去除所有空白
        }

        double parse() {
            double result = parseExpr();
            return result;
        }

        // expr → term (('+' | '-') term)*
        private double parseExpr() {
            double result = parseTerm();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '+') {
                    pos++;
                    result += parseTerm();
                } else if (op == '-') {
                    pos++;
                    result -= parseTerm();
                } else {
                    break;
                }
            }
            return result;
        }

        // term → power (('*' | '/') power)*
        private double parseTerm() {
            double result = parsePower();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '*') {
                    pos++;
                    result *= parsePower();
                } else if (op == '/') {
                    pos++;
                    double divisor = parsePower();
                    if (divisor == 0) throw new ArithmeticException("Division by zero");
                    result /= divisor;
                } else {
                    break;
                }
            }
            return result;
        }

        // power → unary ('^' unary)*  (右结合)
        private double parsePower() {
            double base = parseUnary();
            if (pos < input.length() && input.charAt(pos) == '^') {
                pos++;
                double exp = parsePower(); // 右结合：递归调用自身
                return Math.pow(base, exp);
            }
            return base;
        }

        // unary → ('-' | '+') unary | atom
        private double parseUnary() {
            if (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '-') {
                    pos++;
                    return -parseUnary();
                }
                if (c == '+') {
                    pos++;
                    return parseUnary();
                }
            }
            return parseAtom();
        }

        // atom → NUMBER | '(' expr ')' | FUNC '(' expr ')'
        private double parseAtom() {
            if (pos >= input.length()) {
                throw new ArithmeticException("Unexpected end of expression");
            }

            char c = input.charAt(pos);

            // 括号
            if (c == '(') {
                pos++;
                double result = parseExpr();
                expect(')');
                return result;
            }

            // 数字（包括小数）
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }

            // 函数或常量
            if (Character.isLetter(c)) {
                return parseFuncOrConst();
            }

            throw new ArithmeticException("Unexpected character: '" + c + "' at position " + pos);
        }

        private double parseNumber() {
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            // 支持科学计数法: 1e10, 2.5E-3
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    pos++;
                }
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private double parseFuncOrConst() {
            int start = pos;
            while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
                pos++;
            }
            String name = input.substring(start, pos).toLowerCase();

            // 常量
            switch (name) {
                case "pi": return Math.PI;
                case "e": return Math.E;
            }

            // 函数：必须后跟 '('
            expect('(');
            double arg = parseExpr();
            expect(')');

            switch (name) {
                case "sqrt": return Math.sqrt(arg);
                case "sin": return Math.sin(arg);
                case "cos": return Math.cos(arg);
                case "tan": return Math.tan(arg);
                case "log": return Math.log(arg);      // 自然对数
                case "log10": return Math.log10(arg);  // 常用对数
                case "abs": return Math.abs(arg);
                case "ceil": return Math.ceil(arg);
                case "floor": return Math.floor(arg);
                default: throw new ArithmeticException("Unknown function: " + name);
            }
        }

        private void expect(char expected) {
            if (pos >= input.length() || input.charAt(pos) != expected) {
                throw new ArithmeticException("Expected '" + expected + "' at position " + pos
                        + (pos < input.length() ? ", got '" + input.charAt(pos) + "'" : ", but reached end"));
            }
            pos++;
        }
    }
}
