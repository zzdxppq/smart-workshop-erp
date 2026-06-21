<template>
  <ErpPageShell title="库存预警中心" description="四级预警 KPI · 缺料/超储一眼识别。">
    <div class="alert-kpi-row">
      <div v-for="k in kpiCards" :key="k.key" class="alert-kpi" :class="k.tone">
        <span class="alert-kpi__label">{{ k.label }}</span>
        <span class="alert-kpi__value">{{ stats[k.key] || 0 }}</span>
      </div>
    </div>

    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="级别">
        <el-select v-model="level" clearable placeholder="全部" @change="onSearch">
          <el-option label="信息" value="INFO" />
          <el-option label="警告" value="WARN" />
          <el-option label="错误" value="ERROR" />
          <el-option label="严重" value="CRITICAL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="materialCode" label="料号" min-width="130" />
      <el-table-column prop="alertLevel" label="级别" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.alertLevel" />
        </template>
      </el-table-column>
      <el-table-column prop="currentQty" label="当前库存" width="100" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.currentQty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="minQty" label="安全库存" width="100" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.minQty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="消息" min-width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status === 'OPEN' ? 'PENDING' : row.status" :label="dictLabel(INVENTORY_ALERT_STATUS, row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            class="erp-btn-secondary"
            :disabled="row.status !== 'OPEN'"
            @click="$router.push(`/warehouse/alert-resolve/${row.id}`)"
          >
            处理
          </el-button>
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
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useInventoryStore } from '@/stores/inventory'
import { usePagedList } from '@/composables/usePagedList'
import { dictLabel, INVENTORY_ALERT_STATUS } from '@/utils/dictLabels'

const inventoryStore = useInventoryStore()
const level = ref('')
const stats = ref<Record<string, number>>({})

const kpiCards = [
  { key: 'INFO', label: '信息', tone: 'info' },
  { key: 'WARN', label: '警告', tone: 'warn' },
  { key: 'ERROR', label: '错误', tone: 'error' },
  { key: 'CRITICAL', label: '严重', tone: 'critical' },
]

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  inventoryStore.listAlerts({ level: level.value || undefined, ...params }),
)

function updateStats(list: any[]) {
  const s: Record<string, number> = { INFO: 0, WARN: 0, ERROR: 0, CRITICAL: 0 }
  for (const row of list) {
    const k = row.alertLevel as string
    if (k in s) s[k] += 1
  }
  stats.value = s
}

watch(items, updateStats)

function onSearch() {
  pageNum.value = 1
  reload({ level: level.value || undefined })
}
function onPageChange() {
  reload({ level: level.value || undefined })
}

onMounted(onSearch)
</script>

<style scoped>
.alert-kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.alert-kpi {
  padding: 14px 16px;
  border-radius: var(--erp-radius-md);
  border: 1px solid var(--erp-border);
  background: rgba(36, 42, 58, 0.6);
}
.alert-kpi__label {
  display: block;
  font-size: 12px;
  color: var(--erp-text-secondary);
  margin-bottom: 6px;
}
.alert-kpi__value {
  font-family: var(--erp-font-mono);
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}
.alert-kpi.info .alert-kpi__value { color: #93c5fd; }
.alert-kpi.warn .alert-kpi__value { color: #fcd34d; }
.alert-kpi.error .alert-kpi__value { color: #fca5a5; }
.alert-kpi.critical .alert-kpi__value { color: #ef4444; }
</style>
