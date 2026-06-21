<template>
  <div v-loading="loading" class="pending-production-detail">
    <el-page-header @back="goBack">
      <template #content>待转产订单详情</template>
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
          <span class="amount-hint">（仅供排产参考）</span>
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
      <el-table-column label="图号" min-width="160">
        <template #default="{ row }">
          <DrawingNoCell
            :drawing-no="row.drawingNo"
            :drawing-id="row.drawingId"
            :material-code="row.material"
          />
        </template>
      </el-table-column>
      <el-table-column prop="material" label="材质/物料" min-width="140" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column prop="unitPrice" label="单价" width="110" />
      <el-table-column prop="amount" label="金额" width="110" />
    </el-table>

    <div style="margin-top: 16px">
      <el-button
        v-if="canConvert"
        type="primary"
        :loading="converting"
        @click="convertToWorkorder"
      >
        转工单
      </el-button>
      <el-button
        v-if="order?.drawingId"
        class="erp-btn-ghost"
        @click="openDrawingViewer(order.drawingId)"
      >
        查看图纸
      </el-button>
      <el-button class="erp-btn-ghost" @click="timelineDialogVisible = true">时间线</el-button>
    </div>

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
  items?: { drawingNo?: string; drawingId?: number; material?: string; quantity?: number; unitPrice?: number; amount?: number }[]
} | null>(null)
const profitAlert = ref<{ alertLevel?: string; message?: string; profitRate?: number } | null>(null)
const drawerVisible = ref(false)
const activeDrawingId = ref<number | null>(null)
const timelineDialogVisible = ref(false)

const auth = useAuthStore()
const orderId = computed(() => String(route.params.id))
const canConvert = computed(
  () =>
    hasAnyRole(auth.userRoles, ['PROD_MGR', 'PRODUCTION_MANAGER', 'GM']) &&
    order.value?.status === 'CONFIRMED' &&
    !order.value?.productionOrderNo,
)

function goBack() {
  router.push('/production/pending-production')
}

function openDrawingViewer(drawingId: number) {
  activeDrawingId.value = drawingId
  drawerVisible.value = true
}

async function goWorkorder() {
  if (!order.value?.productionOrderNo) return
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
.pending-production-detail {
  padding: 16px;
}

.amount-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--erp-text-secondary);
}
</style>
