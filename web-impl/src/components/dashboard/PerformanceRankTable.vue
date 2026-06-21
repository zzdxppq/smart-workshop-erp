<template>
  <el-table :data="rows" stripe :size="large ? 'default' : 'small'" :max-height="large ? 560 : 420">
    <el-table-column type="index" label="#" width="50" />
    <el-table-column
      :prop="groupBy === 'machine' ? 'machineCode' : 'operatorName'"
      :label="groupBy === 'machine' ? '机台' : '操作工'"
      :min-width="large ? 160 : 120"
    >
      <template #default="{ row }">
        <el-button
          v-if="groupBy !== 'machine' && row.operatorId"
          link
          type="primary"
          @click="$emit('operator-click', row)"
        >
          {{ row.operatorName }}
        </el-button>
        <span v-else>{{ groupBy === 'machine' ? row.machineCode : row.operatorName }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="finishedQty" label="产量" :width="large ? 100 : 80" align="right" />
    <el-table-column label="合格率" :width="large ? 110 : 90" align="right">
      <template #default="{ row }">{{ pct(row.passRate) }}</template>
    </el-table-column>
    <el-table-column
      :label="groupBy === 'machine' ? '利用率' : '工时利用率'"
      :width="large ? 120 : 100"
      align="right"
    >
      <template #default="{ row }">{{ pct(row.utilizationRate) }}</template>
    </el-table-column>
    <el-table-column
      v-if="groupBy === 'machine'"
      label="故障率"
      :width="large ? 100 : 80"
      align="right"
    >
      <template #default="{ row }">{{ pct(row.faultRate) }}</template>
    </el-table-column>
    <el-table-column v-else label="达成率" :width="large ? 100 : 80" align="right">
      <template #default="{ row }">{{ pct(row.achievementRate ?? row.overRate) }}</template>
    </el-table-column>
    <el-table-column prop="score" label="考核分" :width="large ? 100 : 80" align="right">
      <template #default="{ row }">
        <span class="score">{{ row.score ?? '—' }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="grade" label="等级" :width="large ? 80 : 70">
      <template #default="{ row }">
        <el-tag :type="gradeType(row.grade)" size="small">{{ row.grade ?? '—' }}</el-tag>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
defineProps<{
  rows: Record<string, unknown>[]
  groupBy: 'operator' | 'machine' | string
  large?: boolean
}>()
defineEmits<{ 'operator-click': [Record<string, unknown>] }>()

function pct(v: unknown) {
  const n = Number(v ?? 0)
  return `${Math.round(n * 1000) / 10}%`
}

function gradeType(g: unknown): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  if (g === 'A') return 'success'
  if (g === 'B') return 'info'
  if (g === 'C') return 'warning'
  return 'danger'
}
</script>

<style scoped>
.score { font-weight: 600; color: var(--erp-color-primary); }
</style>
