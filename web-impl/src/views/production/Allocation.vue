<template>
  <div class="allocation-page">
    <h2>工序分配</h2>

    <!-- 流程进度指引 -->
    <div class="step-guide">
      <div class="guide-item active">
        <div class="guide-num">1</div>
        <div class="guide-text">工序分配<br/><small>勾选本厂/委外</small></div>
      </div>
      <div class="guide-arrow">→</div>
      <div class="guide-item">
        <div class="guide-num">2</div>
        <div class="guide-text">排产<br/><small>本厂工序进排产池</small></div>
      </div>
      <div class="guide-arrow">→</div>
      <div class="guide-item">
        <div class="guide-num">3</div>
        <div class="guide-text">开工<br/><small>操作工扫码</small></div>
      </div>
      <div class="guide-arrow">→</div>
      <div class="guide-item">
        <div class="guide-num">4</div>
        <div class="guide-text">完工报工</div>
      </div>
    </div>

    <el-alert v-if="submittedCount > 0" type="success" :closable="true" show-icon style="margin-top: 12px">
      <template #title>分配完成 · {{ submittedCount }} 道工序已保存</template>
      <template v-if="hasOutsource">委外工序已推送采购「待委外清单」</template>
      <template v-if="hasInhouse">本厂工序已进入待排产池</template>
      <template v-if="hasOutsource"> · </template>
      <el-button type="warning" size="small" @click="goSchedule">前往排产 →</el-button>
      <template v-if="hasOutsource">
        <el-button type="info" size="small" @click="goOutsource">查看委外清单</el-button>
      </template>
    </el-alert>

    <el-alert type="warning" :closable="false" show-icon style="margin-top: 12px">
      <template #title>生管职责（PRD E5-S4 · V1.3.7）</template>
      在下方<strong>工单工序清单</strong>中勾选每道工序「本厂 / 委外」，<strong>不选厂商</strong>。
      委外工序提交后进入采购「待委外清单」。
    </el-alert>

    <el-form label-width="80px" style="margin-top: 16px; max-width: 520px">
      <el-form-item label="工单" required>
        <WorkorderSelect v-model="workorderId" @change="onWorkorderPick" />
      </el-form-item>
    </el-form>

    <template v-if="workorderId">
      <el-descriptions v-if="board" :column="4" border size="small" class="erp-descriptions board-desc" style="margin-bottom: 12px">
        <el-descriptions-item label="工单号">{{ board.workorderNo }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ board.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="待分配">
          <el-tag type="warning">{{ board.pendingCount }} 道</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="已分配">
          <el-tag type="success">{{ board.allocatedCount }} 道</el-tag>
          <el-tag v-if="board.outsourcePendingForBuyer" type="danger" style="margin-left: 6px">
            待采购 {{ board.outsourcePendingForBuyer }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-card v-loading="loading" shadow="never">
        <template #header>
          <div class="card-head">
            <span>工序清单 · 勾选归属后提交</span>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!dirtyCount"
              @click="submitBatch"
            >
              提交工序划分（{{ dirtyCount }} 项变更）
            </el-button>
          </div>
        </template>

        <h4 class="section-title">
          <el-tag type="warning" effect="plain">待分配</el-tag>
          <span class="hint">— 尚未划分厂内/委外，请优先处理</span>
        </h4>
        <el-table
          v-if="pendingRows.length"
          :data="pendingRows"
          stripe
          size="small"
          class="pending-table"
          empty-text="暂无待分配工序"
        >
          <el-table-column prop="processSeq" label="序号" width="70" />
          <el-table-column prop="stepName" label="工序名称" min-width="140" />
          <el-table-column prop="equipmentType" label="设备类型" width="110" />
          <el-table-column label="归属" min-width="180">
            <template #default="{ row }">
              <el-radio-group v-model="draft[row.processSeq]" size="small">
                <el-radio value="INHOUSE">本厂</el-radio>
                <el-radio value="OUTSOURCE">委外</el-radio>
              </el-radio-group>
              <span v-if="!draft[row.processSeq]" class="pick-hint">请选择</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="本工单工序均已分配" :image-size="64" />

        <h4 class="section-title" style="margin-top: 20px">
          <el-tag type="success" effect="plain">已分配</el-tag>
          <span class="hint">— 可调整归属（采购已选厂商的工序不可改）</span>
        </h4>
        <el-table
          v-if="allocatedRows.length"
          :data="allocatedRows"
          stripe
          size="small"
        >
          <el-table-column prop="processSeq" label="序号" width="70" />
          <el-table-column prop="stepName" label="工序名称" min-width="140" />
          <el-table-column label="当前归属" width="100">
            <template #default="{ row }">
              <el-tag :type="row.decision === 'OUTSOURCE' ? 'warning' : 'info'">
                {{ row.decision === 'OUTSOURCE' ? '委外' : '本厂' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="采购状态" width="120">
            <template #default="{ row }">
              <span v-if="row.decision !== 'OUTSOURCE'">—</span>
              <el-tag v-else-if="row.vendorAssigned" type="success">已选厂商</el-tag>
              <el-tag v-else type="danger">待委外清单</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="decidedAt" label="分配时间" width="160" />
          <el-table-column label="调整归属" min-width="180">
            <template #default="{ row }">
              <el-radio-group
                v-model="draft[row.processSeq]"
                size="small"
                :disabled="row.vendorAssigned"
              >
                <el-radio value="INHOUSE">本厂</el-radio>
                <el-radio value="OUTSOURCE">委外</el-radio>
              </el-radio-group>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无已分配工序" :image-size="64" />
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import WorkorderSelect from '@/components/form/WorkorderSelect.vue'
import type { WorkorderOption } from '@/composables/useMasterData'

const router = useRouter()

interface StepRow {
  processSeq: number
  stepName?: string
  equipmentType?: string
  allocationStatus?: string
  decision?: string
  allocationId?: number
  decidedAt?: string
  vendorAssigned?: boolean
}

interface BoardData {
  workorderId?: number
  workorderNo?: string
  materialCode?: string
  pendingCount?: number
  allocatedCount?: number
  outsourcePendingForBuyer?: number
  pendingSteps?: StepRow[]
  allocatedSteps?: StepRow[]
}

const api = useBaseStore().api
const workorderId = ref<number | undefined>()
const board = ref<BoardData | null>(null)
const loading = ref(false)
const submitting = ref(false)
const draft = reactive<Record<number, string>>({})
const baseline = reactive<Record<number, string | undefined>>({})
const submittedCount = ref(0)
const hasOutsource = ref(false)
const hasInhouse = ref(false)

const pendingRows = computed(() => board.value?.pendingSteps ?? [])
const allocatedRows = computed(() => board.value?.allocatedSteps ?? [])

const dirtyCount = computed(() =>
  Object.keys(draft).filter((k) => {
    const seq = Number(k)
    const cur = draft[seq]
    return !!cur && cur !== baseline[seq]
  }).length,
)

function resetDraft(data: BoardData) {
  Object.keys(draft).forEach((k) => delete draft[Number(k)])
  Object.keys(baseline).forEach((k) => delete baseline[Number(k)])
  for (const row of data.pendingSteps ?? []) {
    baseline[row.processSeq] = undefined
  }
  for (const row of data.allocatedSteps ?? []) {
    baseline[row.processSeq] = row.decision
    if (!row.vendorAssigned) {
      draft[row.processSeq] = row.decision ?? 'INHOUSE'
    }
  }
}

async function loadBoard(id: number) {
  loading.value = true
  try {
    const data = unwrapResult<BoardData>(
      await api.get('/production/allocations/board', { params: { workorderId: id } }),
    )
    board.value = data
    resetDraft(data)
  } catch (e: unknown) {
    board.value = null
    ElMessage.error((e as { message?: string })?.message || '加载工序清单失败')
  } finally {
    loading.value = false
  }
}

function onWorkorderPick(wo: WorkorderOption | undefined) {
  if (wo?.id) {
    loadBoard(wo.id)
  } else {
    board.value = null
  }
  submittedCount.value = 0
  hasOutsource.value = false
  hasInhouse.value = false
}

function goSchedule() {
  if (workorderId.value) {
    router.push(`/production/schedule?woId=${workorderId.value}`)
  } else {
    router.push('/production/schedule')
  }
}

function goOutsource() {
  router.push('/sourcing/outsub-order')
}

async function submitBatch() {
  if (!workorderId.value || !dirtyCount.value) return
  const items = Object.keys(draft)
    .map((k) => Number(k))
    .filter((seq) => draft[seq] && draft[seq] !== baseline[seq])
    .map((processSeq) => ({ processSeq, decision: draft[processSeq] }))

  if (!items.length) return

  submitting.value = true
  submittedCount.value = 0
  hasOutsource.value = false
  try {
    const r = unwrapResult<{ saved?: number; skippedLocked?: number }>(
      await api.post('/production/allocations/batch', {
        workorderId: workorderId.value,
        items,
      }),
    )
    const saved = r.saved ?? 0
    const skipped = r.skippedLocked ?? 0
    // 检查本次提交是否有委外/本厂工序
    hasOutsource.value = items.some((i) => i.decision === 'OUTSOURCE')
    hasInhouse.value = items.some((i) => i.decision === 'INHOUSE')
    submittedCount.value = saved
    if (saved > 0) {
      ElMessage.success(
        skipped > 0
          ? `已保存 ${saved} 道工序划分，${skipped} 道因采购已选厂商未变更`
          : `已保存 ${saved} 道工序划分，系统已自动推送 MRP 事件，委外工序进入采购待委外清单`,
      )
    } else if (skipped > 0) {
      ElMessage.warning('所选工序采购已选厂商，无法修改归属')
    }
    await loadBoard(workorderId.value)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.allocation-page {
  padding: 16px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 14px;
}
.section-title .hint {
  font-weight: normal;
  color: var(--erp-text-secondary);
  font-size: 13px;
}
.pending-table :deep(.el-table__row) {
  background-color: rgba(245, 158, 11, 0.1) !important;
}
.pending-table :deep(.el-table__row td.el-table__cell) {
  background-color: transparent !important;
  color: var(--erp-text-primary);
}
.board-desc :deep(.el-descriptions__body) {
  background: var(--erp-bg-card);
}
.pick-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--erp-text-muted);
}

/* 流程进度指引 */
.step-guide {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px;
  background: var(--erp-bg-card);
  border-radius: 8px;
  margin-bottom: 4px;
}
.guide-item {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
}
.guide-item.active {
  opacity: 1;
}
.guide-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--erp-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}
.guide-item.active .guide-num {
  background: var(--erp-color-primary);
}
.guide-text {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.3;
}
.guide-text small {
  font-size: 11px;
  color: var(--erp-text-secondary);
  font-weight: normal;
}
.guide-arrow {
  font-size: 16px;
  color: var(--erp-text-muted);
}
</style>
