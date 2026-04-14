<template>
  <div class="checkin-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">累计打卡天数</div>
          <div class="stat-value">{{ totalPunchDays }}</div>
          <div class="stat-hint">以本设备记录的成功打卡日为准</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card accent">
          <div class="stat-label">连续打卡</div>
          <div class="stat-value">{{ streakDays }}<span class="unit">天</span></div>
          <div class="stat-hint">服务端 Redis 统计</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">计划整体完成率</div>
          <div class="stat-value">{{ planProgress }}<span class="unit">%</span></div>
          <div class="stat-hint">末次打卡接口返回，无记录时显示今日项进度</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 今日任务 -->
      <el-col :xs="24" :lg="14">
        <el-card v-loading="loadingTasks" shadow="hover" class="panel">
          <template #header>
            <div class="panel-head">
              <span>今日训练任务</span>
              <el-button type="primary" link @click="refreshAll">刷新</el-button>
            </div>
          </template>

          <el-empty v-if="!loadingTasks && todayTasks.length === 0" description="今日无训练任务或暂无进行中计划" />

          <div v-else class="task-list">
            <div v-for="task in todayTasks" :key="task.planDetailId" class="task-card">
              <div class="task-head">
                <div>
                  <strong>{{ task.exerciseName }}</strong>
                  <el-tag v-if="task.checkedIn" type="success" size="small" class="ml">已打卡</el-tag>
                  <el-tag size="small" type="info" class="ml">第 {{ task.dayIndex }} 天</el-tag>
                </div>
                <span class="muted">建议时长 {{ task.durationMinutes ?? '—' }} 分钟</span>
              </div>
              <el-descriptions :column="3" size="small" border class="target-desc">
                <el-descriptions-item label="目标组数">{{ targetSets(task) }}</el-descriptions-item>
                <el-descriptions-item label="目标次数">{{ targetReps(task) }}</el-descriptions-item>
                <el-descriptions-item label="目标重量">{{ targetWeight(task) }}</el-descriptions-item>
              </el-descriptions>

              <template v-if="!task.checkedIn">
                <el-form :ref="(el) => setFormRef(task.planDetailId, el)" :model="getForm(task)" :rules="rowRules" class="row-form">
                  <el-row :gutter="12">
                    <el-col :span="8">
                      <el-form-item label="实际组数" prop="actualSets">
                        <el-input-number v-model="getForm(task).actualSets" :min="1" :max="500" controls-position="right" class="w-full" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="实际重量(kg)" prop="actualWeight">
                        <el-input-number v-model="getForm(task).actualWeight" :min="0" :max="500" :precision="1" controls-position="right" class="w-full" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8" class="btn-col">
                      <el-button
                        type="primary"
                        :loading="submittingId === task.planDetailId"
                        @click="submitRow(task)"
                      >
                        提交打卡
                      </el-button>
                    </el-col>
                  </el-row>
                </el-form>
              </template>
              <template v-else>
                <el-alert type="success" :closable="false" show-icon>今日该项已完成打卡</el-alert>
              </template>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 日历 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="hover" class="panel calendar-panel">
          <template #header>
            <span>本月打卡日历</span>
          </template>
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div
                class="cal-cell"
                :class="{ 'is-punched': isPunchedDate(data.day), 'is-selected': selectedCalDay === data.day }"
                @click.stop="onPickDay(data.day)"
              >
                <span class="cal-day">{{ cellDayNum(data.day) }}</span>
                <span v-if="isPunchedDate(data.day)" class="dot" />
              </div>
            </template>
          </el-calendar>
          <p class="cal-legend"><span class="dot inline" /> 已打卡日期（本页记录）</p>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dayDialogVisible" :title="`打卡记录 · ${selectedCalDay || ''}`" width="480px" destroy-on-close>
      <template v-if="selectedRecords.length">
        <el-timeline>
          <el-timeline-item v-for="(r, i) in selectedRecords" :key="i" :timestamp="r.time || ''" placement="top">
            <p><strong>{{ r.exerciseName }}</strong></p>
            <p class="muted">组数 {{ r.actualSets }} · 重量 {{ r.actualWeight }} kg</p>
          </el-timeline-item>
        </el-timeline>
      </template>
      <el-empty v-else description="当日无本地打卡记录（或未在本设备打卡）" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loadingTasks = ref(false)
