# Phase 9：Agent 智能体深度解析

## 一、Agent 是什么？

### 1.1 核心概念

**Agent（智能体）** 是能够自主感知环境、做出决策、执行行动的 AI 系统。在 TinyBrain 中，Agent 体现为能够调用工具完成任务的 LLM 应用。

```
用户请求 → Agent 思考 → 选择工具 → 执行 → 观察结果 → 继续/结束
```

### 1.2 Agent vs 传统 ChatBot

| 维度 | 传统 ChatBot | Agent |
|------|-------------|-------|
| 能力 | 仅对话 | 对话 + 工具调用 |
| 决策 | 模板匹配 | LLM 自主决策 |
| 执行 | 无 | 调用外部工具 |
| 记忆 | 短期 | 长期会话记忆 |

### 1.3 Agent 的核心能力

| 能力 | 说明 | TinyBrain 实现 |
|------|------|---------------|
| **推理** | 理解用户意图 | LLM 理解 |
| **规划** | 分解任务 | Function Calling 循环 |
| **执行** | 调用工具 | AgentEngine |
| **记忆** | 保持上下文 | SessionMemory |
| **反思** | 评估结果 | 循环终止判断 |

---

## 二、TinyBrain Agent 架构

### 2.1 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    用户输入                                   │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                  AgentService                                │
│    ┌─────────────────────────────────────────────────────┐  │
│    │              Function Calling 循环                    │  │
│    │    用户输入 → LLM → 工具调用 → 结果回填 → LLM → 输出  │  │
│    └─────────────────────────────────────────────────────┘  │
│    ┌─────────────────────────────────────────────────────┐  │
│    │              Session Memory                          │  │
│    │    会话历史存储、上下文管理、轮次限制                    │  │
│    └─────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                  AgentEngine                                 │
│    ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐     │
│    │Tool     │  │Tool     │  │Tool     │  │Tool     │     │
│    │Registry │  │Execution│  │Definition│ │Prompt   │     │
│    └─────────┘  └─────────┘  └─────────┘  └─────────┘     │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│              LLM API Client                                 │
│         DeepSeek / OpenAI / Ollama                          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件

#### AgentService - 对话服务

```java
@Service
public class AgentService {
    private final AgentEngine agentEngine;
    private final LLMApiClient llmClient;
    private final Map<String, List<Map<String, Object>>> sessionMemory;
    
    public AgentResponse process(AgentRequest request) {
        // 1. 构建 System Prompt（含工具描述）
        // 2. 获取会话历史
        // 3. Function Calling 循环
        // 4. 返回最终回复
    }
}
```

#### AgentEngine - 工具引擎

```java
@Component
public class AgentEngine {
    private final Map<String, AgentTool> toolRegistry;
    
    public void registerTool(AgentTool tool) { ... }
    public List<ObjectNode> getToolDefinitions() { ... }
    public String executeTool(String toolName, String argsJson) { ... }
    public String buildSystemPrompt() { ... }
}
```

---

## 三、Function Calling 机制

### 3.1 工作流程

```
用户: "帮我计算 2 的 10 次方"
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 1: LLM 分析意图                                         │
│    - 需要数学计算                                             │
│    - 应该调用 calculator 工具                                 │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 2: LLM 返回工具调用                                     │
│    {"tool": "calculator", "args": {"expression": "2^10"}}    │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 3: AgentEngine 执行工具                                 │
│    调用 CalculatorTool.execute()                             │
│    返回: "2^10 = 1024"                                       │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 4: 结果回填给 LLM                                       │
│    工具结果: "2^10 = 1024"                                   │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ Step 5: LLM 生成最终回复                                     │
│    "2 的 10 次方等于 1024"                                   │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 工具定义格式（OpenAI 标准）

```json
{
  "type": "function",
  "function": {
    "name": "calculator",
    "description": "Perform mathematical calculations",
    "parameters": {
      "type": "object",
      "properties": {
        "expression": {
          "type": "string",
          "description": "Mathematical expression"
        }
      },
      "required": ["expression"]
    }
  }
}
```

### 3.3 工具调用格式

**LLM 返回**：
```json
{
  "tool_calls": [
    {
      "id": "call_123",
      "type": "function",
      "function": {
        "name": "calculator",
        "arguments": "{\"expression\": \"2^10\"}"
      }
    }
  ]
}
```

**工具结果**：
```json
{
  "role": "tool",
  "tool_call_id": "call_123",
  "content": "2^10 = 1024"
}
```

---

## 四、ReAct 模式

### 4.1 什么是 ReAct

**ReAct (Reason + Act)** 是 Agent 的核心推理模式：

1. **Reason（推理）**：LLM 分析当前状态，决定下一步行动
2. **Act（行动）**：执行工具调用
3. **Observe（观察）**：获取工具结果
4. **循环**：直到任务完成或达到最大轮数

### 4.2 TinyBrain 实现

```java
while (iterations < maxIterations) {
    // 1. Reason: LLM 分析
    var llmResponse = llmClient.chat(messages);
    
    // 2. 检查是否需要 Act
    String toolCallJson = extractToolCall(reply);
    if (toolCallJson == null) {
        break; // LLM 直接回答，无需工具
    }
    
    // 3. Act: 执行工具
    String result = agentEngine.executeTool(toolName, argsJson);
    
    // 4. Observe: 结果回填
    messages.add(toolMessage);
    
    iterations++;
}
```

### 4.3 循环终止条件

| 条件 | 说明 |
|------|------|
| 达到最大轮数 | 防止无限循环 |
| LLM 不调用工具 | 任务完成 |
| 工具执行失败 | 异常处理 |
| 用户中断 | 手动停止 |

---

## 五、会话记忆管理

### 5.1 记忆结构

```java
// sessionId → 消息历史
Map<String, List<Map<String, Object>>> sessionMemory;

