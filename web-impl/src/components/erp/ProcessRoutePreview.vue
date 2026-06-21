<template>
  <div v-if="steps.length" class="process-route-preview">
    <el-steps :active="activeStep" finish-status="success" direction="vertical" :space="48">
      <el-step
        v-for="(s, i) in steps"
        :key="i"
        :title="s.name"
        :description="stepDesc(s)"
      />
    </el-steps>
  </div>
  <el-empty v-else :description="emptyText" :image-size="64" />
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface ProcessRouteStep {
  step?: number
  seq?: number
  name?: string
  workcenter?: string
  stdMinutes?: number
  std_minutes?: number
  /** 成本字段保留在类型中以兼容后端契约，但前端不展示（避免权限泄露 G7） */
  cost?: number
}

const props = withDefaults(defineProps<{
  /** JSON 字符串或步骤数组 */
  route?: string | ProcessRouteStep[] | null
  activeStep?: number
  emptyText?: string
}>(), {
  route: null,
  activeStep: 0,
  emptyText: '暂无已发布工艺路线',
})

/** 过滤掉成本字段，仅展示工序名+工时+工作中心（G7 权限隔离：成本仅财务可见） */
function sanitize(steps: ProcessRouteStep[]): ProcessRouteStep[] {
  return steps.map(s => {
    const { cost: _omit, ...rest } = s
    return rest
  })
}

const steps = computed<ProcessRouteStep[]>(() => {
  if (!props.route) return []
  if (Array.isArray(props.route)) return sanitize(props.route)
  try {
    const parsed = JSON.parse(props.route)
    return Array.isArray(parsed) ? sanitize(parsed) : []
  } catch {
    return []
  }
})

function stepDesc(s: ProcessRouteStep) {
  const parts: string[] = []
  if (s.workcenter) parts.push(s.workcenter)
  const mins = s.stdMinutes ?? s.std_minutes
  if (mins != null) parts.push(`${mins} min`)
  return parts.join(' · ') || undefined
}
</script>

<style scoped>
.process-route-preview {
  padding: 8px 4px;
  max-height: 360px;
  overflow-y: auto;
}
</style>
