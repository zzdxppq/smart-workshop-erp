<template>
  <ErpPageShell
    title="审批事件统计"
    description="查看各业务审批事件分布、趋势与异常率，仅总经理与管理员可访问。"
  >
    <div data-testid="workflow-stats-page">
    <!-- 权限不足：SALES 等角色被端点 403 拦截 -->
    <el-alert
      v-if="permissionDenied"
      type="error"
      :closable="false"
      title="权限不足"
      description="该页面仅 GM + ADMIN 可访问。SALES/PROCUREMENT_MANAGER 等角色请使用各业务模块自带审批视图。"
      show-icon
      data-testid="workflow-stats-permission-alert"
      style="margin-bottom: 16px"
    />

    <!-- 顶部过滤器 -->
    <el-form :inline="true" :model="filters" class="filter-form" data-testid="workflow-stats-filters">
      <el-form-item label="工作流">
        <el-select
          v-model="filters.workflowCode"
          placeholder="全部"
          clearable
          style="width: 200px"
          data-testid="filter-workflow-code"
        >
          <el-option label="PO 审批 PO_APPROVAL" value="PO_APPROVAL" />
          <el-option label="报价审批 QUOTE_APPROVAL" value="QUOTE_APPROVAL" />
          <el-option label="委外审批 OUTSOURCE_APPROVAL" value="OUTSOURCE_APPROVAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="filters.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="默认 7 天前"
          data-testid="filter-start-date"
        />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker
          v-model="filters.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="默认今天"
          data-testid="filter-end-date"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="onQuery" :loading="loading" data-testid="filter-query-btn">
          查询
        </el-button>
        <el-button :icon="Refresh" @click="onReset" data-testid="filter-reset-btn">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 加载骨架 -->
    <el-skeleton v-if="loading" :rows="6" animated data-testid="workflow-stats-skeleton" />

    <!-- 错误提示 -->
    <el-alert
      v-else-if="error"
      type="warning"
      :title="error"
      :closable="false"
      show-icon
      data-testid="workflow-stats-error-alert"
      style="margin-bottom: 16px"
    />

    <!-- 数字卡 + 4 Tab 切换 + 4 图 -->
    <template v-else-if="stats">
      <el-row :gutter="16" class="metric-row">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card" data-testid="metric-total-count">
            <div class="metric-label">总审批事件</div>
            <div class="metric-value">{{ stats.totalCount ?? 0 }}</div>
            <div class="metric-sub">events</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card" data-testid="metric-approver-count">
            <div class="metric-label">涉及审批角色</div>
            <div class="metric-value">{{ approverRoleCount }}</div>
            <div class="metric-sub">roles</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card" data-testid="metric-event-type-count">
            <div class="metric-label">涉及事件类型</div>
            <div class="metric-value">{{ eventTypeCount }}</div>
            <div class="metric-sub">types</div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card shadow="hover" class="metric-card" data-testid="metric-rejected-count">
            <div class="metric-label">驳回事件数</div>
            <div class="metric-value metric-value--danger">{{ rejectedCount }}</div>
            <div class="metric-sub">REJECTED</div>
          </el-card>
        </el-col>
      </el-row>

      <el-tabs v-model="activeTab" class="chart-tabs" data-testid="workflow-stats-tabs">
        <el-tab-pane label="按审批角色工作量" name="workload">
          <WorkloadByApprover :stats="stats" height="380px" />
        </el-tab-pane>
        <el-tab-pane label="按事件类型分布" name="event-type">
          <EventTypeDistribution :stats="stats" height="380px" />
        </el-tab-pane>
        <el-tab-pane label="审批趋势" name="trend">
          <ApprovalDurationLine :stats="stats" height="380px" />
        </el-tab-pane>
        <el-tab-pane label="异常率（驳回占比）" name="anomaly">
          <AnomalyRateGauge :stats="stats" height="380px" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 初始空状态 -->
    <el-empty
      v-else
      description="点击查询加载审批事件统计"
      data-testid="workflow-stats-empty"
    />
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useWorkflowStats } from '@/composables/useWorkflowStats'
import { useAuthStore } from '@/stores/auth'
import WorkloadByApprover from '@/components/charts/WorkloadByApprover.vue'
import EventTypeDistribution from '@/components/charts/EventTypeDistribution.vue'
import ApprovalDurationLine from '@/components/charts/ApprovalDurationLine.vue'
import AnomalyRateGauge from '@/components/charts/AnomalyRateGauge.vue'

