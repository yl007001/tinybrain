# Agent 系统面试深挖

## 一、项目中的实现

### 架构

```
用户输入 → AgentService.process()
    → 构建 System Prompt (含工具描述 JSON Schema)
    → 调用 LLM Chat API
    → 解析回复 (检测工具调用 JSON)
    → 若需要调用工具:
        执行工具 → 结果回填 → 再次调用 LLM
    → 循环直到 LLM 不再调用工具 或 达到最大轮次
    → 返回最终回复
```

### 工具插件体系

```
AgentTool 接口 ← 实现 ← CalculatorTool
                        ← DateTimeTool
                        ← KnowledgeSearchTool (已集成 RAGService)
                        ← WebSearchTool

AgentToolRegistry @PostConstruct
    → applicationContext.getBeansOfType(AgentTool.class)
    → agentEngine.registerTool()
```

## 二、面试必问题

### Q1: Function Calling 的本质是什么？
- LLM 输出结构化 JSON，系统执行，结果回填
- 不是 LLM "调用"了函数，而是 LLM 输出了调用意图
- 类比：编译器生成汇编指令，CPU 执行

### Q2: ReAct 模式和 Plan-and-Execute 的区别？
| 模式 | 特点 | 适用场景 |
|------|------|---------|
| ReAct | 边想边做，reason + act 交替 | 单步工具调用 |
| Plan-and-Execute | 先规划再执行，规划阶段列出所有步骤 | 复杂多步任务 |

### Q3: Agent 循环控制机制有哪些？
1. **最大轮次** (maxIterations)：默认 5，防止无限循环
2. **工具调用次数累计**：response.toolCalls 记录所有调用
3. **历史截断**：只保留最近 10 轮对话，防止 Token 爆炸
4. **异常终止**：工具执行抛异常时 break

### Q4: 如何让 Agent 更稳定？
1. **Structured Output**：让 LLM 输出严格的 JSON Schema（当前实现）
2. **重试机制**：解析失败时重试
3. **降级回答**：工具都不可用时，作为普通 Chat
4. **校验层**：对工具参数校验（类型、范围、必填）

## 三、进阶考点

### 工具描述的 JSON Schema
```json
{
  "name": "knowledge_search",
  "description": "搜索知识库文档",
  "parameters": {
    "type": "object",
    "properties": {
      "query": { "type": "string", "description": "搜索关键词" },
      "top_k": { "type": "integer", "default": 3 }
    },
    "required": ["query"]
  }
}
```
- Schema 质量直接影响 LLM 调用工具的准确性
- description 要认真写，LLM 靠它判断何时调用

### 对话记忆的持久化
- 当前：ConcurrentHashMap（内存，重启丢失）
- 改进方案：Redis List / ZSet 存储历史消息
- 分布式方案：Redis + TTL + 消息压缩
