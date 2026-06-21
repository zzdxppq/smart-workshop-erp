<template>
  <span class="drawing-no-cell">
    <span class="no-text">{{ label }}</span>
    <el-icon
      v-if="resolvedId"
      class="preview-btn"
      title="在线预览图纸"
      @click.stop="openPreview"
    >
      <View />
    </el-icon>
  </span>

  <el-dialog
    v-model="previewVisible"
    title="图纸预览"
    width="80%"
    append-to-body
    destroy-on-close
    @closed="revokePreview"
  >
    <div v-loading="previewLoading" class="preview-wrap">
      <el-alert v-if="previewError" type="error" :title="previewError" show-icon :closable="false" />
      <iframe v-else-if="previewUrl" :src="previewUrl" class="pdf-frame" />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { View } from '@element-plus/icons-vue'
import { resolveDrawingId } from '@/composables/useDrawingLookup'
import { useBaseStore } from '@/stores/_base'

const props = defineProps<{
  drawingNo?: string | null
  drawingId?: number | null
  materialCode?: string | null
}>()

const api = useBaseStore().api
const resolvedId = ref<number | null>(null)
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewError = ref('')
const previewUrl = ref('')

const label = computed(() => props.drawingNo || props.materialCode || '—')

async function resolve() {
  resolvedId.value = await resolveDrawingId({
    drawingId: props.drawingId ?? undefined,
    drawingNo: props.drawingNo ?? undefined,
    materialCode: props.materialCode ?? undefined,
  })
}

watch(
  () => [props.drawingId, props.drawingNo, props.materialCode] as const,
  resolve,
)

onMounted(resolve)

async function openPreview() {
  if (!resolvedId.value) return
  previewVisible.value = true
  previewLoading.value = true
  previewError.value = ''
  previewUrl.value = ''
  try {
    const blob: Blob = await api.get(`/drawings/${resolvedId.value}/preview`, { responseType: 'blob' })
    if (blob instanceof Blob && blob.size > 0) {
      previewUrl.value = URL.createObjectURL(blob)
    } else {
      previewError.value = '预览数据为空'
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string }
    previewError.value = err?.response?.data?.message || err?.message || '无权预览或图纸不存在'
  } finally {
    previewLoading.value = false
  }
}

function revokePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}
</script>

<style scoped>
.drawing-no-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.no-text {
  font-family: var(--erp-font-mono, monospace);
  font-size: 13px;
}
.preview-btn {
  cursor: pointer;
  color: var(--erp-color-primary);
  font-size: 16px;
}
.preview-btn:hover {
  opacity: 0.85;
}
.preview-wrap {
  min-height: 480px;
}
.pdf-frame {
  width: 100%;
  height: 70vh;
  border: 1px solid var(--erp-border);
  border-radius: 4px;
}
</style>
