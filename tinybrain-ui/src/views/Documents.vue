<template>
  <div class="documents-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">知识库</h2>
        <p class="page-desc">管理您的知识文档，支持 Markdown 和纯文本</p>
      </div>
      <div class="page-actions">
        <el-upload
          :show-file-list="false"
          :before-upload="handleUpload"
          accept=".md,.txt,.markdown"
        >
          <el-button type="primary" :icon="UploadFilled" :loading="uploading">
            {{ uploading ? '上传中...' : '上传文件' }}
          </el-button>
        </el-upload>
        <el-button :icon="Plus" @click="showCreateDialog = true">新建文档</el-button>
      </div>
    </div>

    <!-- Search & Filter -->
    <el-card shadow="never" class="search-card">
      <el-row :gutter="16" align="middle">
        <el-col :xs="18" :sm="20">
          <el-input v-model="query.keyword" placeholder="搜索文档标题..." clearable :prefix-icon="Search" @clear="fetchDocuments" @keyup.enter="fetchDocuments" />
        </el-col>
        <el-col :xs="6" :sm="4">
          <el-select v-model="query.status" placeholder="状态" clearable style="width:100%" @change="fetchDocuments">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
          </el-select>
        </el-col>
      </el-row>
    </el-card>

    <!-- Document List -->
    <el-card shadow="never" v-loading="loading">
      <el-table :data="documents" stripe style="width:100%" @row-click="viewDetail">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="doc-title">
              <el-tag v-if="row.contentType === 'markdown'" size="small" type="info">MD</el-tag>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click.stop="viewDetail(row)">查看</el-button>
            <el-button text size="small" type="danger" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @change="fetchDocuments"
      />
    </el-card>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建文档" width="700px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="0">
        <el-form-item prop="title">
          <el-input v-model="createForm.title" placeholder="文档标题" />
        </el-form-item>
        <el-form-item prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="15" placeholder="支持 Markdown 格式内容..." />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="createForm.summary" type="textarea" :rows="2" placeholder="文档摘要（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="showDetailDialog" title="文档详情" width="800px" :close-on-click-modal="false" top="5vh">
      <template v-if="currentDoc">
        <h3 class="detail-title">{{ currentDoc.title }}</h3>
        <div class="detail-meta">
          <el-tag v-if="currentDoc.contentType === 'markdown'" size="small">Markdown</el-tag>
          <el-tag :type="currentDoc.status === 1 ? 'success' : 'warning'" size="small">
            {{ currentDoc.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
          <span class="detail-time">创建: {{ currentDoc.createTime }}</span>
        </div>
        <el-divider />
        <div class="detail-content markdown-body" v-html="renderedContent" />
      </template>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button type="primary" @click="indexDocument">索引到 RAG</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, UploadFilled } from '@element-plus/icons-vue'
import { getDocuments, createDocument, deleteDocument, uploadDocument, getDocumentDetail } from '@/api/document'
import { indexDocument as indexDoc } from '@/api/rag'
import type { DocumentVO } from '@/api/document'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const creating = ref(false)
const uploading = ref(false)
const documents = ref<DocumentVO[]>([])
const total = ref(0)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const currentDoc = ref<DocumentVO | null>(null)
const createFormRef = ref<FormInstance>()

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

const createRules: FormRules = {
  title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文档内容', trigger: 'blur' }],
}

const renderedContent = computed(() => {
  if (!currentDoc.value?.content) return ''
  return currentDoc.value.content
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
    .replace(/#{3}\s+(.+)/g, '<h3>$1</h3>')
    .replace(/#{2}\s+(.+)/g, '<h2>$1</h2>')
    .replace(/#{1}\s+(.+)/g, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
})

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

// 处理上传文件
async function handleUpload(file: File) {
  uploading.value = true
  try {
    const res: any = await uploadDocument(file)
    if (res?.data) {
      ElMessage.success('上传成功')
      await fetchDocuments()
    }
  } catch { /* ignore */ }
  finally { uploading.value = false }
  return false // 阻止默认上传
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const res: any = await createDocument(createForm)
    if (res?.data) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      createForm.title = ''
      createForm.summary = ''
      createForm.content = ''
      await fetchDocuments()
    }
  } catch { /* ignore */ }
  finally { creating.value = false }
}

async function viewDetail(row: DocumentVO) {
  try {
    const res: any = await getDocumentDetail(row.id)
    if (res?.data) {
      currentDoc.value = res.data
      showDetailDialog.value = true
    }
  } catch { /* ignore */ }
}

async function handleDelete(row: DocumentVO) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${row.title}」？`, '确认删除', { type: 'warning' })
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    await fetchDocuments()
  } catch { /* ignore */ }
}

async function indexDocument() {
  if (!currentDoc.value) return
  try {
    await indexDoc(currentDoc.value.id)
    ElMessage.success('文档已索引到 RAG 知识库，可以进行问答了')
  } catch { /* ignore */ }
}

onMounted(fetchDocuments)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.page-desc {
  color: #718096;
  font-size: 14px;
  margin: 4px 0 0 0;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.search-card {
  margin-bottom: 16px;
}

.doc-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.detail-title {
  font-size: 20px;
  margin: 0 0 8px 0;
}

.detail-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.detail-time {
  font-size: 12px;
  color: #a0aec0;
}

.detail-content {
  line-height: 1.8;
  font-size: 14px;
  color: #2d3748;
}
</style>
