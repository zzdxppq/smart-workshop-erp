<template>
  <ErpPageShell :title="pageTitle" :description="pageDescription">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item v-if="!engineeringMode">
        <el-button type="primary" @click="$router.push('/sales/orders/new')">新建销售订单</el-button>
      </el-form-item>
      <el-form-item v-if="engineeringMode" label="任务状态">
        <el-select v-model="engineerPhaseFilter" clearable placeholder="全部" @change="reload">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
      </el-form-item>
      <el-form-item v-else label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="reload">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已生效" value="APPROVED" />
          <el-option label="工程转化中" value="PROCESSING" />
          <el-option label="待转产" value="PENDING_PRODUCTION" />
          <el-option label="已转工单" value="IN_PRODUCTION" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="生产中" value="PRODUCING" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="reload">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="140" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="amount" label="金额" width="140" align="right">
        <template #default="{ row }">
          <MoneyAmount :model-value="Number(row.amount ?? 0)" display-only />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <EngineerTaskStatusTag v-if="engineeringMode" :phase="engineerPhaseFromRow(row)" />
          <ErpStatusTag v-else :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="ownerUserId" label="创建人" width="100">
        <template #default="{ row }">
          {{ row.ownerUserId ? '用户' + row.ownerUserId : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="图纸" width="130">
        <template #default="{ row }">
          <DrawingNoCell
            v-if="row.drawingId || row.drawingNo || row.materialCode"
            :drawing-id="row.drawingId"
            :drawing-no="row.drawingNo"
            :material-code="row.materialCode"
          />
          <span v-else class="erp-text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="engineeringMode ? 200 : 240" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="engineeringMode && row.id"
            type="primary"
            size="small"
            @click="openDefineProcess(row)"
          >
            定义工艺
          </el-button>
          <el-button v-if="row.id && !engineeringMode && row.status === 'DRAFT'" size="small" type="primary" @click="$router.push(`/sales/orders/${row.id}/edit`)">编辑</el-button>
          <el-button v-if="row.id && !engineeringMode" size="small" class="erp-btn-ghost" @click="$router.push(`/sales/orders/${row.id}`)">详情</el-button>
          <el-button v-if="engineeringMode && row.id" size="small" class="erp-btn-ghost" @click="$router.push(`/sales/orders/${row.id}`)">详情</el-button>
          <el-button v-if="row.id && !engineeringMode && canChangeStatus(row.status)" size="small" class="erp-btn-secondary" @click="$router.push(`/sales/orders/${row.id}/change`)">变更</el-button>
          <el-button v-if="row.id && !engineeringMode" size="small" class="erp-btn-ghost" @click="openTimeline(row.id)">时间线</el-button>
        </template>
      </el-table-column>
    </el-table>

    <DefineProcessDrawer
      v-if="activeOrder?.id"
      v-model="drawerOpen"
      mode="order"
      :ref-id="activeOrder.id"
      :ref-no="activeOrder.orderNo ?? ''"
      :title="activeOrder.customerName"
      @saved="reload"
    />

    <!-- 时间线侧滑抽屉（侧边栏滑出，不必跳转子页面） -->
    <el-drawer
      v-model="timelineDrawerVisible"
      title="订单时间线"
      direction="rtl"
      size="640px"
      :destroy-on-close="true"
    >
      <OrderTimelinePanel v-if="timelineOrderId" :order-id="timelineOrderId" />
    </el-drawer>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      @current-change="reload"
      @size-change="reload"
    />

  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { E3OrderService } from '@/api/generated/services/E3OrderService'
import type { Order } from '@/api/generated/models/Order'
import { parsePageItems } from '@/utils/apiPage'
import OrderTimelinePanel from '@/components/sales/OrderTimelinePanel.vue'
import EngineerTaskStatusTag from '@/components/engineering/EngineerTaskStatusTag.vue'
import DefineProcessDrawer from '@/components/engineering/DefineProcessDrawer.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import {
  type EngineerTaskPhase,
  engineerPhaseFromRow,
} from '@/utils/engineeringTask'
import { useEngineeringStore } from '@/stores/engineering'

const props = withDefaults(defineProps<{ engineeringMode?: boolean }>(), { engineeringMode: false })
const route = useRoute()
const eng = useEngineeringStore()

const engineeringMode = computed(() => props.engineeringMode)

const pageTitle = computed(() => (engineeringMode.value ? '订单工程转化' : '销售订单'))
const pageDescription = computed(() =>
  engineeringMode.value
    ? '场景 B：订单确认后细化工艺参数、编制 BOM，提交后进入待转产池'
    : '订单状态色标 · 金额右对齐 · 图纸一键查看。',
)

const orders = ref<(Order & { drawingId?: number; drawingNo?: string; materialCode?: string })[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const status = ref<Order['status']>()
const engineerPhaseFilter = ref<EngineerTaskPhase | ''>('')
const drawerOpen = ref(false)
const activeOrder = ref<(Order & { drawingId?: number; drawingNo?: string; materialCode?: string }) | null>(null)

// 时间线抽屉状态（替代原 OrderTimeline 子页面）
const timelineDrawerVisible = ref(false)
const timelineOrderId = ref<number | null>(null)
function openTimeline(orderId: number) {
  timelineOrderId.value = orderId
  timelineDrawerVisible.value = true
}

async function reload() {
  loading.value = true
  try {
    if (engineeringMode.value) {
      const { items, total: t } = await eng.listOrderQueue({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        phase: engineerPhaseFilter.value || undefined,
      })
      orders.value = items as (Order & {
        drawingId?: number
        drawingNo?: string
        materialCode?: string
        engineerPhase?: EngineerTaskPhase
      })[]
      total.value = t
    } else {
      const r = await E3OrderService.listOrders(pageNum.value, pageSize.value, status.value)
      const { items, total: t } = parsePageItems(r)
      orders.value = items as (Order & { drawingId?: number; drawingNo?: string; materialCode?: string })[]
      total.value = t
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function canChangeStatus(s?: string): boolean {
  return ['DRAFT', 'SUBMITTED', 'CONFIRMED'].includes(s ?? '')
}

function openDefineProcess(row: Order) {
  if (!row.id) return
  activeOrder.value = row
  drawerOpen.value = true
}

onMounted(async () => {
  if (engineeringMode.value && !engineerPhaseFilter.value) {
    engineerPhaseFilter.value = 'PENDING'
  }
  await reload()
  const refId = Number(route.query.refId)
  if (engineeringMode.value && refId) {
    const row = orders.value.find((o) => o.id === refId)
    if (row) openDefineProcess(row)
  }
})
</script>

<style scoped>
.erp-text-muted {
  color: var(--erp-text-muted);
}
:deep(.el-table .cell .el-button + .el-button) {
  margin-left: 6px;
}
</style>
