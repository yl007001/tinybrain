# TinyBrain MCP 模块

## 概述

TinyBrain MCP 模块实现了 Model Context Protocol (MCP) 客户端，用于连接外部 MCP 服务器，扩展 Agent 的工具能力。

## 架构

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

## 核心组件

### MCPClient
MCP 客户端，负责连接 MCP 服务器，发现和调用工具。

### Transport
传输层接口，支持多种传输方式：
- **StdioTransport**: 标准输入输出传输
- **SSETransport**: Server-Sent Events 传输（待实现）
- **WebSocketTransport**: WebSocket 传输（待实现）

### MCPTool
MCP 工具适配器，将 MCP 工具适配为 AgentTool 接口。

## 使用示例

```java
// 创建 MCP 客户端
Transport transport = new StdioTransport("npx", new String[]{"-y", "@modelcontextprotocol/server-filesystem", "/path"});
MCPClient mcpClient = new MCPClient(transport);

// 初始化连接
mcpClient.initialize();

// 获取可用工具
List<Map<String, Object>> tools = mcpClient.getAvailableTools();

// 调用工具
String result = mcpClient.callTool("read_file", Map.of("path", "/path/to/file"));
```

## 配置

在 `application.yml` 中配置 MCP 服务器：

```yaml
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

## 测试

```bash
# 运行 MCP 模块测试
mvn test -pl tinybrain-mcp

# 生成覆盖率报告
mvn test jacoco:report -pl tinybrain-mcp
```

## 依赖

- tinybrain-common
- Jackson (JSON 处理)
- Spring Boot (可选，用于自动配置)