const todayTasks = ref([])
const streakDays = ref(0)
const planProgress = ref(0)
const submittingId = ref(null)
const calendarDate = ref(new Date())
const dayDialogVisible = ref(false)
const selectedCalDay = ref('')
const selectedRecords = ref([])

/** planDetailId -> { actualSets, actualWeight } */
const formsMap = reactive({})
/** planDetailId -> form ref */
const formRefs = reactive({})

function storageKeyLog() {
  const uid = userStore.userInfo?.id || 'guest'
  return `fitness_checkin_log_${uid}`
}

function storageKeyProgress() {
  const uid = userStore.userInfo?.id || 'guest'
  return `fitness_plan_progress_${uid}`
}

/** { [yyyy-MM-dd]: [{ exerciseName, actualSets, actualWeight, time }] } */
const punchLogByDate = ref({})

function loadPunchLog() {
  try {
    const raw = localStorage.getItem(storageKeyLog())
    punchLogByDate.value = raw ? JSON.parse(raw) : {}
  } catch {
    punchLogByDate.value = {}
  }
}

function savePunchLog() {
  localStorage.setItem(storageKeyLog(), JSON.stringify(punchLogByDate.value))
}

function loadStoredProgress() {
  try {
    const v = localStorage.getItem(storageKeyProgress())
    planProgress.value = v != null ? Number(v) : 0
  } catch {
    planProgress.value = 0
  }
}

function saveProgress(pct) {
  if (pct != null && !Number.isNaN(pct)) {
    planProgress.value = Math.min(100, Math.max(0, pct))
    localStorage.setItem(storageKeyProgress(), String(planProgress.value))
  }
}

const totalPunchDays = computed(() => Object.keys(punchLogByDate.value || {}).length)

function setFormRef(id, el) {
  if (el) formRefs[id] = el
}

function getForm(task) {
  const id = task.planDetailId
  if (!formsMap[id]) {
    formsMap[id] = reactive({
      actualSets: 3,
      actualWeight: 0,
    })
  }
  return formsMap[id]
}

const rowRules = {
  actualSets: [{ required: true, message: '请填写组数', trigger: 'change' }],
  actualWeight: [{ required: true, message: '请填写重量', trigger: 'change' }],
}

function targetSets() {
  return '3'
}

function targetReps(task) {
  const n = task.exerciseName || ''
  if (/跑|走|有氧|椭圆|骑行|慢跑|快走/.test(n)) return '持续'
  return '10–12'
}

function targetWeight() {
  return '渐进 / 自重'
}

function cellDayNum(dayStr) {
  if (!dayStr) return ''
  const p = dayStr.split('-')
  return p[2] ? String(Number(p[2])) : ''
}

function isPunchedDate(dayStr) {
  return Boolean(punchLogByDate.value[dayStr]?.length)
}

function onPickDay(dayStr) {
  selectedCalDay.value = dayStr
  selectedRecords.value = punchLogByDate.value[dayStr] ? [...punchLogByDate.value[dayStr]] : []
  dayDialogVisible.value = true
}

function appendPunchLog(dayStr, record) {
  const log = { ...punchLogByDate.value }
  if (!log[dayStr]) log[dayStr] = []
  log[dayStr].push({
    ...record,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
  })
  punchLogByDate.value = log
  savePunchLog()
}

async function loadStreak() {
  try {
    const n = await request.get('/check-ins/streak')
    streakDays.value = typeof n === 'number' ? n : 0
  } catch {
    streakDays.value = 0
  }
}

