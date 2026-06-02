<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- Messages -->
      <div class="messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <path d="M24 4v4M24 40v4M4 24h4M40 24h4M8.6 8.6l2.8 2.8M36.6 36.6l2.8 2.8M8.6 39.4l2.8-2.8M36.6 11.4l2.8-2.8" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <circle cx="24" cy="24" r="8" stroke="currentColor" stroke-width="2.5"/>
            </svg>
          </div>
          <h3>AI Agent</h3>
          <p>智能体可以自主调用工具来回答问题</p>
          <div class="empty-suggestions">
            <button v-for="s in suggestions" :key="s" class="suggestion" @click="userMessage = s">
              {{ s }}
            </button>
            <button v-for="s in skillSuggestions" :key="s.command" class="suggestion skill-suggestion" @click="userMessage = s.command">
              ⚡ {{ s.name }}
            </button>
          </div>
        </div>

        <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
          <div class="msg-avatar">
            <template v-if="msg.role === 'user'">U</template>
            <template v-else>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M8 2v2M8 12v2M2 8h2M12 8h2M4.2 4.2l1.4 1.4M10.4 10.4l1.4 1.4M4.2 11.8l1.4-1.4M10.4 5.6l1.4-1.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                <circle cx="8" cy="8" r="2.5" stroke="currentColor" stroke-width="1.5"/>
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
            <!-- Matched Skill Badge -->
            <div v-if="msg.matchedSkill" class="skill-badge">
              <span class="skill-badge-icon">⚡</span>
              <span class="skill-badge-name">{{ msg.matchedSkill.name }}</span>
              <span class="skill-badge-type">{{ msg.matchedSkill.triggerType === 'manual' ? '手动调用' : '自动触发' }}</span>
            </div>
            <!-- Tool Calls -->
            <div v-if="msg.toolCalls && msg.toolCalls.length > 0" class="tool-calls">
              <div v-for="(tc, ti) in msg.toolCalls" :key="ti" class="tool-call">
                <div class="tc-header">
                  <span class="tc-name">{{ tc.toolName }}</span>
                  <span class="tc-args">{{ tc.args }}</span>
                </div>
                <div class="tc-result">{{ tc.result }}</div>
              </div>
            </div>
            <div v-if="msg.iterations != null && msg.iterations > 1" class="msg-meta">
              {{ msg.iterations }} 轮工具调用
            </div>
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="input-area">
        <div class="input-wrapper">
          <textarea
            v-model="userMessage"
            :placeholder="inputPlaceholder"
            :disabled="chatting"
            @keydown.enter.ctrl="sendMessage"
            @keydown.enter.meta="sendMessage"
            rows="1"
            ref="inputRef"
          ></textarea>
          <button class="send-btn" :disabled="!userMessage.trim() || chatting" @click="sendMessage">
            <svg v-if="!chatting" width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M3 9h12M10 4l5 5-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span v-else class="spinner"></span>
          </button>
        </div>
        <div class="input-footer">
          <span class="input-hint">Ctrl + Enter 发送 · /skill名 调用技能</span>
          <div class="tools-bar">
            <span v-for="tool in tools" :key="tool.name" class="tool-chip">
              🔧 {{ tool.name }}
            </span>
            <span v-for="skill in skills" :key="skill.id" class="tool-chip skill-chip" @click="userMessage = '/' + skill.name + ' '">
              ⚡ {{ skill.name }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { chatWithAgent, getAgentTools, clearSession } from '@/api/agent'
import type { ToolCall as ToolCallType, SkillMatch } from '@/api/agent'
import { getSkills } from '@/api/skill'
import type { SkillInfo } from '@/api/skill'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  toolCalls?: ToolCallType[]
  iterations?: number
  matchedSkill?: SkillMatch
}

interface ToolItem {
  name: string
  description: string
}

const suggestions = [
  '今天几号？',
  '帮我算一下 (15 + 3) * 2',
  '搜索关于 Spring Boot 的知识',
]

const messages = ref<ChatMessage[]>([])
const userMessage = ref('')
const chatting = ref(false)
const tools = ref<ToolItem[]>([])
const skills = ref<SkillInfo[]>([])
const sessionId = ref(generateSessionId())
const messagesRef = ref<HTMLElement>()
const inputRef = ref<HTMLTextAreaElement>()

// 从已启用的 skills 生成建议（取前 3 个）
const skillSuggestions = computed(() => {
  return skills.value
    .filter(s => s.enabled)
    .slice(0, 3)
    .map(s => ({
      name: s.name,
      command: `/${s.name} `,
    }))
})

const inputPlaceholder = computed(() => {
  if (skills.value.length > 0) {
    return `输入消息，或用 /${skills.value[0].name} 调用技能...`
  }
  return '输入消息，Agent 会自动调用工具...'
})

function generateSessionId() {
  return 's_' + Date.now().toString(36) + Math.random().toString(36).substring(2, 6)
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

    if (res?.data) {
      loadingMsg.content = res.data.reply || '处理完成'
      loadingMsg.toolCalls = res.data.toolCalls || []
      loadingMsg.iterations = res.data.iterations
      loadingMsg.matchedSkill = res.data.matchedSkill || undefined
    } else {
      loadingMsg.content = 'Agent 暂时无法响应。'
    }
  } catch {
    loadingMsg.content = '请求失败，请检查后端服务。'
  } finally {
    loadingMsg.loading = false
    chatting.value = false
    scrollToBottom()
    inputRef.value?.focus()
  }
}

async function fetchTools() {
  try {
    const res: any = await getAgentTools()
    if (res?.data) {
      tools.value = Object.entries(res.data).map(([name, desc]) => ({
        name,
        description: desc as string,
      }))
    }
  } catch { /* ignore */ }
}

async function fetchSkills() {
  try {
    const res: any = await getSkills()
    if (res?.data) {
      skills.value = res.data.filter((s: SkillInfo) => s.enabled)
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchTools()
  fetchSkills()
})
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

/* Tool Calls */
.tool-calls {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tool-call {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  font-size: 12px;
}

.tc-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.tc-name {
  font-weight: 600;
  color: var(--color-accent);
  background: var(--color-accent-light);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.tc-args {
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
  font-size: 11px;
}

.tc-result {
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.msg-meta {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
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

.tools-bar {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tool-chip {
  font-size: 10px;
  font-weight: 500;
  padding: 2px 8px;
  background: var(--color-bg-subtle);
  color: var(--color-text-tertiary);
  border-radius: 9999px;
}

.tool-chip.skill-chip {
  cursor: pointer;
  color: var(--color-accent);
  background: var(--color-accent-light);
  transition: all 0.12s ease;
}

.tool-chip.skill-chip:hover {
  background: var(--color-accent);
  color: white;
}

/* Skill Badge */
.skill-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  margin-bottom: 6px;
  background: linear-gradient(135deg, #f0f0ff 0%, #e8e8ff 100%);
  border: 1px solid #d0d0ff;
  border-radius: 9999px;
  font-size: 11px;
}

.skill-badge-icon {
  font-size: 12px;
}

.skill-badge-name {
  font-weight: 600;
  color: var(--color-accent);
}

.skill-badge-type {
  color: var(--color-text-tertiary);
  font-size: 10px;
}

/* Skill suggestion on empty state */
.suggestion.skill-suggestion {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-light);
}
</style>
