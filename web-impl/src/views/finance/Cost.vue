<template>
  <div>
    <h2>成本核算</h2>
    <el-form :inline="true">
      <el-form-item label="期间">
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button type="warning" :loading="calculating" @click="runCalc">运行核算</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border style="margin-top: 12px">
      <el-table-column prop="workorderNo" label="工单号" min-width="130" />
      <el-table-column prop="materialCode" label="料号" min-width="110" />
      <el-table-column prop="materialCost" label="物料成本" width="100" />
      <el-table-column prop="laborCost" label="人工成本" width="100" />
      <el-table-column prop="outsourceCost" label="委外成本" width="100" />
      <el-table-column prop="overhead" label="制造费用" width="100" />
      <el-table-column prop="totalCost" label="总成本" width="100" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/finance/cost-detail/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useFinanceStore } from '@/stores/finance'
import { usePagedList } from '@/composables/usePagedList'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'

const financeStore = useFinanceStore()
const period = ref<string>('2026-06')
const calculating = ref(false)

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  financeStore.listCosts({ period: period.value, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ period: period.value })
}
function onPageChange() {
  reload({ period: period.value })
}

async function runCalc() {
  calculating.value = true
  try {
    await financeStore.runCostCalc({ period: period.value })
    ElMessage.success('成本核算已完成')
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '核算失败')
  } finally {
    calculating.value = false
  }
}

onMounted(onSearch)
</script>
