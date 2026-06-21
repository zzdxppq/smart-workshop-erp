<template>
  <div v-loading="loading">
    <h2>价格建议 (FR-6-3)</h2>
    <el-alert
      v-if="suggestion?.empty"
      type="warning"
      :closable="false"
      :title="suggestion.message || '暂无历史价，请询价'"
      style="margin-bottom: 12px"
    />
    <el-card v-if="suggestion && !suggestion.empty">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="供应商ID">{{ suggestion.vendorId }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ suggestion.processName || suggestion.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="历史均价">{{ suggestion.avgPrice }}</el-descriptions-item>
        <el-descriptions-item label="建议单价（中位数）">{{ suggestion.suggestedPrice }}</el-descriptions-item>
        <el-descriptions-item label="样本数">{{ suggestion.sampleCount }}</el-descriptions-item>
        <el-descriptions-item label="最近成交价">{{ (suggestion.historyPrices || []).join(', ') }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-button style="margin-top: 16px" type="primary" :disabled="!suggestion || suggestion.empty" @click="apply">
      采纳建议
    </el-button>
    <el-button @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useOutsourceStore } from '@/stores/outsource'
import { unwrapResult } from '@/utils/apiPage'

const route = useRoute()
const outsourceStore = useOutsourceStore()
const suggestion = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  const supplierId = Number(route.query.supplierId || 0)
  const processName = String(route.query.processName || '')
  const materialCode = String(route.query.materialCode || '')
  if (!supplierId || (!processName && !materialCode)) {
    ElMessage.warning('请携带 supplierId 与 processName（或 materialCode）参数进入')
    return
  }
  loading.value = true
  try {
    const r = await outsourceStore.getPriceSuggest(supplierId, processName || undefined, materialCode || undefined)
    suggestion.value = unwrapResult(r)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
})

function apply() {
  ElMessage.success(`已采纳建议单价：${suggestion.value?.suggestedPrice ?? ''}`)
}
</script>
