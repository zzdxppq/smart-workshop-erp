<template>
  <el-row :gutter="12" class="kpi-grid">
    <el-col v-for="item in items" :key="item.key" :xs="12" :sm="8" :md="span">
      <el-card shadow="never" class="kpi-card">
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-value" :style="{ color: item.color || 'var(--erp-color-primary)' }">
          {{ item.prefix ?? '' }}{{ item.value ?? '—' }}{{ item.suffix ?? '' }}
        </div>
        <div v-if="item.hint" class="kpi-hint">{{ item.hint }}</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    key: string
    label: string
    value?: string | number | unknown
    hint?: string
    prefix?: string
    suffix?: string
    color?: string
  }>
  span?: number
}>()
</script>

<style scoped>
.kpi-grid { margin-bottom: 16px; }
.kpi-card { margin-bottom: 12px; text-align: center; }
.kpi-label { font-size: 13px; color: var(--erp-text-secondary); }
.kpi-value { font-size: 28px; font-weight: 600; margin-top: 8px; line-height: 1.2; }
.kpi-hint { font-size: 12px; color: var(--erp-text-muted); margin-top: 6px; }
</style>
