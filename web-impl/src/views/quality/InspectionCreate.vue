<template>
  <ErpPageShell :title="pageTitle" description="手机端主力 · PC 辅助录入 · 提交时须判定合格/不合格/让步接收">
    <el-form :model="form" label-width="120px">
      <el-form-item label="检验类型" required>
        <el-select v-model="form.inspectionType" :disabled="!!editingId" @change="onTypeChange">
          <el-option label="IQC 来料检" value="INCOMING" />
          <el-option label="IPQC 过程检" value="IN_PROCESS" />
          <el-option label="OQC 成品检" value="OUTGOING" />
          <el-option label="FA 首件" value="FA" />
          <el-option label="CMM 三次元" value="CMM" />
        </el-select>
      </el-form-item>
      <el-form-item label="图号" required>
        <DrawingPicker v-model="drawingNo" :allow-upload="false" style="max-width: 420px" @select="onDrawingSelect" />
      </el-form-item>
      <el-form-item label="料号">
        <el-input v-model="form.materialCode" readonly placeholder="选择图号后自动带出（无料号时留空）" />
      </el-form-item>

      <el-alert
        v-if="templateHint"
        :type="templateHintType"
        :title="templateHint"
        :closable="false"
        style="margin-bottom: 12px"
        show-icon
      />

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
          <el-input v-model="conditionalReason" placeholder="如：客户特批/不影响功能/批量让步" />
        </el-form-item>
      </template>

      <el-form-item label="检验项" required>
        <el-table :data="inspectItems" border size="small" empty-text="选择图号后自动加载模板；如未命中可手动添加">
          <el-table-column label="#" width="50" type="index" align="center" />
          <el-table-column label="项名称" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.itemName" placeholder="如：外观" />
            </template>
          </el-table-column>
          <el-table-column label="标准" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.standard" placeholder="如：10±0.05mm" />
            </template>
          </el-table-column>
          <el-table-column label="实测值" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.measuredValue" placeholder="输入数值或文本" @input="autoJudge(row)" />
            </template>
          </el-table-column>
          <el-table-column label="结果" width="120">
            <template #default="{ row }">
              <el-select v-model="row.result" :class="{ 'is-judged-ok': row.result === 'OK', 'is-judged-ng': row.result === 'NG' }">
                <el-option label="OK" value="OK" />
                <el-option label="NG" value="NG" />
                <el-option label="NA" value="NA" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="严重度" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="severityTagType(row.severity)">{{ row.severity || 'INFO' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button size="small" type="danger" link @click="removeItem($index)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 8px">
          <el-button size="small" type="primary" plain @click="addItem">+ 手动添加检验项</el-button>
          <el-button size="small" plain @click="goTemplateManager">管理检验模板</el-button>
        </div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item>
        <el-tooltip v-if="!canSubmit && overallResult === 'FAIL'" content="请选择处置方式并填写不良数量" placement="top">
          <span>
            <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">提交检验报告</el-button>
          </span>
        </el-tooltip>
        <el-button v-else type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">提交检验报告</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import { E7QualityService } from '@/api/generated/services/E7QualityService'
import type { InspectionCreateRequest } from '@/api/generated/models/InspectionCreateRequest'
import type { InspectionItemDTO } from '@/api/generated/models/InspectionItemDTO'
import type { Drawing } from '@/api/generated/models/Drawing'
import { unwrapResult } from '@/utils/apiPage'
import { useBaseStore } from '@/stores/_base'
import { useQualityStore } from '@/stores/quality'

type Severity = 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL'

interface TemplateRow {
  id?: number
  templateNo?: string
  templateName?: string
  drawingNoPattern?: string
  items?: Array<{ itemName: string; standard: string; severity?: string }>
}

interface InspectionDetail {
  id?: number
  type?: string
  materialCode?: string
  drawingNo?: string
  remark?: string
  items?: Array<{
    id?: number
    itemName?: string
    standard?: string
    actual?: string
    measuredValue?: string
    result?: string
    severity?: string
  }>
}

const router = useRouter()
const qualityStore = useQualityStore()
const api = useBaseStore().api
const drawingNo = ref('')
const editingId = ref<number | null>(null)
const pageTitle = computed(() => (editingId.value ? '检验录入' : '新建检验单'))
const overallResult = ref<'PASS' | 'FAIL' | 'CONDITIONAL'>('PASS')
const disposition = ref<'RETURN' | 'REWORK' | 'SCRAP' | ''>('')
const defectQty = ref(1)
const conditionalReason = ref('')
const submitting = ref(false)

const form = ref<InspectionCreateRequest & {
  overallResult?: string
  disposition?: string
  defectQty?: number
  conditionalReason?: string
  drawingNo?: string
  inspectionId?: number
}>({
  materialCode: '',
  inspectionType: 'INCOMING',
  qualityStatus: 'PENDING',
  inspectItems: [],
  remark: '',
})

const inspectItems = ref<InspectionItemDTO[]>([])
const templateHint = ref('')
const templateHintType = ref<'success' | 'warning' | 'info'>('info')

const canSubmit = computed(() => {
  if (inspectItems.value.length === 0 || !drawingNo.value) return false
  if (overallResult.value === 'FAIL') {
    return !!disposition.value && defectQty.value >= 1
  }
  if (overallResult.value === 'CONDITIONAL') {
    return !!conditionalReason.value.trim()
  }
  return true
})

function onTypeChange() {
  // 类型切换不重置已加载的检验项
}

async function onDrawingSelect(d: Drawing) {
  drawingNo.value = d.drawingNo ?? ''
  form.value.materialCode = d.materialCode ?? ''
  if (d.title && !form.value.remark) {
    form.value.remark = `关联图纸：${d.drawingNo} · ${d.title}`
  }
  if (!editingId.value || inspectItems.value.length <= 1) {
    await loadTemplateForDrawing(d.drawingNo ?? '')
  }
}

async function loadTemplateForDrawing(drawingNoVal: string) {
  if (!drawingNoVal) return
  try {
    const raw = sessionStorage.getItem('inspection_template')
    if (raw) {
      const t = JSON.parse(raw) as TemplateRow
      const matched = !t.drawingNoPattern || drawingNoVal.includes(t.drawingNoPattern) || (t.drawingNoPattern.includes('%') && drawingNoVal.startsWith(t.drawingNoPattern.replace('%', '')))
      if (matched && t.items?.length) {
        applyTemplate(t)
        return
      }
    }
  } catch {
    /* ignore */
  }
  try {
    const data = unwrapResult<{ items?: TemplateRow[] }>(
      await api.get('/quality/inspection-templates', {
        params: {
          drawingNo: drawingNoVal,
          inspectionType: form.value.inspectionType,
          materialCode: form.value.materialCode || undefined,
        },
      }),
    )
    const matched = (data.items ?? [])[0]
    if (matched?.items?.length) {
      applyTemplate(matched)
    } else if (!editingId.value) {
      templateHintType.value = 'warning'
      templateHint.value = `未命中 ${drawingNoVal} 的检验模板，请手动添加检验项，或先去「管理检验模板」配置`
      inspectItems.value = []
    }
  } catch {
    templateHintType.value = 'info'
    templateHint.value = '模板服务暂不可用，请手动添加检验项'
    if (!editingId.value) inspectItems.value = []
  }
}

function applyTemplate(t: TemplateRow) {
  inspectItems.value = (t.items ?? []).map((it, idx) => ({
    itemName: it.itemName,
    standard: it.standard,
    measuredValue: '',
    result: 'OK' as const,
    severity: it.severity as Severity | undefined,
    sortOrder: idx,
  }))
  templateHintType.value = 'success'
  templateHint.value = `已自动加载模板「${t.templateName ?? t.templateNo ?? '模板'}」· 共 ${inspectItems.value.length} 项`
}

function addItem() {
  inspectItems.value.push({
    itemName: '',
    standard: '',
    measuredValue: '',
    result: 'OK',
    sortOrder: inspectItems.value.length,
  })
}

function removeItem(idx: number) {
  inspectItems.value.splice(idx, 1)
}

function severityTagType(sev?: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (sev) {
    case 'CRITICAL':
    case 'ERROR':
      return 'danger'
    case 'WARN':
      return 'warning'
    case 'INFO':
      return 'success'
    default:
      return 'info'
  }
}

function autoJudge(row: InspectionItemDTO) {
  if (!row.standard || !row.measuredValue) return
  const std = String(row.standard).trim()
  const val = parseFloat(String(row.measuredValue).replace(/[^0-9eE+\-.]/g, ''))
  if (Number.isNaN(val)) return
  let ok: boolean | null = null

  const plusMinus = std.match(/^([0-9.]+)\s*[±]\s*([0-9.]+)/)
  if (plusMinus) {
    const base = parseFloat(plusMinus[1])
    const tol = parseFloat(plusMinus[2])
    ok = val >= base - tol && val <= base + tol
  }
  if (ok === null) {
    const range = std.match(/^([0-9.]+)\s*[-–~]\s*([0-9.]+)/)
    if (range) {
      const lo = parseFloat(range[1])
      const hi = parseFloat(range[2])
      ok = val >= lo && val <= hi
    }
  }
  if (ok === null) {
    const cmp = std.match(/^([≤≥<>]=?)\s*([0-9.]+)/)
    if (cmp) {
      const target = parseFloat(cmp[2])
      const op = cmp[1]
      if (op === '≤' || op === '<=') ok = val <= target
      else if (op === '<') ok = val < target
      else if (op === '≥' || op === '>=') ok = val >= target
      else if (op === '>') ok = val > target
    }
  }
  if (ok === null) {
    const exact = parseFloat(std)
    if (!Number.isNaN(exact)) ok = Math.abs(val - exact) < 1e-9
  }
  if (ok === null) return
  row.result = ok ? 'OK' : 'NG'
}

function mapInspectTypeFromBackend(type?: string): InspectionCreateRequest['inspectionType'] {
  switch ((type ?? '').toUpperCase()) {
    case 'IQC': return 'INCOMING'
    case 'IPQC': return 'IN_PROCESS'
    case 'OQC': return 'OUTGOING'
    default: return 'INCOMING'
  }
}

function goTemplateManager() {
  router.push('/quality/inspection-template')
}

async function loadExistingInspection(id: number) {
  const detail = unwrapResult<InspectionDetail>(await qualityStore.getInspection(id))
  editingId.value = id
  form.value.materialCode = detail.materialCode ?? ''
  form.value.inspectionType = mapInspectTypeFromBackend(detail.type)
  form.value.remark = detail.remark ?? ''
  if (detail.drawingNo) {
    drawingNo.value = detail.drawingNo
  }
  const items = detail.items ?? []
  if (items.length > 0 && !(items.length === 1 && items[0].itemName === '待检验项')) {
    inspectItems.value = items.map((it, idx) => ({
      itemName: it.itemName ?? '',
      standard: it.standard ?? '',
      measuredValue: it.actual ?? it.measuredValue ?? '',
      result: (it.result === 'FAIL' || it.result === 'NG') ? 'NG' as const : 'OK' as const,
      severity: it.severity as Severity | undefined,
      sortOrder: idx,
    }))
    templateHintType.value = 'info'
    templateHint.value = '已加载检验单历史数据，请继续录入并提交判定'
  } else if (drawingNo.value) {
    await loadTemplateForDrawing(drawingNo.value)
  }
}

async function submit() {
  if (!canSubmit.value) {
    if (overallResult.value === 'FAIL') {
      ElMessage.warning('请选择处置方式')
    }
    return
  }

  const payload = {
    ...form.value,
    inspectItems: inspectItems.value,
    overallResult: overallResult.value,
    disposition: overallResult.value === 'FAIL' ? disposition.value : undefined,
    defectQty: overallResult.value === 'FAIL' ? defectQty.value : undefined,
    conditionalReason: overallResult.value === 'CONDITIONAL' ? conditionalReason.value : undefined,
    drawingNo: drawingNo.value,
    inspectionId: editingId.value ?? undefined,
  }

  submitting.value = true
  try {
    const created = unwrapResult<{
      inspectionNo?: string
      status?: string
      statusLabel?: string
      downstreamOrderNo?: string
    }>(await E7QualityService.createInspectionV1389(payload as InspectionCreateRequest))
    const statusLabel = created.statusLabel ?? created.status ?? '已提交'
    ElMessage.success(`检验报告已提交！状态：${statusLabel}`)
    if (created.downstreamOrderNo) {
      ElMessage.info(`已自动生成下游单据：${created.downstreamOrderNo}`)
    }
    router.push('/quality/inspection')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const q = router.currentRoute.value.query
  const idParam = q.id ?? q.inspectionId
  if (typeof idParam === 'string' && idParam) {
    await loadExistingInspection(Number(idParam))
    return
  }
  if (typeof q.materialCode === 'string') {
    form.value.materialCode = q.materialCode
  }
  const tid = q.templateId
  if (typeof tid === 'string') {
    api.get(`/quality/inspection-templates/${tid}`).then(r => {
      const t = unwrapResult<TemplateRow>(r)
      applyTemplate(t)
    }).catch(() => {/* ignore */})
  }
})
</script>

<style scoped>
.is-judged-ok :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #67c23a inset; }
.is-judged-ng :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #f56c6c inset; }
</style>
