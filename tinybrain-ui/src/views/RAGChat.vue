<template>
  <div class="chat-page">
    <!-- Mobile overlay -->
    <div
      v-if="sidebarOpen && isMobile"
      class="sidebar-overlay"
      @click="sidebarOpen = false"
    ></div>

    <!-- Session Sidebar -->
    <aside class="session-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="startNewChat">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          新对话
        </button>
      </div>

      <div class="session-list" ref="sessionListRef">
        <div v-if="loadingSessions" class="session-loading">
          <span class="spinner-sm"></span>
        </div>
        <div v-else-if="sessions.length === 0" class="session-empty">
          暂无会话记录
        </div>
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{
            active: session.sessionId === currentSessionId,
            selected: selectedSessions.has(session.sessionId)
          }"
          @click="switchSession(session.sessionId)"
        >
          <label
            v-if="batchMode"
            class="session-checkbox"
            @click.stop
          >
            <input
              type="checkbox"
              :checked="selectedSessions.has(session.sessionId)"
              @change="toggleSelect(session.sessionId)"
            />
          </label>
          <div class="session-info">
            <div class="session-title">{{ session.title || '新对话' }}</div>
            <div class="session-time">{{ formatTime(session.updatedAt || session.createdAt) }}</div>
          </div>
          <button
            v-if="!batchMode"
            class="session-delete-btn"
            title="删除会话"
            @click.stop="confirmDeleteSession(session.sessionId)"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M3 3.5h8M5.5 3.5V2.5a1 1 0 011-1h1a1 1 0 011 1v1M4.5 3.5v8a1 1 0 001 1h3a1 1 0 001-1v-8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="sidebar-footer">
        <button
          v-if="!batchMode"
          class="batch-btn"
          :disabled="sessions.length === 0"
          @click="enterBatchMode"
        >
          批量删除
        </button>
        <template v-else>
          <button class="batch-action-btn cancel" @click="exitBatchMode">取消</button>
          <button
            class="batch-action-btn confirm"
            :disabled="selectedSessions.size === 0"
            @click="confirmBatchDelete"
          >
            删除 ({{ selectedSessions.size }})
          </button>
        </template>
      </div>
    </aside>

    <!-- Chat Area -->
    <div class="chat-main">
      <!-- Mobile toggle -->
      <button class="mobile-toggle" @click="sidebarOpen = !sidebarOpen">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path d="M3 5h14M3 10h14M3 15h14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </button>

      <div class="chat-container">
        <!-- Messages -->
        <div class="messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="2"/>
                <path d="M16 20h16M16 28h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <h3>RAG 智能问答</h3>
            <p>基于知识库内容的检索增强生成，让 AI 根据你的文档回答问题</p>
            <div class="empty-suggestions">
              <button v-for="s in suggestions" :key="s" class="suggestion" @click="question = s">
                {{ s }}
              </button>
            </div>
          </div>

          <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
            <div class="msg-avatar">
              <template v-if="msg.role === 'user'">U</template>
              <template v-else>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M6 8h4M8 6v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </template>
            </div>
            <div class="msg-body">
              <div class="msg-content">
                <div v-if="msg.loading" class="typing">
                  <span></span><span></span><span></span>
                </div>
                <template v-else>{{ msg.content }}</template>
              </div>
              <!-- Sources -->
              <div v-if="msg.chunks && msg.chunks.length > 0" class="msg-sources">
                <button class="sources-toggle" @click="msg.showSources = !msg.showSources">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path d="M3 5l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  {{ msg.chunks.length }} 个参考来源
                </button>
                <div v-if="msg.showSources" class="sources-list">
                  <div v-for="(chunk, ci) in msg.chunks" :key="ci" class="source-item">
                    <div class="source-header">
                      <span class="source-title">{{ chunk.documentTitle }}</span>
                      <span class="source-score">{{ (chunk.score * 100).toFixed(0) }}%</span>
                    </div>
                    <p class="source-text">{{ chunk.content.substring(0, 150) }}{{ chunk.content.length > 150 ? '...' : '' }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="input-area">
          <div class="input-wrapper">
            <textarea
              v-model="question"
              placeholder="输入问题，基于知识库进行问答..."
              :disabled="asking"
              @keydown.enter.ctrl="sendQuestion"
              @keydown.enter.meta="sendQuestion"
              rows="1"
              ref="inputRef"
            ></textarea>
            <button class="send-btn" :disabled="!question.trim() || asking" @click="sendQuestion">
              <svg v-if="!asking" width="18" height="18" viewBox="0 0 18 18" fill="none">
                <path d="M3 9h12M10 4l5 5-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span v-else class="spinner"></span>
            </button>
          </div>
          <div class="input-footer">
            <span class="input-hint">Ctrl + Enter 发送 · 先在「知识库」上传文档并索引</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSessions,
  getSessionMessages,
  deleteSession,
  batchDeleteSessions,
} from '@/api/agent'
import { askRAG } from '@/api/rag'
import type { ChunkResult } from '@/api/rag'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  chunks?: ChunkResult[]
  showSources?: boolean
}

