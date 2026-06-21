<template>
  <ErpPageShell
    title="采购转单（MRP 缺料）"
    description="仅处理生管 MRP 推送的采购申请 · 物料/数量/交期由 PR 锁定 · 采购员确认供应商与单价后转 PO。"
  >
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="onSearch">
          <el-option label="待处理" value="PENDING" />
          <el-option label="部分转单" value="PARTIAL" />
          <el-option label="已转单" value="CONVERTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="keyword" clearable placeholder="PR / 料号 / 工单" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button class="erp-btn-ghost" @click="$router.push('/sourcing/po')">已生成采购单</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="prNo" label="申请单号" min-width="150" />
      <el-table-column prop="materialCode" label="料号" min-width="120" />
      <el-table-column prop="materialName" label="料号" min-width="140" />
      <el-table-column prop="workorderNo" label="关联工单" min-width="140" />
      <el-table-column label="需求/已转" width="110" align="right">
        <template #default="{ row }">
          {{ row.requiredQty }} / <span class="converted">{{ row.convertedQty ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="requiredDate" label="期望交期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" :label="commonStatusLabel(row.status, PR_STATUS)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 'CONVERTED'"
            size="small"
            type="primary"
            @click="openConvert(row)"
          >
            转采购单
          </el-button>
          <span v-else class="done">已完成</span>
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

    <el-dialog v-model="dialogVisible" title="转采购单确认" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
        title="物料、数量、交期由采购申请锁定，不可修改" />
      <el-descriptions v-if="activePr" :column="1" border size="small">
        <el-descriptions-item label="来源单号">{{ activePr.prNo }}</el-descriptions-item>
        <el-descriptions-item label="关联工单">{{ activePr.workorderNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ activePr.materialCode }} · {{ activePr.materialName }}</el-descriptions-item>
        <el-descriptions-item label="转单数量">{{ activePr.remainingQty }}</el-descriptions-item>
        <el-descriptions-item label="期望交期">{{ activePr.requiredDate || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="convertForm" label-width="100px" style="margin-top: 16px">
        <el-form-item label="供应商" required>
          <el-input v-model="convertForm.vendorName" placeholder="中标/选定供应商名称" />
        </el-form-item>
        <el-form-item label="单价" required>
          <el-input-number v-model="convertForm.unitPrice" :min="0" :step="0.01" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="confirmConvert">确认转单</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { usePagedList } from '@/composables/usePagedList'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { commonStatusLabel, PR_STATUS } from '@/utils/statusLabels'

const router = useRouter()
const sourcingStore = useSourcingStore()
const keyword = ref('')
const status = ref<string>()
const dialogVisible = ref(false)
const converting = ref(false)
const activePr = ref<Record<string, unknown> | null>(null)
const convertForm = ref({ vendorName: '', unitPrice: 0 })

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  sourcingStore.listPurchaseRequests({
    keyword: keyword.value,
    status: status.value || undefined,
    ...params,
  }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value, status: status.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value, status: status.value || undefined })
}

function openConvert(row: Record<string, unknown>) {
  activePr.value = row
  convertForm.value = { vendorName: '', unitPrice: 0 }
  dialogVisible.value = true
}

async function confirmConvert() {
  if (!activePr.value?.id) return
  if (!convertForm.value.vendorName.trim()) {
    ElMessage.warning('请填写供应商')
    return
  }
  converting.value = true
  try {
    const r = unwrapResult<Record<string, unknown>>(
      await sourcingStore.convertPrToPo(Number(activePr.value.id), {
        vendorName: convertForm.value.vendorName.trim(),
        unitPrice: convertForm.value.unitPrice,
        qty: Number(activePr.value.remainingQty ?? activePr.value.requiredQty),
      }),
    )
    ElMessage.success(`已生成采购单 ${r.poNo}，关联 ${r.prNo}`)
    dialogVisible.value = false
    onSearch()
    if (r.poId) router.push(`/sourcing/po-detail/${r.poId}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转单失败')
  } finally {
    converting.value = false
  }
}

onMounted(onSearch)
</script>

<style scoped>
.converted { color: var(--el-color-success); font-weight: 600; }
.done { color: var(--erp-text-muted); font-size: 13px; }
</style>
