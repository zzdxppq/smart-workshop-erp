<template>
  <div v-loading="loading" class="order-form-page">
    <div class="page-header">
      <div>
        <h2>{{ isNew ? '新建销售订单' : '编辑销售订单' }}</h2>
        <p v-if="form.order.orderNo" class="sub-no">订单号：{{ form.order.orderNo }}</p>
        <el-alert type="warning" :closable="false" show-icon title="订单为大，提交即生效，无需审批" style="margin-top: 8px" />
      </div>
      <div class="actions">
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单（无需审批）</el-button>
      </div>
    </div>

    <el-card>
      <el-form :model="form.order" label-width="100px">
        <el-form-item label="客户" required>
          <CustomerSelect v-model="form.order.customerId" @change="onCustomerChange" />
        </el-form-item>
        <el-form-item label="期望交期" required>
          <el-date-picker
            v-model="deliveryDateModel"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择交期"
            :disabled-date="disablePastDate"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="客户需求">
          <el-input
            v-model="form.order.comment"
            type="textarea"
            :rows="2"
            placeholder="可补充客户需求描述"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 12px">
      <template #header>
        <div class="card-header">
          <span>从图纸库添加产品</span>
          <el-button size="small" @click="addLine">+ 添加明细行</el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon title="从图纸库选用，数据完整直接带出；本流程不允许上传新图号" style="margin-bottom: 12px" />
      <el-table :data="form.items" border stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column label="客户图号 / 选图" min-width="200">
          <template #default="{ row, $index }">
            <DrawingPicker
              v-model="row.drawingNo"
              v-model:drawing-id="row.drawingId"
              :allow-upload="false"
              placeholder="搜索客户图号"
              @select="(d) => onDrawingSelect(d, $index)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="drawingNo" label="系统图号" min-width="140" />
        <el-table-column prop="productName" label="产品" min-width="100" />
        <el-table-column label="材质/规格" min-width="130">
          <template #default="{ row }">{{ row.material }} {{ row.spec }}</template>
        </el-table-column>
        <el-table-column label="单件重量" width="90">
          <template #default="{ row }">{{ row.unitWeight != null ? `${row.unitWeight} kg` : '—' }}</template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="canEditPrice"
              v-model="row.unitPrice"
              :min="0"
              :precision="2"
              :step="0.01"
              size="small"
              controls-position="right"
            />
            <span v-else>{{ row.unitPrice != null ? `¥${row.unitPrice}` : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100" align="right">
          <template #default="{ row }">¥{{ lineAmount(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLine($index)">删</el-button>
          </template>
        </el-table-column>
        <el-table-column type="expand" width="70" label="工艺/BOM">
          <template #default="{ row }">
            <div class="row-preview" v-if="row.processRoute || row.bomPreview?.hasBom">
              <div class="preview-toggle">
                <span class="preview-label">工艺路线：</span>
                <span class="preview-val">{{ formatProcessPreview(row.processRoute) || '—' }}</span>
              </div>
              <div v-if="row.bomPreview?.hasBom" class="preview-toggle">
                <span class="preview-label">BOM 物料：</span>
                <span class="preview-val">
                  <span v-for="(b, idx) in row.bomPreview.items" :key="idx" class="bom-item">
                    {{ b.materialCode }} × {{ b.qty }}{{ b.unit || '件' }}
                    <span v-if="idx < row.bomPreview.items.length - 1">；</span>
                  </span>
                </span>
              </div>
              <div v-else class="preview-toggle">
                <span class="preview-val muted">暂无 BOM（工程转化阶段定义）</span>
              </div>
            </div>
            <div v-else class="row-preview muted">选择已有图号后可查看工艺/BOM预览</div>
          </template>
        </el-table-column>
      </el-table>
      <div class="total-row">合计：<strong>¥{{ totalAmount }}</strong></div>
    </el-card>

    <el-card v-if="materialPreview.length" header="提交后将自动生成料号" style="margin-top: 12px">
      <el-table :data="materialPreview" border size="small">
        <el-table-column prop="customerDrawingNo" label="客户图号" min-width="140" />
        <el-table-column prop="drawingNo" label="系统图号" min-width="140" />
        <el-table-column label="料号状态" min-width="180">
          <template #default="{ row }">
            <span v-if="row.existing" class="mat-reuse">已有料号：{{ row.existing }}（复用）</span>
            <span v-else class="mat-new">将生成新料号</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { E3OrderService } from '@/api/generated/services/E3OrderService'
import type { Order } from '@/api/generated/models/Order'
import type { OrderItem } from '@/api/generated/models/OrderItem'
import type { Drawing } from '@/api/generated/models/Drawing'
import { unwrapResult } from '@/utils/apiPage'
import CustomerSelect from '@/components/form/CustomerSelect.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import {
  type OrderFormItem,
  type OrderFormOrder,
  disablePastDate,
} from '@/utils/orderPayload'
import { useOrderStore } from '@/stores/order'
import { useAuthStore } from '@/stores/auth'
import { useBaseStore } from '@/stores/_base'
import { hasAnyRole, ADMIN_ROLES } from '@/utils/roleAccess'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()
const auth = useAuthStore()
const baseStore = useBaseStore()
const loading = ref(false)
const submitting = ref(false)
const materialNoMap = ref<Record<string, string>>({})

const isNew = computed(() => route.name === 'OrderNew' || route.path.endsWith('/new'))
const orderId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : undefined
})

