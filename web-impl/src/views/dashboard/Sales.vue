<template>
  <ErpPageShell
    title="销售驾驶舱"
    description="V2.1 · 报价（工程师算价→审批）→ 客户确认后从图纸库建单（提交即生效、自动生成料号）。"
  >
    <RoleWorkflowPanel />

    <div class="toolbar">
      <el-button type="primary" @click="$router.push('/sales/quotes/new')">+ 新建报价</el-button>
      <el-button type="success" @click="$router.push('/sales/orders/new')">+ 新建销售订单</el-button>
      <el-button :loading="loading" @click="reload">刷新</el-button>
    </div>

    <DashboardKpiGrid :items="kpis" />

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="12">
        <el-card header="报价待办（工程师/审批）" shadow="never">
          <el-table v-loading="loading" :data="pendingQuotes" size="small" stripe max-height="280">
            <el-table-column prop="quoteNo" label="报价单号" min-width="130" />
            <el-table-column prop="customerName" label="客户" min-width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <ErpStatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.id" link type="primary" size="small" @click="goQuote(row.id)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="card-footer">
            <el-button link type="primary" @click="$router.push('/sales/quotes')">全部报价单 →</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :md="12">
        <el-card header="订单待办（草稿 / 工程转化中）" shadow="never">
          <el-table v-loading="loading" :data="pendingOrders" size="small" stripe max-height="280">
            <el-table-column prop="orderNo" label="订单号" min-width="130" />
            <el-table-column prop="customerName" label="客户" min-width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <ErpStatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.id"
                  link
                  type="primary"
                  size="small"
                  @click="goOrder(row)"
                >
                  {{ row.status === 'DRAFT' ? '编辑' : '查看' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="card-footer">
            <el-button link type="primary" @click="$router.push('/sales/orders')">全部销售订单 →</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="14">
        <el-card header="本月销售漏斗" shadow="never">
          <v-chart :option="funnelChart" autoresize style="height: 280px" />
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card header="已审批报价（可转正式订单）" shadow="never">
          <el-table v-loading="loading" :data="approvedQuotes" size="small" stripe max-height="280">
            <el-table-column prop="quoteNo" label="报价单号" min-width="120" />
            <el-table-column prop="customerName" label="客户" min-width="90" show-overflow-tooltip />
            <el-table-column label="金额" width="90" align="right">
              <template #default="{ row }">¥{{ row.totalAmount ?? row.amount ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="" width="70" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.id" link type="primary" size="small" @click="$router.push('/sales/orders/new')">
                  建单
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="销售龙虎榜（Top 10）" shadow="never" style="margin-top: 16px">
      <el-table v-loading="rankLoading" :data="ranking" stripe size="small">
        <el-table-column label="排名" width="70">
          <template #default="{ row }">
            <span :class="rankMedalClass(Number(row.rank))">#{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="salesman" label="业务员" />
        <el-table-column prop="customerName" label="客户" />
        <el-table-column prop="orderCount" label="订单数" width="90" />
        <el-table-column prop="amount" label="销售额" width="120" />
      </el-table>
    </el-card>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useBaseStore } from '@/stores/_base'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { rankMedalClass } from '@/utils/dashboardChart'

const router = useRouter()
const dashboardStore = useDashboardStore()
const api = useBaseStore().api
const loading = ref(false)
const rankLoading = ref(false)
const period = ref('2026-06')

const counts = ref({
  pendingEng: 0,
  pendingApproval: 0,
  draftOrders: 0,
  approvedOrders: 0,
  approvedQuotes: 0,
  processingOrders: 0,
})

const pendingQuotes = ref<Record<string, unknown>[]>([])
const pendingOrders = ref<Record<string, unknown>[]>([])
const approvedQuotes = ref<Record<string, unknown>[]>([])
const ranking = ref<Record<string, unknown>[]>([])

const kpis = computed(() => [
  { key: 'eng', label: '待工程师算价', value: counts.value.pendingEng, color: '#bf8700' },
  { key: 'appr', label: '待报价审批', value: counts.value.pendingApproval, color: '#0969da' },
  { key: 'draft', label: '订单草稿', value: counts.value.draftOrders, color: '#6b7280' },
  { key: 'live', label: '已生效订单', value: counts.value.approvedOrders, color: '#1a7f37' },
  { key: 'ready', label: '可建单报价', value: counts.value.approvedQuotes, color: '#0ea5e9' },
  { key: 'eng-order', label: '工程转化中', value: counts.value.processingOrders, color: '#9333ea' },
])

const funnelChart = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: ['待工程师', '待审批', '已审批', '订单草稿', '已生效', '工程转化'],
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'bar',
    data: [
      counts.value.pendingEng,
      counts.value.pendingApproval,
      counts.value.approvedQuotes,
      counts.value.draftOrders,
      counts.value.approvedOrders,
      counts.value.processingOrders,
    ],
    itemStyle: { color: '#0ea5e9' },
  }],
}))

