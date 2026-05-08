<template>
  <div class="dashboard-page" v-loading="loading">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header>
            <div class="head">
              <span>身体数据趋势</span>
              <el-select v-model="trendDays" size="small" style="width: 120px" @change="loadAll">
                <el-option label="近30天" :value="30" />
                <el-option label="近90天" :value="90" />
                <el-option label="近180天" :value="180" />
              </el-select>
            </div>
          </template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header><span>完成率（本周/本月）</span></template>
          <div ref="completionRef" class="chart"></div>
          <el-segmented v-model="completionRange" :options="['week', 'month']" @change="loadCompletion" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="mt16">
      <template #header>
        <div class="head">
          <span>打卡热力图（本月）</span>
          <el-date-picker v-model="monthValue" type="month" value-format="YYYY-MM" @change="loadHeatmap" />
        </div>
      </template>
      <div ref="heatRef" class="chart heat"></div>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const loading = ref(false)
const trendDays = ref(90)
const completionRange = ref('week')
const monthValue = ref(new Date().toISOString().slice(0, 7))

const trendRef = ref(null)
const completionRef = ref(null)
const heatRef = ref(null)

let trendChart
let completionChart
let heatChart

async function loadTrend() {
  const rows = await request.get('/dashboard/body-trends', { params: { days: trendDays.value }, silent: true })
  const data = Array.isArray(rows) ? rows : []
  trendChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['体重', 'BMI', '体脂率(估算)'] },
    xAxis: { type: 'category', data: data.map((i) => i.date) },
    yAxis: [{ type: 'value' }, { type: 'value' }],
    series: [
      { name: '体重', type: 'line', smooth: true, data: data.map((i) => i.weight) },
      { name: 'BMI', type: 'line', smooth: true, data: data.map((i) => i.bmi) },
      { name: '体脂率(估算)', type: 'line', smooth: true, yAxisIndex: 1, data: data.map((i) => i.bodyFatRate) },
    ],
  })
}

async function loadCompletion() {
  const data = await request.get('/dashboard/completion-rate', { params: { range: completionRange.value }, silent: true })
  completionChart?.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        data: [
          { name: '已完成', value: data?.completed || 0 },
          { name: '未完成', value: data?.pending || 0 },
          { name: '超额完成', value: data?.overCompleted || 0 },
        ],
      },
    ],
  })
}

async function loadHeatmap() {
  const rows = await request.get('/dashboard/checkin-heatmap', {
    params: { yearMonth: monthValue.value },
    silent: true,
  })
  const data = (Array.isArray(rows) ? rows : []).map((i) => [i.date, i.count])
  heatChart?.setOption({
    tooltip: {},
    visualMap: {
      min: 0,
      max: 4,
      orient: 'horizontal',
      left: 'center',
      top: 0,
      calculable: true,
      inRange: { color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'] },
    },
    calendar: { top: 50, left: 20, right: 20, cellSize: ['auto', 22], range: monthValue.value },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data }],
  })
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadTrend(), loadCompletion(), loadHeatmap()])
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  trendChart = echarts.init(trendRef.value)
  completionChart = echarts.init(completionRef.value)
  heatChart = echarts.init(heatRef.value)
  await loadAll()
  window.addEventListener('resize', resizeCharts)
})

function resizeCharts() {
  trendChart?.resize()
  completionChart?.resize()
  heatChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  completionChart?.dispose()
  heatChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  max-width: 1280px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart {
  height: 320px;
}
.heat {
  height: 220px;
}
.mt16 {
  margin-top: 16px;
}
</style>
