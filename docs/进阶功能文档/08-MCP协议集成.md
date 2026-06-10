# Phase 8：MCP 协议集成

## 一、MCP 是什么？

### 1.1 核心概念

**MCP (Model Context Protocol)** 是 Anthropic 推出的开放标准，用于连接 AI 应用与外部系统。它定义了 AI 模型如何发现和调用外部工具、访问数据源。

```
AI 应用 ←──MCP──→ 外部工具/数据源
```

### 1.2 解决的问题

| 问题 | 传统方式 | MCP 方案 |
|------|---------|---------|
| 工具集成 | 每个工具单独适配 | 统一协议 |
| 数据访问 | 自定义 API | 标准化接口 |
| 跨平台 | 绑定特定框架 | 开放标准 |

### 1.3 MCP 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    MCP Host (AI 应用)                        │
│                 如 Claude Desktop, TinyBrain                 │
└──────────────────────────┬──────────────────────────────────┘
                           │ MCP 协议
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                    MCP Server                                │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│    │  Tools   │  │Resources │  │ Prompts  │                │
│    └──────────┘  └──────────┘  └──────────┘                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│              外部系统 / 数据源                                │
│    数据库 · API · 文件系统 · SaaS 服务                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、MCP 核心概念

### 2.1 三大原语

| 原语 | 说明 | 类比 |
|------|------|------|
| **Tools** | 可执行的操作 | Function Calling |
| **Resources** | 可读取的数据 | GET API |
| **Prompts** | 预定义的提示模板 | 系统提示词 |

### 2.2 Tools（工具）

```json
{
  "name": "get_weather",
  "description": "获取指定城市的天气",
  "inputSchema": {
    "type": "object",
    "properties": {
      "city": {"type": "string", "description": "城市名称"}
    },
    "required": ["city"]
  }
}
```

### 2.3 Resources（资源）

```json
{
  "uri": "file:///path/to/document.txt",
  "name": "文档内容",
  "mimeType": "text/plain"
}
```

### 2.4 Prompts（提示模板）

```json
{
  "name": "code_review",
  "description": "代码审查提示",
  "arguments": [
    {"name": "code", "required": true, "description": "待审查代码"}
  ]
}
```

---

## 三、MCP 通信协议

### 3.1 传输方式

| 方式 | 说明 | 适用场景 |
|------|------|---------|
| **stdio** | 标准输入输出 | 本地进程 |
| **SSE** | Server-Sent Events | Web 服务 |
| **WebSocket** | 双向通信 | 实时交互 |

### 3.2 消息格式（JSON-RPC 2.0）

**请求**：
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": {"city": "北京"}
  }
}
```

**响应**：
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "content": [
      {"type": "text", "text": "北京今天晴天，25°C"}
    ]
  }
}
```

### 3.3 生命周期

```
初始化 → 能力协商 → 正常通信 → 关闭
   │         │           │        │
   ▼         ▼           ▼        ▼
initialize  tools/list  tools/call shutdown
```

---

## 四、TinyBrain MCP 集成方案

### 4.1 集成架构

```
┌─────────────────────────────────────────────────────────────┐
│                    TinyBrain Agent                           │
│    ┌─────────────────────────────────────────────────────┐  │
│    │              AgentEngine                             │  │
│    │    ┌─────────┐    ┌─────────┐    ┌─────────┐       │  │
│    │    │Local    │    │MCP      │    │Remote   │       │  │
│    │    │Skills   │    │Skills   │    │Skills   │       │  │
│    │    └─────────┘    └─────────┘    └─────────┘       │  │
│    └─────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                MCP Client (tinybrain-mcp)                    │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│    │stdio     │  │SSE       │  │WebSocket │                │
│    │Transport │  │Transport │  │Transport │                │
│    └──────────┘  └──────────┘  └──────────┘                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────┬──────────────────────────────────┐
│                  MCP Servers                                 │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│    │文件系统   │  │数据库     │  │第三方API │                │
│    └──────────┘  └──────────┘  └──────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 核心组件

#### MCPClient - MCP 客户端

```java
public class MCPClient {
    private final String serverEndpoint;
    private final Transport transport;
    
    // 初始化连接
    public void initialize() {
        // 发送 initialize 请求
        // 能力协商
    }
    
