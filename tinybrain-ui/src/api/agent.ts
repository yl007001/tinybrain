import http from './index'

export interface AgentRequest {
  message: string
  sessionId?: string
  config?: {
    maxIterations?: number
    systemPromptSuffix?: string
  }
}

export interface AgentResponse {
  reply: string
  iterations: number
  toolCalls: ToolCall[]
  matchedSkill?: SkillMatch
}

export interface ToolCall {
  toolName: string
  args: string
  result: string
}

export interface SkillMatch {
  id: string
  name: string
  description: string
  triggerType: 'manual' | 'auto'
}

export interface ToolInfo {
  [name: string]: string
}

export function chatWithAgent(data: AgentRequest) {
  return http.post('/agent/chat', data)
}

export function getAgentTools() {
  return http.get('/agent/tools')
}

export function clearSession(sessionId: string) {
  return http.delete(`/agent/session/${sessionId}`)
}

export function getSessions() {
  return http.get('/sessions/list')
}

export function getSessionMessages(sessionId: string) {
  return http.get(`/sessions/${sessionId}/messages`)
}

export function deleteSession(sessionId: string) {
  return http.delete(`/sessions/${sessionId}`)
}

export function batchDeleteSessions(sessionIds: string[]) {
  return http.post('/sessions/batch-delete', { sessionIds })
}
