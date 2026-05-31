<template>
  <div class="dashboard">
    <h2 class="page-title">控制台</h2>

    <!-- Stats Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: stat.bg }">
              <el-icon :size="24" :color="stat.color">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Actions -->
    <el-row :gutter="20" class="action-row">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">快速入口</span>
          </template>
          <el-row :gutter="20">
            <el-col :xs="12" :sm="6" v-for="action in actions" :key="action.title">
              <el-card shadow="hover" class="action-card" @click="router.push(action.path)">
                <div class="action-inner">
                  <el-icon :size="32" :color="action.color">
                    <component :is="action.icon" />
                  </el-icon>
                  <div class="action-title">{{ action.title }}</div>
                  <div class="action-desc">{{ action.desc }}</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- System Info -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">系统信息</span>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="项目名称">TinyBrain</el-descriptions-item>
            <el-descriptions-item label="版本">1.0.0</el-descriptions-item>
            <el-descriptions-item label="技术栈">Spring Boot 3.2 + Java 17</el-descriptions-item>
            <el-descriptions-item label="AI 能力">RAG 检索增强 + Agent 智能体</el-descriptions-item>
            <el-descriptions-item label="文档格式">Markdown / 纯文本</el-descriptions-item>
            <el-descriptions-item label="部署方式">Docker Compose</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, ChatDotRound, MagicStick, Upload, Odometer, DataBoard, Collection, TrendCharts } from '@element-plus/icons-vue'

const router = useRouter()

const stats = ref([
  { title: '文档总数', value: '--', icon: Document, color: '#63b3ed', bg: 'rgba(99,179,237,0.1)' },
  { title: '知识库', value: '--', icon: Collection, color: '#68d391', bg: 'rgba(104,211,145,0.1)' },
  { title: '问答次数', value: '--', icon: ChatDotRound, color: '#fc8181', bg: 'rgba(252,129,129,0.1)' },
  { title: 'Agent 对话', value: '--', icon: MagicStick, color: '#b794f4', bg: 'rgba(183,148,244,0.1)' },
])

const actions = [
  { title: '知识库', desc: '管理文档，上传文件', path: '/documents', icon: Document, color: '#63b3ed' },
  { title: 'RAG 问答', desc: '基于知识库智能问答', path: '/rag', icon: ChatDotRound, color: '#68d391' },
  { title: 'AI Agent', desc: '智能体对话与工具调用', path: '/agent', icon: MagicStick, color: '#b794f4' },
  { title: 'API 文档', desc: 'Swagger 接口文档', path: 'http://localhost:8080/swagger-ui.html', icon: DataBoard, color: '#fc8181' },
]

onMounted(async () => {
  try {
    const { getDocuments } = await import('@/api/document')
    const docRes: any = await getDocuments({ page: 1, pageSize: 1 })
    if (docRes?.data) {
      stats.value[0].value = String(docRes.data.total || 0)
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 24px 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  cursor: default;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
}

.stat-title {
  font-size: 13px;
  color: #718096;
  margin-top: 2px;
}

.action-row {
  margin-bottom: 20px;
}

.card-title {
  font-weight: 600;
  color: #2d3748;
}

.action-card {
  border-radius: 12px;
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-inner {
  padding: 16px 0;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
  margin-top: 12px;
}

.action-desc {
  font-size: 12px;
  color: #a0aec0;
  margin-top: 4px;
}
</style>
