<template>
  <ErpPageShell
    title="审批路由预览"
    description="根据金额、品类与紧急程度预览采购审批链路，不产生实际审批记录。"
  >
    <el-form :model="form" label-width="120px">
      <el-form-item label="金额">
        <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 200px" />
      </el-form-item>
      <el-form-item label="品类">
        <el-select v-model="form.category" placeholder="选择品类" clearable style="width: 200px">
          <el-option label="刀具 TOOL" value="TOOL" />
          <el-option label="化学品 CHEMICAL" value="CHEMICAL" />
          <el-option label="机械加工 MECHANICAL" value="MECHANICAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商状态">
        <el-select v-model="form.supplierStatus" placeholder="选择状态" clearable style="width: 200px">
          <el-option label="正常 NORMAL" value="NORMAL" />
          <el-option label="观察 WATCH" value="WATCH" />
          <el-option label="黑名单 BLACKLIST" value="BLACKLIST" />
        </el-select>
      </el-form-item>
      <el-form-item label="紧急度">
        <el-radio-group v-model="form.urgency">
          <el-radio value="NORMAL">普通</el-radio>
          <el-radio value="URGENT">紧急</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="preview" :loading="loading">预览路由</el-button>
      </el-form-item>
    </el-form>

    <el-card v-if="result" header="预览结果" class="result-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="审批路由">
          <el-tag v-for="r in result.route" :key="r" type="warning" style="margin-right: 4px">{{ r }}</el-tag>
          <el-tag v-if="!result.route.length" type="info">SELF（业务自审，≤ 1 万）</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预估审批人数">
          {{ result.estimatedSigners }}
        </el-descriptions-item>
        <el-descriptions-item label="命中阈值" :span="2">
          <el-tag v-for="t in result.matchedThresholds" :key="t" size="small" style="margin-right: 4px">
            {{ thresholdName(t) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="result.compatibleLegacyRoute" label="兼容 legacy 路由" :span="2">
          <el-tag v-for="r in result.compatibleLegacyRoute" :key="r" type="info" style="margin-right: 4px">{{ r }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import type { ApprovalRouteResponse } from '@/api/generated/models/ApprovalRouteResponse'

/**
 * V1.3.8 Sprint 7 · Story 4.2 采购主管审批路由预览（前端骨架）
 *
 * 4 阈值：
 *   AMOUNT_10K_50K            1-5 万 → PROCUREMENT_MANAGER
 *   AMOUNT_ABOVE_50K          > 5 万 → GM+PROCUREMENT_MANAGER
 *   CATEGORY_TOOL             刀具 → PROCUREMENT_MANAGER
 *   CATEGORY_CHEMICAL         化学品 → PROCUREMENT_MANAGER
 *   URGENCY_URGENT_AMOUNT_OVER_10K  紧急 + > 1 万 → PROCUREMENT_MANAGER
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */

const loading = ref(false)

const form = ref({
  amount: 30000,
  category: '',
  supplierStatus: 'NORMAL',
  urgency: 'NORMAL',
})

const result = ref<ApprovalRouteResponse | null>(null)

const thresholdNameMap: Record<string, string> = {
  AMOUNT_BELOW_10K: '金额 ≤ 1 万',
  AMOUNT_10K_50K: '金额 1-5 万',
  AMOUNT_ABOVE_50K: '金额 > 5 万',
  CATEGORY_TOOL: '品类=刀具',
  CATEGORY_CHEMICAL: '品类=化学品',
  URGENCY_URGENT_AMOUNT_OVER_10K: '紧急+>1万',
}

function thresholdName(code: string): string {
  return thresholdNameMap[code] || code
}

async function preview() {
  loading.value = true
  try {
    result.value = unwrapResult<ApprovalRouteResponse>(
      await useBaseStore().api.post('/approval/route-preview', form.value),
    )
  } catch (e: unknown) {
    const err = e as { message?: string; response?: { data?: { msg?: string; message?: string } } }
    ElMessage.error(err?.response?.data?.msg || err?.response?.data?.message || err?.message || '路由预览失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.result-card {
  margin-top: 16px;
}
</style>