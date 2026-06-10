import http from './index'

export interface DocumentCreateRequest {
  title: string
  summary?: string
  content: string
  contentType?: string
  tags?: string[]
}

export interface DocumentVO {
  id: number
  title: string
  summary: string
  content: string
  contentType: string
  status: number
  tags: string[]
  userId: number
  createTime: string
  updateTime: string
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  records: T[]
}

export function getDocuments(params: { page?: number; pageSize?: number; keyword?: string; status?: number }) {
  return http.get('/documents', { params })
}

export function getDocumentDetail(id: number) {
  return http.get(`/documents/${id}`)
}

export function createDocument(data: DocumentCreateRequest) {
  return http.post('/documents', data)
}

export function updateDocument(id: number, data: Partial<DocumentCreateRequest>) {
  return http.put(`/documents/${id}`, data)
}

export function deleteDocument(id: number) {
  return http.delete(`/documents/${id}`)
}

export function uploadDocument(file: File, contentType: string = 'markdown') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('contentType', contentType)
  return http.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}

export function batchUploadDocuments(documents: Array<{ title: string; content: string; contentType?: string }>) {
  return http.post('/documents/batch-upload', documents)
}

export function batchDeleteDocuments(ids: number[]) {
  return http.post('/documents/batch-delete', { ids })
}

export function batchIndexDocuments(ids: number[]) {
  return http.post('/documents/batch-index', { ids })
}

export function batchIndexByKeyword(keyword: string) {
  return http.post('/documents/batch-index-by-keyword', { keyword })
}
