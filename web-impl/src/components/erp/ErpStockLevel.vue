<template>
  <div class="erp-stock-level" :class="tone">
    <span class="erp-stock-level__qty">{{ current }}</span>
    <el-progress
      :percentage="fillPct"
      :stroke-width="6"
      :show-text="false"
      :color="barColor"
      class="erp-stock-level__bar"
    />
    <span class="erp-stock-level__range">{{ rangeText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  current?: number
  min?: number
  max?: number
}>()

const current = computed(() => Number(props.current ?? 0))
const min = computed(() => Number(props.min ?? 0))
const max = computed(() => Number(props.max ?? 0))

const fillPct = computed(() => {
  const hi = max.value > 0 ? max.value : Math.max(min.value * 2, current.value, 1)
  return Math.min(100, Math.round((current.value / hi) * 100))
})

const tone = computed(() => {
  if (current.value < min.value) return 'low'
  if (max.value > 0 && current.value > max.value) return 'high'
  return 'ok'
})

const barColor = computed(() => {
  if (tone.value === 'low') return '#ef4444'
  if (tone.value === 'high') return '#f59e0b'
  return '#22c55e'
})

const rangeText = computed(() => {
  if (min.value || max.value) return `${min.value} ~ ${max.value || '∞'}`
  return ''
})
</script>

<style scoped>
.erp-stock-level {
  min-width: 100px;
}
.erp-stock-level__qty {
  display: block;
  font-family: var(--erp-font-mono);
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 4px;
}
.erp-stock-level.low .erp-stock-level__qty { color: #fca5a5; }
.erp-stock-level.ok .erp-stock-level__qty { color: #86efac; }
.erp-stock-level.high .erp-stock-level__qty { color: #fcd34d; }
.erp-stock-level__bar { width: 100%; }
.erp-stock-level__range {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--erp-text-muted);
}
</style>
