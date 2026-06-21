<template>
  <ErpPageShell
    title="工程师驾驶舱"
    description="V2.1 · 报价工艺定义（算价）→ 销售订单提交后的工程转化（细化工艺 + BOM）→ 待转产。"
  >
    <RoleWorkflowPanel />

    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      <el-button @click="$router.push('/engineering/quote-confirmation')">报价工艺定义</el-button>
      <el-button @click="$router.push('/engineering/order-conversion')">订单工程转化</el-button>
    </div>

    <DashboardKpiGrid :items="kpis" />

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="12">
        <el-card header="待报价工艺定义" shadow="never">
          <el-table v-loading="loading" :data="quoteQueue" size="small" stripe max-height="300">
            <el-table-column prop="quoteNo" label="报价单号" min-width="130" />
            <el-table-column prop="customerName" label="客户" min-width="100" show-overflow-tooltip />
            <el-table-column label="任务" width="90">
              <template #default="{ row }">
                <EngineerTaskStatusTag :phase="engineerPhaseFromRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goQuoteProcess(row)">定义工艺</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="card-footer">
            <el-button link type="primary" @click="$router.push('/engineering/quote-confirmation')">全部报价待办 →</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :md="12">
        <el-card header="待订单工程转化" shadow="never">
          <el-table v-loading="loading" :data="orderQueue" size="small" stripe max-height="300">
            <el-table-column prop="orderNo" label="订单号" min-width="130" />
            <el-table-column prop="customerName" label="客户" min-width="100" show-overflow-tooltip />
            <el-table-column prop="deliveryDate" label="交期" width="100" />
            <el-table-column label="任务" width="90">
              <template #default="{ row }">
                <EngineerTaskStatusTag :phase="engineerPhaseFromRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goOrderConvert(row)">工程转化</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="card-footer">
            <el-button link type="primary" @click="$router.push('/engineering/order-conversion')">全部订单待办 →</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="14">
        <el-card header="工程师工作量分布" shadow="never">
          <v-chart :option="workloadChart" autoresize style="height: 280px" />
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card header="快捷入口" shadow="never">
          <div class="quick-grid">
            <el-button type="primary" @click="$router.push('/engineering/quote-confirmation')">报价工艺定义</el-button>
            <el-button type="success" @click="$router.push('/engineering/order-conversion')">订单工程转化</el-button>
            <el-button @click="$router.push('/engineering/my-tasks')">待办任务中心</el-button>
            <el-button @click="$router.push('/engineering/data?tab=drawings')">工程数据 · 图纸</el-button>
            <el-button @click="$router.push('/engineering/data?tab=process')">工艺库</el-button>
            <el-button @click="$router.push('/engineering/data?tab=boms')">BOM</el-button>
            <el-button @click="$router.push('/production/pending-production')">待转产订单池</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import EngineerTaskStatusTag from '@/components/engineering/EngineerTaskStatusTag.vue'
import { useEngineeringStore } from '@/stores/engineering'
import { engineerPhaseFromRow } from '@/utils/engineeringTask'

const router = useRouter()
const eng = useEngineeringStore()
const loading = ref(false)

const quoteQueue = ref<Record<string, unknown>[]>([])
const orderQueue = ref<Record<string, unknown>[]>([])
const quoteInProgress = ref(0)
const orderInProgress = ref(0)
const quoteCompleted = ref(0)
const orderCompleted = ref(0)

const kpis = computed(() => [
  { key: 'q-pending', label: '待报价工艺', value: quoteQueue.value.length, color: '#bf8700' },
  { key: 'o-pending', label: '待订单转化', value: orderQueue.value.length, color: '#0969da' },
  { key: 'q-proc', label: '报价处理中', value: quoteInProgress.value, color: '#6366f1' },
  { key: 'o-proc', label: '订单转化中', value: orderInProgress.value, color: '#9333ea' },
])

const workloadChart = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    data: [
      { name: '待报价工艺', value: quoteQueue.value.length },
      { name: '报价处理中', value: quoteInProgress.value },
      { name: '待订单转化', value: orderQueue.value.length },
      { name: '订单转化中', value: orderInProgress.value },
      { name: '已完成(估)', value: quoteCompleted.value + orderCompleted.value },
    ].filter((d) => d.value > 0),
  }],
}))

function goQuoteProcess(row: Record<string, unknown>) {
  router.push({
    path: '/engineering/quote-confirmation',
    query: { refId: String(row.id ?? ''), phase: 'PENDING' },
  })
}

function goOrderConvert(row: Record<string, unknown>) {
  router.push({ path: '/engineering/order-conversion', query: { refId: String(row.id ?? '') } })
}

async function load() {
  loading.value = true
  try {
    const [qPending, qProg, qDone, oPending, oProg, oDone] = await Promise.all([
      eng.listQuoteQueue({ pageNum: 1, pageSize: 8, phase: 'PENDING' }),
      eng.listQuoteQueue({ pageNum: 1, pageSize: 1, phase: 'IN_PROGRESS' }),
      eng.listQuoteQueue({ pageNum: 1, pageSize: 1, phase: 'COMPLETED' }),
      eng.listOrderQueue({ pageNum: 1, pageSize: 8, phase: 'PENDING' }),
      eng.listOrderQueue({ pageNum: 1, pageSize: 1, phase: 'IN_PROGRESS' }),
      eng.listOrderQueue({ pageNum: 1, pageSize: 1, phase: 'COMPLETED' }),
    ])
    quoteQueue.value = qPending.items as Record<string, unknown>[]
    orderQueue.value = oPending.items as Record<string, unknown>[]
    quoteInProgress.value = qProg.total
    quoteCompleted.value = qDone.total
    orderInProgress.value = oProg.total
    orderCompleted.value = oDone.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.card-footer {
  margin-top: 8px;
  text-align: right;
}
.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