async function loadTodayTasks() {
  loadingTasks.value = true
  try {
    const list = await request.get('/check-ins/today-tasks', { silent: true })
    todayTasks.value = Array.isArray(list) ? list : []
    updateFallbackProgress()
  } catch {
    todayTasks.value = []
  } finally {
    loadingTasks.value = false
  }
}

/** 无服务端进度时，用今日完成项占比估算展示 */
function updateFallbackProgress() {
  const tasks = todayTasks.value
  if (!tasks.length) return
  if (planProgress.value > 0) return
  const done = tasks.filter((t) => t.checkedIn).length
  planProgress.value = Math.round((done / tasks.length) * 100)
}

async function submitRow(task) {
  const id = task.planDetailId
  const formEl = formRefs[id]
  if (!formEl) return
  try {
    await formEl.validate()
  } catch {
    return
  }
  submittingId.value = id
  try {
    const f = getForm(task)
    const res = await request.post('/check-ins', {
      planDetailId: id,
      actualSets: f.actualSets,
      actualWeight: f.actualWeight,
    })
    ElMessage.success(res?.message || '打卡成功')
    if (res?.currentStreakDays != null) {
      streakDays.value = res.currentStreakDays
    }
    if (res?.progressPercent != null) {
      saveProgress(res.progressPercent)
    }
    const todayStr = formatToday()
    appendPunchLog(todayStr, {
      exerciseName: task.exerciseName,
      actualSets: f.actualSets,
      actualWeight: f.actualWeight,
    })
    await loadTodayTasks()
    await loadStreak()
    updateFallbackProgress()
  } catch (e) {
    const msg = e?.message || ''
    if (msg.includes('已打卡') || msg.includes('重复')) {
      ElMessage.warning(msg)
    }
    await loadTodayTasks()
  } finally {
    submittingId.value = null
  }
}

function formatToday() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function refreshAll() {
  loadPunchLog()
  loadStoredProgress()
  await Promise.all([loadTodayTasks(), loadStreak()])
  updateFallbackProgress()
  ElMessage.success('已刷新')
}

watch(
  () => userStore.userInfo?.id,
  () => {
    loadPunchLog()
    loadStoredProgress()
  },
)

onMounted(() => {
  loadPunchLog()
  loadStoredProgress()
  refreshAll()
})
</script>

<style scoped lang="scss">
.checkin-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 12px;
  text-align: center;
  padding: 8px 0;

  &.accent {
    border: 1px solid rgba(64, 158, 255, 0.35);
    background: linear-gradient(180deg, #f0f9ff 0%, #fff 100%);
  }

  .stat-label {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 2rem;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.2;

    .unit {
      font-size: 1rem;
      font-weight: 500;
      margin-left: 4px;
      color: #64748b;
    }
  }

  .stat-hint {
    margin-top: 8px;
    font-size: 12px;
    color: #94a3b8;
  }
}

.panel {
  border-radius: 12px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  background: #fafafa;
}

.task-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.ml {
  margin-left: 8px;
}

.muted {
  color: #64748b;
  font-size: 13px;
}

.target-desc {
  margin-bottom: 12px;
}

.row-form {
  :deep(.el-input-number) {
    width: 100%;
  }
}

.w-full {
  width: 100%;
}

.btn-col {
  display: flex;
  align-items: flex-end;
  padding-bottom: 18px;
}

.calendar-panel {
  :deep(.el-calendar__body) {
    padding: 0 8px 12px;
  }

  :deep(.el-calendar-day) {
    height: 64px;
    padding: 4px;
  }
}

.cal-cell {
  height: 100%;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #f1f5f9;
  }

  &.is-punched {
    background: #ecfdf5;
    border: 1px solid #6ee7b7;
  }

  &.is-selected {
    outline: 2px solid #409eff;
  }

  .cal-day {
    font-weight: 600;
    color: #334155;
  }

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #10b981;
    margin-top: 4px;
  }
}

.cal-legend {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 8px;

  .dot.inline {
    display: inline-block;
    margin: 0;
  }
}
</style>
