import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * Axios 实例：对接后端统一前缀与 Result 包装
 * 基础地址：http://localhost:8080/fitness-api
 */
const service = axios.create({
  baseURL: 'http://localhost:8080/fitness-api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 请求拦截：附加 JWT */
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

/** 响应拦截：Result { code, msg, data }；HTTP 401/500 与业务码处理；config.silent 时不弹业务错误提示 */
service.interceptors.response.use(
  (response) => {
    const silent = Boolean(response.config?.silent)
    const payload = response.data
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code === 200) {
        return payload.data
      }
      if (!silent) {
        ElMessage.error(payload.msg || '请求失败')
      }
      return Promise.reject(new Error(payload.msg || 'Error'))
    }
    return payload
  },
  (error) => {
    const silent = Boolean(error.config?.silent)
    const status = error.response?.status
    const body = error.response?.data

    if (status === 401) {
      if (!silent) {
        ElMessage.error(body?.msg || '登录已过期，请重新登录')
      }
      const userStore = useUserStore()
      userStore.logout()
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if (status === 500) {
      if (!silent) {
        ElMessage.error(body?.msg || '服务器内部错误')
      }
    } else if (body?.msg) {
      if (!silent) {
        ElMessage.error(body.msg)
      }
    } else if (!silent) {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

export default service
