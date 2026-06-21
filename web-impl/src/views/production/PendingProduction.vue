<template>
  <ErpPageShell
    title="待转产订单"
    description="销售订单已确认（CONFIRMED）· 生管点「转工单」后进入工序分配与排产。"
  >
    <el-table v-loading="loading" class="erp-table" :data="orders" stripe>
      <el-table-column prop="orderNo" label="销售订单" min-width="150" />
      <el-table-column prop="customerName" label="客户" min-width="140" />
      <el-table-column prop="totalAmount" label="金额" width="120" align="right">
        <template #default="{ row }">
          <MoneyAmount :model-value="Number(row.totalAmount ?? 0)" display-only />
        </template>
      </el-table-column>
      <el-table-column prop="deliveryDate" label="交期" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/production/pending-production-detail/${row.id}`)">
            详情
          </el-button>
          <el-button
            size="small"
            type="primary"
            class="erp-btn-primary"
            :loading="convertingId === row.id"
            @click="convert(row)"
          >
            转工单
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
      @current-change="reload"
      @size-change="reload"
    />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useBaseStore } from '@/stores/_base'
import { E3OrderTransferService } from '@/api/generated/services/E3OrderTransferService'
import { unwrapResult } from '@/utils/apiPage'

const router = useRouter()
const api = useBaseStore().api
const loading = ref(false)
const convertingId = ref<number | null>(null)
const orders = ref<Array<Record<string, unknown>>>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function reload() {
  loading.value = true
  try {
    const data = unwrapResult<{ list?: unknown[]; total?: number }>(
      await api.get('/orders/pending-production', {
        params: { pageNum: pageNum.value, pageSize: pageSize.value },
      }),
    )
    orders.value = (data.list as typeof orders.value) ?? []
    total.value = Number(data.total ?? 0)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function convert(row: { id?: number; orderNo?: string }) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(
      `确认将销售订单 ${row.orderNo ?? row.id} 转为生产工单？系统将按订单明细/BOM 生成 GD 工单。`,
      '转工单',
      { type: 'info' },
    )
  } catch {
    return
  }
  convertingId.value = row.id
  try {
    const data = unwrapResult<Record<string, unknown>>(
      await E3OrderTransferService.convertOrderToProduction(row.id),
    )
    const woNo = String(data.workorderNo ?? (data.workorder as Record<string, unknown>)?.workorderNo ?? '')
    ElMessage.success(woNo ? `已生成工单 ${woNo}` : '转工单成功')
    await reload()
    const woId = data.workorderId ?? (data.workorder as Record<string, unknown>)?.id
    if (woId) {
      router.push(`/production/workorder-detail/${woId}`)
    } else if (woNo) {
      router.push('/production/workorders')
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转工单失败')
  } finally {
    convertingId.value = null
  }
}

onMounted(reload)
</script>
