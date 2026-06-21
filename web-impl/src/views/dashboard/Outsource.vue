<template>
  <ErpPageShell title="委外驾驶舱" description="E11-S4 · 7 状态分布、在途委外单与告警（PRD AC-11.4）。">
    <el-alert
      v-if="overdueCount > 0"
      type="error"
      :closable="false"
      :title="`已逾期 ${overdueCount} 张委外单`"
      description="剩余天数 &lt; 0 的单据已深红高亮，请优先跟进。"
      style="margin-bottom: 12px"
    />

    <DashboardKpiGrid :items="kpis" />

    <el-form :inline="true" class="erp-filter-bar" style="margin-top: 8px">
      <el-form-item label="厂商">
        <el-input v-model="filterVendor" clearable placeholder="厂商名称" style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filterStatus" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
        <el-button @click="exportCsv">导出 Excel</el-button>
        <span class="live-tag">
          <span class="live-dot" :class="{ online: wsConnected }" /> WS {{ wsConnected ? '已连接' : '轮询' }}
        </span>
      </el-form-item>
      <el-form-item v-if="filteredList.length !== list.length">
        <span class="filter-hint">已筛选 {{ filteredList.length }} / {{ list.length }} 张</span>
      </el-form-item>
    </el-form>

    <el-row :gutter="16" style="margin-top: 8px">
      <el-col :md="10">
        <el-card header="7 状态分布" shadow="never">
          <v-chart :option="statusChart" autoresize style="height: 280px" />
        </el-card>
      </el-col>
      <el-col :md="14">
        <el-card header="7 状态分组（可折叠）" shadow="never">
          <el-collapse v-model="expandedGroups">
            <el-collapse-item
              v-for="group in groupedList"
              :key="group.status"
              :title="`${group.status}（${group.rows.length}）`"
              :name="group.status"
            >
              <el-table
                :data="group.rows"
                stripe
                size="small"
                :row-class-name="tableRowClass"
                @row-click="openDrawer"
              >
                <el-table-column prop="outsourceNo" label="委外单号" min-width="130" />
                <el-table-column prop="vendorName" label="厂商" min-width="100" />
                <el-table-column prop="metricName" label="工序" width="90" />
                <el-table-column label="返修" width="70">
                  <template #default="{ row }">
                    <span :class="{ 'rework-blink': Number(row.reworkCount ?? 0) >= 2 }">
                      {{ row.reworkCount ?? 0 }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="剩余天" width="80">
                  <template #default="{ row }">
                    <span :class="rowClass(row)">{{ remainingDays(row) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="委外告警" shadow="never" style="margin-top: 16px">
      <el-table :data="alerts" stripe size="small">
        <el-table-column prop="outsourceNo" label="委外单" />
        <el-table-column prop="alertMessage" label="告警" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawerOpen" title="委外单详情" size="420px">
      <el-descriptions v-if="drawerRow" :column="1" border size="small">
        <el-descriptions-item label="委外单号">{{ drawerRow.outsourceNo }}</el-descriptions-item>
        <el-descriptions-item label="厂商">{{ drawerRow.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="String(drawerRow.status ?? '')" /></el-descriptions-item>
        <el-descriptions-item label="工序">{{ drawerRow.metricName }}</el-descriptions-item>
        <el-descriptions-item label="指标值">{{ drawerRow.metricValue }}</el-descriptions-item>
        <el-descriptions-item label="合格率">{{ drawerRow.qualityPassRate ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="告警级别"><ErpStatusTag :status="String(drawerRow.alertLevel ?? '')" /></el-descriptions-item>
        <el-descriptions-item label="快照时间">{{ drawerRow.snapshotAt ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useBaseStore } from '@/stores/_base'
import { useRealtimeChannel } from '@/composables/useRealtimeChannel'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const dashboardStore = useDashboardStore()
const api = useBaseStore().api
const stats = ref<Record<string, unknown>>({})
const list = ref<Record<string, unknown>[]>([])
const alerts = ref<Record<string, unknown>[]>([])
const statusDist = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const filterVendor = ref('')
const filterStatus = ref('')
const drawerOpen = ref(false)
const drawerRow = ref<Record<string, unknown> | null>(null)
const expandedGroups = ref<string[]>([])

const statusOptions = ['PENDING', 'IN_PROGRESS', 'DELAYED', 'INCOMING', 'COMPLETED', 'CANCELLED', 'REWORK']

const filteredList = computed(() => {
  return list.value.filter((row) => {
    if (filterVendor.value && !String(row.vendorName ?? '').includes(filterVendor.value)) return false
    if (filterStatus.value && row.status !== filterStatus.value) return false
    return true
  })
})

const groupedList = computed(() => {
  const map = new Map<string, Record<string, unknown>[]>()
  for (const row of filteredList.value) {
    const st = String(row.status ?? 'OTHER')
    if (!map.has(st)) map.set(st, [])
    map.get(st)!.push(row)
  }
  return Array.from(map.entries()).map(([status, rows]) => ({ status, rows }))
})

const overdueCount = computed(() =>
  list.value.filter((r) => remainingDays(r) < 0).length,
)

const kpis = computed(() => [
  { key: 'ip', label: '在途', value: stats.value.inProgress ?? 0 },
  { key: 'done', label: '已交付', value: stats.value.delivered ?? 0, color: '#1a7f37' },
  { key: 'delay', label: '延误', value: stats.value.delayed ?? overdueCount.value, color: '#cf222e' },
  { key: 'alert', label: '告警', value: stats.value.alertCount ?? alerts.value.length, color: '#bf8700' },
])

const statusChart = computed(() => {
  const textColor =
    typeof document !== 'undefined'
      ? getComputedStyle(document.documentElement).getPropertyValue('--erp-text-secondary').trim() || '#94a3b8'
      : '#94a3b8'
  const palette = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: textColor } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      data: statusDist.value.map((r, i) => ({
        name: String(r.status ?? ''),
        value: Number(r.cnt ?? 0),
        itemStyle: { color: palette[i % palette.length] },
      })),
    }],
  }
})

function remainingDays(row: Record<string, unknown>) {
  if (row.remainingDays != null) return Number(row.remainingDays)
  if (row.status === 'DELAYED') return -1
  if (row.alertLevel === 'CRITICAL') return 0
  return 3
}

function rowClass(row: Record<string, unknown>) {
  const days = remainingDays(row)
  if (days < 0) return 'row-critical'
  if (days <= 1) return 'row-warn'
  return ''
}

function tableRowClass({ row }: { row: Record<string, unknown> }) {
  const days = remainingDays(row)
  if (days < 0) return 'tr-overdue'
  if (days <= 1) return 'tr-urgent'
  if (row.status === 'PENDING_INSPECTION' || row.status === 'INCOMING') return 'tr-pending-inspect'
  return ''
}

function openDrawer(row: Record<string, unknown>) {
  drawerRow.value = row
  drawerOpen.value = true
}

async function load() {
  loading.value = true
  try {
    const r = unwrapResult<Record<string, unknown>>(await dashboardStore.loadOutsourceStats())
    applyPayload(r)
  } finally {
    loading.value = false
  }
}

function applyPayload(r: Record<string, unknown>) {
  stats.value = r
  list.value = (r.list as Record<string, unknown>[]) ?? []
  alerts.value = (r.alerts as Record<string, unknown>[]) ?? []
  statusDist.value = (r.statusDistribution as Record<string, unknown>[]) ?? []
  expandedGroups.value = [...new Set(list.value.map((row) => String(row.status ?? 'OTHER')))]
}

function exportCsv() {
  const params = new URLSearchParams()
  if (filterVendor.value) params.set('vendor', filterVendor.value)
  if (filterStatus.value) params.set('status', filterStatus.value)
  window.open(`/erp-business/dashboard/outsource/export?${params.toString()}`, '_blank')
}

const { connected: wsConnected } = useRealtimeChannel({
  channel: 'dashboard:outsource',
  pollUrl: '/dashboard/outsource',
  pollIntervalMs: 5000,
  onMessage: (data) => {
    const payload = unwrapResult<Record<string, unknown>>(data)
    if (payload) applyPayload(payload)
  },
})

onMounted(load)
</script>

<style scoped>
.row-critical { color: #cf222e; font-weight: 700; }
.row-warn { color: #bf8700; font-weight: 600; }
.filter-hint { font-size: 13px; color: var(--erp-text-secondary); }
.live-tag { display: inline-flex; align-items: center; gap: 6px; margin-left: 12px; font-size: 12px; }
.live-dot { width: 8px; height: 8px; border-radius: 50%; background: #cf222e; }
.live-dot.online { background: #1a7f37; }
.rework-blink { color: #cf222e; font-weight: 700; animation: blink 1s step-start infinite; }
@keyframes blink { 50% { opacity: 0.2; } }
:deep(.tr-overdue) { background-color: #ffebe9 !important; }
:deep(.tr-urgent) { background-color: #fff8c5 !important; }
:deep(.tr-pending-inspect td) { background-color: #fff8c5; }
</style>
