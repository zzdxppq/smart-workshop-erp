<template>
  <el-tag :type="tagType" size="small">{{ ENGINEER_TASK_PHASE_LABEL[phase] }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ENGINEER_TASK_PHASE_LABEL, type EngineerTaskPhase } from '@/utils/engineeringTask'

const props = defineProps<{ phase: EngineerTaskPhase }>()

const tagType = computed(() => {
  if (props.phase === 'COMPLETED') return 'success'
  if (props.phase === 'IN_PROGRESS') return 'warning'
  return 'info'
})
</script>
