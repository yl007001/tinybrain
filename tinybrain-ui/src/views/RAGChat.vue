<template>
  <div class="rag-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">RAG 智能问答</h2>
        <p class="page-desc">基于知识库的语义检索 + AI 增强生成，让 AI 根据您的文档回答问题</p>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- Chat Area -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chat-card">
          <!-- Messages -->
          <div class="chat-messages" ref="messagesRef">
            <div v-if="messages.length === 0" class="chat-empty">
              <div class="empty-icon">💬</div>
              <p>输入问题开始基于知识库的智能问答</p>
              <p class="empty-hint">确保已上传文档到知识库并执行索引</p>
            </div>
            <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
              <div class="message-avatar">
                {{ msg.role === 'user' ? '👤' : '🧠' }}
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <div v-if="msg.loading" class="loading-dots">
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                  </div>
                  <div v-else>{{ msg.content }}</div>
                </div>
                <!-- Source chunks (for assistant) -->
                <div v-if="msg.chunks && msg.chunks.length > 0" class="message-sources">
                  <el-collapse accordion>
                    <el-collapse-item title="📚 参考来源" name="sources">
                      <div v-for="(chunk, ci) in msg.chunks" :key="ci" class="source-item">
                        <div class="source-header">
                          <span class="source-title">{{ chunk.documentTitle }}</span>
                          <el-tag size="small" type="info">相似度: {{ (chunk.score * 100).toFixed(1) }}%</el-tag>
                        </div>
                        <p class="source-text">{{ chunk.content.substring(0, 200) }}{{ chunk.content.length > 200 ? '...' : '' }}</p>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </div>
            </div>
          </div>

          <!-- Input -->
          <div class="chat-input">
            <el-input
              v-model="question"
              type="textarea"
              :rows="2"
              placeholder="输入您的问题..."
              :disabled="asking"
              @keyup.ctrl.enter="sendQuestion"
            />
            <el-button type="primary" :loading="asking" :disabled="!question.trim()" @click="sendQuestion" class="send-btn">
              {{ asking ? '思考中...' : '发送' }}
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- Sidebar -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">💡 RAG 工作原理</span>
          </template>
          <div class="workflow">
            <div class="step">
              <span class="step-num">1</span>
              <span>用户提问 → 向量化问题</span>
            </div>
            <div class="step">
              <span class="step-num">2</span>
              <span>向量库 Top-K 语义检索</span>
            </div>
            <div class="step">
              <span class="step-num">3</span>
              <span>拼接上下文 + 问题 → Prompt</span>
            </div>
            <div class="step">
              <span class="step-num">4</span>
              <span>LLM 生成带上下文的回答</span>
            </div>
          </div>
          <el-divider />
          <p class="info-tip">
            💡 提示: 使用 <kbd>Ctrl</kbd> + <kbd>Enter</kbd> 快速发送
          </p>
          <p class="info-tip">
            📚 先去「知识库」上传文档并点击「索引到 RAG」
          </p>
        </el-card>

        <el-card shadow="never" class="info-card" style="margin-top:16px">
          <template #header>
            <span class="card-title">📊 当前会话</span>
          </template>
          <div class="session-info">
            <div class="info-item">
              <span class="info-label">问答次数</span>
              <span class="info-value">{{ messages.filter(m => m.role === 'user').length }}</span>
            </div>
            <el-button type="danger" size="small" plain @click="clearChat" style="width:100%;margin-top:12px">
              清空对话
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { askRAG } from '@/api/rag'
import type { ChunkResult } from '@/api/rag'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  chunks?: ChunkResult[]
}

const messages = ref<ChatMessage[]>([])
const question = ref('')
const asking = ref(false)
const messagesRef = ref<HTMLElement>()

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

  // Assistant loading
  const loadingMsg: ChatMessage = { role: 'assistant', content: '', loading: true }
  messages.value.push(loadingMsg)
  asking.value = true
  scrollToBottom()

  try {
    const res: any = await askRAG(q, 5)
    if (res?.data) {
      const answer = res.data.answer || '未找到相关答案。请确保知识库中有相关文档且已索引。'
      loadingMsg.loading = false
      loadingMsg.content = answer
      loadingMsg.chunks = res.data.chunks || []
    } else {
      loadingMsg.loading = false
      loadingMsg.content = '抱歉，无法获取回答。请检查 LLM API 配置。'
    }
  } catch (e) {
    loadingMsg.loading = false
    loadingMsg.content = '请求失败，请检查后端服务是否正常运行。'
  } finally {
    asking.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
}
</script>

<style scoped>
.page-header {
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

.chat-card {
  border-radius: 12px;
}

.chat-messages {
  height: 500px;
  overflow-y: auto;
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-empty {
  text-align: center;
  padding: 80px 0;
  color: #a0aec0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-hint {
  font-size: 12px;
  margin-top: 4px;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.message.assistant {
  align-self: flex-start;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-bubble {
  background: #f0f4ff;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  color: #2d3748;
}

.message.user .message-bubble {
  background: #63b3ed;
  color: #fff;
}

.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.dot {
  width: 8px;
  height: 8px;
  background: #63b3ed;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.dot:nth-child(2) { animation-delay: 0.16s; }
.dot:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.message-sources {
  margin-top: 8px;
}

.source-item {
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.source-title {
  font-weight: 600;
  font-size: 13px;
  color: #4a5568;
}

.source-text {
  font-size: 12px;
  color: #718096;
  margin: 0;
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.send-btn {
  height: 50px;
  min-width: 100px;
  font-size: 14px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

.workflow {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #4a5568;
}

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #63b3ed;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.info-tip {
  font-size: 13px;
  color: #718096;
  margin: 0;
}

kbd {
  background: #edf2f7;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.session-info {
  font-size: 14px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e2e8f0;
}

.info-label {
  color: #718096;
}

.info-value {
  font-weight: 600;
  color: #2d3748;
}
</style>
