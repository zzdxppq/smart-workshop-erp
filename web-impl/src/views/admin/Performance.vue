<template>
  <div>
    <h2>绩效考核</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="期间">
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="empName" label="员工" min-width="100" />
      <el-table-column prop="kpiScore" label="KPI" width="80" />
      <el-table-column prop="attendanceScore" label="考勤" width="80" />
      <el-table-column prop="qualityScore" label="质量" width="80" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="rank" label="等级" width="80" />
      <el-table-column prop="evaluator" label="评估人" min-width="100" />
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useHrStore } from '@/stores/hr'
import { usePagedList } from '@/composables/usePagedList'

const hrStore = useHrStore()
const period = ref<string>('2026-06')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  hrStore.listPerformances({ period: period.value, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ period: period.value })
}
function onPageChange() {
  reload({ period: period.value })
}

onMounted(onSearch)
</script>
