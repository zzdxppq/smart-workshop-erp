<template>
  <el-drawer
    :model-value="open"
    :title="`生产明细 · ${operatorName || '操作工'}`"
    size="520px"
    @update:model-value="$emit('update:open', $event)"
  >
    <div v-loading="loading">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="统计周期">{{ periodLabel }}</el-descriptions-item>
        <el-descriptions-item label="操作工 ID">{{ operatorId ?? '—' }}</el-descriptions-item>
      </el-descriptions>

      <h4>每日明细</h4>
      <el-table :data="daily" size="small" stripe max-height="200">
        <el-table-column prop="statDate" label="日期" width="110" />
        <el-table-column prop="finishedQty" label="产量" width="70" align="right" />
        <el-table-column label="合格率" width="80" align="right">
          <template #default="{ row }">{{ pct(row.passRate) }}</template>
        </el-table-column>
      </el-table>

      <h4 style="margin-top: 16px">工序分布</h4>
      <el-table :data="processes" size="small" stripe max-height="160">
        <el-table-column prop="processName" label="工序" min-width="100" />
        <el-table-column prop="finishedQty" label="产量" width="70" align="right" />
        <el-table-column prop="scrapQty" label="不良" width="60" align="right" />
      </el-table>

      <h4 style="margin-top: 16px">不良品记录</h4>
      <el-table :data="defects" size="small" stripe max-height="180">
        <el-table-column prop="workorderNo" label="工单" width="120" />
        <el-table-column prop="processName" label="工序" width="90" />
        <el-table-column prop="scrapQty" label="数量" width="60" align="right" />
        <el-table-column prop="reportedAt" label="时间" min-width="140" />
      </el-table>
      <el-empty v-if="!defects.length" description="暂无不良记录" :image-size="48" />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const props = defineProps<{
  open: boolean
  operatorId?: number | null
  operatorName?: string
  period: string
}>()
defineEmits<{ 'update:open': [boolean] }>()

const api = useBaseStore().api
const loading = ref(false)
const daily = ref<Record<string, unknown>[]>([])
const processes = ref<Record<string, unknown>[]>([])
const defects = ref<Record<string, unknown>[]>([])

const periodLabel = { day: '今日', week: '本周', month: '本月' }[props.period] ?? props.period

function pct(v: unknown) {
  return `${Math.round(Number(v ?? 0) * 1000) / 10}%`
}

async function load() {
  if (!props.open || !props.operatorId) return
  loading.value = true
  try {
    const data = unwrapResult<Record<string, unknown>>(
      await api.get(`/dashboard/performance/operator/${props.operatorId}/detail`, {
        params: { period: props.period },
      }),
    )
    daily.value = (data.daily as Record<string, unknown>[]) ?? []
    processes.value = (data.processes as Record<string, unknown>[]) ?? []
    defects.value = (data.defects as Record<string, unknown>[]) ?? []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载明细失败')
  } finally {
    loading.value = false
  }
}

watch(() => [props.open, props.operatorId, props.period], load)
</script>

<style scoped>
h4 { margin: 0 0 8px; font-size: 14px; color: var(--erp-text-secondary); }
</style>
