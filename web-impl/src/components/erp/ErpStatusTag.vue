<template>
  <span class="erp-status-tag" :class="tone">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { commonStatusLabel, commonStatusTone } from '@/utils/statusLabels'

const props = defineProps<{ status?: string; label?: string }>()

const label = computed(() => props.label ?? commonStatusLabel(props.status))
const tone = computed(() => commonStatusTone(props.status))
</script>

<style scoped>
.erp-status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  border: 1px solid transparent;
}
.erp-status-tag.primary {
  color: #93c5fd;
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.35);
}
.erp-status-tag.success {
  color: #86efac;
  background: rgba(34, 197, 94, 0.15);
  border-color: rgba(34, 197, 94, 0.35);
}
.erp-status-tag.warning {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.35);
}
.erp-status-tag.danger {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.35);
}
.erp-status-tag.muted {
  color: var(--erp-text-secondary);
  background: rgba(148, 163, 184, 0.1);
  border-color: var(--erp-border);
}
</style>
