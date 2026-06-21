<template>
  <div class="drawing-viewer">
    <el-card v-loading="loading">
      <template #header>
        <div class="viewer-header">
          <span class="title">图纸查看器</span>
          <el-tag v-if="permission" :type="scopeTagType" size="small">
            {{ scopeLabel }}
          </el-tag>
        </div>
      </template>

      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <div class="action-bar" v-if="permission">
        <el-button
          v-if="permission.permissions?.view"
          type="primary"
          size="small"
          :loading="previewLoading"
          @click="handlePreview"
        >
          {{ showPreview ? '刷新预览' : '查看' }}
        </el-button>
        <el-button
          v-if="permission.permissions?.print"
          type="success"
          size="small"
          :disabled="!previewBlob"
          @click="handlePrint"
        >
          打印
        </el-button>
        <el-button
          v-if="permission.permissions?.download"
          type="warning"
          size="small"
          @click="handleDownload"
        >
          下载原文件
        </el-button>
        <el-button
          v-if="permission.permissions?.upload"
          type="info"
          size="small"
          @click="$emit('upload')"
        >
          上传
        </el-button>
        <el-button
          v-if="permission.permissions?.delete"
          type="danger"
          size="small"
          @click="$emit('delete')"
        >
          删除
        </el-button>
      </div>

      <div v-if="showPreview" class="preview-container">
        <PdfPageViewer :source="previewBlob" :allow-print="!!permission?.permissions?.print" />
      </div>

      <div v-if="permission?.linkedBizIds" class="linked-info">
        <el-divider content-position="left">关联业务单据</el-divider>
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item
            v-for="(ids, bizType) in permission.linkedBizIds"
            :key="bizType"
            :label="bizTypeLabel(bizType)"
          >
            {{ formatIds(ids) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useBaseStore } from '@/stores/_base'
import type { DrawingPermissionDTO } from '@/api/generated/models/DrawingPermissionDTO'
import { unwrapResult } from '@/utils/apiPage'
import PdfPageViewer from '@/components/pdf/PdfPageViewer.vue'

interface Props {
  drawingId: number
  /** 打开对话框时自动加载预览（FR-3-1-3） */
  autoPreview?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  autoPreview: false,
})

const emit = defineEmits<{
  (e: 'preview-success', payload: Blob): void
  (e: 'acl-denied', code: number, message: string): void
  (e: 'upload'): void
  (e: 'delete'): void
}>()

const baseStore = useBaseStore()
const loading = ref(false)
const previewLoading = ref(false)
const errorMsg = ref('')
const permission = ref<DrawingPermissionDTO | null>(null)
const showPreview = ref(false)
const previewBlob = ref<Blob | null>(null)

const scopeLabel = computed(() => {
  if (!permission.value) return ''
  const map: Record<string, string> = {
    ALL: '全量',
    ORDER: '仅关联订单',
    PO: '仅关联 PO',
    INCOMING: '仅关联入库单',
    INSPECTION: '仅关联质检单',
    WORKORDER_PROCESS: '仅当前工序',
    NONE: '无权限',
  }
  return map[permission.value.scope || ''] || permission.value.scope
})

const scopeTagType = computed(() => {
  if (!permission.value) return 'info'
  if (permission.value.scope === 'NONE') return 'danger'
  if (permission.value.scope === 'ALL') return 'success'
  return 'warning'
})

onMounted(async () => {
  await fetchPermission()
  if (props.autoPreview && permission.value?.permissions?.view) {
    await handlePreview()
  }
})

async function fetchPermission() {
  loading.value = true
  errorMsg.value = ''
  try {
    permission.value = unwrapResult<DrawingPermissionDTO>(
      await baseStore.api.get(`/drawings/${props.drawingId}/permission`),
    )
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || e?.message || '权限查询异常'
  } finally {
    loading.value = false
  }
}

async function handlePreview() {
  previewLoading.value = true
  errorMsg.value = ''
  try {
    const blob: Blob = await baseStore.api.get(`/drawings/${props.drawingId}/preview`, {
      responseType: 'blob',
    })
    if (blob instanceof Blob && blob.size > 0) {
      previewBlob.value = blob
      showPreview.value = true
      emit('preview-success', blob)
    } else {
      errorMsg.value = '预览数据为空'
    }
  } catch (e: any) {
    handleAclError(e)
  } finally {
    previewLoading.value = false
  }
}

function handlePrint() {
  if (!previewBlob.value) return
  const url = URL.createObjectURL(previewBlob.value)
  const w = window.open(url)
  w?.print()
}

async function handleDownload() {
  loading.value = true
  try {
    const blob: Blob = await baseStore.api.get(`/drawings/${props.drawingId}/download`, {
      responseType: 'blob',
    })
    if (blob instanceof Blob && blob.size > 0) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `drawing-${props.drawingId}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    }
  } catch (e: any) {
    handleAclError(e)
  } finally {
    loading.value = false
  }
}

function handleAclError(e: any) {
  const code = e?.response?.data?.code
  const msg = e?.response?.data?.message
  if (code === 40304) {
    errorMsg.value = msg || '无权访问该图纸'
    previewBlob.value = null
    showPreview.value = false
    emit('acl-denied', code, msg || '无权访问')
  } else if (code === 40401) {
    errorMsg.value = '图纸不存在'
  } else if (code === 41001) {
    errorMsg.value = '图纸已归档，无法预览'
  } else {
    errorMsg.value = msg || e?.message || '操作失败'
  }
}

function bizTypeLabel(bizType: string): string {
  const map: Record<string, string> = {
    ORDER: '订单',
    PO: '采购单',
    INCOMING: '入库单',
    INSPECTION: '质检单',
    WORKORDER_PROCESS: '工单工序',
  }
  return map[bizType] || bizType
}

function formatIds(ids: number[]): string {
  if (!ids || ids.length === 0) return '—'
  if (ids.length <= 3) return ids.join(', ')
  return `${ids.slice(0, 3).join(', ')} ... (共 ${ids.length})`
}
</script>

<style scoped>
.drawing-viewer { padding: 8px; }
.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title { font-weight: 600; }
.action-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.preview-container { margin-top: 12px; }
.linked-info { margin-top: 12px; }
</style>
