<template>
  <ErpPageShell title="让步接收审批" description="品质主管 + 生管双签 · 审批通过后状态变为「已让步」">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="检验单号/料号">
        <el-input v-model="keyword" placeholder="搜索" clearable @keyup.enter="reload" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="reload">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="rows" stripe>
      <el-table-column prop="inspectionNo" label="检验单号" min-width="140" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="statusLabel" label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="warning">{{ row.statusLabel ?? '待审批' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="inspectedAt" label="提交时间" min-width="160" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openApprove(row)">审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="让步接收双签审批" width="520px" destroy-on-close>
      <template v-if="current">
        <p><strong>检验单：</strong>{{ current.inspectionNo }}</p>
        <p><strong>料号：</strong>{{ current.materialCode ?? '—' }}</p>
        <el-table :data="approvals" border size="small" style="margin: 12px 0">
          <el-table-column prop="approverRoleLabel" label="审批角色" width="120" />
          <el-table-column prop="approvalStatus" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="approvalTagType(row.approvalStatus)">{{ approvalStatusLabel(row.approvalStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="comment" label="意见" min-width="140" show-overflow-tooltip />
        </el-table>
        <el-form label-width="100px">
          <el-form-item label="我的角色" required>
            <el-select v-model="approverRole" placeholder="请选择">
              <el-option label="品质主管" value="QUALITY_MANAGER" />
              <el-option label="生管" value="PRODUCTION_MANAGER" />
            </el-select>
          </el-form-item>
          <el-form-item label="审批意见">
            <el-input v-model="comment" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="doApprove('REJECT')">驳回</el-button>
        <el-button type="primary" :loading="submitting" @click="doApprove('APPROVE')">通过</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useQualityStore } from '@/stores/quality'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

interface InspectionRow {
  id?: number
  inspectionNo?: string
  type?: string
  materialCode?: string
  statusLabel?: string
  inspectedAt?: string
}

interface ApprovalRow {
  approverRole?: string
  approverRoleLabel?: string
  approvalStatus?: string
  comment?: string
}

const qualityStore = useQualityStore()
const rows = ref<InspectionRow[]>([])
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const current = ref<InspectionRow | null>(null)
const approvals = ref<ApprovalRow[]>([])
const approverRole = ref('QUALITY_MANAGER')
const comment = ref('')
const submitting = ref(false)

async function reload() {
  loading.value = true
  try {
    const r = await qualityStore.listInspections({
      status: 'PENDING_APPROVAL',
      keyword: keyword.value || undefined,
      pageNum: 1,
      pageSize: 100,
    })
    const page = parsePageItems(r)
    rows.value = page.items as InspectionRow[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function openApprove(row: InspectionRow) {
  if (!row.id) return
  current.value = row
  approverRole.value = 'QUALITY_MANAGER'
  comment.value = ''
  try {
    approvals.value = unwrapResult<ApprovalRow[]>(await qualityStore.getConcessionApprovals(row.id))
  } catch {
    approvals.value = []
  }
  dialogVisible.value = true
}

async function doApprove(action: 'APPROVE' | 'REJECT') {
  if (!current.value?.id) return
  if (!approverRole.value) {
    ElMessage.warning('请选择审批角色')
    return
  }
  submitting.value = true
  try {
    const resp = unwrapResult<{ statusLabel?: string; status?: string }>(
      await qualityStore.approveConcession(current.value.id, {
        approverRole: approverRole.value,
        action,
        comment: comment.value || undefined,
      }),
    )
    ElMessage.success(`审批完成 · 状态：${resp.statusLabel ?? resp.status ?? '已更新'}`)
    dialogVisible.value = false
    reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '审批失败')
  } finally {
    submitting.value = false
  }
}

function approvalStatusLabel(s?: string): string {
  const map: Record<string, string> = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[s ?? ''] ?? (s ?? '—')
}

function approvalTagType(s?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'warning'
}

onMounted(reload)
</script>
