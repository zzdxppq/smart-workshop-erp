<template>
  <div>
    <h2>委外状态历史</h2>

    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="委外单 ID">
        <el-input-number v-model="outsourceId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <el-button @click="$router.push({ path: '/production/outsub-panel', query: { outsourceId: String(outsourceId) } })">
          状态机面板
        </el-button>
      </el-form-item>
    </el-form>

    <el-timeline v-loading="loading">
      <el-timeline-item
        v-for="h in history"
        :key="h.id"
        :timestamp="h.occurredAt || ''"
        placement="top"
      >
        <h4>{{ h.fromState }} → {{ h.toState }}</h4>
        <p>类型：{{ h.transitionType || '—' }} · 角色：{{ h.operatorRole || '—' }}</p>
        <p v-if="h.reason">原因：{{ h.reason }}</p>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!loading && !history.length" description="暂无历史记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { E6OutsourceStateMachineService } from '@/api/generated/services/E6OutsourceStateMachineService'
import type { OutsourceStateHistory } from '@/api/generated/models/OutsourceStateHistory'
import { parsePageItems } from '@/utils/apiPage'

const route = useRoute()
const outsourceId = ref<number>(Number(route.query.outsourceId) || 1)
const history = ref<OutsourceStateHistory[]>([])
const loading = ref(false)

async function load() {
  if (!outsourceId.value || outsourceId.value < 1) {
    ElMessage.warning('请输入委外单 ID')
    return
  }
  loading.value = true
  try {
    history.value = parsePageItems(await E6OutsourceStateMachineService.getOutsourceStateHistory(outsourceId.value)).items as OutsourceStateHistory[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    history.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
