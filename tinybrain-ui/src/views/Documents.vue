<template>
  <div class="documents-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-actions">
        <div class="search-box">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.5"/>
            <path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <input
            v-model="query.keyword"
            placeholder="搜索文档..."
            @keyup.enter="fetchDocuments"
            @clear="fetchDocuments"
          />
        </div>
        <button class="btn-secondary" @click="showCreateDialog = true">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          新建
        </button>
        <label class="btn-secondary upload-btn">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 10V3M8 3L5 6M8 3l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 10v3h12v-3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          上传
          <input type="file" accept=".md,.txt,.markdown" @change="handleFileUpload" hidden />
        </label>
      </div>
    </div>

    <!-- Document List -->
    <div class="doc-list" v-loading="loading">
      <div v-if="documents.length === 0 && !loading" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <rect x="8" y="4" width="32" height="40" rx="4" stroke="currentColor" stroke-width="2"/>
          <path d="M16 16h16M16 24h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <p>还没有文档</p>
        <span>点击「新建」或「上传」添加你的第一份文档</span>
      </div>

      <div
        v-for="doc in documents"
        :key="doc.id"
        class="doc-card"
        @click="viewDetail(doc)"
      >
        <div class="doc-header">
          <span class="doc-title">{{ doc.title }}</span>
          <span class="doc-status" :class="doc.status === 1 ? 'published' : 'draft'">
            {{ doc.status === 1 ? '已发布' : '草稿' }}
          </span>
        </div>
        <p class="doc-summary">{{ doc.summary || '暂无摘要' }}</p>
        <div class="doc-footer">
          <span class="doc-type">{{ doc.contentType || 'markdown' }}</span>
          <span class="doc-time">{{ formatDate(doc.createTime) }}</span>
          <button class="doc-delete" @click.stop="handleDelete(doc)" title="删除">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M2 4h10M5 4V2h4v2M3 4v8a1 1 0 001 1h6a1 1 0 001-1V4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > query.pageSize">
      <button
        class="page-btn"
        :disabled="query.page <= 1"
        @click="query.page--; fetchDocuments()"
      >上一页</button>
      <span class="page-info">第 {{ query.page }} 页，共 {{ Math.ceil(total / query.pageSize) }} 页</span>
      <button
        class="page-btn"
        :disabled="query.page >= Math.ceil(total / query.pageSize)"
        @click="query.page++; fetchDocuments()"
      >下一页</button>
    </div>

    <!-- Create Dialog -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>新建文档</h3>
          <button class="dialog-close" @click="showCreateDialog = false">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>标题</label>
            <input v-model="createForm.title" placeholder="文档标题" />
          </div>
          <div class="form-group">
            <label>内容</label>
            <textarea v-model="createForm.content" rows="12" placeholder="支持 Markdown 格式..."></textarea>
          </div>
          <div class="form-group">
            <label>摘要（可选）</label>
            <input v-model="createForm.summary" placeholder="简短描述" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-ghost" @click="showCreateDialog = false">取消</button>
          <button class="btn-primary" :disabled="creating" @click="handleCreate">
            {{ creating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Detail Dialog -->
    <div v-if="showDetailDialog && currentDoc" class="dialog-overlay" @click.self="showDetailDialog = false">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h3>{{ currentDoc.title }}</h3>
          <button class="dialog-close" @click="showDetailDialog = false">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="detail-meta">
            <span class="meta-tag">{{ currentDoc.contentType || 'markdown' }}</span>
            <span class="meta-tag" :class="currentDoc.status === 1 ? 'published' : 'draft'">
              {{ currentDoc.status === 1 ? '已发布' : '草稿' }}
            </span>
            <span class="meta-time">{{ currentDoc.createTime }}</span>
          </div>
          <div class="detail-content">{{ currentDoc.content }}</div>
        </div>
        <div class="dialog-footer">
          <button class="btn-ghost" @click="showDetailDialog = false">关闭</button>
          <button class="btn-primary" @click="indexDocument">索引到 RAG</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocuments, createDocument, deleteDocument, uploadDocument, getDocumentDetail } from '@/api/document'
import { indexDocument as indexDoc } from '@/api/rag'
import type { DocumentVO } from '@/api/document'

const loading = ref(false)
const creating = ref(false)
const documents = ref<DocumentVO[]>([])
const total = ref(0)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const currentDoc = ref<DocumentVO | null>(null)

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

const createForm = reactive({
  title: '',
  summary: '',
  content: '',
})

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return d.toLocaleDateString('zh-CN')
}

