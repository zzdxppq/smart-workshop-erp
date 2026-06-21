<template>
  <el-card v-if="guide" shadow="never" class="role-workflow-panel">
    <template #header>
      <div class="panel-header">
        <div>
          <span class="role-badge">{{ guide.roleLabel }}</span>
          <span class="endpoint-tag">{{ guide.endpoint }}</span>
        </div>
        <el-tag type="info" size="small">全链路 · PRD V1.3.8</el-tag>
      </div>
    </template>

    <div class="goal-block">
      <strong>核心职责：</strong>{{ guide.goal }}
    </div>

    <el-table :data="guide.steps" size="small" stripe class="steps-table">
      <el-table-column prop="step" label="步骤" width="72" />
      <el-table-column prop="action" label="主要操作" min-width="280" />
      <el-table-column label="" width="80" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.route"
            link
            type="primary"
            size="small"
            @click="$router.push(row.route)"
          >
            前往
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="quick-routes">
      <span class="quick-label">快捷入口：</span>
      <el-button
        v-for="r in visibleQuickRoutes"
        :key="r.path"
        size="small"
        @click="$router.push(r.path)"
      >
        {{ r.label }}
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveGuideForDashboard } from '@/utils/roleWorkflowGuide'
import { canAccessRoute } from '@/utils/menuAccess'

const props = defineProps<{
  /** 显式指定角色指南（优先于路由推断） */
  roleKey?: string
}>()

const route = useRoute()
const auth = useAuthStore()
const guide = computed(() =>
  resolveGuideForDashboard(route.path, auth.userRoles, props.roleKey),
)

const visibleQuickRoutes = computed(() =>
  (guide.value?.quickRoutes ?? []).filter((r) =>
    canAccessRoute(r.path, auth.userRoles, auth.menuPaths),
  ),
)
</script>

<style scoped>
.role-workflow-panel {
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.role-badge {
  font-weight: 600;
  font-size: 15px;
  margin-right: 8px;
}

.endpoint-tag {
  font-size: 12px;
  color: var(--erp-text-muted);
  background: var(--erp-bg-muted, #f6f8fa);
  padding: 2px 8px;
  border-radius: 4px;
}

.goal-block {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: var(--erp-color-primary-light);
  color: var(--erp-text-primary);
  border: 1px solid var(--erp-border);
  border-radius: var(--erp-radius-md);
  font-size: 13px;
  line-height: 1.5;
}

.steps-table {
  margin-bottom: 12px;
}

.quick-routes {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.quick-label {
  font-size: 13px;
  color: var(--erp-text-secondary);
}
</style>
