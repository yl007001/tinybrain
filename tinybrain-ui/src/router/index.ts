import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Dashboard from '@/views/Dashboard.vue'
import Documents from '@/views/Documents.vue'
import RAGChat from '@/views/RAGChat.vue'
import AgentChat from '@/views/AgentChat.vue'
import McpManager from '@/views/McpManager.vue'
import SkillManager from '@/views/SkillManager.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '控制台' },
      },
      {
        path: 'documents',
        name: 'Documents',
        component: Documents,
        meta: { title: '知识库' },
      },
      {
        path: 'rag',
        name: 'RAGChat',
        component: RAGChat,
        meta: { title: 'RAG 问答' },
      },
      {
        path: 'agent',
        name: 'AgentChat',
        component: AgentChat,
        meta: { title: 'AI Agent' },
      },
      {
        path: 'mcp',
        name: 'McpManager',
        component: McpManager,
        meta: { title: 'MCP 管理' },
      },
      {
        path: 'skills',
        name: 'SkillManager',
        component: SkillManager,
        meta: { title: 'Skill 管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
