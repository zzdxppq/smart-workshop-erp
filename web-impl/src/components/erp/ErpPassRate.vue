<template>
  <div class="erp-pass-rate">
    <span class="erp-pass-rate__value" :class="tone">{{ pctText }}</span>
    <el-progress
      :percentage="pct"
      :stroke-width="6"
      :show-text="false"
      :color="barColor"
      class="erp-pass-rate__bar"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  pass?: number
  total?: number
  qty?: number
  passQty?: number
  failQty?: number
}>()

const pass = computed(() => {
  if (props.passQty != null && props.qty != null && props.qty > 0) return props.passQty
  if (props.pass != null) return props.pass
  if (props.passQty != null) return props.passQty
  return 0
})

const total = computed(() => {
  if (props.qty != null && props.qty > 0) return props.qty
  if (props.total != null && props.total > 0) return props.total
  const p = pass.value
  const f = props.failQty ?? 0
  return p + f > 0 ? p + f : 0
})

const pct = computed(() => {
  if (total.value <= 0) return 0
  return Math.min(100, Math.round((pass.value / total.value) * 100))
})

const pctText = computed(() => (total.value > 0 ? `${pct.value}%` : '—'))

const tone = computed(() => {
  if (pct.value >= 98) return 'good'
  if (pct.value >= 90) return 'ok'
  return 'bad'
})

const barColor = computed(() => {
  if (pct.value >= 98) return '#22c55e'
  if (pct.value >= 90) return '#f59e0b'
  return '#ef4444'
})
</script>

<style scoped>
.erp-pass-rate {
  min-width: 88px;
}
.erp-pass-rate__value {
  display: block;
  font-family: var(--erp-font-mono);
  font-weight: 700;
  font-size: 13px;
  margin-bottom: 4px;
}
.erp-pass-rate__value.good { color: #86efac; }
.erp-pass-rate__value.ok { color: #fcd34d; }
.erp-pass-rate__value.bad { color: #fca5a5; }
.erp-pass-rate__bar {
  width: 100%;
}
</style>
