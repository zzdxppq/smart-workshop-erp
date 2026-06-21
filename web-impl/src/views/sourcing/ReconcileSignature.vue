<template>
  <div v-loading="loading">
    <h2>对账单签字</h2>
    <el-alert type="warning" :closable="false" title='支持上传厂商签字扫描件（PDF/JPG/PNG）' />
    <el-card v-if="reconcile" style="margin-top: 12px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="对账单号">{{ reconcile.reconcileNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">
          <MoneyAmount :model-value="Number(reconcile.totalAmount ?? 0)" display-only />
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 12px">
        <ApprovalChainRenderer :nodes="signNodes" />
      </div>
    </el-card>
    <el-form style="margin-top: 16px" label-width="120px">
      <el-form-item label="签字意见">
        <el-input v-model="comment" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="签字扫描件">
        <input type="file" accept="image/*,.pdf" @change="onFilePick" />
        <el-progress v-if="uploading" :percentage="uploadProgress" style="margin-top: 8px; max-width: 320px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="signing" @click="vendorSign">厂商签字</el-button>
        <el-button type="success" :loading="signing" @click="financeSign">财务签字</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useSourcingStore } from '@/stores/sourcing'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { useChunkUpload } from '@/composables/useChunkUpload'
import type { ApprovalNode } from '@/components/erp/ApprovalChainRenderer.vue'

const sourcingStore = useSourcingStore()
const comment = ref('')
const signing = ref(false)
const scanUrl = ref('')

const { uploading, progress: uploadProgress, upload } = useChunkUpload()
const { data: reconcile, loading, load, id } = useDetailLoad<any>((rid) => sourcingStore.getReconcile(rid))

const signNodes = computed<ApprovalNode[]>(() => [
  { title: '采购发送', status: 'APPROVED' },
  { title: '厂商签字', status: reconcile.value?.vendorSigned ? 'APPROVED' : 'PENDING' },
  { title: '财务确认', status: reconcile.value?.financeSigned ? 'APPROVED' : 'PENDING' },
])

async function onFilePick(ev: Event) {
  const file = (ev.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    const r = await upload({ file, type: 'attachment' })
    scanUrl.value = (r as { fileUrl?: string })?.fileUrl ?? ''
    ElMessage.success('扫描件已上传')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '上传失败')
  }
}

async function vendorSign() {
  const fileInput = document.querySelector('input[type=file]') as HTMLInputElement
  const file = fileInput?.files?.[0]
  if (!file) {
    ElMessage.warning('请先选择签字扫描件')
    return
  }
  signing.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('signerName', '厂商')
    await sourcingStore.uploadReconcileSignature(id(), fd)
    ElMessage.success('厂商签字扫描件已上传')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '上传失败')
  } finally {
    signing.value = false
  }
}

async function financeSign() {
  signing.value = true
  try {
    await sourcingStore.financeConfirmReconcile(id())
    ElMessage.success('财务确认完成')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '确认失败')
  } finally {
    signing.value = false
  }
}
</script>
