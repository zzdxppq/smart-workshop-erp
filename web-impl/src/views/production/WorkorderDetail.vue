<template>
  <div v-loading="loading">
    <h2>工单详情</h2>
    <el-card v-if="workorder">
      <!-- 进度状态条 -->
      <div class="step-progress">
        <div class="step-item" :class="{ active: currentStep >= 1, current: currentStep === 1 }">
          <div class="step-icon">①</div>
          <div class="step-label">工序分配</div>
        </div>
        <div class="step-arrow">→</div>
        <div class="step-item" :class="{ active: currentStep >= 2, current: currentStep === 2 }">
          <div class="step-icon">②</div>
          <div class="step-label">排产</div>
        </div>
        <div class="step-arrow">→</div>
        <div class="step-item" :class="{ active: currentStep >= 3, current: currentStep === 3 }">
          <div class="step-icon">③</div>
          <div class="step-label">开工</div>
        </div>
        <div class="step-arrow">→</div>
        <div class="step-item" :class="{ active: currentStep >= 4, current: currentStep === 4 }">
          <div class="step-icon">④</div>
          <div class="step-label">完工</div>
        </div>
      </div>

      <div class="step-tip">
        <span v-if="currentStep === 1">请先完成工序分配，确定每道工序的归属（本厂/委外）</span>
        <span v-else-if="currentStep === 2">工序分配完成，请在排产看板中将本厂工序拖到机台</span>
        <span v-else-if="currentStep === 3">已排产，等待操作工扫码开工或由生管代开工</span>
        <span v-else-if="currentStep === 4">工单已完工，进入报工结案流程</span>
      </div>

      <el-row :gutter="16" style="margin-top: 20px">
        <el-col :span="16">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单号">{{ workorder.workorderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <ErpStatusTag :status="workorder.status" />
            </el-descriptions-item>
            <el-descriptions-item label="料号">{{ workorder.materialCode }}</el-descriptions-item>
            <el-descriptions-item label="图号">
              <DrawingNoCell
                :drawing-no="workorder.drawingNo"
                :drawing-id="workorder.drawingId"
                :material-code="workorder.materialCode"
              />
            </el-descriptions-item>
            <el-descriptions-item label="产品">{{ workorder.productName }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ workorder.qty }}</el-descriptions-item>
            <el-descriptions-item label="机台">{{ workorder.equipmentType }}</el-descriptions-item>
            <el-descriptions-item label="排产开始">{{ workorder.scheduledStart }}</el-descriptions-item>
            <el-descriptions-item label="排产结束">{{ workorder.scheduledEnd }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="8">
          <div class="qr-panel">
            <div class="qr-title">GD- 工单码（一码到底起点）</div>
            <img v-if="qrDataUrl" :src="qrDataUrl" alt="工单二维码" class="qr-img" />
            <div class="qr-hint">{{ workorder.workorderNo }}</div>
            <el-button size="small" class="erp-btn-secondary" @click="printCard">打印工单卡</el-button>
          </div>
        </el-col>
      </el-row>

      <h3 style="margin-top: 20px">工艺路线 · 工序归属</h3>
      <el-table :data="processRows" stripe border size="small" empty-text="暂无工序，请先在工序分配页维护">
        <el-table-column prop="stepNo" label="序号" width="70" />
        <el-table-column prop="stepName" label="工序" min-width="120" />
        <el-table-column prop="equipmentType" label="设备类型" width="100" />
        <el-table-column label="归属" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.decision === 'OUTSOURCE'" type="warning" size="small">委外</el-tag>
            <el-tag v-else-if="row.decision === 'INHOUSE'" type="success" size="small">自制</el-tag>
            <el-tag v-else type="info" size="small">待分配</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px">
        <el-button type="primary" @click="$router.push(`/production/allocation?woId=${id()}`)">① 工序分配</el-button>
        <el-button @click="goSchedule" :disabled="currentStep < 1">② 排产</el-button>
        <el-button type="warning" :loading="acting" :disabled="currentStep < 2" @click="goStart">③ 开工</el-button>
        <el-button type="success" :loading="acting" :disabled="currentStep < 3" @click="goFinish">④ 完工</el-button>
        <el-button v-if="workorder.drawingId" type="info" @click="openDrawingViewer(workorder.drawingId)">
          查看工序图纸
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="drawerVisible" title="工序图纸" width="80%" destroy-on-close>
      <DrawingViewer v-if="drawerVisible && activeDrawingId" :drawing-id="activeDrawingId" @acl-denied="onAclDenied" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { useWorkorderStore } from '@/stores/workorder'
import { useBaseStore } from '@/stores/_base'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { unwrapResult } from '@/utils/apiPage'
import DrawingViewer from '@/views/crm/drawing/DrawingViewer.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import DrawingNoCell from '@/components/erp/DrawingNoCell.vue'

const router = useRouter()
const workorderStore = useWorkorderStore()
const acting = ref(false)
const drawerVisible = ref(false)
const activeDrawingId = ref<number | null>(null)
const processRows = ref<any[]>([])
const qrDataUrl = ref('')

// 进度步骤计算：1=工序分配 2=排产 3=开工 4=完工
const currentStep = computed(() => {
  if (!workorder.value) return 0
  const status = workorder.value.status
  if (status === 'COMPLETED') return 4
  if (status === 'IN_PROGRESS') return 3
  if (status === 'SCHEDULED') return 2
  return 1 // DRAFT 及其他默认进入工序分配步骤
})

// 路由参数 id 可能是数字 ID 或工单号（GD-XXXX），需自动选择调用接口
function fetchWorkorderByParam(param: string | number) {
  const s = String(param).trim()
  // 含字母则为工单号，否则按数字 ID
  if (/[a-zA-Z]/.test(s)) {
    return workorderStore.getWorkorderByNo(s)
  }
  return workorderStore.getWorkorder(Number(s))
}

const { data: workorder, loading, load, id } = useDetailLoad<any>(fetchWorkorderByParam)

async function loadProcesses() {
  const wid = id()
  if (!wid) return
  try {
    const steps = unwrapResult(await workorderStore.listSteps(wid)) as any[]
    const board = unwrapResult(
      await useBaseStore().api.get('/production/allocations/board', { params: { workorderId: wid } }),
    ) as any
    const allocMap = new Map<number, string>()
    for (const s of [...(board?.pendingSteps ?? []), ...(board?.allocatedSteps ?? [])]) {
      if (s.processSeq != null && s.decision) allocMap.set(s.processSeq, s.decision)
    }
    processRows.value = (steps ?? []).map((s) => ({
      stepNo: s.stepNo ?? s.processSeq,
      stepName: s.stepName ?? s.processName,
      equipmentType: s.equipmentType,
      decision: allocMap.get(s.stepNo ?? s.processSeq),
    }))
  } catch {
    processRows.value = []
  }
}

watch(workorder, async (wo) => {
  if (wo?.workorderNo) {
    qrDataUrl.value = await QRCode.toDataURL(wo.workorderNo, { width: 160, margin: 1 })
    await loadProcesses()
  }
}, { immediate: true })

function printCard() {
  window.print()
}

function openDrawingViewer(drawingId: number) {
  activeDrawingId.value = drawingId
  drawerVisible.value = true
}

function onAclDenied(_code: number, message: string) {
  ElMessage.error(message || '无权访问该图纸')
}

const goSchedule = () => router.push(`/production/schedule?woId=${id()}`)

async function goStart() {
  acting.value = true
  try {
    await workorderStore.startProduction(id())
    ElMessage.success('已开工')
    await load()
    await loadProcesses()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '开工失败')
  } finally {
    acting.value = false
  }
}

async function goFinish() {
  acting.value = true
  try {
    await workorderStore.finishProduction(id())
    ElMessage.success('已完工')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '完工失败')
  } finally {
    acting.value = false
  }
}
</script>

<style scoped>
.qr-panel { text-align: center; padding: 12px; border: 1px dashed var(--erp-border); border-radius: 8px; }
.qr-title { font-size: 13px; color: var(--erp-text-secondary); margin-bottom: 8px; }
.qr-img { width: 160px; height: 160px; }
.qr-hint { font-family: monospace; margin: 8px 0; }

.step-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: var(--erp-bg-card);
  border-radius: 8px;
  margin-bottom: 12px;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  opacity: 0.4;
}
.step-item.active { opacity: 1; }
.step-item.current .step-icon {
  background: var(--erp-color-primary);
  color: #fff;
}
.step-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--erp-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}
.step-item.active .step-icon {
  background: var(--erp-color-success);
  color: #fff;
}
.step-label { font-size: 13px; font-weight: 500; }
.step-arrow { font-size: 18px; color: var(--erp-text-muted); }

.step-tip {
  text-align: center;
  font-size: 13px;
  color: var(--erp-text-secondary);
  padding: 8px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 6px;
  margin-bottom: 4px;
}
</style>
