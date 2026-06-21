<template>
  <div v-loading="loading" class="order-timeline-panel">
    <el-empty v-if="!loading && !events.length" description="暂无时间线记录" />
    <el-timeline v-else class="order-timeline-panel__list">
      <el-timeline-item
        v-for="ev in events"
        :key="ev.id"
        :timestamp="ev.time"
        :type="ev.type"
        placement="top"
      >
        <p class="order-timeline-panel__title">{{ ev.title }}</p>
        <p class="order-timeline-panel__detail">{{ ev.detail }}</p>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

const props = defineProps<{
  orderId: number | string
}>()

const api = useBaseStore().api
const loading = ref(false)
const events = ref<{
  id: number
  time?: string
  title?: string
  detail?: string
  type?: 'primary' | 'success' | 'warning' | 'danger'
}[]>([])

async function loadTimeline() {
  if (!props.orderId) return
  loading.value = true
  try {
    const r = unwrapResult(await api.get(`/orders/${props.orderId}/timeline`))
    events.value = parsePageItems(r).items as typeof events.value
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载时间线失败')
    events.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.orderId, loadTimeline, { immediate: true })
</script>

<style scoped>
.order-timeline-panel__list {
  padding: 4px 0 0 4px;
  max-height: 60vh;
  overflow-y: auto;
}

.order-timeline-panel__title {
  margin: 0 0 4px;
  font-weight: 600;
  color: var(--erp-text-primary);
}

.order-timeline-panel__detail {
  margin: 0;
  font-size: 13px;
  color: var(--erp-text-secondary);
}
</style>
