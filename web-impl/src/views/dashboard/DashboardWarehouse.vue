<template>
  <ErpPageShell title="仓管驾驶舱" description="环节4 · 到货扫码、入库/出库、库存与批次一屏总览（PC 引导 · APP 执行扫码）。">
    <RoleWorkflowPanel />
    <el-button type="primary" :loading="loading" style="margin-bottom: 12px" @click="load">刷新</el-button>
    <DashboardKpiGrid :items="kpis" />

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="14">
        <el-card header="待处理库存预警（OPEN）" shadow="never">
          <el-table :data="openAlerts" size="small" stripe max-height="280">
            <el-table-column prop="materialCode" label="料号" min-width="120" />
            <el-table-column prop="alertLevel" label="级别" width="90">
              <template #default="{ row }">
                <ErpStatusTag :status="row.alertLevel" />
              </template>
            </el-table-column>
            <el-table-column prop="currentQty" label="当前" width="80" align="right" />
            <el-table-column prop="minQty" label="下限" width="80" align="right" />
            <el-table-column prop="message" label="消息" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="resolveAlert(row.id)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card header="仓库概览" shadow="never">
          <el-table :data="warehouses" size="small" stripe max-height="280">
            <el-table-column prop="warehouseCode" label="编码" width="90" />
            <el-table-column prop="warehouseName" label="名称" min-width="100" />
            <el-table-column prop="warehouseType" label="类型" width="90" />
            <el-table-column label="" width="80">
              <template #default="{ row }">
                <el-button link size="small" @click="$router.push(`/warehouse/locations?warehouse=${row.warehouseCode}`)">
                  库位
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :md="12">
        <el-card header="低库存物料（低于安全下限）" shadow="never">
          <el-table :data="lowStock" size="small" stripe max-height="220">
            <el-table-column prop="materialCode" label="料号" min-width="120" />
            <el-table-column prop="materialName" label="名称" min-width="100" show-overflow-tooltip />
            <el-table-column label="库存/下限" width="110" align="right">
              <template #default="{ row }">
                {{ row.currentQty ?? 0 }} / {{ row.minQty ?? 0 }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :md="12">
        <el-card header="快捷入口" shadow="never">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="扫码入/出库请在 APP 执行：扫 WW- 委外到货 · 扫 WL- 物料入库/出库"
            style="margin-bottom: 12px"
          />
          <div class="quick-grid">
            <el-button type="primary" @click="$router.push('/warehouse/inventory')">库存查询</el-button>
            <el-button @click="$router.push('/warehouse/batches')">批次列表</el-button>
            <el-button @click="$router.push('/warehouse/locations')">库位树</el-button>
            <el-button @click="$router.push('/warehouse/inventory-alert')">库存预警</el-button>
            <el-button @click="$router.push('/warehouse/index')">多仓库总览</el-button>
            <el-button @click="$router.push({ path: '/app-only', query: { feature: '扫码入库', from: '/dashboard/warehouse' } })">
              APP 扫码指引
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import RoleWorkflowPanel from '@/components/dashboard/RoleWorkflowPanel.vue'
import { useWarehouseStore } from '@/stores/warehouse'
import { useInventoryStore } from '@/stores/inventory'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

const router = useRouter()
const warehouseStore = useWarehouseStore()
const inventoryStore = useInventoryStore()
const loading = ref(false)
const warehouses = ref<Record<string, unknown>[]>([])
const openAlerts = ref<Record<string, unknown>[]>([])
const lowStock = ref<Record<string, unknown>[]>([])
const batchTotal = ref(0)

const kpis = computed(() => [
  { key: 'wh', label: '启用仓库', value: warehouses.value.length },
  { key: 'alert', label: '开放预警', value: openAlerts.value.length, color: '#cf222e' },
  { key: 'low', label: '低库存 SKU', value: lowStock.value.length, color: '#bf8700' },
  { key: 'batch', label: '批次记录', value: batchTotal.value, color: '#0969da' },
])

async function load() {
  loading.value = true
  try {
    const whRes = unwrapResult(await warehouseStore.listWarehouses())
    warehouses.value = (Array.isArray(whRes) ? whRes : (whRes as { list?: unknown[] })?.list ?? []) as Record<string, unknown>[]

    const alertPage = parsePageItems(await inventoryStore.listAlerts({ status: 'OPEN', pageNum: 1, pageSize: 20 }))
    openAlerts.value = alertPage.items as Record<string, unknown>[]

    const safetyPage = parsePageItems(await inventoryStore.listSafetyConfigs({ pageNum: 1, pageSize: 100 }))
    lowStock.value = (safetyPage.items as Record<string, unknown>[])
      .filter((r) => Number(r.currentQty ?? 0) < Number(r.minQty ?? 0))
      .slice(0, 10)

    const batchPage = parsePageItems(await warehouseStore.listBatchesFefo({ pageNum: 1, pageSize: 1 }))
    batchTotal.value = batchPage.total
  } finally {
    loading.value = false
  }
}

function resolveAlert(id?: number) {
  if (id) router.push(`/warehouse/alert-resolve/${id}`)
}

onMounted(load)
</script>

<style scoped>
.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
