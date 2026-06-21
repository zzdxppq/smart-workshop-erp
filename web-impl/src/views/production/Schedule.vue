<template>
  <ErpPageShell
    title="排产看板"
    description="拖拽待排产工单到机台行完成分配；负荷 >80% 黄色、>100% 红色；交期最近工单置顶。"
  >
    <template #actions>
      <el-button class="erp-btn-secondary" :loading="batchLoading" :disabled="!selectedIds.length" @click="batchAutoSchedule">
        批量自动排产 ({{ selectedIds.length }})
      </el-button>
      <el-button class="erp-btn-secondary" @click="$router.push('/production/schedule-gantt')">甘特图</el-button>
    </template>

    <div class="schedule-board">
      <aside class="pool">
        <div class="pool-head">待排产工单池</div>
        <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选</el-checkbox>
        <draggable
          v-model="pendingWorkorders"
          item-key="id"
          group="workorders"
          class="pool-list"
          :sort="true"
        >
          <template #item="{ element }">
            <div
              class="wo-card"
              :class="{ overdue: isOverdue(element), selected: selectedIds.includes(element.id) }"
              @click.stop="toggleSelect(element.id)"
            >
              <el-checkbox :model-value="selectedIds.includes(element.id)" @change="toggleSelect(element.id)" @click.stop />
              <div>
                <div class="wo-no">{{ element.workorderNo }}</div>
                <div class="wo-sub">{{ element.productName ?? element.materialCode }}</div>
                <div class="wo-due">交期 {{ element.scheduledEnd ?? element.deliveryDate ?? '—' }}</div>
              </div>
            </div>
          </template>
        </draggable>
        <el-empty v-if="!pendingWorkorders.length" description="暂无待排产工单" />
      </aside>

      <section class="machines">
        <div v-for="m in machines" :key="m.id" class="machine-row">
          <div class="machine-info">
            <div class="machine-name">{{ m.code }} · {{ m.name }}</div>
            <MachineLoadBar :percent="Number(m.loadPercent ?? 0)" :machine-name="m.name" :warn-at="80" :danger-at="100" />
          </div>
          <draggable
            :list="machineJobs[m.id]"
            item-key="id"
            group="workorders"
            class="drop-zone"
            :class="loadTone(m.loadPercent)"
            @add="(evt) => onDropToMachine(m.id, evt)"
          >
            <template #item="{ element }">
              <div class="wo-chip">{{ element.workorderNo }}</div>
            </template>
            <template #footer>
              <span v-if="!machineJobs[m.id]?.length" class="drop-hint">拖入工单</span>
            </template>
          </draggable>
        </div>
      </section>
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VueDraggableNext as draggable } from 'vue-draggable-next'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import MachineLoadBar from '@/components/erp/MachineLoadBar.vue'
import { useWorkorderStore } from '@/stores/workorder'
import { useBaseStore } from '@/stores/_base'
import { normalizeMachineList, type MachineRowView } from '@/utils/productionApi'
import { parsePageItems } from '@/utils/apiPage'

const route = useRoute()
const workorderStore = useWorkorderStore()
const batchLoading = ref(false)
const pendingWorkorders = ref<Array<Record<string, unknown>>>([])
const machines = ref<MachineRowView[]>([])
const machineJobs = reactive<Record<number, Array<Record<string, unknown>>>>({})
const selectedIds = ref<number[]>([])
const selectAll = ref(false)

function isOverdue(wo: Record<string, unknown>) {
  const d = String(wo.scheduledEnd ?? wo.deliveryDate ?? '')
  if (!d) return false
  return new Date(d) < new Date()
}

function loadTone(p?: number) {
  const n = Number(p ?? 0)
  if (n > 100) return 'overload'
  if (n > 80) return 'warn'
  return ''
}

function toggleSelect(id: number) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll(v: boolean) {
  selectedIds.value = v ? pendingWorkorders.value.map((w) => Number(w.id)) : []
}

