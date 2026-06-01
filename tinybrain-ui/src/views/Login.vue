<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-brand">
        <div class="brand-mark">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="10" fill="var(--color-accent)"/>
            <path d="M12 13h16M12 20h10M12 27h14" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
        </div>
        <h1>TinyBrain</h1>
        <p>个人 AI 知识引擎</p>
      </div>
      <div class="login-features">
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>RAG 检索增强生成</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>Agent 智能体工具调用</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>文档知识库管理</span>
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="login-form-wrapper">
        <div class="form-header">
          <h2>{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ isLogin ? '登录以继续使用' : '注册开始使用 TinyBrain' }}</p>
        </div>

        <!-- Login Form -->
        <form v-if="isLogin" class="login-form" @submit.prevent="handleLogin">
          <div class="form-group">
            <label>用户名</label>
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              autocomplete="username"
            />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </div>
          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <p class="form-switch">
            还没有账号？<a href="#" @click.prevent="isLogin = false">立即注册</a>
          </p>
        </form>

        <!-- Register Form -->
        <form v-else class="login-form" @submit.prevent="handleRegister">
          <div class="form-group">
            <label>用户名</label>
            <input
              v-model="registerForm.username"
              type="text"
              placeholder="字母、数字、下划线"
              autocomplete="username"
            />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input
              v-model="registerForm.password"
              type="password"
              placeholder="至少 6 位"
              autocomplete="new-password"
            />
          </div>
          <div class="form-group">
            <label>确认密码</label>
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="再次输入密码"
              autocomplete="new-password"
            />
          </div>
          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>
          <p class="form-switch">
            已有账号？<a href="#" @click.prevent="isLogin = true">去登录</a>
          </p>
        </form>

        <p class="form-hint">演示账号: demo / password</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'

const router = useRouter()
const isLogin = ref(true)
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '' })

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await login(loginForm)
    if (res.data) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('username', res.data.user?.username || loginForm.username)
      localStorage.setItem('user', JSON.stringify(res.data.user || {}))
      ElMessage.success('登录成功')
      router.push('/')
    }
  } catch (e) { /* handled */ } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写所有字段')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password
    })
    ElMessage.success('注册成功，请登录')
    isLogin.value = true
    loginForm.username = registerForm.username
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
  } catch (e) { /* handled */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}

.login-left {
  flex: 1;
  background: var(--color-accent);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  color: white;
}

.login-brand h1 {
  font-size: 32px;
  font-weight: 700;
  margin: 16px 0 8px;
  letter-spacing: -0.02em;
}

.login-brand p {
  font-size: 16px;
  opacity: 0.8;
}

.login-features {
  margin-top: 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: var(--color-bg-elevated);
}

.login-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.form-header p {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.form-group input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg-elevated);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  outline: none;
  font-family: var(--font-sans);
}

.form-group input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-group input::placeholder {
  color: var(--color-text-tertiary);
}

.btn-primary {
  height: 40px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
  font-family: var(--font-sans);
}

.btn-primary:hover {
  background: var(--color-accent-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-switch {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.form-switch a {
  color: var(--color-accent);
  font-weight: 500;
}

.form-hint {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 768px) {
  .login-left {
    display: none;
  }
  .login-right {
    padding: 24px;
  }
}
</style>
