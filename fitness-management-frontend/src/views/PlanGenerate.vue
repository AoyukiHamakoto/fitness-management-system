<template>
  <div class="plan-page">
    <el-row :gutter="20">
      <!-- 左侧：身体数据与生成 -->
      <el-col :xs="24" :lg="10">
        <el-card class="panel" shadow="hover">
          <template #header>
            <div class="card-title">
              <span>身体数据与训练偏好</span>
              <el-tag type="info" size="small">必填项将用于 AI 生成</el-tag>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="plan-form"
          >
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="身高 (cm)" prop="heightCm">
                  <el-input-number
                    v-model="form.heightCm"
                    :min="100"
                    :max="250"
                    :step="0.5"
                    :precision="1"
                    controls-position="right"
                    class="w-full"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="体重 (kg)" prop="weightKg">
                  <el-input-number
                    v-model="form.weightKg"
                    :min="30"
                    :max="300"
                    :step="0.1"
                    :precision="1"
                    controls-position="right"
                    class="w-full"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="年龄" prop="age">
                  <el-input-number v-model="form.age" :min="10" :max="120" controls-position="right" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别" prop="gender">
                  <el-select v-model="form.gender" placeholder="请选择" class="w-full">
                    <el-option label="男" value="男" />
                    <el-option label="女" value="女" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="健身目标" prop="fitnessGoal">
              <el-select v-model="form.fitnessGoal" placeholder="请选择主要目标" class="w-full">
                <el-option label="增肌" value="增肌" />
                <el-option label="减脂" value="减脂" />
                <el-option label="塑形" value="塑形" />
              </el-select>
            </el-form-item>

            <el-form-item label="运动经验" prop="exerciseExperience">
              <el-select v-model="form.exerciseExperience" placeholder="请选择" class="w-full">
                <el-option label="零基础" value="零基础" />
                <el-option label="初级" value="初级" />
                <el-option label="中级" value="中级" />
                <el-option label="高级" value="高级" />
              </el-select>
            </el-form-item>

            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="每周训练天数" prop="weeklyTrainingDays">
                  <el-input-number v-model="form.weeklyTrainingDays" :min="1" :max="7" controls-position="right" class="w-full" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="每次训练时长 (分钟)" prop="sessionDurationMinutes">
                  <el-input-number
                    v-model="form.sessionDurationMinutes"
                    :min="10"
                    :max="300"
                    :step="5"
                    controls-position="right"
                    class="w-full"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-button
              type="primary"
              size="large"
              class="gen-btn"
              :loading="generating"
              @click="handleGenerate"
            >
              <el-icon class="mr"><MagicStick /></el-icon>
              生成个性化计划
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：计划展示 -->
      <el-col :xs="24" :lg="14">
        <el-card v-loading="loadingPlan" class="panel plan-result" shadow="hover">
          <template #header>
            <div class="card-title">
              <span>训练计划详情</span>
              <el-button v-if="planFull?.id" link type="primary" @click="reloadCurrent">刷新</el-button>
            </div>
          </template>

          <el-empty v-if="!planFull && !loadingPlan" description="暂无进行中计划，请填写左侧表单并生成" />

          <template v-else-if="planFull">
            <div class="plan-head">
              <h2 class="plan-name">{{ planFull.planName }}</h2>
              <p class="plan-meta">
                <el-tag size="small">周期：{{ planFull.startDate }} ~ {{ planFull.endDate }}</el-tag>
                <el-tag v-if="planFull.status === 1" type="success" size="small" class="ml">进行中</el-tag>
              </p>
              <p class="plan-desc-text">{{ planFull.planDesc }}</p>
            </div>

            <el-divider content-position="left">四周训练安排</el-divider>

            <el-collapse v-model="activeWeeks" class="week-collapse">
              <el-collapse-item
                v-for="block in weeksGrouped"
                :key="block.weekIndex"
                :name="String(block.weekIndex)"
              >
                <template #title>
                  <span class="week-title">第 {{ block.weekIndex }} 周</span>
                  <el-tag size="small" type="info" class="ml-sm">{{ block.rows.length }} 项</el-tag>
                </template>

                <el-table
                  :data="block.rows"
                  border
                  stripe
                  class="detail-table"
                  :header-cell-style="{ background: '#f8fafc' }"
                >
                  <el-table-column label="训练日" width="100" fixed>
                    <template #default="{ row }">第 {{ row.dayIndex }} 天</template>
                  </el-table-column>
                  <el-table-column prop="exerciseName" label="动作" min-width="120" show-overflow-tooltip />
                  <el-table-column label="时长(分)" width="88" align="center">
                    <template #default="{ row }">{{ row.durationMinutes ?? '—' }}</template>
                  </el-table-column>
                  <el-table-column label="组数" width="72" align="center">
                    <template #default="{ row }">{{ displaySets(row) }}</template>
                  </el-table-column>
                  <el-table-column label="次数" width="88" align="center">
                    <template #default="{ row }">{{ displayReps(row) }}</template>
                  </el-table-column>
                  <el-table-column label="重量" width="100" align="center">
                    <template #default="{ row }">{{ displayWeight(row) }}</template>
                  </el-table-column>
                  <el-table-column label="休息(秒)" width="92" align="center">
                    <template #default="{ row, $index }">{{ displayRest(row, $index) }}</template>
                  </el-table-column>
                </el-table>
              </el-collapse-item>
            </el-collapse>

            <el-divider content-position="left">饮食建议</el-divider>
            <el-alert type="success" :closable="false" show-icon class="hint-block">
              <template #title>结合当前目标的饮食要点</template>
              <ul class="hint-list">
                <li v-for="(line, i) in dietLines" :key="i">{{ line }}</li>
              </ul>
            </el-alert>

            <el-divider content-position="left">注意事项</el-divider>
            <el-alert type="warning" :closable="false" show-icon class="hint-block">
              <ul class="hint-list">
                <li v-for="(line, i) in noticeLines" :key="i">{{ line }}</li>
              </ul>
            </el-alert>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import request from '@/utils/request'

