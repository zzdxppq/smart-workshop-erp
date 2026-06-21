<template>
  <div class="quotes-page">
    <h2>{{ pageTitle }}</h2>
    <p v-if="engineeringMode" class="page-desc">
      场景 A：收到报价单待办 → 定义工艺路线与预估工时（车床/CNC/放电/线割等）→ 计算报价
    </p>

    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item :label="engineeringMode ? '任务状态' : '状态'">
        <el-select v-model="engineerPhaseFilter" clearable placeholder="全部" @change="reload">
          <template v-if="engineeringMode">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </template>
          <template v-else>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </template>
        </el-select>
      </el-form-item>
      <el-form-item label="客户">
        <CustomerSelect v-model="filter.customerId" clearable placeholder="全部客户" />
      </el-form-item>
      <el-form-item label="日期从">
        <el-date-picker v-model="filter.dateFrom" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="日期至">
        <el-date-picker v-model="filter.dateTo" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
        <el-button v-if="!engineeringMode" @click="$router.push('/sales/quotes/new')">新建报价</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="displayQuotes" stripe border>
      <el-table-column prop="quoteNo" label="报价单号" min-width="140" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount ?? row.amount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <EngineerTaskStatusTag v-if="engineeringMode" :phase="row._engineerPhase" />
          <ErpStatusTag v-else :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="ownerUserId" label="创建人" width="100">
        <template #default="{ row }">
          {{ row.ownerUserId ? '用户' + row.ownerUserId : '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      <el-table-column label="操作" :width="engineeringMode ? 180 : 100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="engineeringMode && row.id"
            type="primary"
            size="small"
            @click="openDefineProcess(row)"
          >
            定义工艺
          </el-button>
          <el-button size="small" :disabled="!row.id" @click="viewDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <DefineProcessDrawer
      v-if="activeQuote?.id"
      v-model="drawerOpen"
      mode="quote"
      :ref-id="activeQuote.id"
      :ref-no="activeQuote.quoteNo ?? ''"
      :title="activeQuote.customerName"
      @saved="reload"
    />

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      style="margin-top: 12px"
      @current-change="reload"
      @size-change="reload"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { E2QuoteService } from '@/api/generated/services/E2QuoteService'
import type { Quote } from '@/api/generated/models/Quote'
import { parsePageItems } from '@/utils/apiPage'
import CustomerSelect from '@/components/form/CustomerSelect.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import EngineerTaskStatusTag from '@/components/engineering/EngineerTaskStatusTag.vue'
import DefineProcessDrawer from '@/components/engineering/DefineProcessDrawer.vue'
import {
  type EngineerTaskPhase,
  engineerPhaseFromRow,
} from '@/utils/engineeringTask'
import { useEngineeringStore } from '@/stores/engineering'

type QuoteRow = Quote & { engineerPhase?: EngineerTaskPhase; _engineerPhase?: EngineerTaskPhase }

const props = withDefaults(defineProps<{ engineerMode?: boolean }>(), { engineerMode: false })
const route = useRoute()
const router = useRouter()

const engineeringMode = computed(
  () => props.engineerMode || route.query.review === '1' || route.path.startsWith('/engineering/'),
)
const pageTitle = computed(() => (engineeringMode.value ? '报价工艺定义' : '报价单'))
const quotes = ref<QuoteRow[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const engineerPhaseFilter = ref<EngineerTaskPhase | ''>('')
const filter = ref<{
  status?: Quote['status']
  customerId?: number
  dateFrom?: string
  dateTo?: string
}>({})

const eng = useEngineeringStore()
const drawerOpen = ref(false)
const activeQuote = ref<QuoteRow | null>(null)

const displayQuotes = computed(() => {
  if (!engineeringMode.value) return quotes.value
  return quotes.value.map((q) => ({
    ...q,
    _engineerPhase: engineerPhaseFromRow(q),
  }))
})

async function reload() {
  loading.value = true
  try {
    if (engineeringMode.value) {
      const { items, total: t } = await eng.listQuoteQueue({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        phase: engineerPhaseFilter.value || undefined,
        customerId: filter.value.customerId,
        dateFrom: filter.value.dateFrom,
        dateTo: filter.value.dateTo,
      })
      quotes.value = items as QuoteRow[]
      total.value = t
    } else {
      const r = await E2QuoteService.listQuotes(
        pageNum.value,
        pageSize.value,
        filter.value.status,
        filter.value.customerId,
        undefined,
        filter.value.dateFrom,
        filter.value.dateTo,
      )
      const { items, total: t } = parsePageItems(r)
      quotes.value = items as QuoteRow[]
      total.value = t
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    quotes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function viewDetail(id?: number) {
  if (id == null) return
  router.push(`/sales/quotes/${id}`)
}

function openDefineProcess(row: QuoteRow) {
  if (!row.id) return
  activeQuote.value = row
  drawerOpen.value = true
}

onMounted(async () => {
  if (engineeringMode.value && !engineerPhaseFilter.value) {
    engineerPhaseFilter.value = 'PENDING'
  }
  await reload()
  const refId = Number(route.query.refId)
  if (engineeringMode.value && refId) {
    const row = quotes.value.find((q) => q.id === refId)
    if (row) openDefineProcess(row)
  }
})
</script>

<style scoped>
.quotes-page {
  padding: 16px;
}
.page-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--erp-text-secondary);
}
</style>
