import http from './index'

export interface RAGResult {
  question: string
  chunks: ChunkResult[]
  answer: string
  totalTokens: number
  sessionId: string
}

export interface ChunkResult {
  chunkId: number
  documentId: number
  documentTitle: string
  content: string
  score: number
}

export function askRAG(question: string, topK: number = 5, sessionId?: string) {
  return http.get('/rag/ask', { params: { question, topK, sessionId } })
}

export function indexDocument(documentId: number) {
  return http.post(`/rag/index/${documentId}`)
}

export function getRAGStats() {
  return http.get('/rag/stats')
}

// Session management (reuses /api/sessions endpoints)
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
