<template>
  <div class="app-only">
    <el-result icon="info" title="请在 Android APP 操作">
      <template #sub-title>
        <p>{{ featureName }}属于一线仓管/车间 APP 功能（PRD FR-4-2 / FR-5-2 / FR-12-2）。</p>
        <p>Web 端请使用列表查询、单据编辑与审批类功能。</p>
      </template>
      <template #extra>
        <el-button type="primary" @click="$router.push(fallbackPath)">返回</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const featureName = computed(() => String(route.query.feature || '该功能'))

const fallbackPath = computed(() => {
  const from = route.query.from
  if (typeof from === 'string' && from.startsWith('/')) return from
  if (route.path.includes('warehouse')) return '/warehouse/inventory'
  if (route.path.includes('production')) return '/production/workorders'
  if (route.path.includes('sourcing')) return '/sourcing/incoming'
  return '/dashboard'
})
</script>

<style scoped>
.app-only {
  padding: 48px 24px;
}
</style>
