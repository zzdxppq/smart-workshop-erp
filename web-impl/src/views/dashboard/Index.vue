<template>
  <ErpPageShell title="工作台" description="今日待办与关键业务入口。">
    <RoleWorkflowPanel />

    <el-row :gutter="16">
      <el-col
        v-for="entry in visibleEntries"
        :key="entry.path"
        :xs="24"
        :sm="12"
        :md="8"
      >
        <el-card shadow="hover" class="entry-card" @click="$router.push(entry.path)">
          <div class="entry-icon" :class="entry.cssClass">
            <el-icon :size="28"><component :is="entry.icon" /></el-icon>
          </div>
          <h3>{{ entry.title }}</h3>
          <p>{{ entry.description }}</p>
        </el-card>
      </el-col>
      <el-col v-if="showTodoStat" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <h3>今日待办</h3>
          <p class="stat-value">{{ summary?.todoCount ?? 0 }}</p>
          <span class="stat-label">条待处理事项</span>
        </el-card>
      </el-col>
      <el-col v-if="showProductionStat" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="stat-card">
          <h3>本周生产</h3>
          <p class="stat-value">{{ summary?.productionCount ?? 0 }}</p>
          <span class="stat-label">张在制工单</span>
        </el-card>
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, type Component } from 'vue'
import { Odometer, DataAnalysis, TrendCharts, Wallet, ShoppingCart, Bell, Document, Box } from '@element-plus/icons-vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import { useBaseStore } from '@/stores/_base'
import { useAuthStore } from '@/stores/auth'
import { useDashboardStat } from '@/composables/useDashboardData'
import { canAccessRoute } from '@/utils/menuAccess'

interface DashboardEntry {
  path: string
  title: string
  description: string
  icon: Component
  cssClass: string
}

const DASHBOARD_ENTRIES: DashboardEntry[] = [
  {
    path: '/dashboard/index',
    title: '总览驾驶舱',
    description: '各角色驾驶舱入口与今日待办',
    icon: Odometer,
    cssClass: 'production',
  },
  {
    path: '/dashboard/production',
    title: '生产驾驶舱',
    description: '查看产线状态、工单进度与异常告警',
    icon: Odometer,
    cssClass: 'production',
  },
  {
    path: '/dashboard/performance-board',
    title: '绩效驾驶舱',
    description: '操作工/机台产量排行、合格率与 30 天趋势',
    icon: TrendCharts,
    cssClass: 'performance',
  },
  {
    path: '/dashboard/sales',
    title: '销售驾驶舱',
    description: 'V2.1 · 报价流程 → 图纸库建单；订单漏斗、待办与龙虎榜',
    icon: TrendCharts,
    cssClass: 'sales',
  },
  {
    path: '/dashboard/finance',
    title: '财务驾驶舱',
    description: '收入成本、应收与毛利率概览',
    icon: Wallet,
    cssClass: 'finance',
  },
  {
    path: '/dashboard/quality',
    title: '品质驾驶舱',
    description: '来料检、过程检与不良品分析',
    icon: Document,
    cssClass: 'quality',
  },
  {
    path: '/dashboard/outsource',
    title: '委外驾驶舱',
    description: '委外进度、质量与告警分布',
    icon: DataAnalysis,
    cssClass: 'outsource',
  },
  {
    path: '/dashboard/procurement',
    title: '采购驾驶舱',
    description: '询价、PO 执行、待委外与到货提醒',
    icon: ShoppingCart,
    cssClass: 'procurement',
  },
  {
    path: '/dashboard/alerts',
    title: '总经理驾驶舱',
    description: '利润率预警、关键异常与经营告警',
    icon: Bell,
    cssClass: 'gm',
  },
  {
    path: '/dashboard/multi',
    title: '多维度驾驶舱',
    description: '销售 / 生产 / 财务 / 品质四域聚合',
    icon: DataAnalysis,
    cssClass: 'multi',
  },
  {
    path: '/dashboard/engineer',
    title: '工程师驾驶舱',
    description: '报价工艺定义、订单工程转化与待办任务',
    icon: Document,
    cssClass: 'engineer',
  },
  {
    path: '/dashboard/warehouse',
    title: '仓管驾驶舱',
    description: '到货扫码、入库出库、库存与批次',
    icon: Box,
    cssClass: 'warehouse',
  },
]

const auth = useAuthStore()

const visibleEntries = computed(() =>
  DASHBOARD_ENTRIES.filter((entry) =>
    canAccessRoute(entry.path, auth.userRoles, auth.menuPaths),
  ),
)

const showProductionStat = computed(() =>
  canAccessRoute('/dashboard/production', auth.userRoles, auth.menuPaths),
)

const showTodoStat = computed(() => visibleEntries.value.length > 0 || showProductionStat.value)

const { data: summary, load } = useDashboardStat<{
  todoCount?: number
  productionCount?: number
  salesAmount?: number
}>(() => useBaseStore().api.get('/dashboard/index'))

onMounted(load)
</script>

<style scoped>
.entry-card {
  cursor: pointer;
  margin-bottom: 16px;
  transition: transform 0.15s, box-shadow 0.15s;
}

.entry-card:hover {
  transform: translateY(-2px);
}

.entry-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  color: #fff;
}

.entry-icon.production {
  background: linear-gradient(135deg, var(--erp-color-primary), var(--erp-color-primary-dark));
}

.entry-icon.sales {
  background: linear-gradient(135deg, #0ea5e9, #0369a1);
}

.entry-icon.finance {
  background: linear-gradient(135deg, #10b981, #047857);
}

.entry-icon.procurement {
  background: linear-gradient(135deg, #f59e0b, #b45309);
}

.entry-icon.gm {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
}

.entry-icon.multi {
  background: linear-gradient(135deg, #6366f1, #4338ca);
}

.entry-icon.engineer {
  background: linear-gradient(135deg, #14b8a6, #0d9488);
}

.entry-icon.warehouse {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
}

.entry-icon.quality {
  background: linear-gradient(135deg, #ec4899, #be185d);
}

.entry-icon.outsource {
  background: linear-gradient(135deg, #f97316, #c2410c);
}

.entry-card h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.entry-card p {
  margin: 0;
  font-size: 13px;
  color: var(--erp-text-secondary);
}

.stat-card {
  margin-bottom: 16px;
}

.stat-card h3 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--erp-text-secondary);
}

.stat-value {
  margin: 0;
  font-size: 32px;
  font-weight: 600;
  color: var(--erp-color-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--erp-text-muted);
}
</style>
