<template>
  <div v-loading="loading">
    <h2>委外详情</h2>
    <el-card v-if="order">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="委外单号">{{ order.outsourceNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="outsourceStateTagType(order.status)">{{ outsourceStateLabel(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工单号">{{ order.workorderNo }}</el-descriptions-item>
        <el-descriptions-item label="工序归属">
          <el-tag type="warning" size="small">生管划为委外</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="供应商">{{ order.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ order.processName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ order.qty }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ order.unitPrice }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="计划交期">{{ order.deliveryDate }}</el-descriptions-item>
        <el-descriptions-item label="返修次数">{{ order.reworkCount }}</el-descriptions-item>
        <el-descriptions-item label="交期预估（近3次中位数）">
          <span v-if="eta">{{ eta.medianDays ?? eta.medianLeadDays ?? '—' }} 天</span>
          <span v-else>—</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useOutsourceStore } from '@/stores/outsource'
import { useBaseStore } from '@/stores/_base'
import { useParamLoad } from '@/composables/useDetailLoad'
import { unwrapResult } from '@/utils/apiPage'
import { outsourceStateLabel, outsourceStateTagType } from '@/constants/outsourceStates'

const outsourceStore = useOutsourceStore()
const eta = ref<Record<string, unknown> | null>(null)
const { data: order, loading } = useParamLoad<any>('outsourceNo', (no) => outsourceStore.getOrder(no))

watch(order, async (o) => {
  if (!o?.supplierId && !o?.vendorId) return
  const vendorId = o.supplierId ?? o.vendorId
  const processName = o.processName
  if (!processName) return
  try {
    const raw = await useBaseStore().api.get('/outsource/history-price', {
      params: { vendorId, processName },
    })
    eta.value = unwrapResult(raw) as Record<string, unknown>
  } catch {
    eta.value = null
  }
}, { immediate: true })
</script>
