<template>
  <div v-loading="loading" class="contract-sub">
    <el-page-header @back="$router.push('/sales/contracts')">
      <template #content>{{ pageTitle }}</template>
    </el-page-header>

    <el-descriptions v-if="summary" :column="3" border size="small" class="order-summary">
      <el-descriptions-item label="订单号">{{ summary.orderNo ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ summary.customerName ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">
        <MoneyAmount :model-value="Number(summary.amount ?? 0)" display-only />
      </el-descriptions-item>
      <el-descriptions-item label="联系人">{{ summary.contactName ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ summary.contactPhone ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="已回款">{{ summary.receivedAmount ?? 0 }}</el-descriptions-item>
    </el-descriptions>

    <div v-if="canWrite" class="toolbar">
      <el-button type="primary" @click="openDialog()">{{ mode === 'plan' ? '新增计划' : '登记收款' }}</el-button>
    </div>
    <el-alert
      v-else-if="mode === 'reg'"
      type="info"
      :closable="false"
      title="收款由财务登记，销售仅可查看"
      style="margin-top: 16px"
    />

    <el-table v-if="rows.length" :data="rows" stripe border style="margin-top: 16px">
      <template v-if="mode === 'plan'">
        <el-table-column prop="phase" label="期次" width="80" />
        <el-table-column prop="planDate" label="计划日" width="120" />
        <el-table-column label="计划金额" width="140">
          <template #default="{ row }">
            <MoneyAmount :model-value="Number(row.planAmount ?? 0)" display-only />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">{{ planStatusText(row.status) }}</template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
      <template v-else-if="mode === 'reg'">
        <el-table-column prop="paidDate" label="实收日" width="120" />
        <el-table-column label="实收金额" width="140">
          <template #default="{ row }">
            <MoneyAmount :model-value="Number(row.paidAmount ?? 0)" display-only />
          </template>
        </el-table-column>
        <el-table-column prop="method" label="方式" min-width="100" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column v-if="canWrite" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </template>
      <template v-else>
        <el-table-column label="订单金额" width="140">
          <template #default="{ row }">
            <MoneyAmount :model-value="Number(row.orderAmount ?? row.revenue ?? 0)" display-only />
          </template>
        </el-table-column>
        <el-table-column label="收入" width="140">
          <template #default="{ row }">
            <MoneyAmount :model-value="Number(row.revenue ?? 0)" display-only />
          </template>
        </el-table-column>
        <el-table-column label="成本" width="140">
          <template #default="{ row }">
            <MoneyAmount :model-value="Number(row.cost ?? 0)" display-only />
          </template>
        </el-table-column>
        <el-table-column label="利润" width="140">
          <template #default="{ row }">
            <MoneyAmount :model-value="Number(row.profit ?? 0)" display-only />
          </template>
        </el-table-column>
        <el-table-column label="毛利率" width="100">
          <template #default="{ row }">{{ formatRate(row.marginRate) }}</template>
        </el-table-column>
        <el-table-column label="已回款" width="120">
          <template #default="{ row }">{{ row.receivedAmount ?? 0 }}</template>
        </el-table-column>
      </template>
    </el-table>
    <el-empty v-else description="暂无数据" />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <template v-if="mode === 'plan'">
          <el-form-item label="期次" required>
            <el-input-number v-model="planForm.phase" :min="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="计划日" required>
            <el-date-picker v-model="planForm.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="计划金额" required>
            <el-input-number v-model="planForm.planAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="planForm.status" style="width: 100%">
              <el-option label="计划中" value="PLANNED" />
              <el-option label="部分到账" value="PARTIAL" />
              <el-option label="已到账" value="RECEIVED" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="实收日" required>
            <el-date-picker v-model="regForm.paidDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="实收金额" required>
            <el-input-number v-model="regForm.paidAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="收款方式">
            <el-select v-model="regForm.method" allow-create filterable style="width: 100%">
              <el-option label="银行转账" value="银行转账" />
              <el-option label="承兑汇票" value="承兑汇票" />
              <el-option label="现金" value="现金" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="regForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult, parsePageItems } from '@/utils/apiPage'
import { hasAnyRole, ROLE_GROUPS } from '@/utils/roleAccess'
import MoneyAmount from '@/components/erp/MoneyAmount.vue'

const route = useRoute()
const auth = useAuthStore()
const api = useBaseStore().api
const loading = ref(false)
const saving = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const summary = ref<Record<string, unknown> | null>(null)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

interface PlanForm {
  phase: number
  planDate: string
  planAmount: number
  status: string
}

interface RegForm {
  paidDate: string
  paidAmount: number
  method: string
  remark: string
}

const form = ref<PlanForm | RegForm>({ phase: 1, planDate: '', planAmount: 0, status: 'PLANNED' })

const contractId = computed(() => Number(route.params.id))

const mode = computed(() => {
  if (route.path.includes('payment-plan')) return 'plan'
  if (route.path.includes('payment-reg')) return 'reg'
  return 'profit'
})

const canWritePlan = computed(() => mode.value === 'plan' && hasAnyRole(auth.userRoles, ROLE_GROUPS.SALES))
const canWriteReg = computed(() => mode.value === 'reg' && hasAnyRole(auth.userRoles, ROLE_GROUPS.FINANCE))
const canWrite = computed(() => canWritePlan.value || canWriteReg.value)

const pageTitle = computed(() => {
  if (mode.value === 'plan') return '回款计划'
  if (mode.value === 'reg') return canWriteReg.value ? '收款登记' : '收款记录'
  return '订单利润'
})

const dialogTitle = computed(() => {
  const action = editingId.value ? '编辑' : mode.value === 'plan' ? '新增' : '登记'
  return mode.value === 'plan' ? `${action}回款计划` : `${action}收款`
})

const planForm = computed({
  get: () => form.value as PlanForm,
  set: (v: PlanForm) => { form.value = v },
})

const regForm = computed({
  get: () => form.value as RegForm,
  set: (v: RegForm) => { form.value = v },
})

function planStatusText(status?: string) {
  const map: Record<string, string> = {
    PLANNED: '计划中',
    PARTIAL: '部分到账',
    RECEIVED: '已到账',
  }
  return status ? (map[status] ?? status) : '—'
}

function formatRate(rate: unknown) {
  const n = Number(rate)
  if (Number.isNaN(n)) return '—'
  return `${(n <= 1 ? n * 100 : n).toFixed(1)}%`
}

async function loadSummary() {
  if (!contractId.value) return
  try {
    summary.value = unwrapResult(await api.get(`/contracts/${contractId.value}`)) as Record<string, unknown>
  } catch {
    summary.value = null
  }
}

function apiPath(suffix?: string) {
  const base = `/contracts/${contractId.value}/${mode.value === 'plan' ? 'payment-plan' : 'payment-reg'}`
  return suffix ? `${base}/${suffix}` : base
}

async function loadRows() {
  if (!contractId.value || mode.value === 'profit') return
  loading.value = true
  try {
    const path = mode.value === 'plan' ? 'payment-plan' : mode.value === 'reg' ? 'payment-reg' : 'profit'
    const r = unwrapResult(await api.get(`/contracts/${contractId.value}/${path}`))
    rows.value = Array.isArray(r) ? r : parsePageItems(r).items
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Record<string, unknown>) {
  editingId.value = row?.id != null ? Number(row.id) : null
  if (mode.value === 'plan') {
    form.value = {
      phase: Number(row?.phase ?? rows.value.length + 1),
      planDate: String(row?.planDate ?? ''),
      planAmount: row?.planAmount != null ? Number(row.planAmount) : 0,
      status: String(row?.status ?? 'PLANNED'),
    }
  } else {
    form.value = {
      paidDate: String(row?.paidDate ?? ''),
      paidAmount: row?.paidAmount != null ? Number(row.paidAmount) : 0,
      method: String(row?.method ?? '银行转账'),
      remark: String(row?.remark ?? ''),
    }
  }
  dialogVisible.value = true
}

async function submitForm() {
  if (mode.value === 'plan') {
    const f = planForm.value
    if (!f.planDate || f.planAmount == null) {
      ElMessage.warning('请填写计划日与计划金额')
      return
    }
    saving.value = true
    try {
      if (editingId.value) {
        await api.put(apiPath(String(editingId.value)), f)
      } else {
        await api.post(apiPath(), f)
      }
      ElMessage.success('已保存')
      dialogVisible.value = false
      await loadRows()
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '保存失败')
    } finally {
      saving.value = false
    }
    return
  }
  const f = regForm.value
  if (!f.paidDate || f.paidAmount == null) {
    ElMessage.warning('请填写实收日与实收金额')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(apiPath(String(editingId.value)), f)
    } else {
      await api.post(apiPath(), f)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await loadRows()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeRow(row: Record<string, unknown>) {
  if (row.id == null) return
  try {
    await ElMessageBox.confirm('确定删除该记录？', '确认', { type: 'warning' })
    await api.delete(apiPath(String(row.id)))
    ElMessage.success('已删除')
    await loadRows()
  } catch (e: unknown) {
    if ((e as { message?: string })?.message !== 'cancel') {
      ElMessage.error((e as { message?: string })?.message || '删除失败')
    }
  }
}

onMounted(async () => {
  await loadSummary()
  if (mode.value === 'profit') {
    loading.value = true
    try {
      const r = unwrapResult(await api.get(`/contracts/${contractId.value}/profit`))
      rows.value = Array.isArray(r) ? r : parsePageItems(r).items
    } finally {
      loading.value = false
    }
    return
  }
  await loadRows()
})
</script>

<style scoped>
.toolbar {
  margin-top: 16px;
}
.order-summary {
  margin-top: 16px;
}
</style>
