<template>
  <ErpPageShell title="库存管理" description="库存查询 · 批次追溯 · 库存水位 — 三个维度一览无余。">
    <el-tabs v-model="activeTab" class="erp-tabs">
      <el-tab-pane label="库存查询" name="query" />
      <el-tab-pane label="批次追溯" name="batch" />
      <el-tab-pane label="库存水位" name="level" />
    </el-tabs>

    <!-- === Tab1：库存查询 === -->
    <div v-show="activeTab === 'query'">
      <el-form :inline="true" class="erp-filter-bar">
        <el-form-item label="料号">
          <el-input v-model="q.materialCode" clearable placeholder="WL-..." @keyup.enter="loadQuery" />
        </el-form-item>
        <el-form-item label="库位">
          <el-input v-model="q.locationCode" clearable @keyup.enter="loadQuery" />
        </el-form-item>
        <el-form-item label="批次">
          <el-input v-model="q.batchNo" clearable @keyup.enter="loadQuery" />
        </el-form-item>
        <el-form-item>
          <el-button class="erp-btn-secondary" :loading="q.loading" @click="loadQuery">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="q.loading" class="erp-table" :data="q.items" stripe>
        <el-table-column prop="materialCode" label="料号" min-width="120" />
        <el-table-column prop="locationCode" label="库位" min-width="110" />
        <el-table-column prop="batchNo" label="批次" min-width="150" />
        <el-table-column prop="barcodeNo" label="条码号" min-width="190" />
        <el-table-column prop="qty" label="在库数量" width="100" align="right">
          <template #default="{ row }">
            <span class="erp-num-highlight">{{ row.qty ?? 0 }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- === Tab2：批次追溯 === -->
    <div v-show="activeTab === 'batch'">
      <el-form :inline="true" class="erp-filter-bar">
        <el-form-item label="批次号">
          <el-input v-model="b.batchNo" clearable placeholder="BATCH-..." @keyup.enter="loadBatch" />
        </el-form-item>
        <el-form-item label="料号">
          <el-input v-model="b.materialCode" clearable @keyup.enter="loadBatch" />
        </el-form-item>
        <el-form-item>
          <el-button class="erp-btn-secondary" :loading="b.loading" @click="loadBatch">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="b.loading" class="erp-table" :data="b.items" stripe>
        <el-table-column prop="batchNo" label="批次号" min-width="150" />
        <el-table-column prop="materialCode" label="料号" min-width="120" />
        <el-table-column prop="supplierName" label="供应商" min-width="120" />
        <el-table-column label="数量" width="80" align="right">
          <template #default="{ row }">{{ row.quantity ?? row.qty ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="qualityStatus" label="质量状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="qualityTagType(row.qualityStatus)">{{ row.qualityStatus ?? '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="arrivedAt" label="到货时间" min-width="160">
          <template #default="{ row }">{{ row.arrivedAt ?? row.receivedAt ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/warehouse/batch-trace/${row.batchNo}`)">追溯</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- === Tab3：库存水位 === -->
    <div v-show="activeTab === 'level'">
      <el-form :inline="true" class="erp-filter-bar">
        <el-form-item label="料号">
          <el-input v-model="l.keyword" clearable placeholder="WL-..." @keyup.enter="loadLevel" />
        </el-form-item>
        <el-form-item>
          <el-button class="erp-btn-secondary" :loading="l.loading" @click="loadLevel">查询</el-button>
          <el-button class="erp-btn-ghost" @click="$router.push('/warehouse/inventory-alert')">查看预警</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="l.loading" class="erp-table" :data="l.items" stripe>
        <el-table-column prop="materialCode" label="料号" min-width="130" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column label="库存水位" width="160">
          <template #default="{ row }">
            <ErpStockLevel :current="row.currentQty" :min="row.minQty" :max="row.maxQty" />
          </template>
        </el-table-column>
        <el-table-column prop="minQty" label="安全下限" width="100" align="right">
          <template #default="{ row }">
            <span class="erp-num-highlight">{{ row.minQty ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="maxQty" label="安全上限" width="100" align="right">
          <template #default="{ row }">
            <span class="erp-num-highlight">{{ row.maxQty ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reorderQty" label="补货量" width="90" align="right">
          <template #default="{ row }">
            <span class="erp-num-highlight">{{ row.reorderQty ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="stockTagType(row)" size="small">{{ stockLabel(row) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useWarehouseStore } from '@/stores/warehouse'
import { useInventoryStore } from '@/stores/inventory'
import { parsePageItems } from '@/utils/apiPage'

const activeTab = ref<'query' | 'batch' | 'level'>('query')
const warehouseStore = useWarehouseStore()
const inventoryStore = useInventoryStore()

// Tab1: 库存查询
const q = reactive({ loading: false, items: [] as any[], materialCode: '', locationCode: '', batchNo: '' })
async function loadQuery() {
  q.loading = true
  try {
    const r = await warehouseStore.listBatchesFefo({
      materialCode: q.materialCode || undefined,
      locationCode: q.locationCode || undefined,
      batchNo: q.batchNo || undefined,
      pageNum: 1,
      pageSize: 50,
    })
    const { items } = parsePageItems(r)
    q.items = items
  } finally {
    q.loading = false
  }
}

// Tab2: 批次追溯
const b = reactive({ loading: false, items: [] as any[], batchNo: '', materialCode: '' })
async function loadBatch() {
  b.loading = true
  try {
    const r = await warehouseStore.listBatchesFefo({
      batchNo: b.batchNo || undefined,
      materialCode: b.materialCode || undefined,
      pageNum: 1,
      pageSize: 50,
    })
    const { items } = parsePageItems(r)
    b.items = items
  } finally {
    b.loading = false
  }
}

// Tab3: 库存水位
const l = reactive({ loading: false, items: [] as any[], keyword: '' })
async function loadLevel() {
  l.loading = true
  try {
    const r = await inventoryStore.listSafetyConfigs({ keyword: l.keyword || undefined })
    const { items } = parsePageItems(r)
    l.items = items
  } finally {
    l.loading = false
  }
}

function qualityTagType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (!status) return 'info'
  const s = status.toUpperCase()
  if (s === 'PASSED' || s === 'QUALIFIED') return 'success'
  if (s === 'PENDING') return 'warning'
  if (s === 'REJECTED') return 'danger'
  return 'info'
}

function stockTagType(row: any): 'success' | 'warning' | 'danger' {
  const cur = Number(row.currentQty ?? 0)
  const min = Number(row.minQty ?? 0)
  const max = Number(row.maxQty ?? 0)
  if (cur < min && min > 0) return 'danger'
  if (max > 0 && cur > max) return 'warning'
  return 'success'
}

function stockLabel(row: any): string {
  const cur = Number(row.currentQty ?? 0)
  const min = Number(row.minQty ?? 0)
  const max = Number(row.maxQty ?? 0)
  if (cur < min && min > 0) return '低于下限'
  if (max > 0 && cur > max) return '高于上限'
  return '正常'
}
</script>

<style scoped>
.erp-tabs {
  margin-bottom: 12px;
}
</style>
