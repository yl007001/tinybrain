import http from './index'

export interface SkillConfig {
  name: string
  description: string
  type?: string
  toolName: string
  toolDescription?: string
  parametersSchema?: string
  triggers?: string[]
  config?: Record<string, any>
  enabled?: boolean
  priority?: number
  tags?: string[]
}

export interface SkillInfo {
  id: string
  name: string
  description: string
  type: string
  toolName: string
  toolDescription: string
  parametersSchema?: string
  triggers?: string[]
  config?: Record<string, any>
  enabled: boolean
  priority: number
  tags?: string[]
  source: string
  version?: string
  author?: string
  downloads?: number
  rating?: number
  createdAt: string
  updatedAt: string
}

export interface SkillDistillRequest {
  sourceType: string
  sourceId?: string
  sourceContent?: string
  skillName: string
  skillDescription?: string
  triggerHints?: string[]
  autoGenerateSchema?: boolean
}

// 获取所有 Skill
export function getSkills() {
  return http.get('/skills/list')
}

// 获取 Skill 详情
export function getSkill(id: string) {
  return http.get(`/skills/${id}`)
}

// 创建 Skill
export function createSkill(data: SkillConfig) {
  return http.post('/skills/create', data)
}

// 更新 Skill
export function updateSkill(id: string, data: SkillConfig) {
  return http.put(`/skills/${id}`, data)
}

// 删除 Skill
export function deleteSkill(id: string) {
  return http.delete(`/skills/${id}`)
}

// 启用/禁用 Skill
export function toggleSkill(id: string) {
  return http.post(`/skills/${id}/toggle`)
}

// 蒸馏 Skill
export function distillSkill(data: SkillDistillRequest) {
  return http.post('/skills/distill', data)
}

// 安装 Skill
export function installSkill(skillId: string) {
  return http.post('/skills/install', { skillId })
}

// 获取 Skill 市场列表
export function getMarketSkills() {
  return http.get('/skills/market')
}
