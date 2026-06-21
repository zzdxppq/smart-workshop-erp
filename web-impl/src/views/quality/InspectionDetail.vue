<template>
  <ErpPageShell
    :title="pending ? '检验录入' : '检验单详情'"
    :description="pending ? '手机端主力 · PC 亦可录入并提交判定' : '已归档 · 只读查看'"
  >
    <el-card v-if="inspection">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="检验单号">{{ inspection.inspectionNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="pending ? 'warning' : 'success'">{{ statusLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="类型">{{ inspection.type }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ inspection.materialCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="图号">
          <DrawingNoCell
            :drawing-no="inspection.drawingNo"
            :drawing-id="inspection.drawingId"
            :material-code="inspection.materialCode"
          />
        </el-descriptions-item>
        <el-descriptions-item label="工单">{{ inspection.workOrderNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ inspection.qty ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="检验员">{{ inspection.inspector || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="!pending" label="合格">{{ inspection.passQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item v-if="!pending" label="不合格">{{ inspection.failQty ?? 0 }}</el-descriptions-item>
        <el-descriptions-item v-if="!pending" label="检验时间">{{ inspection.inspectedAt || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <template v-if="pending">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin: 16px 0"
        title="待检验单：填写实测值与总体判定后提交；也可跳转完整录入页（含图号/模板）"
      />

      <el-form label-width="120px">
        <el-form-item label="总体判定" required>
          <el-radio-group v-model="overallResult">
            <el-radio value="PASS">合格</el-radio>
            <el-radio value="FAIL">不合格</el-radio>
            <el-radio value="CONDITIONAL">让步接收</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="overallResult === 'FAIL'">
          <el-form-item label="处置方式" required>
            <el-select v-model="disposition" placeholder="请选择">
              <el-option label="退货" value="RETURN" />
              <el-option label="返工" value="REWORK" />
              <el-option label="报废" value="SCRAP" />
            </el-select>
          </el-form-item>
          <el-form-item label="不良数量" required>
            <el-input-number v-model="defectQty" :min="1" />
          </el-form-item>
        </template>
        <template v-else-if="overallResult === 'CONDITIONAL'">
          <el-form-item label="让步原因" required>
            <el-input v-model="conditionalReason" placeholder="如：客户特批/不影响功能" />
          </el-form-item>
        </template>

        <el-form-item label="检验项" required>
          <el-table :data="editItems" border size="small">
            <el-table-column prop="itemName" label="项目" min-width="120" />
            <el-table-column prop="standard" label="标准" min-width="140" />
            <el-table-column label="实测" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.measuredValue" placeholder="实测值" />
              </template>
            </el-table-column>
            <el-table-column label="结果" width="110">
              <template #default="{ row }">
                <el-select v-model="row.result">
                  <el-option label="合格" value="PASS" />
                  <el-option label="不合格" value="FAIL" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="严重度" width="110">
              <template #default="{ row }">
                <el-select v-model="row.severity">
                  <el-option label="INFO" value="INFO" />
                  <el-option label="WARN" value="WARN" />
                  <el-option label="ERROR" value="ERROR" />
                  <el-option label="CRITICAL" value="CRITICAL" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="不良描述" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.defectDesc" placeholder="可选" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">
            提交检验报告
          </el-button>
          <el-button @click="goFullForm">完整录入（图号/模板）</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </template>

    <template v-else>
      <h3 style="margin-top: 16px">检验明细</h3>
      <el-table :data="inspection?.items || []" stripe>
        <el-table-column prop="itemName" label="项目" />
        <el-table-column prop="standard" label="标准" />
        <el-table-column prop="actual" label="实测" />
        <el-table-column prop="result" label="结果">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'">
              {{ row.result === 'PASS' ? '合格' : row.result === 'FAIL' ? '不合格' : row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="严重度" width="90" />
      </el-table>
      <div style="margin-top: 16px">
        <el-button type="success" @click="goReport">检验报告</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </template>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { unwrapResult } from '@/utils/apiPage'

interface InspectionItemRow {
  id?: number
  itemName?: string
  standard?: string
  actual?: string
  result?: string
  severity?: string
  defectDesc?: string
}

interface EditableItem {
  id?: number
  itemName: string
  standard: string
  measuredValue: string
  result: 'PASS' | 'FAIL'
  severity: string
  defectDesc: string
}

interface InspectionDetail {
  id?: number
  inspectionNo?: string
  type?: string
  materialCode?: string
  drawingNo?: string
  drawingId?: number
  workOrderNo?: string
  qty?: number
  passQty?: number
  failQty?: number
  inspector?: string | number
  inspectedAt?: string
  result?: string
  statusLabel?: string
  remark?: string
  items?: InspectionItemRow[]
}

const route = useRoute()
const router = useRouter()
const qualityStore = useQualityStore()
const { data: inspection, loading } = useDetailLoad<InspectionDetail>((id) => qualityStore.getInspection(id))

const editItems = ref<EditableItem[]>([])
const overallResult = ref<'PASS' | 'FAIL' | 'CONDITIONAL'>('PASS')
const disposition = ref<'RETURN' | 'REWORK' | 'SCRAP' | ''>('')
const defectQty = ref(1)
const conditionalReason = ref('')
const remark = ref('')
const submitting = ref(false)

function isPendingResult(result?: string): boolean {
  const s = (result ?? '').toUpperCase()
  return !s || s === 'DRAFT' || s === 'PENDING'
}

const pending = computed(() => isPendingResult(inspection.value?.result))

const statusLabel = computed(() => {
  if (pending.value) return '待检验'
  return inspection.value?.statusLabel ?? inspection.value?.result ?? '—'
})

const canSubmit = computed(() => {
  if (editItems.value.length === 0) return false
  if (overallResult.value === 'FAIL') return !!disposition.value && defectQty.value >= 1
  if (overallResult.value === 'CONDITIONAL') return !!conditionalReason.value.trim()
  return true
})

watch(inspection, (val) => {
  if (!val) return
  remark.value = val.remark ?? ''
  editItems.value = (val.items ?? []).map((it) => ({
    id: it.id,
    itemName: it.itemName ?? '',
    standard: it.standard ?? '',
    measuredValue: it.actual ?? '',
    result: it.result === 'FAIL' ? 'FAIL' : 'PASS',
    severity: it.severity ?? 'INFO',
    defectDesc: it.defectDesc ?? '',
  }))
}, { immediate: true })

async function submit() {
  const id = Number(route.params.id)
  if (!id || !canSubmit.value) {
    ElMessage.warning('请完善检验项与总体判定')
    return
  }
  const conclusion = overallResult.value === 'FAIL' ? 'FAIL' : 'PASS'
  submitting.value = true
  try {
    unwrapResult(await qualityStore.submitInspection(id, {
      conclusion,
      overallResult: overallResult.value,
      disposition: overallResult.value === 'FAIL' ? disposition.value : undefined,
      defectQty: overallResult.value === 'FAIL' ? defectQty.value : undefined,
      conditionalReason: overallResult.value === 'CONDITIONAL' ? conditionalReason.value : undefined,
      remark: remark.value || undefined,
      items: editItems.value.map((it) => ({
        id: it.id,
        itemName: it.itemName,
        measuredValue: it.measuredValue,
        result: it.result,
        severity: it.severity,
        defectDesc: it.defectDesc || undefined,
      })),
    }))
    ElMessage.success('检验报告已提交')
    router.push('/quality/inspection')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function goFullForm() {
  const id = route.params.id
  if (id) router.push({ path: '/quality/inspection-create', query: { id: String(id) } })
}

function goReport() {
  const id = route.params.id
  if (id) router.push(`/quality/inspection-report/${id}`)
}
</script>