const formRef = ref(null)
const generating = ref(false)
const loadingPlan = ref(false)
const planFull = ref(null)
const activeWeeks = ref([])

const form = reactive({
  heightCm: 170,
  weightKg: 65,
  age: 24,
  gender: '男',
  fitnessGoal: '减脂',
  exerciseExperience: '初级',
  weeklyTrainingDays: 3,
  sessionDurationMinutes: 45,
})

const rules = {
  heightCm: [{ required: true, message: '请填写身高', trigger: 'change' }],
  weightKg: [{ required: true, message: '请填写体重', trigger: 'change' }],
  age: [{ required: true, message: '请填写年龄', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  fitnessGoal: [{ required: true, message: '请选择健身目标', trigger: 'change' }],
  exerciseExperience: [{ required: true, message: '请选择运动经验', trigger: 'change' }],
  weeklyTrainingDays: [{ required: true, message: '请填写每周训练天数', trigger: 'change' }],
  sessionDurationMinutes: [{ required: true, message: '请填写每次训练时长', trigger: 'change' }],
}

/** 按周拆分明细：第 1–7 天为第 1 周，以此类推；仅展示有内容的周（最多 4 周） */
const weeksGrouped = computed(() => {
  const details = planFull.value?.details || []
  const sorted = [...details].sort((a, b) => {
    const d = (a.dayIndex || 0) - (b.dayIndex || 0)
    if (d !== 0) return d
    return (a.sortOrder || 0) - (b.sortOrder || 0)
  })
  const buckets = [[], [], [], []]
  for (const row of sorted) {
    const day = row.dayIndex || 1
    const wi = Math.min(Math.max(Math.ceil(day / 7) - 1, 0), 3)
    buckets[wi].push(row)
  }
  return buckets
    .map((rows, idx) => ({ weekIndex: idx + 1, rows }))
    .filter((b) => b.rows.length > 0)
})

const goalKey = computed(() => form.fitnessGoal || '减脂')

const dietLines = computed(() => {
  const g = goalKey.value
  if (g === '增肌') {
    return [
      '保证每日蛋白质摄入（瘦肉、鱼、蛋、豆制品），训练日可适当增加碳水补充肌糖原。',
      '分餐进食，避免一次性过量；充足饮水，少喝含糖饮料。',
    ]
  }
  if (g === '塑形') {
    return [
      '均衡饮食：适量蛋白 + 复合碳水 + 蔬菜水果；控制油炸与精制糖。',
      '关注总热量与进食规律，配合力量训练维持肌肉线条。',
    ]
  }
  return [
    '适度热量缺口，优先高蛋白、高纤维、少油少糖；避免极端节食。',
    '多蔬菜、全谷物；记录体重与围度变化，循序渐进。',
  ]
})

const noticeLines = computed(() => {
  const base = [
    '训练前充分热身，训练后拉伸放松；出现胸痛、眩晕等请立即停止并就医。',
    '循序渐进增加负荷，保证睡眠与恢复；组次与重量为参考，以体感与教练指导为准。',
  ]
  const desc = planFull.value?.planDesc
  if (desc && desc.trim()) {
    return [desc.trim(), ...base]
  }
  return base
})

function displaySets() {
  return '3'
}

function displayReps(row) {
  const name = row.exerciseName || ''
  if (/跑|走|有氧|椭圆|骑行|慢跑|快走/.test(name)) {
    return '持续'
  }
  return '10–12'
}

function displayWeight() {
  return '渐进'
}

function displayRest(row, index) {
  const base = 60 + ((row.sortOrder || 0) + index) % 4 * 15
  return base
}

async function loadCurrentPlan() {
  loadingPlan.value = true
  planFull.value = null
  try {
    const summary = await request.get('/plans/current', { silent: true })
    if (summary?.id) {
      const full = await request.get(`/plans/${summary.id}`)
      planFull.value = full
    }
  } catch {
    planFull.value = null
  } finally {
    loadingPlan.value = false
  }
}

async function reloadCurrent() {
  await loadCurrentPlan()
  ElMessage.success('已刷新')
}

async function handleGenerate() {
  const f = formRef.value
  if (!f) return
  try {
    await f.validate()
  } catch {
    return
  }
  generating.value = true
  try {
    const planId = await request.post('/plans/generate', {
      heightCm: form.heightCm,
      weightKg: form.weightKg,
      age: form.age,
      gender: form.gender,
      fitnessGoal: form.fitnessGoal,
      exerciseExperience: form.exerciseExperience,
      weeklyTrainingDays: form.weeklyTrainingDays,
      sessionDurationMinutes: form.sessionDurationMinutes,
    })
    ElMessage.success('计划生成成功')
    if (planId) {
      const full = await request.get(`/plans/${planId}`)
      planFull.value = full
    } else {
      await loadCurrentPlan()
    }
  } catch (e) {
    if (!e?.response) {
      ElMessage.error(e?.message || '生成失败')
    }
  } finally {
    generating.value = false
  }
}

watch(
  weeksGrouped,
  (list) => {
    activeWeeks.value = list.map((b) => String(b.weekIndex))
  },
  { immediate: true },
)

onMounted(() => {
  loadCurrentPlan()
})
</script>

<style scoped lang="scss">
.plan-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.panel {
  border-radius: 12px;
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
}

.plan-form {
  :deep(.el-input-number) {
    width: 100%;
  }
}

.w-full {
  width: 100%;
}

.gen-btn {
  width: 100%;
  margin-top: 8px;
}

.mr {
  margin-right: 6px;
}

.ml {
  margin-left: 8px;
}

.ml-sm {
  margin-left: 10px;
}

.plan-result {
  min-height: 420px;
}

.plan-head {
  margin-bottom: 8px;

  .plan-name {
    margin: 0 0 8px;
    font-size: 1.35rem;
    color: #0f172a;
  }

  .plan-meta {
    margin: 0 0 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
  }

  .plan-desc-text {
    margin: 0;
    color: #475569;
    line-height: 1.6;
    font-size: 14px;
  }
}

.week-collapse {
  border: none;

  :deep(.el-collapse-item__header) {
    font-weight: 600;
    color: #334155;
  }
}

.week-title {
  font-size: 15px;
}

.detail-table {
  width: 100%;
}

.hint-block {
  align-items: flex-start;

  .hint-list {
    margin: 8px 0 0;
    padding-left: 18px;
    line-height: 1.7;
    color: #334155;
  }
}

@media (max-width: 992px) {
  .plan-page {
    padding: 12px;
  }
}
</style>
