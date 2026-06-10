<template>
  <div class="documents-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-top">
        <div class="doc-stats">
          <span class="stats-count">共 {{ total }} 份文档</span>
          <span v-if="selectedIds.size > 0" class="stats-selected">已选 {{ selectedIds.size }} 份</span>
        </div>
      </div>
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
          />
          <button v-if="query.keyword" class="search-clear" @click="query.keyword = ''; fetchDocuments()">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M4 4l6 6M10 4l-6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
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
      <!-- Batch Actions Bar -->
      <div class="batch-actions">
        <div class="batch-left">
          <button class="btn-batch" @click="showBatchUploadDialog = true">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M7 9V2M7 2L4 5M7 2l3 3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 9v2.5h10V9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            批量上传
          </button>
          <button class="btn-batch btn-batch-danger" :disabled="selectedIds.size === 0" @click="handleBatchDelete">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M2 4h10M5 4V2h4v2M3 4v8a1 1 0 001 1h6a1 1 0 001-1V4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            批量删除
          </button>
          <button class="btn-batch btn-batch-accent" :disabled="selectedIds.size === 0" @click="handleBatchIndex">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.3"/>
              <path d="M7 5v4M5 7h4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            批量索引
          </button>
          <button class="btn-batch" @click="showKeywordDialog = true">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <circle cx="6" cy="6" r="4" stroke="currentColor" stroke-width="1.3"/>
              <path d="M9 9l3 3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            关键词索引
          </button>
        </div>
        <div class="batch-right">
          <button class="btn-text" @click="toggleSelectAll">
            {{ isAllSelected ? '取消全选' : '全选' }}
          </button>
        </div>
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
        :class="{ 'doc-selected': selectedIds.has(doc.id) }"
        @click="viewDetail(doc)"
      >
        <div class="doc-checkbox-wrap" @click.stop>
          <input
            type="checkbox"
            :checked="selectedIds.has(doc.id)"
            @change="toggleSelect(doc.id)"
            class="doc-checkbox"
          />
        </div>
        <div class="doc-body">
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

    <!-- Batch Upload Dialog -->
    <div v-if="showBatchUploadDialog" class="dialog-overlay" @click.self="showBatchUploadDialog = false">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h3>批量上传文档</h3>
          <button class="dialog-close" @click="showBatchUploadDialog = false">&times;</button>
        </div>
        <div class="dialog-body">
          <!-- 文件选择区域 -->
          <div class="form-group">
            <label>选择本地文件</label>
            <div class="file-drop-zone" @dragover.prevent @drop.prevent="handleFileDrop">
              <input
                type="file"
                ref="fileInputRef"
                multiple
                accept=".md,.txt,.markdown,.text,.json,.csv,.html,.htm,.xml,.log"
                @change="handleFileSelect"
                hidden
              />
              <div class="drop-content" @click="fileInputRef?.click()">
                <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
                  <path d="M20 25V10M20 10l-6 6M20 10l6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M6 25v5h28v-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <p>点击选择文件 或 拖拽文件到此处</p>
                <span class="drop-hint">支持 .md .txt .json .csv .html .xml .log 等格式，可多选</span>
              </div>
            </div>
            <!-- 已选文件列表 -->
            <div v-if="selectedFiles.length > 0" class="selected-files">
              <div class="files-header">
                <span>已选 {{ selectedFiles.length }} 个文件</span>
                <button class="btn-link" @click="selectedFiles = []">清空</button>
              </div>
              <div class="files-list">
                <div v-for="(file, idx) in selectedFiles" :key="idx" class="file-item">
                  <span class="file-icon">📄</span>
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  <button class="file-remove" @click="selectedFiles.splice(idx, 1)">&times;</button>
                </div>
              </div>
            </div>
          </div>
          <!-- 分隔线 -->
          <div class="divider-or"><span>或</span></div>
          <!-- 手动输入区域 -->
          <div class="form-group">
            <label>手动输入</label>
            <div class="batch-upload-tip">每行一个文档，格式：标题 | 内容（用 | 分隔）</div>
            <textarea
              v-model="batchUploadText"
              rows="6"
              placeholder="标题1 | 内容1&#10;标题2 | 内容2"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-ghost" @click="showBatchUploadDialog = false">取消</button>
          <button class="btn-primary" :disabled="batchUploading" @click="handleBatchUpload">
            {{ batchUploading ? '上传中...' : '上传' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Keyword Index Dialog -->
    <div v-if="showKeywordDialog" class="dialog-overlay" @click.self="showKeywordDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>关键词索引</h3>
          <button class="dialog-close" @click="showKeywordDialog = false">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>关键词</label>
            <input v-model="keywordIndex" placeholder="输入关键词，将搜索匹配的文档并索引到 RAG" @keyup.enter="handleKeywordIndex" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-ghost" @click="showKeywordDialog = false">取消</button>
          <button class="btn-primary" :disabled="keywordIndexing || !keywordIndex.trim()" @click="handleKeywordIndex">
            {{ keywordIndexing ? '索引中...' : '开始索引' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDocuments,
  createDocument,
  deleteDocument,
  uploadDocument,
  getDocumentDetail,
  batchUploadDocuments,
  batchDeleteDocuments,
  batchIndexDocuments,
  batchIndexByKeyword,
} from '@/api/document'
import { indexDocument as indexDoc } from '@/api/rag'
import type { DocumentVO } from '@/api/document'

const loading = ref(false)
const creating = ref(false)
const documents = ref<DocumentVO[]>([])
const total = ref(0)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showBatchUploadDialog = ref(false)
const showKeywordDialog = ref(false)
const currentDoc = ref<DocumentVO | null>(null)
const selectedIds = ref<Set<number>>(new Set())
const batchUploading = ref(false)
const keywordIndexing = ref(false)

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

const batchUploadText = ref('')
const batchContentType = ref('markdown')
const keywordIndex = ref('')
const selectedFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)

const isAllSelected = computed(() => {
  return documents.value.length > 0 && documents.value.every(doc => selectedIds.value.has(doc.id))
})

function toggleSelect(id: number) {
  const newSet = new Set(selectedIds.value)
  if (newSet.has(id)) {
    newSet.delete(id)
  } else {
    newSet.add(id)
  }
  selectedIds.value = newSet
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(documents.value.map(doc => doc.id))
  }
}

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

async function handleBatchUpload() {
  const docs: Array<{ title: string; content: string; contentType?: string }> = []

  // 1. 处理本地文件
  if (selectedFiles.value.length > 0) {
    for (const file of selectedFiles.value) {
      try {
        const content = await readFileContent(file)
        const ext = file.name.split('.').pop()?.toLowerCase() || 'txt'
        const contentType = ext === 'md' || ext === 'markdown' ? 'markdown' : 'text'
        docs.push({
          title: file.name.replace(/\.[^.]+$/, ''), // 去掉扩展名
          content,
          contentType,
        })
      } catch (e) {
        ElMessage.warning(`读取文件 ${file.name} 失败`)
      }
    }
  }

  // 2. 处理手动输入
  const lines = batchUploadText.value.trim().split('\n').filter(line => line.trim())
  for (const line of lines) {
    const parts = line.split('|').map(s => s.trim())
    if (parts.length < 2 || !parts[0] || !parts[1]) {
      ElMessage.warning(`格式错误：${line}`)
      return
    }
    docs.push({
      title: parts[0],
      content: parts[1],
      contentType: batchContentType.value,
    })
  }

  if (docs.length === 0) {
    ElMessage.warning('请选择文件或输入文档内容')
    return
  }

  batchUploading.value = true
  try {
    await batchUploadDocuments(docs)
    ElMessage.success(`成功上传 ${docs.length} 份文档`)
    showBatchUploadDialog.value = false
    batchUploadText.value = ''
    selectedFiles.value = []
    await fetchDocuments()
  } catch { /* ignore */ }
  finally { batchUploading.value = false }
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    selectedFiles.value = [...selectedFiles.value, ...Array.from(input.files)]
    input.value = '' // 清空 input 允许重复选择同文件
  }
}

