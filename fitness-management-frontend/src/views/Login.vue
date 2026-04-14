<template>
  <div class="login-page">
    <div class="login-shell">
      <header class="login-header">
        <h1 class="title">个人智能健身管理系统</h1>
        <p class="subtitle">登录或注册以继续使用</p>
      </header>

      <el-card class="login-card" shadow="hover">
        <el-tabs v-model="activeTab" class="login-tabs" stretch @tab-change="onTabChange">
          <el-tab-pane label="账号登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              class="form-block"
              @submit.prevent
            >
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  clearable
                  :prefix-icon="User"
                  autocomplete="username"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  show-password
                  :prefix-icon="Lock"
                  autocomplete="current-password"
                />
              </el-form-item>
              <div class="row-between">
                <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
                <el-link type="primary" :underline="false" @click="activeTab = 'register'">
                  没有账号？去注册
                </el-link>
              </div>
              <el-button
                type="primary"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="新用户注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
              class="form-block"
              @submit.prevent
            >
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="3-50 个字符，字母数字下划线或中文"
                  clearable
                  :prefix-icon="User"
                  autocomplete="username"
                />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input
                  v-model="registerForm.phone"
                  placeholder="11 位中国大陆手机号"
                  clearable
                  maxlength="11"
                  :prefix-icon="Iphone"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="6-50 位，需同时包含字母与数字"
                  show-password
                  :prefix-icon="Lock"
                  autocomplete="new-password"
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  show-password
                  :prefix-icon="Lock"
                  autocomplete="new-password"
                />
              </el-form-item>
              <div class="row-between">
                <span />
                <el-link type="primary" :underline="false" @click="activeTab = 'login'">
                  已有账号？去登录
                </el-link>
              </div>
              <el-button
                type="primary"
                class="submit-btn"
                :loading="registerLoading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <footer class="login-footer">© 智能健身 MVP</footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const REMEMBER_KEY = 'fitness_remember_username'
const REMEMBER_FLAG = 'fitness_remember_me'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
})

const registerForm = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

/** 用户名：长度与字符集（唯一性由注册接口返回提示） */
const validateUsername = (_rule, value, callback) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入用户名'))
    return
  }
  const v = value.trim()
  if (v.length < 3 || v.length > 50) {
    callback(new Error('用户名为 3-50 个字符'))
    return
  }
  if (!/^[\w\u4e00-\u9fa5]+$/.test(v)) {
    callback(new Error('仅支持字母、数字、下划线与中文'))
    return
  }
  callback()
}

/** 密码强度：6-50，且同时包含字母与数字 */
const validatePasswordStrength = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 6 || value.length > 50) {
    callback(new Error('密码长度为 6-50 位'))
    return
  }
  const hasLetter = /[a-zA-Z]/.test(value)
  const hasDigit = /\d/.test(value)
  if (!hasLetter || !hasDigit) {
    callback(new Error('密码需同时包含字母与数字'))
    return
  }
  callback()
}

const validatePhone = (_rule, value, callback) => {
  if (!value || !value.trim()) {
    callback(new Error('请输入手机号'))
    return
  }
  if (!/^1[3-9]\d{9}$/.test(value.trim())) {
    callback(new Error('请输入正确的 11 位手机号'))
    return
  }
  callback()
}

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const loginRules = {
  username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [{ required: true, validator: validateUsername, trigger: ['blur', 'change'] }],
  phone: [{ required: true, validator: validatePhone, trigger: ['blur', 'change'] }],
  password: [{ required: true, validator: validatePasswordStrength, trigger: ['blur', 'change'] }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
}

function loadRememberedUser() {
  try {
    const flag = localStorage.getItem(REMEMBER_FLAG) === 'true'
    const name = localStorage.getItem(REMEMBER_KEY)
    if (flag && name) {
      loginForm.username = name
      loginForm.rememberMe = true
    }
  } catch {
    /* ignore */
  }
}

function persistRemember() {
  if (loginForm.rememberMe && loginForm.username.trim()) {
    localStorage.setItem(REMEMBER_FLAG, 'true')
    localStorage.setItem(REMEMBER_KEY, loginForm.username.trim())
  } else {
    localStorage.removeItem(REMEMBER_FLAG)
    localStorage.removeItem(REMEMBER_KEY)
  }
}

function onTabChange() {
  loginFormRef.value?.clearValidate()
  registerFormRef.value?.clearValidate()
}

async function handleLogin() {
  const form = loginFormRef.value
  if (!form) return
  try {
    await form.validate()
  } catch {
    return
  }
  loginLoading.value = true
  try {
    await userStore.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
    })
    persistRemember()
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    await router.replace(redirect || '/home')
  } catch (e) {
    const msg = e?.message || '登录失败，请检查账号密码'
    if (!e?.response) {
      ElMessage.error(msg)
    }
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  const form = registerFormRef.value
  if (!form) return
  try {
    await form.validate()
  } catch {
    return
  }
  registerLoading.value = true
  try {
    await userStore.register({
      username: registerForm.username.trim(),
      phone: registerForm.phone.trim(),
      password: registerForm.password,
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username.trim()
    loginForm.password = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.phone = ''
    registerFormRef.value?.resetFields()
  } catch {
    /* 业务/HTTP 错误已由 axios 拦截器统一提示（含用户名/手机号已存在等） */
  } finally {
    registerLoading.value = false
  }
}

onMounted(() => {
  loadRememberedUser()
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: linear-gradient(145deg, #f0f4f8 0%, #e2e8f0 45%, #f8fafc 100%);
}

.login-shell {
  width: 100%;
  max-width: 440px;
}

.login-header {
  text-align: center;
  margin-bottom: 20px;

  .title {
    margin: 0 0 8px;
    font-size: 1.5rem;
    font-weight: 600;
    color: #1e293b;
    letter-spacing: 0.02em;
  }

  .subtitle {
    margin: 0;
    font-size: 0.9rem;
    color: #64748b;
  }
}

.login-card {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.35);

  :deep(.el-card__body) {
    padding: 8px 20px 24px;
  }
}

.login-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
  }
}

.form-block {
  padding-top: 4px;
}

.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  margin-top: 4px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 480px) {
  .login-header .title {
    font-size: 1.25rem;
  }
}
</style>
