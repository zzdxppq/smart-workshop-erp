<template>
  <ErpPageShell title="报价审批" description="审批前展开查看明细图号 · 支持在线预览图纸（Epic 3）。">
    <div class="page-header">
      <el-button type="primary" @click="batchApprove">Enter 批量通过</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="pending"
      stripe
      border
      row-key="id"
      @selection-change="onSelect"
      @expand-change="onExpandChange"
    >
      <el-table-column type="expand" width="48">
        <template #default="{ row }">
          <div v-loading="detailLoading[row.id!]" class="expand-panel">
            <template v-if="quoteDetails[row.id!]?.length">
              <h4 class="expand-title">报价明细 · 图号 / 材质 / 尺寸</h4>
              <el-table :data="quoteDetails[row.id!]" size="small" stripe border>
                <el-table-column label="图号" min-width="180">
                  <template #default="{ row: item }">
                    <DrawingNoCell
                      :drawing-no="drawingLabel(item)"
                      :material-code="item.material"
                    />
                  </template>
                </el-table-column>
                <el-table-column prop="material" label="材质" width="110" />
                <el-table-column prop="spec" label="尺寸" min-width="120" show-overflow-tooltip />
                <el-table-column prop="quantity" label="数量" width="80" align="right" />
                <el-table-column label="单价" width="100" align="right">
                  <template #default="{ row: item }">¥{{ item.unitPrice ?? 0 }}</template>
                </el-table-column>
                <el-table-column label="金额" width="100" align="right">
                  <template #default="{ row: item }">
                    ¥{{ ((item.quantity ?? 0) * (item.unitPrice ?? 0)).toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column label="FA" width="60" align="center">
                  <template #default="{ row: item }">
                    <el-tag v-if="item.isFa" size="small" type="warning">FA</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-empty v-else description="暂无明细或未加载" :image-size="64" />
          </div>
        </template>
      </el-table-column>
      <el-table-column type="selection" width="48" />
      <el-table-column prop="quoteNo" label="报价单号" min-width="140" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="审批链" min-width="280">
        <template #default="{ row }">
          <ApprovalChainRenderer :nodes="chainFor(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="approveOne(row.id)">通过</el-button>
          <el-button size="small" type="danger" @click="rejectOne(row.id)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { E2QuoteService } from '@/api/generated/services/E2QuoteService'
import { E2QuoteFlowService } from '@/api/generated/services/E2QuoteFlowService'
import type { Quote } from '@/api/generated/models/Quote'
import type { QuoteItem } from '@/api/generated/models/QuoteItem'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { quoteApprovalPreviewNodes } from '@/utils/quoteApprovalChain'
import { useAuthStore } from '@/stores/auth'
import { hasAnyRole } from '@/utils/roleAccess'

const router = useRouter()
const auth = useAuthStore()
import { useKeyboardShortcuts } from '@/composables/useKeyboardShortcuts'

const loading = ref(false)
const pending = ref<Quote[]>([])
const selected = ref<Quote[]>([])
const quoteDetails = reactive<Record<number, QuoteItem[]>>({})
const detailLoading = reactive<Record<number, boolean>>({})

function chainFor(row: Quote) {
  return quoteApprovalPreviewNodes(row.totalAmount ?? 0, row.currentNode ?? 1)
}

function drawingLabel(item: QuoteItem & { customerDrawingNo?: string }) {
  return item.customerDrawingNo || item.drawingNo
}

function onSelect(rows: Quote[]) {
  selected.value = rows
}

async function loadQuoteDetail(quoteId: number) {
  if (quoteDetails[quoteId]) return
  detailLoading[quoteId] = true
  try {
    const r = unwrapResult(await E2QuoteService.getQuote(quoteId))
    const data = r as { items?: QuoteItem[]; quote?: { items?: QuoteItem[] } }
    quoteDetails[quoteId] = data.items ?? data.quote?.items ?? []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载明细失败')
    quoteDetails[quoteId] = []
  } finally {
    detailLoading[quoteId] = false
  }
}

function onExpandChange(row: Quote, expandedRows: Quote[] | boolean) {
  if (!row.id) return
  const expanded = Array.isArray(expandedRows)
    ? expandedRows.some((r) => r.id === row.id)
    : expandedRows
  if (expanded) loadQuoteDetail(row.id)
}

async function reload() {
  loading.value = true
  try {
    const api = (await import('@/stores/_base')).useBaseStore().api
    const [r1, r2] = await Promise.all([
      api.get('/quotes', { params: { pageNum: 1, pageSize: 50, status: 'PENDING_APPROVAL' } }),
      E2QuoteService.listQuotes(1, 50, 'SUBMITTED'),
    ])
    const items1 = parsePageItems(r1).items as Quote[]
    const items2 = parsePageItems(r2).items as Quote[]
    const seen = new Set<number>()
    pending.value = [...items1, ...items2].filter((q) => {
      if (!q.id || seen.has(q.id)) return false
      seen.add(q.id)
      return true
    })
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function approveOne(id?: number) {
  if (!id) return
  const resp = unwrapResult(await E2QuoteFlowService.approveQuote(id)) as Quote
  if (resp.status !== 'APPROVED') {
    ElMessage.success('当前审批节点已通过，等待下一审批人')
    delete quoteDetails[id]
    reload()
    return
  }
  // 审批通过后询问是否一键转销售单（PRD 2.2 一键转单闭环）
  try {
    await ElMessageBox.confirm(
      '报价已审批通过，是否立即生成销售订单？',
      '转销售单',
      { confirmButtonText: '立即转单', cancelButtonText: '暂不', type: 'success' },
    )
    const resp = unwrapResult<{ orderNo?: string; orderId?: number }>(
      await E2QuoteService.convertQuoteToOrder(id),
    )
    ElMessage.success(`销售订单已生成：${resp.orderNo ?? ''}`)
    router.push('/sales/orders')
    return
  } catch {
    /* 用户取消转单 */
  }
  delete quoteDetails[id]
  reload()
}

async function rejectOne(id?: number) {
  if (!id) return
  const { value } = await ElMessageBox.prompt('驳回原因', '驳回报价')
  await E2QuoteFlowService.rejectQuote(id, { reason: value })
  ElMessage.success('已驳回')
  delete quoteDetails[id]
  reload()
}

async function batchApprove() {
  for (const q of selected.value) {
    if (q.id) await E2QuoteFlowService.approveQuote(q.id)
  }
  ElMessage.success(`已通过 ${selected.value.length} 条`)
  // 批量审批后弹窗提示转单
  try {
    await ElMessageBox.confirm(
      `已批量通过 ${selected.value.length} 条报价，是否立即转为销售订单？`,
      '批量转销售单',
      { confirmButtonText: '全部转单', cancelButtonText: '暂不', type: 'success' },
    )
    const converted: string[] = []
    for (const q of selected.value) {
      if (!q.id) continue
      try {
        const r = unwrapResult<{ orderNo?: string }>(
          await E2QuoteService.convertQuoteToOrder(q.id),
        )
        if (r.orderNo) converted.push(r.orderNo)
      } catch {
        /* skip single failure */
      }
    }
    ElMessage.success(`已生成 ${converted.length} 个销售订单`)
    router.push('/sales/orders')
  } catch {
    reload()
  }
}

useKeyboardShortcuts({ submit: batchApprove })

onMounted(() => {
  if (!hasAnyRole(auth.userRoles, ['SALES_MGR', 'SALES_MANAGER', 'GM', 'ADMIN', 'SYS_ADMIN'])) {
    ElMessage.error('无报价审批权限')
    router.replace('/sales/quotes')
    return
  }
  reload()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.expand-panel {
  padding: 12px 24px 16px;
  background: var(--erp-bg-card);
}
.expand-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
}
</style>
