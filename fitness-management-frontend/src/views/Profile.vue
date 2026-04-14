<template>
  <div class="profile-page">
    <el-card shadow="hover" class="profile-card" v-loading="loading">
      <template #header>
        <div class="card-head">
          <span>个人中心</span>
          <el-button type="primary" link @click="refresh">刷新资料</el-button>
        </div>
      </template>

      <el-descriptions v-if="user" :column="1" border size="large" class="desc">
        <el-descriptions-item label="用户 ID">{{ user.id ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user.username ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user.nickname || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">
            {{ user.status === 1 ? '正常' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无用户信息，请重新登录" />

      <div class="actions">
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)

const user = computed(() => userStore.userInfo)

async function refresh() {
  loading.value = true
  try {
    await userStore.fetchUserInfo()
  } finally {
    loading.value = false
  }
}

function logout() {
  ElMessageBox.confirm('确定退出当前账号？', '退出登录', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消',
  })
    .then(() => {
      userStore.logout()
      router.replace({ name: 'Login' })
    })
    .catch(() => {})
}

onMounted(() => {
  refresh()
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 640px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.desc {
  margin-top: 8px;
}

.actions {
  margin-top: 24px;
}
</style>
