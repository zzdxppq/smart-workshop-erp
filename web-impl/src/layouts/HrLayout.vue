<template>
  <ErpPageShell title="人事管理" description="员工档案 · 考勤月报 · 薪酬核算 · 绩效 · 招聘（WiFi/蓝牙打卡请用 Android APP）。">
    <nav class="hr-tabs">
      <router-link
        v-for="t in tabs"
        :key="t.path"
        :to="t.path"
        class="hr-tab"
        :class="{ active: isActive(t.path) }"
      >
        {{ t.label }}
      </router-link>
    </nav>
    <div class="hr-content">
      <router-view />
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'

const route = useRoute()

const tabs = [
  { label: '员工列表', path: '/hr/employees' },
  { label: '系统账号', path: '/hr/accounts' },
  { label: '考勤月报', path: '/hr/attendance' },
  { label: '薪酬核算', path: '/hr/payroll' },
  { label: '绩效管理', path: '/hr/performance' },
  { label: '招聘管理', path: '/hr/recruitment' },
]

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<style scoped>
.hr-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--erp-border);
}

.hr-tab {
  padding: 8px 16px;
  border-radius: var(--erp-radius-md);
  font-size: 14px;
  color: var(--erp-text-secondary);
  text-decoration: none;
  border: 1px solid transparent;
  transition: color 0.15s, background 0.15s, border-color 0.15s;
}

.hr-tab:hover {
  color: var(--erp-color-primary);
  background: var(--erp-color-primary-light);
}

.hr-tab.active {
  color: var(--erp-color-primary);
  font-weight: 600;
  background: var(--erp-color-primary-light);
  border-color: rgba(59, 130, 246, 0.35);
}

.hr-content {
  min-height: 320px;
}
</style>
