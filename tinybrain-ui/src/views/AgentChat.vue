<template>
  <div class="agent-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">AI Agent</h2>
        <p class="page-desc">智能体对话，自动调用工具（知识搜索、计算器、日期时间等）</p>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- Chat Area -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chat-card">
          <div class="chat-messages" ref="messagesRef">
            <div v-if="messages.length === 0" class="chat-empty">
              <div class="empty-icon">🤖</div>
              <p>开始与 AI Agent 对话</p>
              <p class="empty-hint">Agent 可以调用工具来回答问题，试试 "今天几号？" 或 "搜索关于 Spring 的知识"</p>
            </div>
            <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
              <div class="message-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
              <div class="message-content">
                <div class="message-bubble">
                  <div v-if="msg.loading" class="loading-dots">
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                  </div>
                  <div v-else>{{ msg.content }}</div>
                </div>
                <!-- Tool calls -->
                <div v-if="msg.toolCalls && msg.toolCalls.length > 0" class="tool-calls">
                  <div v-for="(tc, ti) in msg.toolCalls" :key="ti" class="tool-call-item">
                    <div class="tool-call-header">
                      <el-tag size="small" type="warning">🔧 {{ tc.toolName }}</el-tag>
                      <el-tag size="small" type="info">参数: {{ tc.args }}</el-tag>
                    </div>
                    <div class="tool-call-result">
                      {{ tc.result }}
                    </div>
                  </div>
                </div>
                <div v-if="msg.iterations != null && msg.iterations > 0" class="msg-meta">
                  共 {{ msg.iterations }} 轮工具调用
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="userMessage"
              type="textarea"
              :rows="2"
              placeholder="输入消息..."
              :disabled="chatting"
              @keyup.ctrl.enter="sendMessage"
            />
            <el-button type="primary" :loading="chatting" :disabled="!userMessage.trim()" @click="sendMessage" class="send-btn">
              {{ chatting ? '思考中...' : '发送' }}
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- Sidebar -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">🤖 可用工具</span>
          </template>
          <div v-if="tools.length === 0" class="tools-empty">
            <p>加载工具列表中...</p>
          </div>
          <div v-else class="tool-list">
            <div v-for="tool in tools" :key="tool.name" class="tool-item">
              <div class="tool-name">
                <el-tag size="small" type="warning" effect="plain">{{ tool.name }}</el-tag>
              </div>
              <div class="tool-desc">{{ tool.description }}</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="info-card" style="margin-top:16px">
          <template #header>
            <span class="card-title">📊 会话信息</span>
          </template>
          <div class="session-info">
            <div class="info-item">
              <span class="info-label">消息数</span>
              <span class="info-value">{{ messages.filter(m => m.role === 'user').length }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Session ID</span>
              <span class="info-value session-id" :title="sessionId">{{ sessionId.substring(0, 8) }}...</span>
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
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAgent, getAgentTools, clearSession } from '@/api/agent'
import type { ToolCall as ToolCallType } from '@/api/agent'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  toolCalls?: ToolCallType[]
  iterations?: number
}

interface ToolItem {
  name: string
  description: string
}

const messages = ref<ChatMessage[]>([])
const userMessage = ref('')
const chatting = ref(false)
const tools = ref<ToolItem[]>([])
const sessionId = ref(generateSessionId())
const messagesRef = ref<HTMLElement>()

function generateSessionId() {
  return 'session_' + Date.now().toString(36) + '_' + Math.random().toString(36).substring(2, 8)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const msg = userMessage.value.trim()
  if (!msg || chatting.value) return

  messages.value.push({ role: 'user', content: msg })
  userMessage.value = ''
  scrollToBottom()

  const loadingMsg: ChatMessage = { role: 'assistant', content: '', loading: true }
  messages.value.push(loadingMsg)
  chatting.value = true
  scrollToBottom()

  try {
    const res: any = await chatWithAgent({
      message: msg,
      sessionId: sessionId.value,
    })

    loadingMsg.loading = false
    if (res?.data) {
      loadingMsg.content = res.data.reply || 'Agent 处理完成'
      loadingMsg.toolCalls = res.data.toolCalls || []
      loadingMsg.iterations = res.data.iterations
    } else {
      loadingMsg.content = 'Agent 暂时无法响应，请稍后重试。'
    }
  } catch (e) {
    loadingMsg.loading = false
    loadingMsg.content = '对话请求失败，请检查后端服务。'
  } finally {
    chatting.value = false
    scrollToBottom()
  }
}

async function fetchTools() {
  try {
    const res: any = await getAgentTools()
    if (res?.data) {
      tools.value = Object.entries(res.data).map(([name, description]) => ({
        name,
        description: description as string,
      }))
    }
  } catch { /* ignore */ }
}

function clearChat() {
  messages.value = []
  try {
    clearSession(sessionId.value)
  } catch { /* ignore */ }
  sessionId.value = generateSessionId()
  ElMessage.success('对话已清空')
}

onMounted(fetchTools)
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
  color: #a0aec0;
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
  white-space: pre-wrap;
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
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
  background: #667eea;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.dot:nth-child(2) { animation-delay: 0.16s; }
.dot:nth-child(3) { animation-delay: 0.32s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.tool-calls {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tool-call-item {
  background: #fffbeb;
  border-radius: 8px;
  padding: 8px;
  border: 1px solid #fef3c7;
}

.tool-call-header {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.tool-call-result {
  font-size: 12px;
  color: #92400e;
  margin-top: 4px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
}

.msg-meta {
  font-size: 11px;
  color: #a0aec0;
  margin-top: 4px;
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.send-btn {
  height: 50px;
  min-width: 100px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

.tools-empty {
  color: #a0aec0;
  font-size: 13px;
}

.tool-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-item {
  padding: 8px;
  border-radius: 8px;
  background: #f7fafc;
}

.tool-name {
  margin-bottom: 4px;
}

.tool-desc {
  font-size: 12px;
  color: #718096;
  margin-top: 4px;
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

.session-id {
  font-family: monospace;
  font-size: 12px;
}
</style>
