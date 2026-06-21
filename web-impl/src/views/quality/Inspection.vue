<template>
  <ErpPageShell title="品质检验" description="IQC 来料 · IPQC 过程 · OQC 成品 · 委外检 — 合格率一眼可见。">
    <el-tabs v-model="currentTab" @tab-change="onTabChange">
      <el-tab-pane label="IQC 来料检" name="IQC" />
      <el-tab-pane label="IPQC 过程检" name="IPQC" />
      <el-tab-pane label="OQC 成品检" name="OQC" />
      <el-tab-pane label="委外检" name="OUTSOURCE" />
    </el-tabs>

    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="检验单号/物料">
        <el-input v-model="keyword" placeholder="搜索" clearable @keyup.enter="reload" />
      </el-form-item>
      <el-form-item label="检验状态">
        <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 140px" @change="reload">
          <el-option label="待检验" value="PENDING" />
          <el-option label="已合格" value="PASSED" />
          <el-option label="待审批" value="PENDING_APPROVAL" />
          <el-option label="已让步" value="CONDITIONAL" />
          <el-option label="已退货" value="RETURNED" />
          <el-option label="待返工" value="REWORK" />
          <el-option label="已报废" value="SCRAPPED" />
          <el-option label="不合格" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="reload">查询</el-button>
        <el-button type="primary" class="erp-btn-primary" @click="goCreate">新建检验单</el-button>
        <el-button class="erp-btn-ghost" @click="goConcession">让步审批</el-button>
        <el-button class="erp-btn-ghost" @click="goOutsource">委外检视图</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="inspections" stripe>
      <el-table-column prop="inspectionNo" label="检验单号" min-width="140" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="inspectionStatus" label="检验状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row)">{{ inspectionStatusLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="qty" label="数量" width="80" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.qty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合格率" width="120" align="right">
        <template #default="{ row }">
          <ErpPassRate :qty="row.qty" :pass-qty="row.passQty" :fail-qty="row.failQty" />
        </template>
      </el-table-column>
      <el-table-column prop="passQty" label="合格" width="72" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.passQty ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="failQty" label="不合格" width="80" align="right">
        <template #default="{ row }">
          <span :class="row.failQty > 0 ? 'erp-num-warn' : 'erp-num-highlight'">{{ row.failQty ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="inspector" label="检验员" width="90" />
      <el-table-column prop="inspectedAt" label="检验时间" min-width="160" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="isPendingApproval(row)" size="small" type="warning" @click="goConcession">审批</el-button>
          <el-button v-if="isPending(row)" size="small" type="primary" @click="goCreateFor(row)">去检验</el-button>
          <el-button v-if="isPending(row)" size="small" class="erp-btn-ghost" @click="goDetail(row)">详情录入</el-button>
          <el-button v-else size="small" class="erp-btn-ghost" @click="goDetail(row)">查看报告</el-button>
          <el-button v-if="!isPending(row)" size="small" type="success" class="erp-btn-success" @click="goReport(row)">报告</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="erp-pagination"
      @current-change="reload"
      @size-change="reload"
    />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useQualityStore } from '@/stores/quality'
import { parsePageItems } from '@/utils/apiPage'

interface InspectionRow {
  id?: number
  inspectionNo?: string
  type?: string
  materialCode?: string
  inspectionStatus?: string
  status?: string
  result?: string
  statusLabel?: string
  qty?: number
  passQty?: number
  failQty?: number
  inspector?: number | string
  inspectedAt?: string
}

const router = useRouter()
const qualityStore = useQualityStore()
const inspections = ref<InspectionRow[]>([])
const currentTab = ref<'IQC' | 'IPQC' | 'OQC' | 'OUTSOURCE'>('IQC')
const keyword = ref('')
const statusFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)

async function reload() {
  loading.value = true
  try {
    const r = await qualityStore.listInspections({
      type: currentTab.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const page = parsePageItems(r)
    inspections.value = page.items as InspectionRow[]
    total.value = page.total
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    inspections.value = []
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  pageNum.value = 1
  reload()
}

function goCreate() {
  router.push('/quality/inspection-create')
}

function goOutsource() {
  router.push('/quality/outsource-inspection')
}

function goDetail(row: InspectionRow) {
  if (row.id) router.push(`/quality/inspection-detail/${row.id}`)
}

function goReport(row: InspectionRow) {
  if (row.id) router.push(`/quality/inspection-report/${row.id}`)
}

function rowStatus(row: InspectionRow): string {
  return row.inspectionStatus ?? row.status ?? row.result ?? 'PENDING'
}

function isPending(row: InspectionRow): boolean {
  const s = rowStatus(row).toUpperCase()
  return s === 'PENDING' || s === 'DRAFT' || s === '待检验' || s === '待检'
}

function isPendingApproval(row: InspectionRow): boolean {
  return rowStatus(row).toUpperCase() === 'PENDING_APPROVAL'
}

function goConcession() {
  router.push('/quality/concession-approval')
}

function inspectionStatusLabel(row: InspectionRow): string {
  const s = rowStatus(row).toUpperCase()
  const map: Record<string, string> = {
    PENDING: '待检验', DRAFT: '待检验', INSPECTING: '检验中',
    PASSED: '已合格', PASS: '已合格',
    FAILED: '不合格', FAIL: '不合格',
    PENDING_APPROVAL: '待审批',
    CONDITIONAL: '已让步',
    RETURNED: '已退货',
    REWORK: '待返工',
    SCRAPPED: '已报废',
  }
  return row.statusLabel ?? map[s] ?? rowStatus(row)
}

function statusTagType(row: InspectionRow): 'success' | 'warning' | 'danger' | 'info' {
  const s = rowStatus(row).toUpperCase()
  if (s === 'PASSED' || s === 'PASS') return 'success'
  if (s === 'CONDITIONAL') return 'success'
  if (s === 'RETURNED' || s === 'SCRAPPED' || s === 'FAILED' || s === 'FAIL') return 'danger'
  if (s === 'REWORK' || s === 'PENDING_APPROVAL') return 'warning'
  return 'info'
}

function goCreateFor(row: InspectionRow) {
  if (row.id) {
    router.push({ path: '/quality/inspection-create', query: { id: String(row.id) } })
  } else {
    router.push({ path: '/quality/inspection-create', query: { materialCode: row.materialCode } })
  }
}

onMounted(reload)
</script>

<style scoped>
.erp-num-warn {
  font-weight: 700;
  font-family: var(--erp-font-mono);
  color: var(--erp-color-danger);
}
</style>
