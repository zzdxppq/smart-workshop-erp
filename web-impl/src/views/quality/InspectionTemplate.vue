<template>
  <ErpPageShell title="检验方案模板" description="工程师定义检验项 · 品质主管发布生效 · 品质员检验时自动加载">
    <div style="margin-bottom: 16px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="loadTemplates">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="生效中" value="ACTIVE" />
        <el-option label="已停用" value="ARCHIVED" />
      </el-select>
      <el-button v-if="canEdit" type="primary" @click="openCreate">新建模板</el-button>
      <el-text v-if="!canEdit" type="info">当前角色仅可查看模板，编辑需工程师或品质主管权限</el-text>
    </div>

    <el-table v-loading="loading" :data="templates" stripe border>
      <el-table-column prop="templateNo" label="模板编号" min-width="140" />
      <el-table-column prop="templateName" label="模板名称" min-width="160" />
      <el-table-column prop="drawingNoPattern" label="适用图号" min-width="120">
        <template #default="{ row }">{{ row.drawingNoPattern || '通用' }}</template>
      </el-table-column>
      <el-table-column prop="materialCode" label="料号" min-width="110">
        <template #default="{ row }">{{ row.materialCode || '—' }}</template>
      </el-table-column>
      <el-table-column prop="inspectionType" label="检验类型" width="100">
        <template #default="{ row }">{{ inspectTypeLabel(row.inspectionType) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="itemCount" label="检验项数" width="90" align="center" />
      <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewTemplate(row)">查看</el-button>
          <el-button v-if="canEdit && row.status === 'DRAFT'" size="small" @click="editTemplate(row)">编辑</el-button>
          <el-button
            v-if="canPublish && row.status === 'DRAFT'"
            size="small"
            type="success"
            @click="publishTemplate(row)"
          >发布</el-button>
          <el-button
            v-if="canPublish && row.status === 'ACTIVE'"
            size="small"
            type="warning"
            @click="archiveTemplate(row)"
          >停用</el-button>
          <el-button
            v-if="canEdit && row.status === 'DRAFT'"
            size="small"
            type="danger"
            @click="deleteTemplate(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
      <el-form :model="form" label-width="100px" :disabled="readOnly">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="如：法兰盘 IQC 通用模板" />
        </el-form-item>
        <el-form-item label="适用图号">
          <el-input v-model="form.drawingNoPattern" placeholder="支持模糊匹配，如 DWG-2025% 或留空表示通用" />
        </el-form-item>
        <el-form-item label="关联料号">
          <el-input v-model="form.materialCode" placeholder="可选，精确匹配料号" />
        </el-form-item>
        <el-form-item label="检验类型">
          <el-select v-model="form.inspectionType" clearable placeholder="留空=通用">
            <el-option label="IQC 来料检" value="IQC" />
            <el-option label="IPQC 过程检" value="IPQC" />
            <el-option label="OQC 成品检" value="OQC" />
          </el-select>
        </el-form-item>
        <el-form-item label="抽检比例">
          <el-input-number v-model="form.sampleRatio" :min="0" :max="100" :precision="2" /> %
        </el-form-item>
        <el-divider content-position="left">检验项目</el-divider>
        <el-table :data="form.items" stripe size="small">
          <el-table-column label="序号" width="60" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="项名称" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.itemName" placeholder="如：外观、尺寸" />
            </template>
          </el-table-column>
          <el-table-column label="标准" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.standard" placeholder="如：10±0.05mm" />
            </template>
          </el-table-column>
          <el-table-column label="严重度" width="110">
            <template #default="{ row }">
              <el-select v-model="row.severity">
                <el-option label="INFO" value="INFO" />
                <el-option label="WARN" value="WARN" />
                <el-option label="ERROR" value="ERROR" />
                <el-option label="CRITICAL" value="CRITICAL" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column v-if="!readOnly" width="60">
            <template #default="{ $index }">
              <el-button size="small" type="danger" @click="removeItem($index)">×</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button v-if="!readOnly" size="small" type="primary" plain @click="addItem" style="margin-top: 8px">
          + 添加检验项
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ readOnly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!readOnly" type="primary" :loading="saving" @click="saveTemplate">保存草稿</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useQualityStore } from '@/stores/quality'
import { useAuthStore } from '@/stores/auth'
import { canEditInspectionTemplate, canPublishInspectionTemplate } from '@/utils/roleAccess'
import { unwrapResult } from '@/utils/apiPage'

interface TemplateItem {
  itemName: string
  standard: string
  severity: string
  sortOrder?: number
}

interface TemplateRow {
  id?: number
  templateNo?: string
  templateName?: string
  drawingNoPattern?: string
  materialCode?: string
  inspectionType?: string
  sampleRatio?: number
  status?: string
  items?: TemplateItem[]
  itemCount?: number
  updatedAt?: string
}

const qualityStore = useQualityStore()
const authStore = useAuthStore()
const canEdit = computed(() => canEditInspectionTemplate(authStore.userRoles))
const canPublish = computed(() => canPublishInspectionTemplate(authStore.userRoles))

