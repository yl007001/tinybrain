import http from './index'

export interface RAGResult {
  question: string
  chunks: ChunkResult[]
  answer: string
  totalTokens: number
}

export interface ChunkResult {
  chunkId: number
  documentId: number
  documentTitle: string
  content: string
  score: number
}

export function askRAG(question: string, topK: number = 5) {
  return http.get('/rag/ask', { params: { question, topK } })
}

export function indexDocument(documentId: number) {
  return http.post(`/rag/index/${documentId}`)
}

export function getRAGStats() {
  return http.get('/rag/stats')
}
