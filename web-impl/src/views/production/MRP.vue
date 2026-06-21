<template>
  <div v-loading="loading">
    <h2>MRP 中心</h2>
    <el-alert type="info" :closable="false" show-icon title="MRP 触发方式"
      description="事件触发 + 每日定时 + 手动运行。缺料清单由生管一键转采购申请（PR），采购员在「采购转单」中确认转 PO。"
      style="margin-bottom: 12px" />

    <el-card>
      <el-form :inline="true">
        <el-form-item label="开始">
          <el-date-picker v-model="rangeStart" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="rangeEnd" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="runMrp">手动运行 MRP</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" style="margin-top: 16px">
      <template #header>
        <div class="card-head">
          <span>运行结果 · {{ result.runNo }}</span>
          <div>
            <el-button size="small" :disabled="!result.runId" @click="goShortage">缺料清单</el-button>
            <el-button size="small" type="success" :loading="exporting" :disabled="!result.runId || !shortageRows.length" @click="exportPr">
              一键转采购申请
            </el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="触发方式">{{ triggerLabel(result.triggerType) }}</el-descriptions-item>
        <el-descriptions-item label="缺料总数">{{ result.totalShortage ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="建议采购">{{ result.totalPurchaseSuggestion ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="String(result.status ?? '')" /></el-descriptions-item>
      </el-descriptions>
      <el-table :data="shortageRows" stripe border size="small" style="margin-top: 12px" max-height="360">
        <el-table-column prop="materialCode" label="料号" min-width="120" />
        <el-table-column prop="requiredQty" label="需求" width="80" />
        <el-table-column prop="currentStock" label="库存" width="80" />
        <el-table-column prop="onOrderQty" label="在途" width="80" />
        <el-table-column prop="shortageQty" label="缺口" width="80">
          <template #default="{ row }">
            <span class="shortage">{{ row.shortageQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseSuggestion" label="建议采购" width="90" />
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>运行历史</template>
      <el-table v-loading="historyLoading" :data="runHistory" stripe border size="small">
        <el-table-column prop="runNo" label="运行号" min-width="140" />
        <el-table-column label="触发" width="90">
          <template #default="{ row }">{{ triggerLabel(row.triggerType) }}</template>
        </el-table-column>
        <el-table-column prop="totalShortage" label="缺料" width="70" />
        <el-table-column prop="startedAt" label="开始" min-width="160" />
        <el-table-column label="耗时(ms)" width="90">
          <template #default="{ row }">{{ row.durationMs ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="loadRunDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useMrpStore } from '@/stores/mrp'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const router = useRouter()
const mrpStore = useMrpStore()
const loading = ref(false)
const exporting = ref(false)
const historyLoading = ref(false)
const rangeStart = ref('2026-06-01')
const rangeEnd = ref('2026-07-01')
const result = ref<Record<string, unknown> | null>(null)
const shortageRows = ref<any[]>([])
const runHistory = ref<any[]>([])

function triggerLabel(t?: unknown) {
  const map: Record<string, string> = { MANUAL: '手动', EVENT: '事件', SCHEDULED: '定时' }
  return map[String(t ?? 'MANUAL')] ?? String(t ?? '—')
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const raw = unwrapResult(await mrpStore.listRuns({ page: 0, size: 10 }))
    runHistory.value = (raw as { list?: unknown[] })?.list ?? []
  } finally {
    historyLoading.value = false
  }
}

async function loadRunDetail(runId: number) {
  const raw = unwrapResult(await mrpStore.getMrpResult(runId))
  shortageRows.value = Array.isArray(raw) ? raw : []
  result.value = { runId, runNo: runHistory.value.find((r) => r.id === runId)?.runNo, status: 'COMPLETED' }
}

const runMrp = async () => {
  loading.value = true
  try {
    const data = unwrapResult(await mrpStore.runMrp({
      dateRangeStart: rangeStart.value,
      dateRangeEnd: rangeEnd.value,
      warehouseIds: [1, 2, 3],
      runType: 'FULL',
    })) as Record<string, unknown>
    result.value = data
    shortageRows.value = (data.shortages as any[]) ?? []
    ElMessage.success('MRP 运行完成')
    await loadHistory()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || 'MRP 运行失败')
  } finally {
    loading.value = false
  }
}

function goShortage() {
  if (result.value?.runId) {
    router.push(`/production/mrp-shortage/${result.value.runId}`)
  }
}

async function exportPr() {
  if (!result.value?.runId) return
  await ElMessageBox.confirm('将缺料清单生成采购申请（PR），采购员在「采购转单」中确认转 PO。是否继续？', '一键转采购申请')
  exporting.value = true
  try {
    const data = unwrapResult(await mrpStore.exportToPurchase(Number(result.value.runId))) as Record<string, unknown>
    ElMessage.success(`已生成 ${data.prCount ?? 0} 条采购申请`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转采购申请失败')
  } finally {
    exporting.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.card-head { display: flex; justify-content: space-between; align-items: center; }
.shortage { color: #f56c6c; font-weight: 600; }
</style>
