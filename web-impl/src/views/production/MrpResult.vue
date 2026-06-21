<template>
  <div v-loading="loading">
    <h2>MRP 结果详情</h2>
    <el-table :data="items" stripe border>
      <el-table-column prop="materialCode" label="料号" min-width="130" />
      <el-table-column prop="requiredQty" label="需求" width="90" />
      <el-table-column prop="currentStock" label="库存" width="90" />
      <el-table-column prop="shortageQty" label="缺料" width="90" />
      <el-table-column prop="purchaseSuggestion" label="建议采购" width="100" />
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMrpStore } from '@/stores/mrp'
import { parsePageItems } from '@/utils/apiPage'

const route = useRoute()
const mrpStore = useMrpStore()
const items = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  const runId = Number(route.params.runId)
  if (!runId) {
    ElMessage.error('无效的运算 ID')
    return
  }
  loading.value = true
  try {
    const r = await mrpStore.getMrpResult(runId)
    items.value = parsePageItems(r).items
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
})
</script>