/**
 * V1.3.9 Sprint 13 · Story 13.4 · sys_workflow_event 仪表盘顶层容器
 *
 * 路由: /reports/workflow-stats (GM + ADMIN only)
 * 数据源: Sprint 10.3 `GET /workflow/events/stats`
 *
 * IMPL 注意事项（arch REVIEW §6）：
 * 1. mounted 时调 10.3（不要在 setup 阶段）
 * 2. ECharts option 用 shallowRef 包裹（4 子组件已遵循）
 * 3. onUnmounted 销毁 ECharts 实例（4 子组件已遵循）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const { stats, loading, error, fetchStats } = useWorkflowStats()

const activeTab = ref<string>('workload')
const permissionDenied = ref<boolean>(false)

/**
 * 过滤器默认值：startDate = 7 天前 · endDate = 今天
 * 与 architect REVIEW §2.4 默认值一致
 */
function defaultFilters() {
  const end = new Date()
  const start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
  return {
    workflowCode: undefined as string | undefined,
    approverRole: undefined as string | undefined,
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10),
  }
}

const filters = ref(defaultFilters())

const approverRoleCount = computed(() => Object.keys(stats.value?.byApproverRole || {}).length)
const eventTypeCount = computed(() => Object.keys(stats.value?.byEventType || {}).length)
const rejectedCount = computed(() => {
  const m = stats.value?.byEventType || {}
  return (m['REJECTED'] as number) || 0
})

async function onQuery() {
  permissionDenied.value = false
  // IMPL 注意 6.1：fetchStats 必须在 mounted 后的用户事件中调用（非 setup 阶段）
  // 这里的 onQuery 是用户点击事件 · 完全合规
  await fetchStats({
    workflowCode: filters.value.workflowCode,
    approverRole: filters.value.approverRole,
    startDate: filters.value.startDate,
    endDate: filters.value.endDate,
  })
  if (error.value && /403|GM_OR_ADMIN|权限|forbidden|denied/i.test(error.value)) {
    permissionDenied.value = true
  }
  // URL query 同步（用于深链接 + 浏览器后退/前进）
  router.replace({ query: { ...route.query, tab: activeTab.value } })
}

function onReset() {
  filters.value = defaultFilters()
  onQuery()
}

/**
 * IMPL 注意事项 6.1：mounted 时调 10.3（不要在 setup 阶段）
 *
 * 默认拉取：7 天范围 · 不传 workflowCode
 * 端点 backend 10.3 强约束 workflowCode 必填 · composable 默认落 PO_APPROVAL
 * 后续用户切换 workflowCode/日期范围 触发 onQuery 重拉
 */
onMounted(() => {
  // 深链接 ?tab=xxx 支持
  const tabQuery = route.query.tab
  if (typeof tabQuery === 'string' && ['workload', 'event-type', 'trend', 'anomaly'].includes(tabQuery)) {
    activeTab.value = tabQuery
  }
  // 默认角色校验（前端 meta + 后端 @PreAuthorize 双层）
  if (auth.user && !auth.hasRole('GM') && !auth.hasRole('ADMIN')) {
    permissionDenied.value = true
    ElMessage.warning('该页面仅 GM + ADMIN 可访问')
  }
  onQuery()
})
</script>

<style scoped>
.workflow-stats {
  padding: 20px;
}
.description {
  color: #909399;
  margin-bottom: 16px;
  font-size: 13px;
}
.description code {
  background: var(--erp-bg-muted);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  color: var(--erp-color-primary);
}
.filter-form {
  background: var(--erp-bg-card);
  padding: 16px;
  border: 1px solid var(--erp-border);
  border-radius: var(--erp-radius-md);
  margin-bottom: 16px;
}
.metric-row {
  margin-bottom: 16px;
}
.metric-card {
  text-align: center;
  background: var(--erp-bg-card);
  border: 1px solid var(--erp-border);
}
.metric-label {
  font-size: 13px;
  color: var(--erp-text-secondary);
  margin-bottom: 8px;
}
.metric-value {
  font-size: 32px;
  font-weight: 600;
  color: var(--erp-text-primary);
  line-height: 1.2;
}
.metric-value--danger {
  color: var(--erp-color-danger);
}
.metric-sub {
  font-size: 11px;
  color: var(--erp-text-muted);
  margin-top: 4px;
}
.chart-tabs {
  background: var(--erp-bg-card);
  border: 1px solid var(--erp-border);
  border-radius: var(--erp-radius-md);
  padding: 16px;
}
</style>
