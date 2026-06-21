<template>
  <div v-loading="loading">
    <h2>采购单详情</h2>
    <el-card v-if="po">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="采购单号">{{ po.poNo }}</el-descriptions-item>
        <el-descriptions-item label="来源单号（PR）">{{ po.prNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="关联工单">{{ po.workorderNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="采购来源">{{ sourceLabel(po.sourceType) }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ po.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ po.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="po.status" /></el-descriptions-item>
        <el-descriptions-item label="交期">{{ po.deliveryDate }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ po.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <h3 style="margin-top: 16px">采购明细</h3>
    <el-table :data="po?.items || []" stripe>
      <el-table-column prop="materialCode" label="料号" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="qty" label="数量" />
      <el-table-column prop="unitPrice" label="单价" />
      <el-table-column prop="amount" label="金额" />
      <el-table-column label="图纸" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.drawingId"
            size="small"
            link
            type="primary"
            @click="openDrawingViewer(row.drawingId)"
          >
            查看图纸
          </el-button>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 16px">
      <el-button type="primary" @click="confirmPo">确认订单</el-button>
      <el-button type="danger" @click="closePo">关闭订单</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <!-- Story 12.1 图纸查看器弹窗 -->
    <el-dialog
      v-model="drawerVisible"
      title="图纸详情"
      width="80%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <DrawingViewer
        v-if="drawerVisible && activeDrawingId"
        :drawing-id="activeDrawingId"
        @acl-denied="onAclDenied"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSourcingStore } from '@/stores/sourcing'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useDetailLoad } from '@/composables/useDetailLoad'
import DrawingViewer from '@/views/crm/drawing/DrawingViewer.vue'

const sourcingStore = useSourcingStore()
const { data: po, loading, load, id } = useDetailLoad<any>((pid) => sourcingStore.getPo(pid))

function sourceLabel(t?: string) {
  return ({ FROM_MRP: 'MRP 缺料', NO_ORDER: '无订单采购', FROM_ORDER: '订单' })[t ?? ''] ?? t ?? '—'
}

// Story 12.1 图纸查看器状态
const drawerVisible = ref(false)
const activeDrawingId = ref<number | null>(null)

function openDrawingViewer(drawingId: number) {
  activeDrawingId.value = drawingId
  drawerVisible.value = true
}

function onAclDenied(code: number, message: string) {
  ElMessage.error(message || '无权访问该图纸')
}

async function confirmPo() {
  try {
    await sourcingStore.confirmPo(id())
    ElMessage.success('采购单已确认')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '确认失败')
  }
}
async function closePo() {
  try {
    await sourcingStore.closePo(id())
    ElMessage.success('采购单已关闭')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '关闭失败')
  }
}
</script>