async function fetchDocuments() {
  loading.value = true
  try {
    const res: any = await getDocuments(query)
    if (res?.data) {
      documents.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleFileUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    await uploadDocument(file)
    ElMessage.success('上传成功')
    await fetchDocuments()
  } catch { /* ignore */ }
  input.value = ''
}

async function handleCreate() {
  if (!createForm.title || !createForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  creating.value = true
  try {
    await createDocument(createForm)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.title = ''
    createForm.summary = ''
    createForm.content = ''
    await fetchDocuments()
  } catch { /* ignore */ }
  finally { creating.value = false }
}

async function viewDetail(doc: DocumentVO) {
  try {
    const res: any = await getDocumentDetail(doc.id)
    if (res?.data) {
      currentDoc.value = res.data
      showDetailDialog.value = true
    }
  } catch { /* ignore */ }
}

async function handleDelete(doc: DocumentVO) {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.title}」？`, '确认删除', { type: 'warning' })
    await deleteDocument(doc.id)
    ElMessage.success('删除成功')
    await fetchDocuments()
  } catch { /* ignore */ }
}

async function indexDocument() {
  if (!currentDoc.value) return
  try {
    await indexDoc(currentDoc.value.id)
    ElMessage.success('索引成功，可以开始 RAG 问答了')
  } catch { /* ignore */ }
}

onMounted(fetchDocuments)
</script>

<style scoped>
.documents-page {
  max-width: 960px;
}

.page-header {
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 36px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-tertiary);
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 13px;
  background: none;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

.search-box input::placeholder {
  color: var(--color-text-tertiary);
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: var(--font-sans);
}

.btn-secondary:hover {
  background: var(--color-bg-hover);
}

.upload-btn {
  cursor: pointer;
}

/* Document List */
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-tertiary);
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.4;
}

.empty-state p {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.empty-state span {
  font-size: 13px;
}

.doc-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.12s ease;
}

.doc-card:hover {
  border-color: var(--color-accent-subtle);
  box-shadow: var(--shadow-sm);
}

.doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.doc-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.doc-status {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 9999px;
}

.doc-status.published {
  background: #dcfce7;
  color: #16a34a;
}

.doc-status.draft {
  background: #fef3c7;
  color: #d97706;
}

.doc-summary {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.doc-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.doc-type {
  text-transform: uppercase;
  font-weight: 500;
  font-size: 11px;
}

.doc-delete {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  opacity: 0;
  transition: all 0.12s ease;
}

.doc-card:hover .doc-delete {
  opacity: 1;
}

.doc-delete:hover {
  color: var(--color-error);
  background: #fef2f2;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding: 16px 0;
}

.page-btn {
  height: 32px;
  padding: 0 12px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-primary);
  cursor: pointer;
  font-family: var(--font-sans);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.dialog {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  width: 560px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-xl);
}

.dialog-lg {
  width: 720px;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.dialog-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 4px;
  line-height: 1;
}

.dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border-subtle);
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg);
  font-family: var(--font-sans);
  outline: none;
  transition: border-color 0.15s ease;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--color-accent);
}

.form-group textarea {
  resize: vertical;
  line-height: 1.6;
}

.detail-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}

.meta-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 9999px;
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
}

.meta-tag.published {
  background: #dcfce7;
  color: #16a34a;
}

.meta-tag.draft {
  background: #fef3c7;
  color: #d97706;
}

.meta-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-sm);
}

.btn-primary {
  height: 36px;
  padding: 0 16px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-sans);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-ghost {
  height: 36px;
  padding: 0 16px;
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: var(--font-sans);
}
</style>
