<template>
  <el-container class="main-layout" direction="vertical">
    <el-header class="layout-header" height="56px">
      <div class="header-left">
        <div class="logo-mark" aria-hidden="true">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M8 22c0-4 2-8 8-10 6 2 8 6 8 10M12 12c2-2 4-3 4-3s2 1 4 3"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
            <circle cx="16" cy="24" r="3" fill="currentColor" />
          </svg>
        </div>
        <span class="brand-title">个人智能健身管理</span>
        <el-tag v-if="route.meta?.title" type="info" effect="plain" size="small" class="route-tag">
          {{ route.meta.title }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="onUserCommand">
          <span class="user-entry">
            <el-avatar :size="32" class="user-avatar">{{ userInitial }}</el-avatar>
            <span class="user-name">{{ displayName }}</span>
            <el-icon class="caret"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="layout-body">
      <el-aside width="220px" class="layout-aside">
        <el-scrollbar class="aside-scroll">
          <el-menu
            :default-active="activeMenu"
            class="side-menu"
            router
            :collapse="false"
            background-color="var(--layout-aside-bg)"
            text-color="var(--layout-menu-text)"
            active-text-color="var(--layout-menu-active)"
          >
            <el-menu-item index="/home">
              <el-icon><House /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/plan/generate">
              <el-icon><Calendar /></el-icon>
              <span>健身计划</span>
            </el-menu-item>
            <el-menu-item index="/check-in">
              <el-icon><CircleCheck /></el-icon>
              <span>打卡</span>
            </el-menu-item>
            <el-menu-item index="/ai-chat">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI 对话</span>
            </el-menu-item>
            <el-menu-item index="/profile">
              <el-icon><User /></el-icon>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <el-main class="layout-main">
        <div class="main-inner">
          <router-view v-slot="{ Component }">
            <transition name="layout-fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  House,
  Calendar,
  CircleCheck,
  ChatDotRound,
  User,
  ArrowDown,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const displayName = computed(() => {
  const u = userStore.userInfo
  if (!u) return '用户'
  return u.nickname || u.username || '用户'
})

const userInitial = computed(() => {
  const name = displayName.value
  return name ? name.slice(0, 1).toUpperCase() : 'U'
})

function onUserCommand(cmd) {
  if (cmd === 'profile') {
    router.push({ name: 'Profile' })
    return
  }
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出当前账号？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
      .then(() => {
        userStore.logout()
        router.replace({ name: 'Login', query: { redirect: route.fullPath } })
      })
      .catch(() => {})
  }
}

onMounted(() => {
  if (userStore.token) {
    userStore.fetchUserInfo().catch(() => {})
  }
})
</script>

<style scoped lang="scss">
.main-layout {
  min-height: 100vh;
  min-width: 1024px;
  background: var(--layout-page-bg, #f1f5f9);
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 0 rgba(15, 23, 42, 0.04);
  z-index: 20;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.logo-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #16a34a 0%, #22c55e 50%, #4ade80 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  svg {
    width: 22px;
    height: 22px;
  }
}

.brand-title {
  font-weight: 700;
  font-size: 17px;
  color: #0f172a;
  letter-spacing: -0.02em;
  white-space: nowrap;
}

.route-tag {
  margin-left: 4px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  flex-shrink: 0;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  transition: background 0.15s;

  &:hover {
    background: #f1f5f9;
  }
}

.user-avatar {
  background: linear-gradient(135deg, #15803d, #22c55e);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: #334155;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  color: #94a3b8;
  font-size: 12px;
}

.layout-body {
  flex: 1;
  min-height: 0;
}

.layout-aside {
  background: var(--layout-aside-bg, #0f172a);
  border-right: 1px solid rgba(148, 163, 184, 0.12);
}

.aside-scroll {
  height: calc(100vh - 56px);
}

.side-menu {
  border-right: none;
  padding: 12px 8px;

  :deep(.el-menu-item) {
    border-radius: 10px;
    margin-bottom: 4px;
    height: 46px;

    &:hover {
      background: rgba(255, 255, 255, 0.06) !important;
    }

    &.is-active {
      background: rgba(34, 197, 94, 0.18) !important;
      font-weight: 600;
    }
  }
}

.layout-main {
  padding: 0;
  overflow: auto;
  min-height: calc(100vh - 56px);
}

.main-inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 20px 24px 32px;
  min-height: 100%;
}

.layout-fade-enter-active,
.layout-fade-leave-active {
  transition: opacity 0.15s ease;
}

.layout-fade-enter-from,
.layout-fade-leave-to {
  opacity: 0;
}
</style>
