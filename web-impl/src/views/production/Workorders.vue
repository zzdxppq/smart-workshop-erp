<template>
  <ErpPageShell title="工单列表" description="GD 工单跟踪 · 数量加粗 · 状态/优先级色标。">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="reload">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已排产" value="SCHEDULED" />
          <el-option label="生产中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="reload">查询</el-button>
        <el-button type="primary" class="erp-btn-primary" @click="$router.push('/production/pending-production')">
          待转产订单
        </el-button>
        <el-button class="erp-btn-secondary" @click="$router.push('/production/workorder-create')">
          补录工单
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="workorders" stripe>
      <el-table-column prop="workorderNo" label="工单号" min-width="150" />
      <el-table-column prop="salesOrderNo" label="销售订单" min-width="140">
        <template #default="{ row }">
          {{ row.salesOrderNo ?? '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column label="图号" min-width="150">
        <template #default="{ row }">
          <DrawingNoCell
            :drawing-no="row.drawingNo"
            :drawing-id="row.drawingId"
            :material-code="row.materialCode"
          />
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="产品名称" min-width="120" />
      <el-table-column prop="qty" label="数量" width="88" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.qty ?? '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="90" align="center">
        <template #default="{ row }">
          <ErpStatusTag :status="priorityTone(row.priority)" :label="String(row.priority ?? '—')" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="equipmentType" label="机台" width="100" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/production/workorder-detail/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="erp-pagination"
      @current-change="reload"
      @size-change="reload"
    />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { E5WorkorderService } from '@/api/generated/services/E5WorkorderService'
import type { Workorder } from '@/api/generated/models/Workorder'
import { parsePageItems } from '@/utils/apiPage'

const workorders = ref<(Workorder & { drawingNo?: string; drawingId?: number; materialCode?: string; productName?: string; salesOrderNo?: string; priority?: number; equipmentType?: string })[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const status = ref<string>()

function priorityTone(p: unknown) {
  const n = Number(p)
  if (n >= 8) return 'CRITICAL'
  if (n >= 5) return 'WARN'
  return 'INFO'
}

async function reload() {
  loading.value = true
  try {
    const r = await E5WorkorderService.listWorkorders(pageNum.value, pageSize.value, status.value)
    const { items, total: t } = parsePageItems(r)
    workorders.value = items as Workorder[]
    total.value = t
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    workorders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>
