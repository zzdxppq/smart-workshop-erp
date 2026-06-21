<template>
  <div v-loading="loading" class="schedule-gantt">
    <div class="gantt-header">
      <h2>排产甘特</h2>
      <div class="header-right">
        <span class="live-dot" :class="{ online: wsConnected }" />
        <el-button @click="refresh">R 刷新</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSchedule">确认排产</el-button>
      </div>
    </div>

    <el-alert v-if="!jobs.length && !loading" type="info" :closable="false" show-icon title="如何排产？"
      description="1) 先在「排产看板」将工单拖到机台；2) 本页可微调时间轴色块位置；3) 点击「确认排产」下发车间；4) 操作工 APP 扫 GD- 开工。"
      style="margin-bottom: 12px" />

    <div v-if="!jobs.length && !loading" class="gantt-empty">
      <p>当前无排产任务 · 请从排产看板拖入工单，或点击刷新加载已有计划</p>
      <el-button type="primary" @click="$router.push('/production/schedule')">前往排产看板</el-button>
    </div>

    <div v-else class="gantt-grid">
      <div class="gantt-sidebar">
        <div class="sidebar-head">机台</div>
        <div v-for="m in machines" :key="m.id" class="machine-row">
          <div class="machine-name">{{ m.name }}</div>
          <MachineLoadBar :percent="m.loadPercent" :machine-name="m.name" @click="selectMachine(m.id)" />
        </div>
      </div>
      <div class="gantt-timeline">
        <div class="timeline-head">
          <span v-for="h in hours" :key="h" class="hour-cell">{{ h }}:00</span>
        </div>
        <draggable
          v-model="jobs"
          item-key="id"
          group="schedule"
          class="jobs-layer"
          @change="onDragChange"
        >
          <template #item="{ element }">
            <div
              class="job-card"
              :class="{ conflict: element.conflict, selected: element.machineId === selectedMachineId }"
              :style="jobStyle(element)"
              :title="`工单: ${element.workorderNo}\n产品: ${element.productName || '-'}\n数量: ${element.qty || '-'}\n交期: ${element.deliveryDate || '-'}`"
            >
              <span class="job-no">{{ shortWorkorderNo(element.workorderNo) }}</span>
            </div>
          </template>
        </draggable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { VueDraggableNext as draggable } from 'vue-draggable-next'
import { E5MachineService } from '@/api/generated/services/E5MachineService'
import { useWorkorderStore } from '@/stores/workorder'
import { useBaseStore } from '@/stores/_base'
import { useRealtimeChannel } from '@/composables/useRealtimeChannel'
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'
import { unwrapResult, parsePageItems } from '@/utils/apiPage'

interface ScheduleJob {
  id: string
  workorderNo: string
  machineId: number
  startHour: number
  duration: number
  conflict?: boolean
  productName?: string
  qty?: number
  deliveryDate?: string
  [key: string]: unknown
}

import { normalizeMachineRow, type MachineRowView } from '@/utils/productionApi'

const loading = ref(false)
const saving = ref(false)
const machines = ref<MachineRowView[]>([
  { id: 1, code: 'SB-CNC-001', name: 'CNC-01', loadPercent: 55, oee: 55 },
  { id: 2, code: 'SB-LATHE-001', name: 'CNC-02', loadPercent: 78, oee: 78 },
  { id: 3, code: 'SB-MILL-001', name: 'CNC-03', loadPercent: 92, oee: 92 },
])
const jobs = ref<ScheduleJob[]>([
  { id: '1', workorderNo: 'GD20260615-0001', machineId: 1, startHour: 8, duration: 2 },
  { id: '2', workorderNo: 'GD20260615-0002', machineId: 2, startHour: 10, duration: 3 },
])
const selectedMachineId = ref<number>()
const hours = computed(() => Array.from({ length: 12 }, (_, i) => i + 8))

const workorderStore = useWorkorderStore()
const api = useBaseStore().api

const { connected: wsConnected, refresh } = useRealtimeChannel({
  channel: 'schedule:machine',
  pollUrl: '/production/schedule',
  pollIntervalMs: 5000,
  onMessage: (data) => {
    const payload = data as { machines?: Record<string, unknown>[]; jobs?: ScheduleJob[] }
    if (payload.machines?.length) {
      machines.value = payload.machines.map((m, i) => normalizeMachineRow(m, i))
    }
    if (payload.jobs) jobs.value = payload.jobs
  },
})

