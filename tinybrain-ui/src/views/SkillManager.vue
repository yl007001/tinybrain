<template>
  <div class="skill-manager">
    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建 Skill
      </el-button>
      <el-button @click="showDistillDialog">
        <el-icon><MagicStick /></el-icon>
        蒸馏 Skill
      </el-button>
      <el-button @click="showMarketDialog">
        <el-icon><Shop /></el-icon>
        Skill 市场
      </el-button>
      <el-button @click="refreshSkills">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- Skill 列表 -->
    <el-table :data="skills" style="width: 100%" v-loading="loading">
      <el-table-column prop="name" label="Skill 名称" width="150" />
      <el-table-column prop="description" label="描述" min-width="250" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type)">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.source }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="toggleSkill(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="editSkill(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteSkill(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建 Skill 对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建 Skill" width="600px">
      <el-form :model="skillForm" label-width="120px">
        <el-form-item label="Skill 名称" required>
          <el-input v-model="skillForm.name" placeholder="例如：网页抓取助手" />
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="skillForm.description" type="textarea" :rows="3" placeholder="描述这个 Skill 的功能" />
        </el-form-item>
        <el-form-item label="工具名称" required>
          <el-select v-model="skillForm.toolName" style="width: 100%">
            <el-option v-for="tool in availableTools" :key="tool.name" :label="tool.name" :value="tool.name">
              <span>{{ tool.name }}</span>
              <span style="color: #999; margin-left: 10px;">{{ tool.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="触发条件">
          <el-input v-model="skillForm.triggersStr" placeholder="多个条件用逗号分隔" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="skillForm.tagsStr" placeholder="多个标签用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createSkill" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 蒸馏 Skill 对话框 -->
    <el-dialog v-model="distillDialogVisible" title="蒸馏 Skill" width="600px">
      <el-form :model="distillForm" label-width="120px">
        <el-form-item label="蒸馏来源" required>
          <el-select v-model="distillForm.sourceType" style="width: 100%">
            <el-option label="从对话历史蒸馏" value="conversation" />
            <el-option label="从文档内容蒸馏" value="document" />
            <el-option label="从代码片段蒸馏" value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源内容" required>
          <el-input v-model="distillForm.sourceContent" type="textarea" :rows="6" placeholder="粘贴对话历史、文档内容或代码片段" />
        </el-form-item>
        <el-form-item label="Skill 名称" required>
          <el-input v-model="distillForm.skillName" placeholder="给蒸馏出的 Skill 起个名字" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="distillForm.skillDescription" placeholder="Skill 功能描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="distillDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="distillSkill" :loading="distilling">开始蒸馏</el-button>
      </template>
    </el-dialog>

    <!-- Skill 市场对话框 -->
    <el-dialog v-model="marketDialogVisible" title="Skill 市场" width="800px">
      <div class="market-list">
        <el-card v-for="skill in marketSkills" :key="skill.id" class="market-card">
          <template #header>
            <div class="market-card-header">
              <span>{{ skill.name }}</span>
              <el-tag size="small">v{{ skill.version }}</el-tag>
            </div>
          </template>
          <p class="market-desc">{{ skill.description }}</p>
          <div class="market-meta">
            <span>下载: {{ skill.downloads }}</span>
            <span>评分: {{ skill.rating?.toFixed(1) }}</span>
            <span>作者: {{ skill.author }}</span>
          </div>
          <el-button type="primary" size="small" @click="installSkill(skill)">安装</el-button>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, MagicStick, Shop } from '@element-plus/icons-vue'
import http from '@/api/index'

interface Skill {
  id: string
  name: string
  description: string
  type: string
  source: string
  enabled: boolean
  version?: string
  author?: string
  downloads?: number
  rating?: number
}

const skills = ref<Skill[]>([])
const marketSkills = ref<Skill[]>([])
const availableTools = ref<any[]>([])
const loading = ref(false)
const creating = ref(false)
const distilling = ref(false)
const createDialogVisible = ref(false)
const distillDialogVisible = ref(false)
const marketDialogVisible = ref(false)

const skillForm = ref({
  name: '',
  description: '',
  toolName: '',
  triggersStr: '',
  tagsStr: ''
})

const distillForm = ref({
  sourceType: 'conversation',
  sourceContent: '',
  skillName: '',
  skillDescription: ''
})

// 获取 Skill 列表
async function refreshSkills() {
  loading.value = true
  try {
    const res: any = await http.get('/skills/list')
    if (res?.data) {
      skills.value = res.data
    }
  } catch (error) {
    console.error('获取 Skill 列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取可用工具
async function refreshTools() {
  try {
    const res: any = await http.get('/agent/tools')
    if (res?.data) {
      availableTools.value = Object.entries(res.data).map(([name, desc]) => ({
        name,
        description: desc
      }))
    }
  } catch (error) {
    console.error('获取工具列表失败:', error)
  }
}

// 显示创建对话框
function showCreateDialog() {
  skillForm.value = {
    name: '',
    description: '',
    toolName: '',
    triggersStr: '',
    tagsStr: ''
  }
  createDialogVisible.value = true
}

// 显示蒸馏对话框
function showDistillDialog() {
  distillForm.value = {
    sourceType: 'conversation',
    sourceContent: '',
    skillName: '',
    skillDescription: ''
  }
  distillDialogVisible.value = true
}

// 显示市场对话框
async function showMarketDialog() {
  try {
    const res: any = await http.get('/skills/market')
    if (res?.data) {
      marketSkills.value = res.data
    }
  } catch (error) {
    console.error('获取市场列表失败:', error)
  }
  marketDialogVisible.value = true
}

// 创建 Skill
async function createSkill() {
  if (!skillForm.value.name || !skillForm.value.description || !skillForm.value.toolName) {
    ElMessage.warning('请填写必填项')
    return
  }

  creating.value = true
  try {
    await http.post('/skills/create', {
      name: skillForm.value.name,
      description: skillForm.value.description,
      toolName: skillForm.value.toolName,
      triggers: skillForm.value.triggersStr ? skillForm.value.triggersStr.split(',').map(s => s.trim()) : [],
      tags: skillForm.value.tagsStr ? skillForm.value.tagsStr.split(',').map(s => s.trim()) : []
    })
    ElMessage.success('Skill 创建成功')
    createDialogVisible.value = false
    refreshSkills()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 蒸馏 Skill
async function distillSkill() {
  if (!distillForm.value.sourceContent || !distillForm.value.skillName) {
    ElMessage.warning('请填写必填项')
    return
  }

  distilling.value = true
  try {
    await http.post('/skills/distill', {
      sourceType: distillForm.value.sourceType,
      sourceContent: distillForm.value.sourceContent,
      skillName: distillForm.value.skillName,
      skillDescription: distillForm.value.skillDescription
    })
    ElMessage.success('Skill 蒸馏成功')
    distillDialogVisible.value = false
    refreshSkills()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '蒸馏失败')
  } finally {
    distilling.value = false
  }
}

// 切换 Skill 状态
async function toggleSkill(skill: Skill) {
  try {
    await http.post(`/skills/${skill.id}/toggle`)
    ElMessage.success(`Skill 已${skill.enabled ? '启用' : '禁用'}`)
  } catch (error: any) {
    ElMessage.error('操作失败')
    skill.enabled = !skill.enabled
  }
}

// 编辑 Skill
function editSkill(skill: Skill) {
  ElMessage.info('编辑功能开发中...')
}

// 删除 Skill
async function deleteSkill(skill: Skill) {
  try {
    await ElMessageBox.confirm(`确定要删除 Skill "${skill.name}" 吗？`, '确认删除', {
      type: 'warning'
    })
    await http.delete(`/skills/${skill.id}`)
    ElMessage.success('删除成功')
    refreshSkills()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 安装 Skill
async function installSkill(skill: Skill) {
  try {
    await http.post('/skills/install', { skillId: skill.id })
    ElMessage.success('安装成功')
    refreshSkills()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '安装失败')
  }
}

// 获取类型标签类型
function getTypeTagType(type: string) {
  switch (type) {
    case 'builtin': return 'success'
    case 'custom': return 'primary'
    case 'market': return 'warning'
    case 'installed': return 'info'
    default: return 'info'
  }
}

onMounted(() => {
  refreshSkills()
  refreshTools()
})
</script>

<style scoped>
.skill-manager {
  padding: 20px;
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.market-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.market-card {
  cursor: pointer;
}

.market-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.market-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.market-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}
</style>
