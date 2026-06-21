<template>
  <div v-loading="loading" class="quote-form-page">
    <div class="page-header">
      <div>
        <h2>{{ isNew ? '新建报价' : '编辑报价' }}</h2>
        <p v-if="form.quote.quoteNo" class="sub-no">报价单号：{{ form.quote.quoteNo }}</p>
      </div>
      <div class="actions">
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button v-if="canSubmitEngineer" type="primary" @click="submitToEngineer">提交给工程师 →</el-button>
        <el-button v-if="canSubmitApproval" type="success" @click="submitApproval">提交审批</el-button>
        <el-button v-if="canExportPdf" :loading="exporting" @click="exportPdf">导出 PDF</el-button>
        <el-button v-if="canSendEmail" type="primary" :loading="sendingEmail" @click="sendCustomerEmail">发送客户邮箱</el-button>
      </div>
    </div>

    <el-card>
      <el-form :model="form.quote" label-width="100px">
        <el-form-item label="客户" required>
          <CustomerSelect v-model="form.quote.customerId" />
        </el-form-item>
        <el-form-item label="客户需求">
          <el-input
            v-model="form.quote.comment"
            type="textarea"
            :rows="3"
            placeholder="整体描述，如表面粗糙度、表处要求等"
            :disabled="!editable"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>报价明细行（支持多个图号）</span>
          <el-button v-if="editable" size="small" @click="addLine">+ 添加明细行</el-button>
        </div>
      </template>
      <el-table :data="form.items" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="客户图号 / 选图" min-width="220">
          <template #default="{ row, $index }">
            <DrawingPicker
              v-if="editable"
              v-model="row.drawingNo"
              v-model:drawing-id="row.drawingId"
              :allow-upload="true"
              placeholder="搜索客户图号或上传"
              @select="(d) => onDrawingSelect(d, $index)"
            />
            <span v-else>{{ row.customerDrawingNo || row.drawingNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="100">
          <template #default="{ row }">
            <el-input v-if="editable" v-model="row.productName" size="small" />
            <span v-else>{{ row.productName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="材质/规格" min-width="140">
          <template #default="{ row }">
            <template v-if="editable">
              <el-input v-model="row.material" size="small" placeholder="材质" style="margin-bottom: 4px" />
              <el-input v-model="row.spec" size="small" placeholder="规格尺寸" />
            </template>
            <span v-else>{{ row.material }} {{ row.spec }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单件重量" width="100">
          <template #default="{ row }">
            <el-input-number v-if="editable" v-model="row.unitWeight" :min="0" :step="0.1" size="small" controls-position="right" />
            <span v-else>{{ row.unitWeight ?? '—' }} kg</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }">
            <el-input-number v-if="editable" v-model="row.quantity" :min="1" size="small" controls-position="right" />
            <span v-else>{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="right">
          <template #default="{ row }">
            {{ row.unitPrice && row.unitPrice > 0 ? `¥${row.unitPrice}` : '待计算' }}
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100" align="right">
          <template #default="{ row }">¥{{ lineAmount(row) }}</template>
        </el-table-column>
        <el-table-column v-if="editable" label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLine($index)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="total-row">合计：<strong>¥{{ totalAmount }}</strong></div>
    </el-card>

    <el-card v-if="processPreview.length" header="工艺预览（只读，工程师定义后显示）" style="margin-top: 12px">
      <div v-for="(p, i) in processPreview" :key="i" class="process-line">{{ p }}</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { E2QuoteService } from '@/api/generated/services/E2QuoteService'
import type { Quote } from '@/api/generated/models/Quote'
import type { QuoteItem } from '@/api/generated/models/QuoteItem'
import type { Drawing } from '@/api/generated/models/Drawing'
import { unwrapResult } from '@/utils/apiPage'
import CustomerSelect from '@/components/form/CustomerSelect.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import { type QuoteFormItem, type QuoteFormQuote } from '@/utils/quotePayload'
import { useQuoteStore } from '@/stores/quote'

const route = useRoute()
const router = useRouter()
const quoteStore = useQuoteStore()
const loading = ref(false)
const exporting = ref(false)
const sendingEmail = ref(false)

const isNew = computed(() => route.name === 'QuoteNew' || route.path.endsWith('/new'))
const quoteId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : undefined
})

const form = ref<{ quote: QuoteFormQuote; items: QuoteFormItem[] }>({
  quote: { status: 'DRAFT', currency: 'CNY', isFa: 0, isNew: 0 },
  items: [emptyLine()],
})

const editable = computed(() => form.value.quote.status === 'DRAFT' || !form.value.quote.status)
const canSubmitEngineer = computed(() => editable.value && !!quoteId.value)
const canSubmitApproval = computed(() =>
  (form.value.quote.status as string) === 'PENDING_ENG' && form.value.quote.engineerCompleted === 1,
)
const canExportPdf = computed(() => form.value.quote.status === 'APPROVED' && !!quoteId.value)
const canSendEmail = computed(() => canExportPdf.value)

const totalAmount = computed(() =>
  form.value.items.reduce((s, i) => s + lineAmount(i), 0),
)

const processPreview = computed(() =>
  form.value.items
    .filter((i) => i.processRoute)
    .map((i) => `${i.customerDrawingNo || i.drawingNo}：${i.processRoute}`),
)

function emptyLine(): QuoteFormItem {
  return {
    drawingNo: '',
    customerDrawingNo: '',
    material: '',
    spec: '',
    quantity: 1,
    unitPrice: 0,
    unitWeight: undefined,
    productName: '',
  }
}

function lineAmount(row: QuoteFormItem) {
  const price = row.unitPrice ?? 0
  return Math.round((row.quantity ?? 0) * price * 100) / 100
}

function onCustomerChange(_id: number, row?: { name?: string }) {
  if (row?.name) form.value.quote.customerName = row.name
}

function addLine() {
  form.value.items.push(emptyLine())
}

function removeLine(idx: number) {
  form.value.items.splice(idx, 1)
}

function onDrawingSelect(d: Drawing & { customerDrawingNo?: string; materialGrade?: string; specSize?: string; unitWeight?: number }, rowIndex: number) {
  const row = form.value.items[rowIndex]
  row.drawingNo = d.drawingNo
  row.drawingId = d.id
  row.customerDrawingNo = d.customerDrawingNo ?? d.title ?? d.drawingNo
  row.productName = d.title ?? row.productName
  row.material = d.materialGrade ?? d.materialCode ?? row.material
  row.spec = d.specSize ?? row.spec
  row.unitWeight = d.unitWeight != null ? Number(d.unitWeight) : row.unitWeight
}

async function persistDraft(): Promise<number | undefined> {
  if (!form.value.quote.customerId) {
    ElMessage.warning('请选择客户')
    return undefined
  }
  if (!form.value.items.length || !form.value.items.some((i) => i.drawingNo || i.customerDrawingNo)) {
    ElMessage.warning('请至少添加一条含图号的明细')
    return undefined
  }
  if (isNew.value) {
    const r = await quoteStore.createDraft(form.value) as { id?: number }
    return r?.id ?? quoteId.value
  }
  if (quoteId.value) {
    await quoteStore.saveDraft(quoteId.value, form.value)
    return quoteId.value
  }
  return undefined
}

async function saveDraft() {
  loading.value = true
  try {
    const id = await persistDraft()
    ElMessage.success('草稿已保存')
    if (id && isNew.value) router.replace(`/sales/quotes/${id}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    loading.value = false
  }
}

async function submitToEngineer() {
  loading.value = true
  try {
    const id = await persistDraft()
    if (!id) return
    await quoteStore.submitToEngineer(id)
    ElMessage.success('已提交给工程师，请在「报价工艺定义」中处理')
    router.push('/engineering/quote-confirmation')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    loading.value = false
  }
}

async function submitApproval() {
  if (!quoteId.value) return
  loading.value = true
  try {
    await quoteStore.submitForApproval(quoteId.value)
    ElMessage.success('已提交审批')
    router.push('/sales/quotes/approval')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    loading.value = false
  }
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function exportPdf() {
  if (!quoteId.value) return
  exporting.value = true
  try {
    const blob = await quoteStore.exportPdf(quoteId.value)
    const name = `${form.value.quote.quoteNo || 'quote-' + quoteId.value}.pdf`
    downloadBlob(blob, name)
    ElMessage.success('PDF 已下载')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function sendCustomerEmail() {
  if (!quoteId.value) return
  sendingEmail.value = true
  try {
    const r = await quoteStore.sendToCustomerEmail(quoteId.value)
    ElMessage.success(`已发送至 ${(r as { toAddress?: string }).toAddress ?? '客户邮箱'}`)
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || '发送失败'
    if (msg.includes('CUSTOMER_EMAIL_MISSING') || msg.includes('邮箱')) {
      ElMessage.error('客户档案未维护联系邮箱，请先在客户档案中填写')
    } else {
      ElMessage.error(msg)
    }
  } finally {
    sendingEmail.value = false
  }
}

onMounted(async () => {
  if (isNew.value || !quoteId.value) return
  loading.value = true
  try {
    const r = unwrapResult(await E2QuoteService.getQuote(quoteId.value))
    const rawQuote = (r as { quote?: Quote & { engineerCompleted?: number } })?.quote ?? (r as Quote) ?? {}
    form.value = {
      quote: {
        ...rawQuote,
        isFa: rawQuote.isFa ? 1 : 0,
        isNew: rawQuote.isNew ? 1 : 0,
        engineerCompleted: (rawQuote as { engineerCompleted?: number }).engineerCompleted,
      },
      items: ((r as { items?: QuoteItem[] })?.items ?? [emptyLine()]).map((item) => ({
        ...item,
        customerDrawingNo: (item as QuoteFormItem).customerDrawingNo,
        productName: (item as QuoteFormItem).productName,
        unitWeight: (item as QuoteFormItem).unitWeight,
        drawingId: (item as QuoteFormItem).drawingId,
      })),
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.quote-form-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.sub-no { margin: 4px 0 0; font-size: 13px; color: var(--erp-text-secondary); }
.actions { display: flex; gap: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.total-row { margin-top: 12px; text-align: right; font-size: 14px; }
.process-line { font-size: 13px; padding: 4px 0; color: var(--erp-text-secondary); }
</style>
