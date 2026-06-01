<template>
  <div class="dashboard">
    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-label">{{ stat.label }}</div>
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-icon" v-html="stat.icon"></div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="section">
      <h3 class="section-title">快速开始</h3>
      <div class="actions-grid">
        <router-link to="/documents" class="action-card">
          <div class="action-icon" style="background: #eef2ff; color: #4f46e5;">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M4 2h9l3 3v13a1 1 0 01-1 1H4a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" stroke-width="1.5"/>
              <path d="M7 11h6M7 14h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="action-content">
            <span class="action-title">上传文档</span>
            <span class="action-desc">添加知识库文档，支持 Markdown 和文本</span>
          </div>
          <svg class="action-arrow" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </router-link>

        <router-link to="/rag" class="action-card">
          <div class="action-icon" style="background: #ecfdf5; color: #059669;">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/>
              <path d="M10 7v6M7 10h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="action-content">
            <span class="action-title">RAG 问答</span>
            <span class="action-desc">基于知识库内容进行智能问答</span>
          </div>
          <svg class="action-arrow" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </router-link>

        <router-link to="/agent" class="action-card">
          <div class="action-icon" style="background: #fef3c7; color: #d97706;">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M10 2v2M10 16v2M2 10h2M16 10h2M4.2 4.2l1.4 1.4M14.4 14.4l1.4 1.4M4.2 15.8l1.4-1.4M14.4 5.6l1.4-1.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="10" cy="10" r="3" stroke="currentColor" stroke-width="1.5"/>
            </svg>
          </div>
          <div class="action-content">
            <span class="action-title">AI Agent</span>
            <span class="action-desc">智能体对话，支持工具调用</span>
          </div>
          <svg class="action-arrow" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </router-link>
      </div>
    </div>

    <!-- Tech Stack -->
    <div class="section">
      <h3 class="section-title">技术栈</h3>
      <div class="tech-grid">
        <div class="tech-item" v-for="tech in techStack" :key="tech.name">
          <span class="tech-name">{{ tech.name }}</span>
          <span class="tech-desc">{{ tech.desc }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const stats = ref([
  {
    label: '文档',
    value: '--',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M4 2h9l3 3v13a1 1 0 01-1 1H4a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    label: '向量',
    value: '--',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M8 10l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    label: '工具',
    value: '4',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M14.7 3.3a1 1 0 011.4 0l.6.6a1 1 0 010 1.4l-8.5 8.5-2.8.7.7-2.8 8.6-8.4z" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    label: '状态',
    value: '运行中',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="3" fill="#16a34a"/><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/></svg>'
  }
])

const techStack = [
  { name: 'Spring Boot 3.2', desc: '应用框架' },
  { name: 'Spring Security', desc: '认证授权' },
  { name: 'MyBatis-Plus', desc: 'ORM 持久层' },
  { name: 'WebClient', desc: 'LLM API 调用' },
  { name: 'Resilience4j', desc: '熔断降级' },
  { name: 'IVF Index', desc: '向量检索' },
  { name: 'Docker Compose', desc: '容器部署' },
  { name: 'Prometheus', desc: '指标监控' },
]

onMounted(async () => {
  try {
    const { getDocuments } = await import('@/api/document')
    const docRes: any = await getDocuments({ page: 1, pageSize: 1 })
    if (docRes?.data) {
      stats.value[0].value = String(docRes.data.total || 0)
    }
  } catch { /* ignore */ }

  try {
    const { getRAGStats } = await import('@/api/rag')
    const ragRes: any = await getRAGStats()
    if (ragRes?.data) {
      stats.value[1].value = String(ragRes.data.totalVectors || 0)
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.dashboard {
  max-width: 960px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

.stat-icon {
  position: absolute;
  top: 16px;
  right: 16px;
  color: var(--color-text-tertiary);
  opacity: 0.5;
}

.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.actions-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: all 0.12s ease;
}

.action-card:hover {
  border-color: var(--color-accent-subtle);
  box-shadow: var(--shadow-sm);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.action-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.action-arrow {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.action-card:hover .action-arrow {
  opacity: 1;
  transform: translateX(2px);
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.tech-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 16px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.tech-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.tech-desc {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .tech-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
