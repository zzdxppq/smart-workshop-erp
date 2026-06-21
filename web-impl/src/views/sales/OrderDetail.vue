<template>
  <div v-loading="loading" class="order-detail">
    <el-page-header @back="$router.push('/sales/orders')">
      <template #content>订单详情</template>
    </el-page-header>

    <el-alert
      v-if="profitAlert && profitAlert.alertLevel !== 'NORMAL'"
      :type="profitAlert.alertLevel === 'CRITICAL' ? 'error' : 'warning'"
      :closable="false"
      :title="profitAlert.message || '利润率预警'"
      show-icon
      style="margin-top: 12px"
    />

    <el-card v-if="order" style="margin-top: 16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ order.customerName }}</el-descriptions-item>
        <el-descriptions-item label="金额">
          <MoneyAmount :model-value="Number(order.amount ?? 0)" display-only />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <ErpStatusTag :status="order.status" />
        </el-descriptions-item>
        <el-descriptions-item label="交期">{{ order.deliveryDate ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="生产工单">
          <template v-if="order.productionOrderNo">
            <el-link type="primary" @click="goWorkorder">{{ order.productionOrderNo }}</el-link>
          </template>
          <span v-else class="erp-text-muted">尚未转工单</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createdAt ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <h3 v-if="order?.items?.length" style="margin-top: 16px">订单明细</h3>
    <el-table v-if="order?.items?.length" :data="order.items" stripe border>
      <el-table-column label="客户图号" min-width="130">
        <template #default="{ row }">{{ row.customerDrawingNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="系统图号" min-width="140">
        <template #default="{ row }">
          <DrawingNoCell
            :drawing-no="row.drawingNo"
            :drawing-id="row.drawingId"
            :material-code="row.materialNo"
          />
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="产品" min-width="100" />
      <el-table-column prop="materialNo" label="料号" min-width="110" />
      <el-table-column prop="material" label="材质" min-width="100" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column prop="unitPrice" label="单价" width="110" />
      <el-table-column prop="amount" label="金额" width="110" />
    </el-table>

    <el-card v-if="processLines.length" header="工艺预览" style="margin-top: 12px">
      <div v-for="(p, i) in processLines" :key="i" class="process-line">{{ p }}</div>
    </el-card>

    <div style="margin-top: 16px">
      <el-button
        v-if="order?.status === 'DRAFT'"
        type="primary"
        @click="$router.push(`/sales/orders/${orderId}/edit`)"
      >
        继续编辑
      </el-button>
      <el-button
        v-if="canConvert"
        type="primary"
        :loading="converting"
        @click="convertToWorkorder"
      >
        转工单
      </el-button>
      <el-button class="erp-btn-ghost" :disabled="!canChange" @click="changeDialogVisible = true">订单变更</el-button>
      <el-button class="erp-btn-ghost" @click="timelineDialogVisible = true">时间线</el-button>
      <el-button
        v-if="order?.drawingId"
        class="erp-btn-ghost"
        @click="openDrawingViewer(order.drawingId)"
      >
        查看图纸
      </el-button>
    </div>

    <el-dialog
      v-model="changeDialogVisible"
      title="订单变更"
      width="560px"
      destroy-on-close
    >
      <OrderChangePanel
        v-if="changeDialogVisible"
        :order-id="orderId"
        @success="onChangeSuccess"
        @cancel="changeDialogVisible = false"
      />
    </el-dialog>

    <el-drawer
      v-model="timelineDialogVisible"
      title="订单时间线"
      size="480px"
      destroy-on-close
    >
      <OrderTimelinePanel v-if="timelineDialogVisible" :order-id="orderId" />
    </el-drawer>

    <el-dialog v-model="drawerVisible" title="图纸详情" width="80%" destroy-on-close>
      <DrawingViewer v-if="drawerVisible && activeDrawingId" :drawing-id="activeDrawingId" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { E3OrderTransferService } from '@/api/generated/services/E3OrderTransferService'
import { unwrapResult } from '@/utils/apiPage'
import { useAuthStore } from '@/stores/auth'
import { hasAnyRole } from '@/utils/roleAccess'
import DrawingViewer from '@/views/crm/drawing/DrawingViewer.vue'
import OrderChangePanel from '@/components/sales/OrderChangePanel.vue'
import OrderTimelinePanel from '@/components/sales/OrderTimelinePanel.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const route = useRoute()
const router = useRouter()
const api = useBaseStore().api
const loading = ref(false)
const converting = ref(false)
const order = ref<{
  orderNo?: string
  customerName?: string
  amount?: number
  status?: string
  deliveryDate?: string
  createdAt?: string
  productionOrderNo?: string
  drawingId?: number
  items?: {
    drawingNo?: string
    drawingId?: number
    customerDrawingNo?: string
    productName?: string
    materialNo?: string
    material?: string
    processRoute?: string
    quantity?: number
    unitPrice?: number
    amount?: number
  }[]
} | null>(null)
const profitAlert = ref<{ alertLevel?: string; message?: string; profitRate?: number } | null>(null)
const drawerVisible = ref(false)
const activeDrawingId = ref<number | null>(null)
const changeDialogVisible = ref(false)
const timelineDialogVisible = ref(false)

const auth = useAuthStore()
const orderId = computed(() => String(route.params.id))
const canConvert = computed(
  () =>
    hasAnyRole(auth.userRoles, ['PROD_MGR', 'PRODUCTION_MANAGER', 'GM']) &&
    (order.value?.status === 'CONFIRMED' || order.value?.status === 'PENDING_PRODUCTION') &&
    !order.value?.productionOrderNo,
)

const processLines = computed(() =>
  (order.value?.items ?? [])
    .filter((i) => i.processRoute)
    .map((i) => `${i.customerDrawingNo || i.drawingNo}：${i.processRoute}`),
)

const canChange = computed(() =>
  ['DRAFT', 'SUBMITTED', 'CONFIRMED', 'APPROVED'].includes(order.value?.status ?? ''),
)

function openDrawingViewer(drawingId: number) {
  activeDrawingId.value = drawingId
  drawerVisible.value = true
}

async function goWorkorder() {
  if (!order.value?.productionOrderNo) return
  // 跳转工单详情页
  router.push(`/production/workorder-detail/${order.value.productionOrderNo}`)
}

async function reloadOrder() {
  loading.value = true
  try {
    const data = unwrapResult(await api.get(`/orders/${orderId.value}`)) as Record<string, unknown>
    const o = (data.order as Record<string, unknown>) ?? data
    order.value = {
      orderNo: String(data.orderNo ?? o.orderNo ?? ''),
      customerName: String(data.customerName ?? o.customerName ?? ''),
      amount: Number(data.amount ?? o.totalAmount ?? 0),
      status: String(data.status ?? o.status ?? ''),
      deliveryDate: String(data.deliveryDate ?? o.deliveryDate ?? ''),
      createdAt: String(data.createdAt ?? o.createdAt ?? ''),
      productionOrderNo: String(data.productionOrderNo ?? o.productionOrderNo ?? '') || undefined,
      items: (data.items as typeof order.value.items) ?? [],
    }
    profitAlert.value = (data.profitAlert as typeof profitAlert.value) ?? null
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onChangeSuccess() {
  changeDialogVisible.value = false
  reloadOrder()
}

async function convertToWorkorder() {
  try {
    await ElMessageBox.confirm('确认将该销售订单转为生产工单？', '转工单', { type: 'info' })
  } catch {
    return
  }
  converting.value = true
  try {
    const data = unwrapResult<Record<string, unknown>>(
      await E3OrderTransferService.convertOrderToProduction(Number(orderId.value)),
    )
    const woNo = String(data.productionOrderNo ?? data.workorderNo ?? '')
    order.value = {
      ...order.value,
      status: String(data.status ?? 'PRODUCING'),
      productionOrderNo: woNo,
    }
    ElMessage.success(woNo ? `已生成工单 ${woNo}` : '转工单成功')
    const woId = data.workorderId ?? (data.workorder as Record<string, unknown>)?.id
    if (woId) {
      router.push(`/production/workorder-detail/${woId}`)
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转工单失败')
  } finally {
    converting.value = false
  }
}

onMounted(reloadOrder)
</script>

<style scoped>
.order-detail {
  padding: 16px;
}
.process-line {
  font-size: 13px;
  padding: 4px 0;
  color: var(--erp-text-secondary);
}
</style>
