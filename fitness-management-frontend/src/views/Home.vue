<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-text">
        <p class="hero-kicker">欢迎回来</p>
        <h1 class="hero-title">{{ greeting }}，{{ displayName }}</h1>
        <p class="hero-desc">
          一站式管理训练计划、每日打卡与 AI 教练对话，让每一次训练都有据可依。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" round @click="goPlan">
          <el-icon class="btn-icon"><Calendar /></el-icon>
          生成 / 查看计划
        </el-button>
        <el-button size="large" round @click="goCheckIn">
          <el-icon class="btn-icon"><CircleCheck /></el-icon>
          今日打卡
        </el-button>
        <el-button size="large" round plain type="success" @click="goAiChat">
          <el-icon class="btn-icon"><ChatDotRound /></el-icon>
          问 AI 教练
        </el-button>
      </div>
    </section>

    <el-row :gutter="20" class="stats-row" v-loading="loading">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon streak">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">连续打卡</div>
            <div class="stat-value">
              {{ streakDays }}<span class="unit">天</span>
            </div>
            <div class="stat-hint">服务端统计，坚持就是胜利</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon tasks">
            <el-icon><List /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">今日训练项</div>
            <div class="stat-value">
              {{ todayDone }}<span class="slash">/</span>{{ todayTotal }}
            </div>
            <div class="stat-hint">已完成 / 今日任务总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon plan">
            <el-icon><Notebook /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">进行中计划</div>
            <div class="stat-value text-ellipsis" :title="planTitle">{{ planTitle }}</div>
            <div class="stat-hint">{{ planHint }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon ai">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">智能助手</div>
            <div class="stat-value sm">结合计划与打卡上下文</div>
            <div class="stat-hint">限流保护 · 流式回复更自然</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <section class="section">
      <h2 class="section-title">核心功能</h2>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :lg="6" v-for="item in featureEntries" :key="item.path">
          <div class="feature-card" @click="router.push(item.path)">
            <div class="feature-icon" :class="item.tone">
              <el-icon :size="28"><component :is="item.icon" /></el-icon>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
            <span class="feature-link">进入 <el-icon><Right /></el-icon></span>
          </div>
        </el-col>
      </el-row>
    </section>

    <section class="section quick">
      <h2 class="section-title">快捷操作</h2>
      <div class="quick-bar">
        <el-button type="primary" plain round @click="goPlan">管理健身计划</el-button>
        <el-button type="success" plain round @click="goCheckIn">打开打卡页</el-button>
        <el-button type="warning" plain round @click="goAiChat">AI 对话</el-button>
        <el-button round @click="router.push({ name: 'Profile' })">账号与资料</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  CircleCheck,
  ChatDotRound,
  TrendCharts,
  List,
  Notebook,
  Cpu,
  Right,
  User,
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const streakDays = ref(0)
const todayTotal = ref(0)
const todayDone = ref(0)
const planTitle = ref('—')
const planHint = ref('加载中…')

const displayName = computed(() => {
  const u = userStore.userInfo
  if (!u) return '健身伙伴'
  return u.nickname || u.username || '健身伙伴'
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const featureEntries = [
  {
    title: '健身计划',
    desc: '基于身体数据与目标生成个性化周期计划',
    path: '/plan/generate',
    icon: Calendar,
    tone: 'green',
  },
  {
    title: '每日打卡',
    desc: '记录训练执行与完成度，形成连续打卡 streak',
    path: '/check-in',
    icon: CircleCheck,
    tone: 'teal',
  },
  {
    title: 'AI 对话',
    desc: '流式问答，结合当前计划与今日任务给建议',
    path: '/ai-chat',
    icon: ChatDotRound,
    tone: 'violet',
  },
  {
    title: '个人中心',
    desc: '查看账号信息，安全退出与资料维护',
    path: '/profile',
    icon: User,
    tone: 'slate',
  },
]

function goPlan() {
  router.push({ name: 'PlanGenerate' })
}

function goCheckIn() {
  router.push({ name: 'CheckIn' })
}

function goAiChat() {
  router.push({ name: 'AiChat' })
}

async function loadOverview() {
  loading.value = true
  try {
    const [streakRes, tasksRes, planRes] = await Promise.allSettled([
      request.get('/check-ins/streak'),
      request.get('/check-ins/today-tasks', { silent: true }),
      request.get('/plans/current', { silent: true }),
    ])

    if (streakRes.status === 'fulfilled' && streakRes.value != null) {
      streakDays.value = Number(streakRes.value) || 0
    } else {
      streakDays.value = 0
    }

    if (tasksRes.status === 'fulfilled' && Array.isArray(tasksRes.value)) {
      const list = tasksRes.value
      todayTotal.value = list.length
      todayDone.value = list.filter((t) => t.checkedIn).length
    } else {
      todayTotal.value = 0
      todayDone.value = 0
    }

    if (planRes.status === 'fulfilled' && planRes.value?.planName) {
      planTitle.value = planRes.value.planName
      planHint.value = `${planRes.value.startDate || '—'} ~ ${planRes.value.endDate || '—'}`
    } else {
      planTitle.value = '暂无'
      planHint.value =
        planRes.status === 'rejected'
          ? '计划信息暂时不可用'
          : '可前往「健身计划」生成新方案'
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped lang="scss">
.home-page {
  max-width: 1200px;
  margin: 0 auto;
}

.hero {
  background: linear-gradient(125deg, #ecfdf5 0%, #f0fdf4 35%, #f8fafc 100%);
  border: 1px solid #d1fae5;
  border-radius: 16px;
  padding: 28px 32px 32px;
  margin-bottom: 24px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.06);
}

.hero-kicker {
  margin: 0 0 6px;
  font-size: 13px;
  color: #15803d;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0 0 12px;
  font-size: clamp(22px, 2.4vw, 28px);
  font-weight: 700;
  color: #0f172a;
  line-height: 1.25;
}

.hero-desc {
  margin: 0 0 22px;
  max-width: 560px;
  font-size: 15px;
  line-height: 1.65;
  color: #475569;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.btn-icon {
  margin-right: 6px;
  vertical-align: middle;
}

.stats-row {
  margin-bottom: 28px;
}

.stat-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 20px;
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;

  &.streak {
    background: #dcfce7;
    color: #166534;
  }
  &.tasks {
    background: #cffafe;
    color: #0e7490;
  }
  &.plan {
    background: #e0e7ff;
    color: #4338ca;
  }
  &.ai {
    background: #ede9fe;
    color: #6d28d9;
  }
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;

  &.sm {
    font-size: 15px;
    font-weight: 600;
    line-height: 1.4;
  }
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.unit {
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  margin-left: 2px;
}

.slash {
  margin: 0 2px;
  color: #94a3b8;
  font-weight: 500;
}

.stat-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.section {
  margin-bottom: 28px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.feature-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 22px 20px;
  height: 100%;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.15s;

  &:hover {
    border-color: #86efac;
    box-shadow: 0 10px 30px rgba(22, 163, 74, 0.12);
    transform: translateY(-2px);
  }

  h3 {
    margin: 14px 0 8px;
    font-size: 16px;
    color: #0f172a;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.55;
    color: #64748b;
    min-height: 40px;
  }
}

.feature-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;

  &.green {
    background: linear-gradient(135deg, #22c55e, #4ade80);
    color: #fff;
  }
  &.teal {
    background: linear-gradient(135deg, #14b8a6, #2dd4bf);
    color: #fff;
  }
  &.violet {
    background: linear-gradient(135deg, #8b5cf6, #a78bfa);
    color: #fff;
  }
  &.slate {
    background: linear-gradient(135deg, #475569, #64748b);
    color: #fff;
  }
}

.feature-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 14px;
  font-size: 13px;
  font-weight: 600;
  color: #16a34a;
}

.quick-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

@media (max-width: 1200px) {
  .hero {
    padding: 22px 20px;
  }
}
</style>