async function countList(url: string, params: Record<string, unknown>) {
  const r = await api.get(url, { params: { pageNum: 1, pageSize: 1, ...params } })
  return parsePageItems(r).total
}

async function fetchList(url: string, params: Record<string, unknown>, size = 8) {
  const r = await api.get(url, { params: { page: 1, size, pageNum: 1, pageSize: size, ...params } })
  return parsePageItems(r).items as Record<string, unknown>[]
}

function goQuote(id: number) {
  router.push(`/sales/quotes/${id}`)
}

function goOrder(row: Record<string, unknown>) {
  const id = row.id as number
  if (row.status === 'DRAFT') router.push(`/sales/orders/${id}/edit`)
  else router.push(`/sales/orders/${id}`)
}

async function reload() {
  loading.value = true
  try {
    const [engT, apprT, draftT, approvedT, quoteApprT, procT] = await Promise.all([
      countList('/quotes', { status: 'PENDING_ENG' }),
      countList('/quotes', { status: 'PENDING_APPROVAL' }),
      countList('/orders', { status: 'DRAFT' }),
      countList('/orders', { status: 'APPROVED' }),
      countList('/quotes', { status: 'APPROVED' }),
      countList('/orders', { status: 'PROCESSING' }),
    ])
    counts.value = {
      pendingEng: engT,
      pendingApproval: apprT,
      draftOrders: draftT,
      approvedOrders: approvedT,
      approvedQuotes: quoteApprT,
      processingOrders: procT,
    }

    const [engList, apprList, draftList, procList, apprQuoteList] = await Promise.all([
      fetchList('/quotes', { status: 'PENDING_ENG' }, 5),
      fetchList('/quotes', { status: 'PENDING_APPROVAL' }, 5),
      fetchList('/orders', { status: 'DRAFT' }, 5),
      fetchList('/orders', { status: 'PROCESSING' }, 5),
      fetchList('/quotes', { status: 'APPROVED' }, 6),
    ])
    pendingQuotes.value = [...engList, ...apprList].slice(0, 8)
    pendingOrders.value = [...draftList, ...procList].slice(0, 8)
    approvedQuotes.value = apprQuoteList

    try {
      const stats = unwrapResult<Record<string, unknown>>(
        await dashboardStore.loadSalesStats({ period: period.value }),
      )
      void stats
    } catch {
      /* 统计 API 不可用时仅用列表计数 */
    }
  } finally {
    loading.value = false
  }

  rankLoading.value = true
  try {
    const r = unwrapResult<{ list?: Record<string, unknown>[] }>(
      await api.get('/reports/sales-ranking', { params: { period: period.value, topN: 10 } }),
    )
    ranking.value = r.list ?? []
  } catch {
    ranking.value = []
  } finally {
    rankLoading.value = false
  }
}

onMounted(reload)
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
.rank-gold { color: #d4a017; font-weight: 700; }
.rank-silver { color: #8b949e; font-weight: 600; }
.rank-normal { color: var(--erp-text-secondary); }
</style>
