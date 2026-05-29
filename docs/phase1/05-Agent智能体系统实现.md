# Phase 3：Agent 智能体系统实现

## 一、Agent 是什么？

### 1.1 定义

AI Agent（智能体）是一个能 **自主理解、规划、使用工具、执行任务** 的 AI 系统。它不仅仅是"问答"，而是能 **主动行动**。

### 1.2 LLM vs Agent

| 维度 | LLM (大语言模型) | Agent (智能体) |
|------|----------------|---------------|
| 能力 | 对话、生成 | 感知 + 决策 + 行动 |
| 工具使用 | ❌ 不能 | ✅ 可以调用 API、查询数据库等 |
| 记忆 | 单次对话 | ✅ 多轮记忆 + 长期记忆 |
| 自主性 | 被动回答 | ✅ 主动规划执行 |
| TinyBrain 中 | RAG 问答 | Agent 主动检索 + 调用工具 |

---

## 二、TinyBrain Agent 架构

### 2.1 系统流程

```
用户输入: "帮我查一下Spring事务的资料，然后告诉我是几点了"
    │
    ▼
┌─────────────────────────────────────────────┐
│ AgentService (编排器)                         │
│  1. 构建 System Prompt (含工具描述)           │
│  2. 发送给 LLM                                │
│  3. LLM 回复: {"tool":"knowledge_search",     │
│                "args":{"query":"Spring事务"}}  │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ AgentEngine (执行器)                         │
│  4. 解析 Function Call JSON                  │
│  5. 查找并执行 KnowledgeSearchTool           │
│  6. 返回检索结果                              │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ 回填给 LLM                                    │
│  7. 工具结果 + 原始问题 → LLM                 │
│  8. LLM 回复: {"tool":"get_datetime",         │
│                "args":{}}                     │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ AgentEngine (执行器)                         │
│  9. 执行 DateTimeTool                         │
│  10. 返回当前时间                              │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ 最终回复                                      │
│  "Spring事务的ACID特性是... 现在是 14:30"      │
└─────────────────────────────────────────────┘
```

### 2.2 核心组件

| 组件 | 类名 | 职责 |
|------|------|------|
| 工具接口 | `AgentTool` | 定义工具契约（名称、描述、参数Schema、执行） |
| 工具实现 | `KnowledgeSearchTool` | 知识库搜索（Phase 3 示例） |
| 工具实现 | `DateTimeTool` | 获取时间日期 |
| 工具引擎 | `AgentEngine` | 工具注册、查找、执行 |
| 自动注册 | `AgentToolRegistry` | Spring 启动时扫描并注册所有 Tool |
| 对话服务 | `AgentService` | Function Calling 循环编排 + 会话记忆 |
| 控制器 | `AgentController` | REST API (`/api/agent/chat`) |

---

## 三、Function Calling 详解

### 3.1 什么是 Function Calling？

传统 API 调用：
```
前端硬编码调用 → 写死 API 逻辑 → 用户无法自定义
```

Function Calling：
```
LLM 自主判断 → 生成结构化 JSON → 系统执行 → 结果回填
```

### 3.2 Tool 定义格式

```java
public class KnowledgeSearchTool implements AgentTool {
    public String getName() { return "knowledge_search"; }

    public String getDescription() {
        return "搜索知识库中的文档...";
    }

    public ObjectNode getParametersSchema() {
        // JSON Schema 格式
        {
            "type": "object",
            "properties": {
                "query": { "type": "string", "description": "搜索关键词" },
                "top_k": { "type": "integer", "default": 5 }
            },
            "required": ["query"]
        }
    }

    public String execute(JsonNode args, ObjectMapper mapper) {
        String query = args.get("query").asText();
        // 执行搜索逻辑
        return 搜索结果;
    }
}
```

**面试重点**：为什么用 JSON Schema？
> 因为 LLM 原生理解 JSON Schema，可精确生成符合格式的结构化输出。

### 3.3 Function Calling 循环

```java
while (iterations < maxIterations) {
    // 1. 调用 LLM
    String reply = llmClient.chat(systemPrompt, messages);

    // 2. LLM 是否要调用工具？
    String toolCall = extractToolCall(reply);
    if (toolCall == null) break;  // LLM 直接回答了

    // 3. 解析工具调用 → 执行工具
    String toolName = parseToolName(toolCall);
    String result = agentEngine.executeTool(toolName, args);

    // 4. 将结果回填给 LLM
    messages.add(toolCallResult(toolName, result));
    // 继续下一次循环
}
```

---

## 四、工具插件系统

### 4.1 SPI 架构

```
AgentTool 接口
    ├── KnowledgeSearchTool (@Component)   ← 自动注册
    ├── DateTimeTool (@Component)          ← 自动注册
    └── 你写的下一个工具 (@Component)      ← 只需加一个类！
```

**扩展方式**：只需要新建一个类实现 `AgentTool` 接口，加上 `@Component`，Spring 启动时自动扫描注册。

### 4.2 内置工具清单

| 工具 | 功能 | 演示价值 |
|------|------|---------|
| `knowledge_search` | 搜索知识库文档 | 展示 RAG + Agent 集成 |
| `get_datetime` | 获取当前时间 | 展示无需参数的工具 |

---

## 五、会话记忆管理

### 5.1 记忆结构

```java
// sessionId → 消息历史列表
Map<String, List<Map<String, String>>> sessionMemory

// 消息格式：
{ "role": "user",      "content": "帮我查一下Spring事务" }
{ "role": "assistant", "content": "Spring事务的核心概念是..." }
```

### 5.2 记忆策略

| 策略 | 说明 | TinyBrain 实现 |
|------|------|--------------|
| 滑动窗口 | 只保留最近 N 轮对话 | ✅ 最近 10 轮 |
| Token 限制 | 按 Token 截断 | ❌ Phase 3 暂未实现 |
| 摘要压缩 | 总结历史代替完整记录 | ❌ 后续优化 |
| 向量记忆 | 用 RAG 检索历史 | ❌ 进阶方案 |

---

## 六、面试题精讲

### 基础题

1. **什么是 AI Agent？和 LLM 有什么区别？**
   > LLM 只能对话，Agent 能用工具、有记忆、能自主规划执行。

2. **Function Calling 是什么原理？**
   > LLM 输出结构化 JSON，系统解析后执行对应函数，结果再回填给 LLM。

3. **Agent 的 Tool 是怎么注册的？**
   > Spring 启动时，AgentToolRegistry 扫描所有 AgentTool 实现，注册到 AgentEngine。

### 进阶题

4. **怎么防止 Agent 陷入无限循环？**
   > 设置 maxIterations 上限（TinyBrain 默认为 5）。检测连续相同工具调用。

5. **Agent 的 System Prompt 怎么设计？**
   > 精确描述每个工具的用途和参数。告诉 LLM 何时该用、何时不该用。

6. **多 Agent 协作怎么实现？**
   > Supervisor + Worker 模式：一个主 Agent 分配任务，多个子 Agent 执行。

### 场景题

7. **"如果 LLM 返回了不存在的工具名怎么办？"**
   > AgentEngine.executeTool 中 catch 异常，返回友好错误信息告诉 LLM 重试。

8. **"怎么让 Agent 支持更复杂的多步骤任务？"**
   > ReAct 模式（Reason + Act）：LLM 先思考"需要什么信息"→ 调用工具 → 观察结果 → 再思考 → 再调用。