const canEditPrice = computed(() =>
  hasAnyRole(auth.userRoles, ['SALES_MGR', 'SALES_MANAGER', 'GM', ...ADMIN_ROLES]),
)

const form = ref<{ order: OrderFormOrder; items: OrderFormItem[] }>({
  order: { status: 'DRAFT', currency: 'CNY', isFa: 0, isNew: 0, isUrgent: 0 },
  items: [emptyLine()],
})

const deliveryDateModel = computed({
  get: () => form.value.order.deliveryDate ?? '',
  set: (v: string) => { form.value.order.deliveryDate = v || undefined },
})

const totalAmount = computed(() =>
  form.value.items.reduce((s, i) => s + lineAmount(i), 0),
)

const materialPreview = computed(() =>
  form.value.items
    .filter((i) => i.drawingNo && i.drawingNo !== '—')
    .map((i) => ({
      customerDrawingNo: i.customerDrawingNo || i.drawingNo,
      drawingNo: i.drawingNo,
      existing: materialNoMap.value[i.drawingNo!],
    })),
)

function emptyLine(): OrderFormItem {
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

function lineAmount(row: OrderFormItem) {
  const price = row.unitPrice ?? 0
  return Math.round((row.quantity ?? 0) * price * 100) / 100
}

function onCustomerChange(_id: number, row?: { name?: string }) {
  if (row?.name) form.value.order.customerName = row.name
}

function addLine() {
  form.value.items.push(emptyLine())
}

function removeLine(idx: number) {
  form.value.items.splice(idx, 1)
  if (!form.value.items.length) form.value.items.push(emptyLine())
}

async function onDrawingSelect(
  d: Drawing & { customerDrawingNo?: string; materialGrade?: string; specSize?: string; unitWeight?: number },
  rowIndex: number,
) {
  const row = form.value.items[rowIndex]
  row.drawingNo = d.drawingNo
  row.drawingId = d.id
  row.customerDrawingNo = d.customerDrawingNo ?? d.title ?? d.drawingNo
  row.productName = d.title ?? row.productName
  row.material = d.materialGrade ?? (d as { materialCode?: string }).materialCode ?? row.material
  row.spec = d.specSize ?? row.spec
  row.unitWeight = d.unitWeight != null ? Number(d.unitWeight) : row.unitWeight
  row.processRoute = formatProcessPreview(d.processRoute)
  // 按需加载 BOM 预览
  if (d.id) {
    try {
      const r = await baseStore.api.get(`/boms/preview/by-drawing/${d.id}`)
      const data = unwrapResult<any>(r)
      row.bomPreview = data?.hasBom ? data : { hasBom: false, items: [] }
    } catch {
      row.bomPreview = { hasBom: false, items: [] }
    }
  } else {
    row.bomPreview = { hasBom: false, items: [] }
  }
  refreshMaterialPreview()
}

function formatProcessPreview(raw?: string | null): string {
  if (!raw) return ''
  if (raw.includes('→')) return raw
  return raw.replace(/[,，]/g, ' → ')
}

async function refreshMaterialPreview() {
  const nos = form.value.items.map((i) => i.drawingNo).filter((n): n is string => !!n && n !== '—')
  if (!nos.length) {
    materialNoMap.value = {}
    return
  }
  try {
    materialNoMap.value = await orderStore.checkMaterialNos(nos)
  } catch {
    materialNoMap.value = {}
  }
}

watch(() => form.value.items.map((i) => i.drawingNo).join(','), refreshMaterialPreview)

async function persistDraft(): Promise<number | undefined> {
  if (!form.value.order.customerId) {
    ElMessage.warning('请选择客户')
    return undefined
  }
  const validItems = form.value.items.filter((i) => i.drawingId || i.drawingNo)
  if (!validItems.length) {
    ElMessage.warning('请至少从图纸库添加一条产品')
    return undefined
  }
  if (isNew.value) {
    const r = await orderStore.createDraft(form.value)
    return r?.id ?? orderId.value
  }
  if (orderId.value) {
    await orderStore.saveDraft(orderId.value, form.value)
    return orderId.value
  }
  return undefined
}

async function saveDraft() {
  loading.value = true
  try {
    const id = await persistDraft()
    ElMessage.success('草稿已保存')
    if (id && isNew.value) router.replace(`/sales/orders/${id}/edit`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (!form.value.order.deliveryDate) {
    ElMessage.warning('请填写期望交期')
    return
  }
  try {
    await ElMessageBox.confirm('提交后订单直接生效，并通知工程师进行工程转化。确认提交？', '提交订单', {
      type: 'warning',
    })
  } catch {
    return
  }
  submitting.value = true
  try {
    const id = await persistDraft()
    if (!id) return
    const r = await orderStore.submitOrder(id)
    ElMessage.success(String(r.message ?? '订单已提交'))
    router.push(`/sales/orders/${id}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (isNew.value || !orderId.value) return
  loading.value = true
  try {
    const r = unwrapResult(await E3OrderService.getOrder(orderId.value))
    const rawOrder = (r as { order?: Order })?.order ?? (r as Order) ?? {}
    if (rawOrder.status && rawOrder.status !== 'DRAFT') {
      router.replace(`/sales/orders/${orderId.value}`)
      return
    }
    form.value = {
      order: {
        ...rawOrder,
        isFa: rawOrder.isFa ? 1 : 0,
        isNew: rawOrder.isNew ? 1 : 0,
        isUrgent: rawOrder.isUrgent ? 1 : 0,
      },
      items: ((r as { items?: OrderItem[] })?.items ?? [emptyLine()]).map((item) => ({
        ...item,
        customerDrawingNo: (item as OrderFormItem).customerDrawingNo,
        productName: (item as OrderFormItem).productName,
        unitWeight: (item as OrderFormItem).unitWeight,
        drawingId: (item as OrderFormItem).drawingId,
        processRoute: (item as OrderFormItem).processRoute,
      })),
    }
    if (!form.value.items.length) form.value.items.push(emptyLine())
    await refreshMaterialPreview()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.order-form-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.sub-no { margin: 4px 0 0; font-size: 13px; color: var(--erp-text-secondary); }
.actions { display: flex; gap: 8px; flex-shrink: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.total-row { margin-top: 12px; text-align: right; font-size: 14px; }
.process-line { font-size: 13px; padding: 4px 0; color: var(--erp-text-secondary); }
.hint { font-size: 12px; color: var(--erp-text-muted); margin: 8px 0 0; }
.mat-reuse { color: var(--el-color-success); }
.mat-new { color: var(--el-color-warning); }
.row-preview {
  padding: 8px 12px;
  background: var(--erp-bg-subtle, #f9fafb);
  border-radius: 4px;
  font-size: 12px;
}
.preview-toggle {
  display: flex;
  gap: 8px;
  line-height: 1.8;
}
.preview-label {
  flex-shrink: 0;
  color: var(--erp-text-muted, #6b7280);
  min-width: 72px;
}
.preview-val {
  color: var(--erp-text-primary, #374151);
}
.bom-item + .bom-item::before { content: '；'; }
.muted { color: var(--erp-text-muted, #9ca3af); font-style: italic; }
</style>
