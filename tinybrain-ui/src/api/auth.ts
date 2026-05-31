import http from './index'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  role: string
  token: string
}

export function login(data: LoginRequest) {
  return http.post('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return http.post('/auth/register', data)
}

export function getCurrentUser() {
  return http.get('/auth/me')
}
