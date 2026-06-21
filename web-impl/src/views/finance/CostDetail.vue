<template>
  <div v-loading="loading" class="cost-detail">
    <h2>成本明细</h2>
    <el-card v-if="cost">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单">{{ cost.workorderNo ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ cost.materialCode ?? cost.materialName ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="物料成本">{{ cost.materialCost ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="人工成本">{{ cost.laborCost ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="委外成本">{{ cost.outsourceCost ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="制造费用">{{ cost.overhead ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="总成本">{{ cost.totalCost ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="期间">{{ cost.period ?? cost.costDate ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-empty v-else-if="!loading" description="暂无成本数据" />
    <el-button style="margin-top: 16px" class="erp-btn-ghost" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useFinanceStore } from '@/stores/finance'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { unwrapResult } from '@/utils/apiPage'

const financeStore = useFinanceStore()
const { data: raw, loading } = useDetailLoad<any>(async (id) => {
  const r = await financeStore.getCost(id)
  return unwrapResult(r)
})

const cost = computed(() => {
  const d = raw.value
  if (!d) return null
  const acc = d.accounting ?? d
  const segs = (d.segments ?? []) as { segmentCode?: string; amount?: number }[]
  let materialCost = Number(acc.materialCost ?? 0)
  let laborCost = Number(acc.laborCost ?? 0)
  let outsourceCost = Number(acc.outsourceCost ?? 0)
  let overhead = Number(acc.overhead ?? 0)
  for (const seg of segs) {
    const amt = Number(seg.amount ?? 0)
    switch (seg.segmentCode) {
      case 'MATERIAL': materialCost += amt; break
      case 'PROCESS': laborCost += amt; break
      case 'OUTSOURCE': outsourceCost += amt; break
      case 'MANAGE':
      case 'DEPRECIATION': overhead += amt; break
    }
  }
  return {
    workorderNo: acc.refNo ?? acc.workorderNo,
    materialCode: acc.materialCode,
    materialName: acc.materialName,
    materialCost,
    laborCost,
    outsourceCost,
    overhead,
    totalCost: acc.totalCost ?? acc.totalAmount,
    period: acc.costDate,
    costDate: acc.costDate,
  }
})
</script>

<style scoped>
.cost-detail { padding: 16px; }
</style>
