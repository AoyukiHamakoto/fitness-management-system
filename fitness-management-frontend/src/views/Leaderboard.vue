<template>
  <div class="rank-page">
    <el-card shadow="hover" v-loading="loading">
      <template #header>
        <div class="head">
          <span>运动排行榜</span>
          <el-segmented v-model="range" :options="['week', 'month']" @change="loadData" />
        </div>
      </template>
      <el-table :data="rows" stripe border>
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column prop="nickname" label="昵称" min-width="180" />
        <el-table-column prop="value" :label="range === 'week' ? '本周打卡次数' : '本月打卡次数'" min-width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const range = ref('week')
const rows = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    rows.value = (await request.get('/dashboard/leaderboard', { params: { range: range.value }, silent: true })) || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.rank-page {
  max-width: 960px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
