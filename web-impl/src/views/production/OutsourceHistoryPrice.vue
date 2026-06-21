<template>
  <div v-loading="loading">
    <h2>委外历史价格 (FR-6-3)</h2>
    <el-form :inline="true">
      <el-form-item label="供应商ID">
        <el-input v-model="vendorId" placeholder="vendorId" />
      </el-form-item>
      <el-form-item label="工序名称">
        <el-input v-model="processName" placeholder="processName" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="refresh">查询</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="result?.empty"
      type="warning"
      :closable="false"
      :title="result.message || '暂无历史价，请询价'"
      style="margin-bottom: 12px"
    />

    <el-descriptions v-if="result && !result.empty" :column="2" border style="margin-bottom: 16px">
      <el-descriptions-item label="建议单价（中位数）">{{ result.suggestedPrice }}</el-descriptions-item>
      <el-descriptions-item label="历史均价">{{ result.avgPrice }}</el-descriptions-item>
      <el-descriptions-item label="样本数">{{ result.sampleCount }}</el-descriptions-item>
      <el-descriptions-item label="工序">{{ result.processName }}</el-descriptions-item>
    </el-descriptions>

    <el-table :data="priceRows" stripe>
      <el-table-column prop="seq" label="序号" width="80" />
      <el-table-column prop="price" label="成交价" />
      <el-table-column label="操作" width="120">
        <template #default>
          <el-button
            size="small"
            type="primary"
            @click="$router.push({
              path: '/production/price-suggest',
              query: { supplierId: vendorId, processName },
            })"
          >价格建议</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useOutsourceStore } from '@/stores/outsource'
import { unwrapResult } from '@/utils/apiPage'

interface HistoryPriceResult {
  empty?: boolean
  message?: string
  suggestedPrice?: number
  avgPrice?: number
  sampleCount?: number
  processName?: string
  historyPrices?: number[]
}

const outsourceStore = useOutsourceStore()
const result = ref<HistoryPriceResult | null>(null)
const vendorId = ref('')
const processName = ref('')
const loading = ref(false)

const priceRows = computed(() =>
  (result.value?.historyPrices ?? []).map((price, idx) => ({ seq: idx + 1, price })),
)

async function refresh() {
  const vid = Number(vendorId.value)
  if (!vid || !processName.value) {
    ElMessage.warning('请填写供应商ID与工序名称')
    return
  }
  loading.value = true
  try {
    const r = await outsourceStore.getHistoryPrice(vid, processName.value)
    result.value = unwrapResult<HistoryPriceResult>(r)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    result.value = null
  } finally {
    loading.value = false
  }
}
</script>
