<template>
  <div class="cad-attachments">
    <el-upload
      :show-file-list="false"
      :auto-upload="false"
      accept=".dxf,.step,.stp,.nc,.dwg,.pdf"
      :on-change="onFileSelect"
    >
      <el-button type="primary" size="small" :loading="uploading">上传 CAD/CAM 文件</el-button>
    </el-upload>
    <span class="hint">支持 .dxf / .step / .nc / .dwg（FR-3-2-2）</span>
    <el-table v-loading="loading" :data="items" stripe border size="small" style="margin-top: 8px">
      <el-table-column prop="fileName" label="文件名" min-width="160" />
      <el-table-column prop="fileType" label="类型" width="80" />
      <el-table-column label="大小" width="90">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="download(row as AttachmentRow)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useMaterialStore } from '@/stores/material'
import { useBaseStore } from '@/stores/_base'
import { useAuthStore } from '@/stores/auth'
import { extractUserId } from '@/utils/jwt'
import { unwrapResult } from '@/utils/apiPage'

interface AttachmentRow {
  id: number
  fileName: string
  fileType: string
  fileSize?: number
}

const props = defineProps<{ drawingId: number }>()

const materialStore = useMaterialStore()
const api = useBaseStore().api
const auth = useAuthStore()
const loading = ref(false)
const uploading = ref(false)
const items = ref<AttachmentRow[]>([])

function formatSize(bytes?: number) {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  return `${(bytes / 1024).toFixed(1)} KB`
}

async function reload() {
  if (!props.drawingId) return
  loading.value = true
  try {
    const list = unwrapResult<AttachmentRow[]>(await materialStore.listDrawingAttachments(props.drawingId))
    items.value = Array.isArray(list) ? list : []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function onFileSelect(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw || !props.drawingId) return
  uploading.value = true
  try {
    const operatorUserId = extractUserId(auth.token) ?? 1001
    unwrapResult(await materialStore.uploadDrawingAttachment(props.drawingId, raw, operatorUserId))
    ElMessage.success('CAD 文件已挂载')
    await reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function download(row: AttachmentRow) {
  try {
    const blob: Blob = await api.get(`/drawings/attachments/${row.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName || `cad-${row.id}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '下载失败')
  }
}

watch(() => props.drawingId, reload)
onMounted(reload)
</script>

<style scoped>
.cad-attachments { margin-top: 8px; }
.hint { margin-left: 8px; font-size: 12px; color: var(--erp-text-muted); }
</style>