async function loadWorkorders() {
  const raw = await workorderStore.listWorkorders({ pageNum: 1, pageSize: 100 })
  const { items } = parsePageItems(raw)
  const list = (items as Array<Record<string, unknown>>).filter(
    (w) => !['COMPLETED', 'CANCELLED', 'CLOSED', 'IN_PROGRESS'].includes(String(w.status ?? '')),
  )
  list.sort((a, b) => {
    const da = new Date(String(a.scheduledEnd ?? a.deliveryDate ?? '9999')).getTime()
    const db = new Date(String(b.scheduledEnd ?? b.deliveryDate ?? '9999')).getTime()
    return da - db
  })
  pendingWorkorders.value = list
  const woId = route.query.woId
  if (woId) selectedIds.value = [Number(woId)]
}

async function loadMachines() {
  const raw = await useBaseStore().api.get('/machines', { params: { pageNum: 1, pageSize: 200 } })
  const { items } = parsePageItems(raw)
  machines.value = normalizeMachineList(items)
  for (const m of machines.value) {
    if (!machineJobs[m.id]) machineJobs[m.id] = []
  }
}

async function onDropToMachine(machineId: number, evt: { item?: { _underlying_vm_?: Record<string, unknown> } }) {
  const wo = evt.item?._underlying_vm_ ?? machineJobs[machineId]?.slice(-1)[0]
  if (!wo?.id) return
  const planStart = new Date()
  const planEnd = new Date(Date.now() + 8 * 3600 * 1000)
  try {
    await workorderStore.scheduleWorkorder(Number(wo.id), {
      equipmentId: machineId,
      planStart,
      planEnd,
    })
    ElMessage.success(`已排产到机台 ${machineId}`)
    pendingWorkorders.value = pendingWorkorders.value.filter((w) => w.id !== wo.id)
    await loadMachines()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '排产失败')
  }
}

async function batchAutoSchedule() {
  if (!selectedIds.value.length) return
  batchLoading.value = true
  try {
    let mi = 0
    for (const id of selectedIds.value) {
      const m = machines.value[mi % machines.value.length]
      if (!m) break
      await workorderStore.scheduleWorkorder(id, {
        equipmentId: m.id,
        planStart: new Date(),
        planEnd: new Date(Date.now() + 8 * 3600 * 1000),
      })
      mi++
    }
    ElMessage.success('批量排产完成')
    selectedIds.value = []
    await loadWorkorders()
    await loadMachines()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '批量排产失败')
  } finally {
    batchLoading.value = false
  }
}

onMounted(async () => {
  await loadMachines()
  await loadWorkorders()
})
</script>

<style scoped>
.schedule-board { display: flex; gap: 16px; min-height: 480px; }
.pool { width: 280px; flex-shrink: 0; border: 1px solid var(--erp-border); border-radius: 8px; padding: 12px; }
.pool-head { font-weight: 600; margin-bottom: 8px; }
.pool-list { min-height: 200px; }
.wo-card { display: flex; gap: 8px; padding: 8px; margin-bottom: 8px; border: 1px solid var(--erp-border); border-radius: 6px; cursor: grab; background: var(--erp-bg-elevated); }
.wo-card.overdue { border-color: #f56c6c; background: rgba(245, 108, 108, 0.08); }
.wo-no { font-weight: 600; font-family: monospace; }
.wo-sub, .wo-due { font-size: 12px; color: var(--erp-text-secondary); }
.machines { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.machine-row { display: flex; gap: 12px; align-items: stretch; border: 1px solid var(--erp-border); border-radius: 8px; padding: 10px; }
.machine-info { width: 200px; flex-shrink: 0; }
.machine-name { font-weight: 600; margin-bottom: 6px; }
.drop-zone { flex: 1; min-height: 48px; display: flex; flex-wrap: wrap; gap: 6px; align-items: center; padding: 8px; border: 2px dashed var(--erp-border); border-radius: 6px; }
.drop-zone.warn { border-color: #e6a23c; background: rgba(230, 162, 60, 0.06); }
.drop-zone.overload { border-color: #f56c6c; background: rgba(245, 108, 108, 0.08); }
.drop-hint { color: var(--erp-text-secondary); font-size: 12px; }
.wo-chip { padding: 4px 10px; background: rgba(59, 130, 246, 0.2); border-radius: 4px; font-size: 12px; font-family: monospace; }
</style>
