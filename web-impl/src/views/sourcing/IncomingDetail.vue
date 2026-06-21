<template>
  <div v-loading="loading">
    <h2>到货详情</h2>
    <el-card v-if="incoming">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="到货单号">{{ incoming.incomingNo }}</el-descriptions-item>
        <el-descriptions-item label="采购单号">{{ incoming.poNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ incoming.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ incoming.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ incoming.qty }}</el-descriptions-item>
        <el-descriptions-item label="到货时间">{{ incoming.arrivedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <div v-if="incoming?.drawingId" style="margin-top: 16px">
      <el-button type="primary" link @click="openDrawingViewer(incoming.drawingId)">
        查看关联图纸
      </el-button>
    </div>
    <el-button
      v-if="incoming && !incoming.received"
      style="margin-top: 16px"
      type="primary"
      :loading="submitting"
      @click="markReceived"
    >
      标记入库
    </el-button>
    <el-button @click="$router.back()">返回</el-button>

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
import { useDetailLoad } from '@/composables/useDetailLoad'
import DrawingViewer from '@/views/crm/drawing/DrawingViewer.vue'

const sourcingStore = useSourcingStore()
const submitting = ref(false)
const drawerVisible = ref(false)
const activeDrawingId = ref<number | null>(null)

const { data: incoming, loading, load, id } = useDetailLoad<any>((iid) => sourcingStore.getIncoming(iid))

function openDrawingViewer(drawingId: number) {
  activeDrawingId.value = drawingId
  drawerVisible.value = true
}

function onAclDenied(_code: number, message: string) {
  ElMessage.error(message || '无权访问该图纸')
}

async function markReceived() {
  submitting.value = true
  try {
    await sourcingStore.markIncomingReceived(id())
    ElMessage.success('已标记入库')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>
