<template>
  <div class="outsource-page">
    <h2>委外列表</h2>

    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="工单号">
        <el-input v-model="filter.workorderNo" clearable placeholder="WO..." @keyup.enter="reload" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filter.status" clearable placeholder="全部" @change="reload">
          <el-option v-for="s in statusOptions" :key="s" :label="outsourceStateLabel(s)" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商 ID">
        <el-input-number v-model="filter.supplierId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="orders" stripe border>
      <el-table-column prop="outsourceNo" label="委外单号" min-width="150" />
      <el-table-column prop="workorderNo" label="工单号" min-width="130" />
      <el-table-column prop="supplierName" label="供应商" min-width="120" />
      <el-table-column prop="processName" label="工序" min-width="100" />
      <el-table-column prop="qty" label="数量" width="80" />
      <el-table-column prop="totalAmount" label="总金额" width="110" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" :label="outsourceStateLabel(row.status)" />
        </template>
      </el-table-column>
      <el-table-column prop="reworkCount" label="返修次数" width="90" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goDetail(row)">详情</el-button>
          <el-button size="small" type="primary" @click="goStateMachine(row)">状态机</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="reload"
      @size-change="reload"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useOutsourceStore } from '@/stores/outsource'
import { parsePageItems } from '@/utils/apiPage'
import { OUTSOURCE_STATES, outsourceStateLabel } from '@/constants/outsourceStates'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const statusOptions = OUTSOURCE_STATES

interface OutsourceRow {
  id?: number
  outsourceNo?: string
  workorderNo?: string
  supplierName?: string
  processName?: string
  qty?: number
  totalAmount?: number
  status?: string
  reworkCount?: number
}

const router = useRouter()
const outsourceStore = useOutsourceStore()
const orders = ref<OutsourceRow[]>([])
const loading = ref(false)
const page = ref(0)
const pageSize = ref(20)
const total = ref(0)
const filter = ref<{ status?: string; workorderNo?: string; supplierId?: number }>({})

async function reload() {
  loading.value = true
  try {
    const r = await outsourceStore.listOrders({
      page: page.value,
      size: pageSize.value,
      status: filter.value.status || undefined,
      workorderNo: filter.value.workorderNo || undefined,
      supplierId: filter.value.supplierId || undefined,
    })
    const { items, total: t } = parsePageItems(r)
    orders.value = items as OutsourceRow[]
    total.value = t
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goDetail(row: OutsourceRow) {
  if (!row.outsourceNo) return
  router.push(`/production/outsource-detail/${row.outsourceNo}`)
}

function goStateMachine(row: OutsourceRow) {
  if (row.id == null) {
    ElMessage.warning('缺少委外单 ID')
    return
  }
  router.push({ path: '/production/outsub-panel', query: { outsourceId: String(row.id) } })
}

onMounted(reload)
</script>

<style scoped>
.outsource-page {
  padding: 16px;
}
</style>
