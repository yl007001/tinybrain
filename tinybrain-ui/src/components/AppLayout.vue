<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="var(--color-accent)"/>
            <path d="M7 8h10M7 12h6M7 16h8" stroke="white" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <span class="brand-name">TinyBrain</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card" @click="showUserMenu = !showUserMenu">
          <div class="user-avatar">{{ username.charAt(0).toUpperCase() }}</div>
          <div class="user-meta">
            <span class="user-name">{{ username }}</span>
            <span class="user-role">{{ userRole }}</span>
          </div>
        </div>
        <div v-if="showUserMenu" class="user-menu">
          <button class="menu-item" @click="handleLogout">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M6 14H3a1 1 0 01-1-1V3a1 1 0 011-1h3M11 11l3-3-3-3M14 8H6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </aside>

    <!-- Main -->
    <main class="main-content">
      <header class="main-header">
        <div class="header-title">
          <h1>{{ currentTitle }}</h1>
          <span v-if="currentDesc" class="header-desc">{{ currentDesc }}</span>
        </div>
      </header>
      <div class="main-body">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const showUserMenu = ref(false)
const username = ref(localStorage.getItem('username') || 'User')
const userRole = computed(() => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    return user.role === 'ROLE_ADMIN' ? '管理员' : '用户'
  } catch { return '用户' }
})

const navItems = [
  {
    path: '/',
    label: '控制台',
    icon: '<svg width="18" height="18" viewBox="0 0 18 18" fill="none"><rect x="1" y="1" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/><rect x="10" y="1" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/><rect x="1" y="10" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/><rect x="10" y="10" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    path: '/documents',
    label: '知识库',
    icon: '<svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 2h9l3 3v11a1 1 0 01-1 1H3a1 1 0 01-1-1V3a1 1 0 011-1z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M6 10h6M6 13h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  },
  {
    path: '/rag',
    label: 'RAG 问答',
    icon: '<svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 14l2-2m0 0l4-4m-4 4l-2-2m2 2l4 4m-4-4l2-2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="13" cy="5" r="3" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    path: '/agent',
    label: 'AI Agent',
    icon: '<svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 1v2M9 15v2M1 9h2M15 9h2M3.3 3.3l1.4 1.4M13.3 13.3l1.4 1.4M3.3 14.7l1.4-1.4M13.3 4.7l1.4-1.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="9" cy="9" r="3" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    path: '/mcp',
    label: 'MCP 管理',
    icon: '<svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 3h12v12H3z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M3 7h12M7 7v8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  },
  {
    path: '/skills',
    label: 'Skill 管理',
    icon: '<svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 1l2.5 5 5.5.8-4 3.9.9 5.3L9 13.5 4.1 16l.9-5.3-4-3.9 5.5-.8L9 1z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  }
]

const currentTitle = computed(() => route.meta?.title as string || '控制台')
const currentDesc = computed(() => route.meta?.description as string)

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('user')
  showUserMenu.value = false
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ======== Sidebar ======== */
.sidebar {
  width: var(--sidebar-width);
  background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--color-border-subtle);
}

.brand-icon {
  display: flex;
  align-items: center;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
}

.sidebar-nav {
  flex: 1;
  padding: var(--space-2) var(--space-3);
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.12s ease;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: var(--color-accent-light);
  color: var(--color-accent);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

/* ======== Sidebar Footer ======== */
.sidebar-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--color-border-subtle);
  position: relative;
}

.user-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.12s ease;
}

.user-card:hover {
  background: var(--color-bg-hover);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.user-menu {
  position: absolute;
  bottom: 100%;
  left: var(--space-3);
  right: var(--space-3);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: var(--space-1);
  margin-bottom: var(--space-2);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.12s ease;
}

.menu-item:hover {
  background: var(--color-bg-hover);
  color: var(--color-error);
}

/* ======== Main Content ======== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg);
}

.main-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 var(--space-6);
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-elevated);
  flex-shrink: 0;
}

.header-title h1 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-desc {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-left: var(--space-2);
}

.main-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
}
</style>
