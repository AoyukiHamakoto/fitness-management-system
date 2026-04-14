import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

const TOKEN_KEY = 'fitness_token'
const USER_KEY = 'fitness_user'

/**
 * 用户状态：Token、资料；与 localStorage 同步便于刷新后恢复
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(safeParseUser(localStorage.getItem(USER_KEY)))

  const isLoggedIn = computed(() => Boolean(token.value))

  function setToken(value) {
    token.value = value || ''
    if (value) {
      localStorage.setItem(TOKEN_KEY, value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  function setUserInfo(info) {
    userInfo.value = info
    if (info) {
      localStorage.setItem(USER_KEY, JSON.stringify(info))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  /**
   * 注册：POST /users/register（成功后需自行登录或跳转登录 Tab）
   */
  async function register(payload) {
    await request.post('/users/register', payload)
  }

  /**
   * 登录：POST /users/login，写入 token 与用户信息
   */
  async function login(payload) {
    const data = await request.post('/users/login', payload)
    const accessToken = data?.token
    if (!accessToken) {
      throw new Error('登录响应缺少 token')
    }
    setToken(accessToken)
    setUserInfo(data.user || null)
    return data
  }

  /**
   * 退出：清空本地状态（后端若有无状态 JWT 可不调用接口）
   */
  function logout() {
    setToken('')
    setUserInfo(null)
  }

  /**
   * 获取当前用户资料：GET /users/me
   */
  async function fetchUserInfo() {
    const data = await request.get('/users/me')
    setUserInfo(data)
    return data
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    register,
    login,
    logout,
    fetchUserInfo,
  }
})

function safeParseUser(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}
