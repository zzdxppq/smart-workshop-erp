<template>
  <div
    class="machine-load-bar"
    :class="levelClass"
    role="progressbar"
    :aria-valuenow="percent"
    aria-valuemin="0"
    aria-valuemax="100"
    @click="emit('click', percent)"
  >
    <div class="bar-track">
      <div class="bar-fill" :style="{ width: `${Math.min(100, percent)}%` }" />
    </div>
    <span class="bar-label">{{ label || machineName }} {{ percent }}%</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  percent: number
  machineName?: string
  label?: string
  warnAt?: number
  dangerAt?: number
}>(), {
  machineName: '',
  warnAt: 70,
  dangerAt: 90,
})

const emit = defineEmits<{ click: [percent: number] }>()

const levelClass = computed(() => {
  if (props.percent >= props.dangerAt) return 'is-danger'
  if (props.percent >= props.warnAt) return 'is-warning'
  return 'is-success'
})
</script>

<style scoped>
.machine-load-bar {
  cursor: pointer;
  padding: 4px 0;
}
.bar-track {
  height: 8px;
  background: var(--erp-border, #d1d9e0);
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  transition: width 0.3s ease;
}
.is-success .bar-fill { background: var(--erp-color-success, #1a7f37); }
.is-warning .bar-fill { background: var(--erp-color-warning, #bf8700); }
.is-danger .bar-fill { background: var(--erp-color-danger, #cf222e); }
.bar-label {
  font-size: 12px;
  color: var(--erp-text-secondary);
  margin-top: 4px;
  display: block;
}
</style>
