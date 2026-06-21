<template>
  <div class="engineering-data">
    <el-tabs v-model="activeTab" class="engineering-data-tabs" @tab-change="onTabChange">
      <el-tab-pane label="图纸管理" name="drawings" />
      <el-tab-pane label="工艺库" name="process" />
      <el-tab-pane label="BOM" name="boms" />
      <el-tab-pane label="工艺路线维护" name="process-routes" />
    </el-tabs>
    <Drawings v-if="activeTab === 'drawings'" engineering-context />
    <Process v-else-if="activeTab === 'process'" />
    <BOMs v-else-if="activeTab === 'boms'" />
    <ProcessRoutes v-else-if="activeTab === 'process-routes'" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Drawings from '@/views/material/Drawings.vue'
import Process from '@/views/material/Process.vue'
import BOMs from '@/views/material/BOMs.vue'
import ProcessRoutes from '@/views/material/ProcessRoutes.vue'

const route = useRoute()
const router = useRouter()

const VALID_TABS = ['drawings', 'process', 'boms', 'process-routes'] as const
type EngTab = (typeof VALID_TABS)[number]

const tabFromQuery = (): EngTab => {
  const t = String(route.query.tab ?? 'drawings')
  if (VALID_TABS.includes(t as EngTab)) return t as EngTab
  if (t === 'lookup') return 'drawings'
  return 'drawings'
}

const activeTab = ref<EngTab>(tabFromQuery())

watch(
  () => route.query.tab,
  () => {
    activeTab.value = tabFromQuery()
  },
)

function onTabChange(name: string | number) {
  const tab = String(name)
  if (route.query.tab !== tab) {
    router.replace({ query: { ...route.query, tab } })
  }
}
</script>

<style scoped>
.engineering-data-tabs {
  padding: 0 16px;
  background: var(--erp-bg-card, #fff);
}
</style>
