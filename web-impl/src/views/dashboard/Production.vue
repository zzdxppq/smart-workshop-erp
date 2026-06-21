<template>
  <div v-loading="loading" class="production-dashboard">
    <RoleWorkflowPanel />
    <div class="dash-header">
      <h2>生产驾驶舱</h2>
      <div class="header-controls">
        <el-select v-model="roleView" style="width: 180px" @change="refresh">
          <el-option label="生管-全公司" value="planner-all" />
          <el-option label="车间主管-本车间" value="supervisor-shop" />
        </el-select>
        <span class="live-indicator">
          <span class="live-dot" :class="{ online: kpiConnected }" />
          实时 {{ kpiConnected ? 'WS' : '轮询' }} 5s
        </span>
        <span class="shortcut-hint">N 新建 · A 待办 · R 刷新 · Esc 关闭</span>
      </div>
    </div>

    <el-row :gutter="12" class="kpi-row">
      <el-col v-for="k in kpis" :key="k.key" :span="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{ color: k.color }">{{ k.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never" header="待办 / 告警（PRD：审批·逾期·缺料·待检 · 限 5 条）">
          <el-table :data="todos.slice(0, 5)" size="small" stripe max-height="220">
            <el-table-column prop="workorderNo" label="工单/来源" width="140" />
            <el-table-column prop="alertMessage" label="事项" />
            <el-table-column label="级别" width="90">
              <template #default="{ row }"><ErpStatusTag :status="row.alertType" /></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="设备 / 人员（PRD E11-S1）" class="overview-card">
          <el-descriptions :column="1" border size="small" class="erp-descriptions">
            <el-descriptions-item label="设备稼动率">{{ overview.equipmentRate ?? stats.equipmentRate ?? 0 }}%</el-descriptions-item>
            <el-descriptions-item label="设备状态">{{ machineSummary.idle }} 空闲 / {{ machineSummary.busy }} 加工中</el-descriptions-item>
            <el-descriptions-item label="今日完工">{{ overview.todayFinished ?? stats.todayFinished ?? 0 }} 单</el-descriptions-item>
            <el-descriptions-item label="待处理异常">{{ overview.pendingAlerts ?? stats.pendingAlerts ?? 0 }} 条</el-descriptions-item>
            <el-descriptions-item label="人员出勤">{{ attendance.present }} / {{ attendance.total }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never" header="订单进度（PRD · 10 条）">
          <el-table :data="orderProgress" size="small" stripe max-height="240">
            <el-table-column prop="workorderNo" label="工单" width="120" />
            <el-table-column prop="productName" label="产品" show-overflow-tooltip />
            <el-table-column label="进度" width="90">
              <template #default="{ row }">{{ row.progress ?? 0 }}%</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><ErpStatusTag :status="row.workorderStatus" /></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="派工（待分配 · 近 3 天）">
          <el-table :data="dispatchList" size="small" stripe max-height="240">
            <el-table-column prop="workorderNo" label="工单" width="110" />
            <el-table-column prop="processName" label="工序" show-overflow-tooltip />
            <el-table-column prop="decision" label="归属" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="never" header="设备实时（PRD · 8 台）" class="equipment-card">
          <el-table :data="machines.slice(0, 8)" size="small" stripe class="equipment-table">
            <el-table-column prop="code" label="机台" min-width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="loadPercent" label="负荷" width="100">
              <template #default="{ row }">{{ row.loadPercent ?? row.oee ?? 0 }}%</template>
            </el-table-column>
            <el-table-column label="OEE" width="100">
              <template #default="{ row }">{{ row.oee ?? row.loadPercent ?? 0 }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top: 16px">
      <el-col v-for="col in kanbanColumns" :key="col.key" :span="6">
        <el-card shadow="never" :header="col.title">
          <draggable v-model="col.items" item-key="id" group="kanban" class="kanban-col">
            <template #item="{ element }">
              <div class="kanban-card" @click="openDrawer(element)">
                <div class="card-title">{{ element.workorderNo }}</div>
                <div class="card-sub">{{ element.materialCode }}</div>
                <el-progress :percentage="element.progress ?? 0" :stroke-width="6" />
              </div>
            </template>
          </draggable>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="异常事件流" style="margin-top: 16px" shadow="never">
      <el-table :data="events" stripe size="small">
        <el-table-column prop="time" label="时间" width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="dictTagType(EVENT_LEVEL_TAG, row.type)" size="small">
              {{ dictLabel(EVENT_LEVEL, row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="描述" />
      </el-table>
    </el-card>

    <el-drawer v-model="drawerOpen" title="工单详情" size="40%">
      <pre v-if="drawerItem">{{ JSON.stringify(drawerItem, null, 2) }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VueDraggableNext as draggable } from 'vue-draggable-next'
import { useDashboardStore } from '@/stores/dashboard'
import { useBaseStore } from '@/stores/_base'
import { useRealtimeChannel } from '@/composables/useRealtimeChannel'
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { normalizeMachineList } from '@/utils/productionApi'
import { dictLabel, dictTagType, EVENT_LEVEL, EVENT_LEVEL_TAG } from '@/utils/dictLabels'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

interface TodoRow {
  workorderNo?: string
  alertMessage?: string
  alertType?: string
}

interface OrderProgressRow {
  workorderNo?: string
  productName?: string
  progress?: number
  workorderStatus?: string
}

interface DispatchRow {
  workorderNo?: string
  processName?: string
  decision?: string
}

interface KanbanItem {
  id: number | string
  workorderNo?: string
  materialCode?: string
  progress?: number
  status?: string
}

interface DashEvent {
  time?: string
  type?: string
  message?: string
}

const router = useRouter()
const dashboardStore = useDashboardStore()
const loading = ref(false)
const roleView = ref('planner-all')
const drawerOpen = ref(false)
const drawerItem = ref<KanbanItem | null>(null)

const stats = ref<Record<string, unknown>>({})
const overview = ref<Record<string, unknown>>({})
const todos = ref<TodoRow[]>([])
const orderProgress = ref<OrderProgressRow[]>([])
const dispatchList = ref<DispatchRow[]>([])
const machines = ref<{ code: string; status?: string; loadPercent?: number; oee?: number }[]>([])
const attendance = ref({ present: 18, total: 20 })
const machineSummary = computed(() => {
  let idle = 0
  let busy = 0
  for (const m of machines.value) {
    const st = String(m.status ?? '').toUpperCase()
    if (st === 'RUNNING' || st === 'BUSY' || st === 'WORKING') busy += 1
    else idle += 1
  }
  if (!machines.value.length) return { idle: 8, busy: 2 }
  return { idle, busy }
})
const kanbanColumns = ref([
  { key: 'pending', title: '待排产', items: [] as KanbanItem[] },
  { key: 'active', title: '在产', items: [] as KanbanItem[] },
  { key: 'inspect', title: '待检', items: [] as KanbanItem[] },
  { key: 'alert', title: '异常', items: [] as KanbanItem[] },
])
const events = ref<DashEvent[]>([])

const kpis = computed(() => [
  { key: 'active', label: '在产工单', value: stats.value.activeWorkorders ?? 0, color: '#0969da' },
  { key: 'finished', label: '今日完工', value: stats.value.todayFinished ?? 0, color: '#1a7f37' },
  { key: 'rate', label: '设备稼动率', value: `${stats.value.equipmentRate ?? 0}%`, color: '#bf8700' },
  { key: 'alerts', label: '待处理异常', value: stats.value.pendingAlerts ?? 0, color: '#cf222e' },
])

function applyKanban(items: KanbanItem[]) {
  kanbanColumns.value[0].items = items.filter((i) => i.status === 'PENDING' || !i.status)
  kanbanColumns.value[1].items = items.filter((i) => i.status === 'IN_PROGRESS')
  kanbanColumns.value[2].items = items.filter((i) => i.status === 'INSPECT')
  kanbanColumns.value[3].items = items.filter((i) => i.status === 'ALERT')
}

function openDrawer(item: KanbanItem) {
  drawerItem.value = item
  drawerOpen.value = true
}

function parseEvents(r: unknown): DashEvent[] {
  const data = unwrapResult<{ events?: DashEvent[] }>(r)
  return data?.events ?? []
}

async function refresh() {
  loading.value = true
  try {
    stats.value = unwrapResult(await dashboardStore.loadProductionStats()) as Record<string, unknown>
    const ov = unwrapResult<{ overview?: Record<string, unknown> }>(
      await useBaseStore().api.get('/dashboard/production/overview'),
    )
    overview.value = ov.overview ?? {}
    const alertRows = unwrapResult<Record<string, unknown>[]>(await dashboardStore.loadProductionAlerts())
    todos.value = (Array.isArray(alertRows) ? alertRows : []).map((a) => ({
      time: String(a.snapshotAt ?? ''),
      type: String(a.alertType ?? 'WARN'),
      message: String(a.alertMessage ?? a.workorderNo ?? ''),
      workorderNo: String(a.workorderNo ?? ''),
      alertMessage: String(a.alertMessage ?? ''),
      alertType: String(a.alertType ?? ''),
    }))
    const woPage = parsePageItems(await dashboardStore.loadProductionWorkorders())
    const wo = woPage.items as KanbanItem[]
    applyKanban(wo)
    orderProgress.value = (woPage.items as Record<string, unknown>[]).slice(0, 10).map((r) => ({
      workorderNo: String(r.workorderNo ?? ''),
      productName: String(r.productName ?? r.materialCode ?? ''),
      progress: Number(r.progress ?? 0),
      workorderStatus: String(r.workorderStatus ?? r.status ?? ''),
    }))
    try {
      const pending = unwrapResult<DispatchRow[]>(
        await useBaseStore().api.get('/production/allocations/pending'),
      )
      dispatchList.value = (pending ?? []).slice(0, 10)
    } catch {
      dispatchList.value = wo.slice(0, 3).map((w) => ({
        workorderNo: w.workorderNo,
        processName: w.materialCode,
        decision: '自制',
      }))
    }
    try {
      const mRes = parsePageItems(await useBaseStore().api.get('/machines', { params: { pageNum: 1, pageSize: 20 } }))
      machines.value = normalizeMachineList(mRes.items as unknown[])
    } catch {
      machines.value = []
    }
    if (overview.value.attendancePresent != null) {
      attendance.value = {
        present: Number(overview.value.attendancePresent ?? 18),
        total: Number(overview.value.attendanceTotal ?? 20),
      }
    }
    events.value = parseEvents(await dashboardStore.loadProductionEvents())
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const { connected: kpiConnected } = useRealtimeChannel({
  channel: 'dashboard:kpi',
  pollUrl: '/dashboard/production',
  pollIntervalMs: 5000,
  onMessage: (data) => {
    const payload = unwrapResult<Record<string, unknown>>(data)
    if (payload) stats.value = payload
  },
})

useRealtimeChannel({
  channel: 'dashboard:kanban',
  pollUrl: '/dashboard/kanban',
  pollIntervalMs: 5000,
  onMessage: (data) => {
    const payload = unwrapResult<{ items?: KanbanItem[] }>(data)
    if (payload?.items) applyKanban(payload.items)
  },
})

useRealtimeChannel({
  channel: 'dashboard:events',
  pollUrl: '/dashboard/events',
  pollIntervalMs: 5000,
  onMessage: (data) => {
    events.value = parseEvents(data)
  },
})

useKeyboardShortcuts({
  refresh,
  newItem: () => router.push('/production/pending-production'),
  closeDrawer: () => { drawerOpen.value = false },
})

onMounted(refresh)
</script>

<style scoped>
.production-dashboard { padding: 16px; }
.dash-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-controls { display: flex; align-items: center; gap: 16px; }
.live-indicator { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.live-dot { width: 8px; height: 8px; border-radius: 50%; background: #cf222e; }
.live-dot.online { background: #1a7f37; }
.shortcut-hint { font-size: 12px; color: var(--erp-text-muted); }
.kpi-card { text-align: center; }
.kpi-label { font-size: 14px; color: var(--erp-text-secondary); }
.kpi-value { font-size: 32px; font-weight: 600; margin-top: 8px; }
.kanban-col { min-height: 120px; }
.kanban-card {
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid var(--erp-border);
  border-radius: 4px;
  cursor: pointer;
  background: var(--erp-bg-card);
}
.card-title { font-weight: 600; font-size: 13px; }
.card-sub { font-size: 12px; color: var(--erp-text-muted); margin: 4px 0; }
.equipment-card :deep(.el-card__body) { padding-top: 8px; }
.equipment-table { width: 100%; }
.overview-card :deep(.el-descriptions__body) {
  background: var(--erp-bg-card);
}
</style>