const loading = ref(false)
const saving = ref(false)
const templates = ref<TemplateRow[]>([])
const statusFilter = ref('')
const dialogVisible = ref(false)
const readOnly = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  templateName: '',
  drawingNoPattern: '',
  materialCode: '',
  inspectionType: '' as string,
  sampleRatio: undefined as number | undefined,
  items: [] as TemplateItem[],
})

const dialogTitle = computed(() => {
  if (readOnly.value) return '查看模板'
  return editingId.value ? '编辑模板' : '新建模板'
})

function statusLabel(s?: string) {
  switch (s) {
    case 'DRAFT': return '草稿'
    case 'ACTIVE': return '生效中'
    case 'ARCHIVED': return '已停用'
    default: return s ?? '—'
  }
}

function statusTagType(s?: string): 'info' | 'success' | 'warning' {
  switch (s) {
    case 'ACTIVE': return 'success'
    case 'ARCHIVED': return 'warning'
    default: return 'info'
  }
}

function inspectTypeLabel(t?: string) {
  switch (t) {
    case 'IQC': return 'IQC'
    case 'IPQC': return 'IPQC'
    case 'OQC': return 'OQC'
    default: return t || '通用'
  }
}

function resetForm() {
  form.value = {
    templateName: '',
    drawingNoPattern: '',
    materialCode: '',
    inspectionType: '',
    sampleRatio: undefined,
    items: [],
  }
  editingId.value = null
}

function openCreate() {
  resetForm()
  readOnly.value = false
  addItem()
  dialogVisible.value = true
}

function addItem() {
  form.value.items.push({ itemName: '', standard: '', severity: 'INFO' })
}

function removeItem(idx: number) {
  form.value.items.splice(idx, 1)
}

async function loadTemplateDetail(id: number): Promise<TemplateRow> {
  return unwrapResult<TemplateRow>(await qualityStore.getTemplate(id))
}

async function viewTemplate(row: TemplateRow) {
  if (!row.id) return
  readOnly.value = true
  const detail = await loadTemplateDetail(row.id)
  fillForm(detail)
  dialogVisible.value = true
}

async function editTemplate(row: TemplateRow) {
  if (!row.id) return
  readOnly.value = false
  editingId.value = row.id
  const detail = await loadTemplateDetail(row.id)
  fillForm(detail)
  dialogVisible.value = true
}

function fillForm(row: TemplateRow) {
  form.value.templateName = row.templateName ?? ''
  form.value.drawingNoPattern = row.drawingNoPattern ?? ''
  form.value.materialCode = row.materialCode ?? ''
  form.value.inspectionType = row.inspectionType ?? ''
  form.value.sampleRatio = row.sampleRatio
  form.value.items = (row.items ?? []).map((it, idx) => ({
    itemName: it.itemName,
    standard: it.standard ?? '',
    severity: it.severity ?? 'INFO',
    sortOrder: idx,
  }))
  if (!readOnly.value && form.value.items.length === 0) addItem()
}

async function saveTemplate() {
  if (!form.value.templateName.trim()) {
    ElMessage.warning('模板名称必填')
    return
  }
  if (form.value.items.length === 0) {
    ElMessage.warning('至少添加 1 个检验项')
    return
  }
  const payload = {
    templateName: form.value.templateName.trim(),
    drawingNoPattern: form.value.drawingNoPattern.trim() || undefined,
    materialCode: form.value.materialCode.trim() || undefined,
    inspectionType: form.value.inspectionType || undefined,
    sampleRatio: form.value.sampleRatio,
    items: form.value.items.map((it, idx) => ({
      itemName: it.itemName,
      standard: it.standard,
      severity: it.severity,
      sortOrder: idx,
    })),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await qualityStore.updateTemplate(editingId.value, payload)
      ElMessage.success('模板已更新')
    } else {
      await qualityStore.createTemplate(payload)
      ElMessage.success('草稿已保存，请由品质主管发布后生效')
    }
    dialogVisible.value = false
    await loadTemplates()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function publishTemplate(row: TemplateRow) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认发布模板「${row.templateName}」？发布后将自动停用同范围旧模板。`, '发布确认')
    await qualityStore.publishTemplate(row.id)
    ElMessage.success('模板已发布生效')
    await loadTemplates()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '发布失败')
  }
}

async function archiveTemplate(row: TemplateRow) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认停用模板「${row.templateName}」？`, '停用确认')
    await qualityStore.archiveTemplate(row.id)
    ElMessage.success('模板已停用')
    await loadTemplates()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '停用失败')
  }
}

async function deleteTemplate(row: TemplateRow) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除草稿「${row.templateName}」？`, '删除确认', { type: 'warning' })
    await qualityStore.deleteTemplate(row.id)
    ElMessage.success('已删除')
    await loadTemplates()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

async function loadTemplates() {
  loading.value = true
  try {
    const query = statusFilter.value ? { status: statusFilter.value } : undefined
    const data = unwrapResult<{ items?: TemplateRow[] }>(await qualityStore.listTemplates(query))
    templates.value = Array.isArray(data.items) ? data.items : []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载模板失败')
    templates.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadTemplates)
</script>