interface SessionItem {
  sessionId: string
  title: string
  createdAt: string
  updatedAt?: string
}

const route = useRoute()
const router = useRouter()

const suggestions = [
  '这个项目的主要功能是什么？',
  '如何配置系统参数？',
  '有哪些常见的使用问题？',
]

const messages = ref<ChatMessage[]>([])
const question = ref('')
const asking = ref(false)
const currentSessionId = ref('')
const isNewSession = ref(true)
const messagesRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()
const sessions = ref<SessionItem[]>([])
const loadingSessions = ref(false)
const sidebarOpen = ref(true)
const batchMode = ref(false)
const selectedSessions = ref(new Set<string>())
const sessionListRef = ref<HTMLElement>()
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    sidebarOpen.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  if (diffHour < 24) return `${diffHour} 小时前`
  if (diffDay < 7) return `${diffDay} 天前`

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function startNewChat() {
  currentSessionId.value = ''
  isNewSession.value = true
  messages.value = []
  router.replace({ name: 'RAGChat' })
  if (isMobile.value) {
    sidebarOpen.value = false
  }
}

async function switchSession(sessionId: string) {
  if (sessionId === currentSessionId.value) return
  currentSessionId.value = sessionId
  isNewSession.value = false
  router.replace({ name: 'RAGChat', params: { sessionId } })
  await loadSessionMessages(sessionId)
  if (isMobile.value) {
    sidebarOpen.value = false
  }
}

async function loadSessionMessages(sessionId: string) {
  try {
    const res: any = await getSessionMessages(sessionId)
    if (res?.data) {
      const msgs: ChatMessage[] = []
      const raw = Array.isArray(res.data) ? res.data : []
      for (const item of raw) {
        if (item.role === 'user') {
          msgs.push({ role: 'user', content: item.content || '' })
        } else if (item.role === 'assistant') {
          msgs.push({
            role: 'assistant',
            content: item.content || item.reply || '',
            chunks: item.chunks || [],
            showSources: false,
          })
        }
      }
      messages.value = msgs
    }
  } catch {
    ElMessage.error('加载会话消息失败')
  }
  scrollToBottom()
}

async function fetchSessionList() {
  loadingSessions.value = true
  try {
    const res: any = await getSessions()
    if (res?.data) {
      const list = Array.isArray(res.data) ? res.data : []
      list.sort((a: SessionItem, b: SessionItem) => {
        const ta = new Date(b.updatedAt || b.createdAt).getTime()
        const tb = new Date(a.updatedAt || a.createdAt).getTime()
        return ta - tb
      })
      sessions.value = list
    }
  } catch {
    // ignore
  } finally {
    loadingSessions.value = false
  }
}

function confirmDeleteSession(sessionId: string) {
  ElMessageBox.confirm('确定要删除这个会话吗？此操作不可恢复。', '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteSession(sessionId)
      ElMessage.success('会话已删除')
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
      if (currentSessionId.value === sessionId) {
        startNewChat()
      }
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

function enterBatchMode() {
  batchMode.value = true
  selectedSessions.value = new Set()
}

function exitBatchMode() {
  batchMode.value = false
  selectedSessions.value = new Set()
}

function toggleSelect(sessionId: string) {
  const set = new Set(selectedSessions.value)
  if (set.has(sessionId)) {
    set.delete(sessionId)
  } else {
    set.add(sessionId)
  }
  selectedSessions.value = set
}

function confirmBatchDelete() {
  const count = selectedSessions.value.size
  if (count === 0) return
  ElMessageBox.confirm(`确定要删除选中的 ${count} 个会话吗？此操作不可恢复。`, '批量删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const ids = Array.from(selectedSessions.value)
      await batchDeleteSessions(ids)
      ElMessage.success(`已删除 ${count} 个会话`)
      sessions.value = sessions.value.filter(s => !selectedSessions.value.has(s.sessionId))
      if (selectedSessions.value.has(currentSessionId.value)) {
        startNewChat()
      }
      exitBatchMode()
    } catch {
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

async function sendQuestion() {
  const q = question.value.trim()
  if (!q || asking.value) return

  messages.value.push({ role: 'user', content: q })
  question.value = ''
  scrollToBottom()

  const loadingMsg: ChatMessage = { role: 'assistant', content: '', loading: true }
  messages.value.push(loadingMsg)
  asking.value = true
  scrollToBottom()

  try {
    const res: any = await askRAG(q, 5, currentSessionId.value || undefined)
    if (res?.data) {
      loadingMsg.content = res.data.answer || '未找到相关答案。请确保知识库中有相关文档且已索引。'
      loadingMsg.chunks = res.data.chunks || []
      loadingMsg.showSources = false

      // Update sessionId from response if this was a new session
      if (isNewSession.value && res.data.sessionId) {
        currentSessionId.value = res.data.sessionId
        isNewSession.value = false
        router.replace({ name: 'RAGChat', params: { sessionId: res.data.sessionId } })
        fetchSessionList()
      }
    } else {
      loadingMsg.content = '无法获取回答，请检查 LLM API 配置。'
    }
  } catch {
    loadingMsg.content = '请求失败，请检查后端服务是否正常运行。'
  } finally {
    loadingMsg.loading = false
    asking.value = false
    scrollToBottom()
    inputRef.value?.focus()
  }
}

function handleResize() {
  checkMobile()
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', handleResize)
  fetchSessionList()

  // If URL has sessionId, load that session
  const urlSessionId = route.params.sessionId as string
  if (urlSessionId) {
    currentSessionId.value = urlSessionId
    isNewSession.value = false
    loadSessionMessages(urlSessionId)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 56px - 48px);
  display: flex;
  overflow: hidden;
}

/* ======== Session Sidebar ======== */
.session-sidebar {
  width: 260px;
  min-width: 260px;
  background: #1e1e2e;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  color: #e0e0e0;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: var(--font-sans);
}

.new-chat-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.18);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-list::-webkit-scrollbar {
  width: 4px;
}

.session-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.session-loading {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.spinner-sm {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.15);
  border-top-color: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.session-empty {
  text-align: center;
  padding: 24px 12px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.12s ease;
  margin-bottom: 2px;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.session-item.active {
  background: rgba(79, 70, 229, 0.25);
  border: 1px solid rgba(79, 70, 229, 0.3);
}

.session-item.selected {
  background: rgba(79, 70, 229, 0.15);
}

.session-checkbox {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  cursor: pointer;
}

.session-checkbox input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--color-accent);
  cursor: pointer;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 13px;
  font-weight: 500;
  color: #e0e0e0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 2px;
}

.session-delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  color: rgba(255, 255, 255, 0.25);
  border-radius: var(--radius-sm);
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: all 0.12s ease;
}

.session-item:hover .session-delete-btn {
  opacity: 1;
}

.session-delete-btn:hover {
  background: rgba(220, 38, 38, 0.2);
  color: #f87171;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  gap: 8px;
}

.batch-btn {
  width: 100%;
  padding: 8px 16px;
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: var(--font-sans);
}

.batch-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.7);
}