    // 列出可用工具
    public List<MCPTool> listTools() {
        // 调用 tools/list
    }
    
    // 调用工具
    public MCPResult callTool(String name, Map<String, Object> args) {
        // 调用 tools/call
    }
}
```

#### MCPTool - MCP 工具适配器

```java
public class MCPTool implements AgentTool {
    private final MCPClient client;
    private final String toolName;
    private final String description;
    private final ObjectNode schema;
    
    @Override
    public String execute(JsonNode args, ObjectMapper mapper) {
        MCPResult result = client.callTool(toolName, 
            mapper.convertValue(args, Map.class));
        return result.getContent();
    }
}
```

### 4.3 配置示例

```yaml
# application.yml
tinybrain:
  mcp:
    servers:
      - name: filesystem
        transport: stdio
        command: "npx"
        args: ["-y", "@modelcontextprotocol/server-filesystem", "/path"]
      - name: database
        transport: sse
        url: "http://localhost:3001/sse"
```

---

## 五、MCP vs Function Calling

| 维度 | Function Calling | MCP |
|------|-----------------|-----|
| 范围 | 单个应用内 | 跨系统 |
| 标准 | 厂商特定 | 开放标准 |
| 发现 | 静态配置 | 动态发现 |
| 安全 | 应用内控制 | 协议级安全 |

**TinyBrain 策略**：
- 本地 Skill：使用 Function Calling
- 外部工具：使用 MCP 协议

---

## 六、实现步骤

### 6.1 添加 MCP 依赖

```xml
<dependency>
    <groupId>com.tinybrain</groupId>
    <artifactId>tinybrain-mcp</artifactId>
</dependency>
```

### 6.2 创建 MCP 模块

```
tinybrain-mcp/
├── src/main/java/com/tinybrain/mcp/
│   ├── MCPClient.java
│   ├── MCPTool.java
│   ├── transport/
│   │   ├── Transport.java
│   │   ├── StdioTransport.java
│   │   └── SSETransport.java
│   └── dto/
│       ├── MCPRequest.java
│       └── MCPResponse.java
└── pom.xml
```

### 6.3 集成到 AgentEngine

```java
@Autowired
public void setTools(List<AgentTool> localTools, 
                     MCPClient mcpClient) {
    // 注册本地 Skill
    localTools.forEach(this::registerTool);
    
    // 注册 MCP 工具
    mcpClient.listTools().forEach(tool -> {
        registerTool(new MCPTool(mcpClient, tool));
    });
}
```

---

## 七、安全考虑

### 7.1 权限控制

```java
@PreAuthorize("hasRole('ADMIN')")
public MCPResult callTool(String name, Map<String, Object> args) {
    // 仅管理员可调用外部工具
}
```

### 7.2 参数验证

```java
public String execute(JsonNode args, ObjectMapper mapper) {
    // 验证参数符合 Schema
    validateAgainstSchema(args, schema);
    // 执行调用
    return client.callTool(name, args);
}
```

### 7.3 超时控制

```java
public MCPResult callTool(String name, Map<String, Object> args) {
    return CompletableFuture.supplyAsync(() -> {
        return doCall(name, args);
    }).orTimeout(30, TimeUnit.SECONDS).join();
}
```

---

## 八、总结

### 关键知识点

1. **MCP 是开放标准**：用于连接 AI 应用与外部系统
2. **三大原语**：Tools（工具）、Resources（资源）、Prompts（提示模板）
3. **JSON-RPC 2.0**：标准消息格式
4. **多种传输**：stdio、SSE、WebSocket

### 在 TinyBrain 中的应用

- **本地 Skill**：AgentTool 接口，Function Calling
- **外部工具**：MCP 协议，动态发现
- **统一管理**：AgentEngine 统一调度

### 面试高频问题

**Q1：MCP 和 Function Calling 有什么区别？**
A：Function Calling 是单个应用内的工具调用机制；MCP 是跨系统的开放标准协议。

**Q2：MCP 如何保证安全性？**
A：协议级权限控制、参数验证、超时限制、传输加密。

**Q3：TinyBrain 为什么同时使用两种方式？**
A：本地 Skill 用 Function Calling 效率高；外部工具用 MCP 标准化、可扩展。