<template>
  <div class="drawing-picker">
    <div class="picker-row">
      <el-input
        :model-value="displayText"
        :placeholder="placeholder"
        :disabled="disabled"
        readonly
        class="picker-input"
        @click="!disabled && openLibrary()"
      >
        <template #suffix>
          <el-icon v-if="selectedDrawing?.id" class="action-icon preview" title="在线预览" @click.stop="openPreview">
            <View />
          </el-icon>
          <el-icon class="action-icon" title="从图纸库选择" @click.stop="!disabled && openLibrary()">
            <Search />
          </el-icon>
        </template>
      </el-input>
      <el-button v-if="modelValue && !disabled" link type="danger" @click="clear">清除</el-button>
    </div>

    <el-dialog v-model="libraryVisible" title="图纸库 · 选择图号" width="720px" append-to-body destroy-on-close>
      <el-form :inline="true" style="margin-bottom: 12px">
        <el-form-item label="关键词">
          <el-input v-model="searchKeyword" placeholder="客户图号 / 系统图号 / 标题" clearable @keyup.enter="searchLibrary" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="libraryLoading" @click="searchLibrary">搜索</el-button>
          <el-button v-if="canUpload" type="success" @click="uploadVisible = true">上传新图纸</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="libraryLoading"
        :data="libraryItems"
        highlight-current-row
        stripe
        border
        max-height="360"
        @row-click="onRowPick"
      >
        <el-table-column prop="customerDrawingNo" label="客户图号" min-width="130" />
        <el-table-column prop="drawingNo" label="系统图号" min-width="140" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="materialCode" label="料号" min-width="100" />
        <el-table-column prop="title" label="标题/尺寸" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="pickDrawing(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="libraryPage"
        :page-size="20"
        :total="libraryTotal"
        layout="total, prev, pager, next"
        style="margin-top: 12px"
        @current-change="searchLibrary"
      />
    </el-dialog>

    <el-dialog v-model="uploadVisible" title="上传新图纸" width="520px" append-to-body destroy-on-close @closed="resetUpload">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
        title="请将 PDF 文件名改为与客户图号一致（如 615-03953-0009.pdf），系统会自动读取；同时生成系统图号 DWG-…"
      />
      <el-form label-width="100px">
        <el-form-item label="文件" required class="upload-file-item">
          <el-upload
            ref="uploadRef"
            class="upload-block"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.dwg,.dxf,.step,.stp,.nc"
            :on-change="onFileChange"
            :on-exceed="() => ElMessage.warning('仅支持单文件上传')"
          >
            <el-button type="primary">选择 PDF / CAD / STEP</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="客户图号">
          <el-input v-model="uploadCustomerDrawingNo" placeholder="自动读取文件名，可修改" />
        </el-form-item>
        <el-form-item label="系统图号">
          <el-input value="保存后自动生成" disabled />
        </el-form-item>
        <el-form-item label="材质" required>
          <el-input v-model="uploadMaterialGrade" placeholder="如 45#钢、6061 铝" />
        </el-form-item>
        <el-form-item label="规格尺寸" required>
          <el-input v-model="uploadSpecSize" placeholder="如 Φ120×20mm" />
        </el-form-item>
        <el-form-item label="单件重量">
          <el-input-number v-model="uploadUnitWeight" :min="0" :step="0.1" controls-position="right" /> kg
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="uploadTitle" placeholder="可选，默认取客户图号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="submitUpload">上传并选用</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="图纸预览" width="80%" append-to-body destroy-on-close @closed="revokePreview">
      <div v-loading="previewLoading" class="preview-wrap">
        <el-alert v-if="previewError" type="error" :title="previewError" show-icon :closable="false" />
        <iframe v-else-if="previewUrl" :src="previewUrl" class="pdf-frame" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { View, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import { E3DrawingService } from '@/api/generated/services/E3DrawingService'
import type { Drawing } from '@/api/generated/models/Drawing'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { useAuthStore } from '@/stores/auth'
import { extractUserId } from '@/utils/jwt'
import { useBaseStore } from '@/stores/_base'
import { hasAnyRole, ADMIN_ROLES } from '@/utils/roleAccess'

const props = withDefaults(defineProps<{
  modelValue?: string
  drawingId?: number
  placeholder?: string
  disabled?: boolean
  /** false=只读选图/预览，隐藏上传（采购/品质/销售等） */
  allowUpload?: boolean
  /** true=仅展示已绑定料号的图纸（工艺路线维护用） */
  requireMaterialCode?: boolean
}>(), {
  modelValue: '',
  placeholder: '点击选择图号',
  disabled: false,
  allowUpload: undefined,
  requireMaterialCode: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:drawingId': [id: number | undefined]
  select: [drawing: Drawing]
}>()

const auth = useAuthStore()
const api = useBaseStore().api

const canUpload = computed(() => {
  if (props.allowUpload === false) return false
  if (props.allowUpload === true) return true
  return hasAnyRole(auth.userRoles, [...ADMIN_ROLES, 'ENGINEER'])
})

const selectedDrawing = ref<Drawing | null>(null)
const libraryVisible = ref(false)
const libraryLoading = ref(false)
const libraryItems = ref<Drawing[]>([])
const libraryTotal = ref(0)
const libraryPage = ref(1)
const searchKeyword = ref('')

const uploadVisible = ref(false)
const uploadLoading = ref(false)
const uploadTitle = ref('')
const uploadCustomerDrawingNo = ref('')
const uploadMaterialGrade = ref('')
const uploadSpecSize = ref('')
const uploadUnitWeight = ref<number | undefined>()
const uploadFile = ref<File | null>(null)
const uploadRef = ref<UploadInstance>()

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewError = ref('')
const previewUrl = ref('')

const displayText = computed(() => {
  if (!props.modelValue) return ''
  const d = selectedDrawing.value
  if (d?.drawingNo === props.modelValue) {
    return `${d.drawingNo} · ${d.title ?? d.materialCode ?? ''}`
  }
  return props.modelValue
})

watch(() => props.modelValue, async (v) => {
  if (!v) {
    selectedDrawing.value = null
    return
  }
  if (selectedDrawing.value?.drawingNo === v) return
  if (props.drawingId) {
    try {
      const d = unwrapResult<Drawing>(await E3DrawingService.getDrawing(props.drawingId))
      selectedDrawing.value = d
    } catch { /* ignore */ }
  }
})

function operatorUserId(): number {
  return extractUserId(auth.token) ?? 1001
}

function openLibrary() {
  libraryVisible.value = true
  searchLibrary()
}

async function searchLibrary() {
  libraryLoading.value = true
  const kw = searchKeyword.value.trim() || undefined
  try {
    const r = await api.get('/drawings', {
      params: {
        keyword: kw,
        status: 'RELEASED',
        hasMaterialCode: props.requireMaterialCode ? true : undefined,
        page: libraryPage.value - 1,
        size: 20,
      },
    })
    const { items, total } = parsePageItems(r)
    libraryItems.value = items as Drawing[]
    libraryTotal.value = total
  } catch {
    try {
      const r = await E3DrawingService.listDrawings(
        kw, undefined, undefined, undefined, undefined, 'RELEASED', libraryPage.value - 1, 20,
      )
      const { items, total } = parsePageItems(r)
      libraryItems.value = (items as Drawing[]).filter((d) =>
        !props.requireMaterialCode || Boolean(d.materialCode),
      )
      libraryTotal.value = total
    } catch {
      libraryItems.value = []
      libraryTotal.value = 0
    }
  } finally {
    libraryLoading.value = false
  }
}

function onRowPick(row: Drawing) {
  pickDrawing(row)
}

function pickDrawing(d: Drawing) {
  selectedDrawing.value = d
  emit('update:modelValue', d.drawingNo ?? '')
  emit('update:drawingId', d.id)
  emit('select', d)
  libraryVisible.value = false
}

function clear() {
  selectedDrawing.value = null
  emit('update:modelValue', '')
  emit('update:drawingId', undefined)
}

function onFileChange(file: UploadFile) {
  uploadFile.value = file.raw ?? null
  if (file.name) {
    const base = file.name.replace(/\.[^.]+$/, '')
    if (!uploadCustomerDrawingNo.value) uploadCustomerDrawingNo.value = base
    if (!uploadTitle.value) uploadTitle.value = base
  }
}

function resetUpload() {
  uploadTitle.value = ''
  uploadCustomerDrawingNo.value = ''
  uploadMaterialGrade.value = ''
  uploadSpecSize.value = ''
  uploadUnitWeight.value = undefined
  uploadFile.value = null
  uploadRef.value?.clearFiles()
}

async function submitUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择图纸文件')
    return
  }
  if (!uploadMaterialGrade.value.trim()) {
    ElMessage.warning('请填写材质')
    return
  }
  if (!uploadSpecSize.value.trim()) {
    ElMessage.warning('请填写规格尺寸')
    return
  }
  uploadLoading.value = true
  try {
    const form = new FormData()
    form.append('file', uploadFile.value)
    if (uploadTitle.value.trim()) form.append('title', uploadTitle.value.trim())
    form.append('materialGrade', uploadMaterialGrade.value.trim())
    form.append('specSize', uploadSpecSize.value.trim())
    if (uploadCustomerDrawingNo.value.trim()) {
      form.append('customerDrawingNo', uploadCustomerDrawingNo.value.trim())
    }
    if (uploadUnitWeight.value != null) {
      form.append('unitWeight', String(uploadUnitWeight.value))
    }
    form.append('operatorUserId', String(operatorUserId()))
    form.append('releaseAfter', 'true')

    const d = unwrapResult<Drawing>(
      await api.post('/drawings/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    )
    ElMessage.success(`图纸已录入：${d.drawingNo}`)
    pickDrawing(d)
    uploadVisible.value = false
    libraryVisible.value = false
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '上传失败')
  } finally {
    uploadLoading.value = false
  }
}

async function openPreview() {
  const id = selectedDrawing.value?.id ?? props.drawingId
  if (!id) {
    ElMessage.info('请先选择图纸')
    return
  }
  previewVisible.value = true
  previewLoading.value = true
  previewError.value = ''
  previewUrl.value = ''
  try {
    const blob: Blob = await api.get(`/drawings/${id}/preview`, { responseType: 'blob' })
    if (blob instanceof Blob && blob.size > 0) {
      previewUrl.value = URL.createObjectURL(blob)
    } else {
      previewError.value = '预览数据为空'
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string; code?: number } }; message?: string }
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

function focus() {
  openLibrary()
}

defineExpose({ focus, openPreview })
</script>

<style scoped>
.drawing-picker { width: 100%; }
.picker-row { display: flex; align-items: center; gap: 4px; }
.picker-input { flex: 1; cursor: pointer; }
.picker-input :deep(.el-input__inner) { cursor: pointer; }
.action-icon { cursor: pointer; margin-left: 4px; color: var(--erp-text-muted); }
.action-icon.preview { color: var(--erp-color-primary); }
.action-icon:hover { color: var(--erp-color-primary); }
.upload-tip { font-size: 12px; color: var(--erp-text-muted); margin-top: 8px; }
.upload-file-item :deep(.el-form-item__content) {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.upload-block :deep(.el-upload) {
  display: block;
}
.preview-wrap { min-height: 480px; }
.pdf-frame { width: 100%; height: 70vh; border: 1px solid var(--erp-border); border-radius: 4px; }
</style>
