import http from './index'

export interface McpServerConfig {
  name: string
  description?: string
  transportType: string
  command?: string
  args?: string[]
  url?: string
  env?: Record<string, string>
  autoConnect?: boolean
  connectTimeout?: number
}

export interface McpServerInfo {
  id: string
  name: string
  description: string
  transportType: string
  status: string
  toolCount: number
  tools: any[]
  lastConnectedAt?: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

// 获取所有 MCP 服务器
export function getMcpServers() {
  return http.get('/mcp/servers')
}

// 添加 MCP 服务器
export function addMcpServer(data: McpServerConfig) {
  return http.post('/mcp/servers', data)
}

// 更新 MCP 服务器
export function updateMcpServer(id: string, data: McpServerConfig) {
  return http.put(`/mcp/servers/${id}`, data)
}

// 删除 MCP 服务器
export function deleteMcpServer(id: string) {
  return http.delete(`/mcp/servers/${id}`)
}

// 测试 MCP 服务器连接
export function testMcpConnection(id: string) {
  return http.post(`/mcp/servers/${id}/test`)
}

// 获取 MCP 服务器工具列表
export function getMcpServerTools(id: string) {
  return http.get(`/mcp/servers/${id}/tools`)
}

// 调用 MCP 工具
export function callMcpTool(id: string, toolName: string, arguments_: Record<string, any>) {
  return http.post(`/mcp/servers/${id}/tools/${toolName}/call`, arguments_)
}