function jobStyle(job: ScheduleJob) {
  return {
    left: `${(job.startHour - 8) * 80}px`,
    width: `${job.duration * 80}px`,
    top: `${(job.machineId - 1) * 56 + 8}px`,
  }
}

function selectMachine(id: number) {
  selectedMachineId.value = id
}

function shortWorkorderNo(woNo: string) {
  if (!woNo) return '-'
  // 保留后6位或完整前缀
  if (woNo.length <= 8) return woNo
  return woNo.slice(-6)
}

function onDragChange() {
  jobs.value = jobs.value.map((j) => ({ ...j, conflict: detectConflict(j) }))
}

function detectConflict(job: ScheduleJob) {
  return jobs.value.some(
    (o) => o.id !== job.id && o.machineId === job.machineId
      && Math.abs(o.startHour - job.startHour) < Math.min(o.duration, job.duration),
  )
}

async function loadMachines() {
  loading.value = true
  try {
    const r = unwrapResult(await E5MachineService.listMachines())
    const list = parsePageItems(r).items as Record<string, unknown>[]
    if (list.length) {
      machines.value = list.map((m, i) => normalizeMachineRow(m, i))
    }
  } catch {
    /* 使用演示数据 */
  } finally {
    loading.value = false
  }
}

async function confirmSchedule() {
  if (!jobs.value.length) {
    ElMessage.warning('当前没有待确认的排产任务')
    return
  }
  saving.value = true
  try {
    // 检查是否有冲突
    const conflicts = jobs.value.filter((j) => j.conflict)
    if (conflicts.length > 0) {
      ElMessage.error(`存在 ${conflicts.length} 个排产冲突，请先解决冲突后再确认`)
      return
    }
    // 逐个保存工单排产
    for (const job of jobs.value) {
      await workorderStore.scheduleWorkorder(Number(job.id), {
        equipmentId: job.machineId,
        planStart: new Date(new Date().toDateString() + ` ${job.startHour}:00`),
        planEnd: new Date(new Date().toDateString() + ` ${job.startHour + job.duration}:00`),
      })
    }
    await api.post('/production/schedule/save', { jobs: jobs.value })
    ElMessage.success(`已确认 ${jobs.value.length} 个排产任务，状态已锁定为「已排产」`)
    await refresh()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '确认排产失败')
  } finally {
    saving.value = false
  }
}

useKeyboardShortcuts({ refresh: () => refresh(), newItem: () => jobs.value.push({
  id: String(Date.now()),
  workorderNo: 'GD-NEW',
  machineId: 1,
  startHour: 8,
  duration: 2,
}) })

onMounted(loadMachines)
</script>

<style scoped>
.schedule-gantt { padding: 16px; }
.gantt-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.live-dot { width: 10px; height: 10px; border-radius: 50%; background: #cf222e; }
.live-dot.online { background: #1a7f37; animation: pulse 5s infinite; }
@keyframes pulse { 50% { opacity: 0.5; } }
.gantt-grid { display: flex; border: 1px solid var(--erp-border); border-radius: 4px; overflow: hidden; }
.gantt-sidebar { width: 200px; border-right: 1px solid var(--erp-border); background: var(--erp-bg-card); }
.sidebar-head, .timeline-head { height: 40px; line-height: 40px; padding: 0 12px; font-weight: 600; border-bottom: 1px solid var(--erp-border); }
.machine-row { padding: 8px 12px; height: 56px; border-bottom: 1px solid var(--erp-border); }
.machine-name { font-size: 13px; margin-bottom: 4px; }
.gantt-timeline { flex: 1; position: relative; min-height: 220px; overflow-x: auto; }
.timeline-head { display: flex; }
.hour-cell { width: 80px; flex-shrink: 0; font-size: 12px; color: var(--erp-text-muted); }
.jobs-layer { position: relative; min-height: 180px; }
.job-card {
  position: absolute;
  height: 40px;
  line-height: 40px;
  padding: 0 8px;
  background: var(--erp-color-primary-light);
  border: 1px solid var(--erp-color-primary);
  border-radius: 4px;
  font-size: 12px;
  cursor: grab;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.job-no {
  font-family: monospace;
  font-weight: 600;
  font-size: 11px;
}
.job-card.conflict { border-color: var(--erp-color-danger); background: #ffebe9; }
.job-card.selected { box-shadow: 0 0 0 2px var(--erp-color-primary); }
</style>