// 消息格式
{
    "role": "user",
    "content": "帮我计算 2^10"
}
{
    "role": "assistant", 
    "content": "2 的 10 次方等于 1024"
}
```

### 5.2 记忆策略

| 策略 | 说明 | TinyBrain 实现 |
|------|------|---------------|
| **滑动窗口** | 只保留最近 N 轮 | 保留最近 10 轮 |
| **摘要压缩** | 历史消息压缩 | 未实现 |
| **向量检索** | 相关历史检索 | 未实现 |

### 5.3 会话清理

```java
public void clearSession(String sessionId) {
    sessionMemory.remove(sessionId);
}
```

---

## 六、错误处理与容错

### 6.1 工具调用失败

```java
try {
    String result = tool.execute(args, mapper);
} catch (Exception e) {
    return "工具执行出错: " + e.getMessage();
}
```

### 6.2 LLM 响应异常

```java
if (llmResponse == null || llmResponse.getReplyText() == null) {
    break; // 退出循环
}
```

### 6.3 超时控制

```java
@Timed(value = "agent.process.time", description = "Agent 对话处理耗时")
public AgentResponse process(AgentRequest request) {
    // 方法执行时间被监控
}
```

---

## 七、性能优化

### 7.1 并发安全

```java
// 使用 ConcurrentHashMap
private final Map<String, AgentTool> toolRegistry = new ConcurrentHashMap<>();

// 使用 CopyOnWriteArrayList
List<Map<String, Object>> history = sessionMemory.computeIfAbsent(
    sessionId, k -> new CopyOnWriteArrayList<>());
```

### 7.2 监控指标

```java
@Timed(value = "agent.process.time", 
       description = "Agent 对话处理耗时",
       percentiles = {0.5, 0.95, 0.99})
```

### 7.3 缓存策略

- 工具定义缓存
- 会话历史缓存
- LLM 响应缓存（可选）

---

## 八、扩展能力

### 8.1 多工具调用

```java
// 单次调用多个工具
{
    "tool_calls": [
        {"name": "calculator", "args": {"expression": "2^10"}},
        {"name": "datetime", "args": {}}
    ]
}
```

### 8.2 工具链

```
用户: "今天是星期几？距离春节还有多少天？"
         │
         ▼
    调用 datetime → 获取今天日期
         │
         ▼
    调用 calculator → 计算日期差
         │
         ▼
    返回最终结果
```

### 8.3 并行执行

```java
// 并行调用多个工具
List<CompletableFuture<String>> futures = toolCalls.stream()
    .map(tc -> CompletableFuture.supplyAsync(() -> 
        agentEngine.executeTool(tc.name(), tc.args())))
    .toList();
```

---

## 九、与其他框架对比

| 框架 | 特点 | TinyBrain 优势 |
|------|------|---------------|
| LangChain | Python 生态 | Java 原生，性能好 |
| AutoGPT | 全自动 | 可控性强 |
| BabyAGI | 任务分解 | 轻量级，易理解 |

---

## 十、总结

### 关键知识点

1. **Agent = LLM + 工具调用 + 记忆**
2. **Function Calling 是核心机制**：LLM 输出结构化 JSON，系统执行
3. **ReAct 模式**：Reason → Act → Observe 循环
4. **会话记忆**：保持多轮对话上下文

### 架构设计要点

1. **分层设计**：AgentService → AgentEngine → AgentTool
2. **接口抽象**：AgentTool 接口，支持多种工具实现
3. **并发安全**：ConcurrentHashMap、CopyOnWriteArrayList
4. **可观测性**：Micrometer 监控指标

### 面试高频问题

**Q1：Agent 和 ChatBot 有什么区别？**
A：Agent 能够自主调用工具完成任务，ChatBot 只能对话。

**Q2：Function Calling 的原理是什么？**
A：LLM 输出结构化 JSON 描述工具调用，系统解析执行后将结果回填给 LLM。

**Q3：如何防止 Agent 无限循环？**
A：设置最大迭代次数、监控循环状态、异常退出机制。

**Q4：如何保证 Agent 的安全性？**
A：工具权限控制、参数验证、执行沙箱、超时限制。