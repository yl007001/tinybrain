# Phase 7：Skill 技能系统实现

## 一、Skill 是什么？

### 1.1 核心概念

**Skill（技能）** 是 AI Agent 的能力模块，每个 Skill 封装了一种特定功能。在 TinyBrain 中，Skill 体现为 `AgentTool` 接口的实现。

```
用户请求 → Agent 引擎 → 选择 Skill → 执行 → 返回结果
```

### 1.2 Skill 的特点

| 特点 | 说明 | TinyBrain 实现 |
|------|------|---------------|
| **模块化** | 每个 Skill 独立封装 | `AgentTool` 接口 |
| **可插拔** | 运行时动态加载 | Spring Bean 自动注册 |
| **自描述** | 包含名称、描述、参数 Schema | `getParametersSchema()` |
| **可组合** | 多个 Skill 协同工作 | Agent 循环调度 |

### 1.3 Skill vs 传统 API

| 维度 | 传统 API 调用 | Skill 调用 |
|------|--------------|-----------|
| 调用方 | 开发者硬编码 | LLM 动态决策 |
| 参数 | 固定格式 | JSON Schema 描述 |
| 发现 | 需要文档 | 自描述，LLM 可理解 |
| 灵活性 | 低 | 高 |

---

## 二、TinyBrain Skill 架构

### 2.1 系统流程

```
┌─────────────────────────────────────────────────────────────┐
│                    用户自然语言输入                           │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│              AgentEngine (工具注册中心)                       │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │calculator│ │datetime │ │websearch│ │knowledge│  ...       │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                   LLM Function Calling                       │
│         "需要调用 calculator，参数: {expression: '2+3'}"      │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                  执行 Skill 并返回结果                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心接口：AgentTool

```java
public interface AgentTool {
    // 工具唯一标识
    String getName();
    
    // 工具描述（LLM 理解何时调用）
    String getDescription();
    
    // 参数 JSON Schema（OpenAI Tool Calling 格式）
    ObjectNode getParametersSchema();
    
    // 执行工具
    String execute(JsonNode args, ObjectMapper mapper) throws Exception;
}
```

### 2.3 Skill 注册机制

```java
@Component
public class AgentEngine {
    private final Map<String, AgentTool> toolRegistry = new ConcurrentHashMap<>();
    
    // Spring 自动注入所有 AgentTool 实现
    @Autowired
    public void setTools(List<AgentTool> tools) {
        tools.forEach(this::registerTool);
    }
}
```

---

## 三、内置 Skill 详解

### 3.1 Calculator Skill

**功能**：安全的数学表达式计算

**特性**：
- 支持四则运算：`+`, `-`, `*`, `/`
- 支持幂运算：`^`
- 支持函数：`sqrt`, `sin`, `cos`, `tan`, `log`, `abs`, `ceil`, `floor`
- 支持常量：`pi`, `e`
- 安全设计：递归下降解析器，无任意代码执行风险

**参数 Schema**：
```json
{
  "type": "object",
  "properties": {
    "expression": {
      "type": "string",
      "description": "数学表达式，如 '(15 + 3) * 2 / 4'"
    }
  },
  "required": ["expression"]
}
```

**示例调用**：
```json
{"tool": "calculator", "args": {"expression": "sqrt(144) + 2^10"}}
```

### 3.2 DateTime Skill

**功能**：获取当前日期时间

**返回格式**：`yyyy-MM-dd HH:mm:ss`

### 3.3 Knowledge Search Skill

**功能**：搜索知识库中的文档

**参数**：
- `query`：搜索关键词
- `topK`：返回结果数量

### 3.4 Web Search Skill

**功能**：网络搜索（需要 SerpAPI Key）

---

## 四、如何自定义 Skill

### 4.1 创建新 Skill

```java
@Component
public class WeatherTool implements AgentTool {
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "查询指定城市的天气信息";
    }
    
    @Override
    public ObjectNode getParametersSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = mapper.createObjectNode();
        ObjectNode cityField = mapper.createObjectNode();
        cityField.put("type", "string");
        cityField.put("description", "城市名称");
        properties.set("city", cityField);
        
        schema.set("properties", properties);
        schema.set("required", mapper.createArrayNode().add("city"));
        return schema;
    }
    
    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        String city = args.get("city").asText();
        // 调用天气 API
        return city + "今天晴天，温度 25°C";
    }
}
```

### 4.2 Skill 最佳实践

1. **单一职责**：每个 Skill 只做一件事
2. **清晰描述**：getDescription() 要让 LLM 理解何时调用
3. **参数验证**：在 execute() 中验证参数
4. **错误处理**：返回友好的错误信息
5. **幂等性**：相同输入产生相同输出

---

## 五、Skill 的高级特性

### 5.1 依赖注入

```java
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool implements AgentTool {
    private final DocumentService documentService;
    private final RAGService ragService;
}
```

### 5.2 异步执行

```java
@Override
public String execute(JsonNode args, ObjectMapper mapper) {
    return CompletableFuture.supplyAsync(() -> {
        // 耗时操作
        return result;
    }).join();
}
```

### 5.3 缓存支持

```java
@Cacheable(value = "toolResult", key = "#args")
public String execute(JsonNode args, ObjectMapper mapper) {
    // 首次执行后缓存结果
}
```

---

## 六、Skill 与 MCP 的关系

| 概念 | Skill | MCP |
|------|-------|-----|
| 定义 | 能力模块 | 通信协议 |
| 范围 | 单个 Agent 内 | 跨系统 |
| 标准 | 自定义接口 | 开放标准 |
| 用途 | 封装功能 | 连接外部系统 |

**在 TinyBrain 中**：
- Skill = `AgentTool` 接口实现
- MCP = 连接外部工具的标准协议

---

## 七、总结

### 关键知识点

1. **Skill 是 Agent 的能力单元**：每个 Skill 封装一种功能
2. **AgentTool 是 Skill 的 Java 实现**：定义了名称、描述、参数、执行逻辑
3. **Spring 自动注册**：实现 AgentTool 接口的 Bean 自动注册到 AgentEngine
4. **Function Calling 是调用机制**：LLM 根据描述决定调用哪个 Skill

### 面试高频问题

**Q1：Skill 和 API 有什么区别？**
A：Skill 是面向 LLM 的能力模块，包含自描述信息；API 是面向开发者的接口，需要文档说明。

**Q2：如何保证 Skill 调用的安全性？**
A：参数 Schema 验证、执行沙箱、权限控制、超时限制。

**Q3：Skill 如何实现热插拔？**
A：Spring 动态 Bean 注册 + 运行时重新加载。