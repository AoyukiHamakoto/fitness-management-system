import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 路由：登录页独立；业务页统一挂在 MainLayout 下（顶栏 + 侧栏 + 主区）。
 * meta.requiresAuth === false 仅登录页
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/Home.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'plan/generate',
          name: 'PlanGenerate',
          component: () => import('@/views/PlanGenerate.vue'),
          meta: { title: '健身计划' },
        },
        {
          path: 'check-in',
          name: 'CheckIn',
          component: () => import('@/views/CheckIn.vue'),
          meta: { title: '每日打卡' },
        },
        {
          path: 'ai-chat',
          name: 'AiChat',
          component: () => import('@/views/AIChat.vue'),
          meta: { title: 'AI 对话' },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { title: '个人中心' },
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '数据看板' },
        },
        {
          path: 'exercise-library',
          name: 'ExerciseLibrary',
          component: () => import('@/views/ExerciseLibrary.vue'),
          meta: { title: '训练动作库' },
        },
        {
          path: 'leaderboard',
          name: 'Leaderboard',
          component: () => import('@/views/Leaderboard.vue'),
          meta: { title: '运动排行榜' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/home',
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const needAuth = to.meta.requiresAuth !== false
  if (needAuth && !userStore.token) {
    next({
      name: 'Login',
      query: { redirect: to.fullPath },
    })
    return
  }

  if (to.name === 'Login' && userStore.token) {
    next({ path: '/home' })
    return
  }

  const titleRoute = to.matched.find((r) => r.meta?.title)
  const title = titleRoute?.meta?.title
  if (title) {
    document.title = `${title} - 智能健身`
  }
  next()
})

export default router