function handleFileDrop(e: DragEvent) {
  if (e.dataTransfer?.files) {
    selectedFiles.value = [...selectedFiles.value, ...Array.from(e.dataTransfer.files)]
  }
}

function readFileContent(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsText(file)
  })
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleBatchDelete() {
  if (selectedIds.value.size === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.size} 份文档？`, '确认批量删除', { type: 'warning' })
    await batchDeleteDocuments(Array.from(selectedIds.value))
    ElMessage.success('批量删除成功')
    selectedIds.value = new Set()
    await fetchDocuments()
  } catch { /* ignore */ }
}

async function handleBatchIndex() {
  if (selectedIds.value.size === 0) return
  try {
    await batchIndexDocuments(Array.from(selectedIds.value))
    ElMessage.success(`成功索引 ${selectedIds.value.size} 份文档到 RAG`)
  } catch { /* ignore */ }
}

async function handleKeywordIndex() {
  if (!keywordIndex.value.trim()) return
  keywordIndexing.value = true
  try {
    await batchIndexByKeyword(keywordIndex.value.trim())
    ElMessage.success('关键词索引完成')
    showKeywordDialog.value = false
    keywordIndex.value = ''
  } catch { /* ignore */ }
  finally { keywordIndexing.value = false }
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

.header-top {
  margin-bottom: 12px;
}

.doc-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.stats-count {
  color: var(--color-text-secondary);
}

.stats-selected {
  color: var(--color-accent);
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
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

.search-clear {
  background: none;
  border: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
  border-radius: 2px;
}

.search-clear:hover {
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
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

/* Batch Actions */
.batch-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.batch-left {
  display: flex;
  gap: 6px;
  align-items: center;
}

.btn-batch {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 10px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: var(--font-sans);
}

.btn-batch:hover:not(:disabled) {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.btn-batch:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-batch-danger:hover:not(:disabled) {
  color: var(--color-error);
  border-color: var(--color-error);
  background: #fef2f2;
}

.btn-batch-accent:hover:not(:disabled) {
  color: var(--color-accent);
  border-color: var(--color-accent);
  background: var(--color-accent-subtle);
}

.btn-text {
  background: none;
  border: none;
  font-size: 12px;
  color: var(--color-accent);
  cursor: pointer;
  font-family: var(--font-sans);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}

.btn-text:hover {
  background: var(--color-accent-subtle);
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
  display: flex;
  align-items: flex-start;
  gap: 12px;
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

.doc-card.doc-selected {
  border-color: var(--color-accent);
  background: var(--color-accent-subtle);
}

.doc-checkbox-wrap {
  display: flex;
  align-items: center;
  padding-top: 2px;
}

.doc-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--color-accent);
}

.doc-body {
  flex: 1;
  min-width: 0;
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

.form-select {
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
  cursor: pointer;
}

.form-select:focus {
  border-color: var(--color-accent);
}

.batch-upload-tip {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-sm);
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

/* File Drop Zone */
.file-drop-zone {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.file-drop-zone:hover {
  border-color: var(--color-accent);
  background: var(--color-bg-subtle);
}

.drop-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-tertiary);
}

.drop-content p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.drop-hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* Selected Files */
.selected-files {
  margin-top: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.files-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--color-bg-subtle);
  font-size: 13px;
  font-weight: 500;
}

.btn-link {
  background: none;
  border: none;
  color: var(--color-accent);
  cursor: pointer;
  font-size: 12px;
  padding: 0;
}

.files-list {
  max-height: 200px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid var(--color-border-subtle);
  font-size: 13px;
}

.file-icon {
  font-size: 16px;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: var(--color-text-tertiary);
  font-size: 12px;
  flex-shrink: 0;
}

.file-remove {
  background: none;
  border: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: 16px;
  padding: 0 4px;
}

.file-remove:hover {
  color: #ef4444;
}

/* Divider */
.divider-or {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.divider-or::before,
.divider-or::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}
</style>
