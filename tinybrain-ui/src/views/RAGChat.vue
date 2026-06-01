<template>
  <div class="chat-page">
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
          <div class="empty-hints">
            <span>确保已上传文档并执行索引</span>
            <span>支持 Markdown 和纯文本文档</span>
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
        <p class="input-hint">Ctrl + Enter 发送 · 先在「知识库」上传文档并索引</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { askRAG } from '@/api/rag'
import type { ChunkResult } from '@/api/rag'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  chunks?: ChunkResult[]
  showSources?: boolean
}

const messages = ref<ChatMessage[]>([])
const question = ref('')
const asking = ref(false)
const messagesRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
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
    const res: any = await askRAG(q, 5)
    if (res?.data) {
      loadingMsg.content = res.data.answer || '未找到相关答案。请确保知识库中有相关文档且已索引。'
      loadingMsg.chunks = res.data.chunks || []
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
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 56px - 48px);
  display: flex;
  justify-content: center;
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
  padding: 80px 20px;
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
  margin-bottom: 16px;
}

.empty-hints {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-tertiary);
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

.input-hint {
  font-size: 11px;
  color: var(--color-text-tertiary);
  text-align: center;
  margin: 8px 0 0;
}
</style>