.batch-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.batch-action-btn {
  flex: 1;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: var(--font-sans);
}

.batch-action-btn.cancel {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
}

.batch-action-btn.cancel:hover {
  background: rgba(255, 255, 255, 0.12);
}

.batch-action-btn.confirm {
  background: rgba(220, 38, 38, 0.2);
  color: #f87171;
}

.batch-action-btn.confirm:hover:not(:disabled) {
  background: rgba(220, 38, 38, 0.35);
}

.batch-action-btn.confirm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ======== Chat Main ======== */
.chat-main {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
  position: relative;
}

.mobile-toggle {
  display: none;
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  width: 36px;
  height: 36px;
  border: none;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-sm);
}

.chat-container {
  width: 100%;
  max-width: 768px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-tertiary);
}

.empty-icon {
  color: var(--color-text-tertiary);
  opacity: 0.4;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

.empty-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.suggestion {
  padding: 8px 16px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 9999px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: var(--font-sans);
}

.suggestion:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

/* Messages */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 100%;
}

.message.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.message.user .msg-avatar {
  background: var(--color-accent);
  color: white;
}

.message.assistant .msg-avatar {
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.msg-body {
  min-width: 0;
  max-width: 85%;
}

.msg-content {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .msg-content {
  background: var(--color-accent);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .msg-content {
  background: var(--color-bg-elevated);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: 4px;
}

/* Typing animation */
.typing {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing span {
  width: 6px;
  height: 6px;
  background: var(--color-text-tertiary);
  border-radius: 50%;
  animation: pulse 1.4s infinite ease-in-out;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes pulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* Sources */
.msg-sources {
  margin-top: 8px;
}

.sources-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  font-size: 12px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 4px 0;
  font-family: var(--font-sans);
}

.sources-toggle:hover {
  color: var(--color-text-secondary);
}

.sources-list {
  margin-top: 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.source-item {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.source-item:last-child {
  border-bottom: none;
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.source-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.source-score {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-accent);
  background: var(--color-accent-light);
  padding: 1px 6px;
  border-radius: 9999px;
}

.source-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

/* Input */
.input-area {
  padding: 16px 0 20px;
  border-top: 1px solid var(--color-border-subtle);
}

.input-wrapper {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 8px 8px 8px 16px;
  transition: border-color 0.15s ease;
}

.input-wrapper:focus-within {
  border-color: var(--color-accent);
}

.input-wrapper textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text-primary);
  background: none;
  font-family: var(--font-sans);
  max-height: 120px;
}

.input-wrapper textarea::placeholder {
  color: var(--color-text-tertiary);
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.12s ease;
}

.send-btn:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

/* ======== Overlay ======== */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 99;
}

/* ======== Responsive ======== */
@media (max-width: 767px) {
  .session-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .session-sidebar.open {
    transform: translateX(0);
  }

  .mobile-toggle {
    display: flex;
  }

  .chat-container {
    max-width: 100%;
  }

  .messages {
    padding-top: 52px;
  }

  .input-hint {
    display: none;
  }
}
</style>
