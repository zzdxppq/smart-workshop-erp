<template>
  <div>
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="WiFi/蓝牙考勤打卡为 Android APP 专属功能；本页展示 Web 端考勤月报。"
      style="margin-bottom: 16px"
    />
    <div class="erp-toolbar">
      <el-button class="erp-btn-secondary" @click="$router.push({ path: '/app-only', query: { feature: '考勤打卡', from: '/hr/attendance' } })">
        前往 APP 打卡说明
      </el-button>
    </div>
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="月份">
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="employeeName" label="员工" min-width="100" />
      <el-table-column prop="checkIn" label="上班" width="100" />
      <el-table-column prop="checkOut" label="下班" width="100" />
      <el-table-column prop="hours" label="工时" width="80" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.hours ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="erp-pagination"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useHrStore } from '@/stores/hr'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const hrStore = useHrStore()
const period = ref('2026-06')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  hrStore.listAttendances({ period: period.value, ...params }),
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
