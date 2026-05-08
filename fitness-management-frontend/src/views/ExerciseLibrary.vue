<template>
  <div class="lib-page">
    <el-card shadow="hover">
      <template #header>
        <div class="head">
          <span>训练动作库</span>
          <el-input v-model="keyword" placeholder="搜索动作/肌群" clearable style="width: 240px" />
        </div>
      </template>

      <el-row :gutter="16">
        <el-col v-for="item in filtered" :key="item.name" :xs="24" :sm="12" :lg="8">
          <el-card class="exercise-card" shadow="never">
            <img :src="item.mediaUrl" :alt="item.name" class="media" />
            <h3>{{ item.name }}</h3>
            <div class="meta">
              <el-tag size="small" type="success">{{ item.muscleGroup }}</el-tag>
              <el-tag size="small" type="info">{{ item.difficulty }}</el-tag>
            </div>
            <p>{{ item.description }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const keyword = ref('')
const list = ref([])

const filtered = computed(() => {
  const k = keyword.value.trim()
  if (!k) return list.value
  return list.value.filter((i) => `${i.name}${i.muscleGroup}${i.description}`.includes(k))
})

onMounted(async () => {
  list.value = (await request.get('/dashboard/exercise-library', { silent: true })) || []
  if (route.query.action && !keyword.value) {
    keyword.value = String(route.query.action)
  }
})
</script>

<style scoped lang="scss">
.lib-page {
  max-width: 1280px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.exercise-card {
  margin-bottom: 16px;
  min-height: 330px;
}
.media {
  width: 100%;
  height: 170px;
  object-fit: cover;
  border-radius: 8px;
}
.meta {
  display: flex;
  gap: 8px;
  margin: 8px 0;
}
p {
  color: #475569;
  line-height: 1.6;
}
</style>
