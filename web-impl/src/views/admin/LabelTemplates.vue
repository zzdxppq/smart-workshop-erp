<template>
  <ErpPageShell title="标签模板" description="V1.3.9 Story 12.3 · GD/LZ/WW/WL 四类模板 · SB 复用 GD 色条 #6B7280">
    <el-alert type="info" :closable="false" title="50mm×30mm 三区布局 · 修改后可在右侧预览即时生效" style="margin-bottom: 12px" />

    <el-row :gutter="16">
      <el-col :span="14">
        <el-table v-loading="loading" :data="templates" stripe border>
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag :color="row.colorStrip" effect="dark" size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="名称" width="100" />
          <el-table-column prop="prefix" label="前缀" width="70" />
          <el-table-column label="色条" width="100">
            <template #default="{ row }">
              <span class="color-chip" :style="{ background: row.colorStrip }" />
              {{ row.colorStrip }}
            </template>
          </el-table-column>
          <el-table-column label="复用" width="90">
            <template #default="{ row }">
              <span v-if="row.reuseFrom">{{ row.reuseFrom }}</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="dpi" label="DPI" width="70" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.type !== 'SB'"
                size="small"
                @click="openEdit(row as LabelTemplate)"
              >
                编辑
              </el-button>
              <span v-else class="muted">只读</span>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :span="10">
        <el-card header="实时预览" shadow="never">
          <el-form label-width="80px" size="small">
            <el-form-item label="预览类型">
              <el-select v-model="previewType" @change="onPreviewTypeChange">
                <el-option v-for="t in previewTypes" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="二维码">
              <el-input v-model="previewQr" />
            </el-form-item>
            <el-form-item label="明文行">
              <el-input v-model="previewLinesText" type="textarea" :rows="4" placeholder="每行一条，最多 6 行" />
            </el-form-item>
          </el-form>
          <div class="preview-wrap">
            <LabelPreview
              :key="previewKey"
              :type="previewType"
              :qr-content="previewQr"
              :lines="previewLines"
              :factory-name="companyName"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="`编辑模板 · ${editing?.type}`" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="色条" prop="colorStrip">
          <el-color-picker v-model="form.colorStrip" />
          <el-input v-model="form.colorStrip" style="width: 140px; margin-left: 8px" />
        </el-form-item>
        <el-form-item label="厂名" prop="factoryName">
          <el-input v-model="form.factoryName" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="DPI" prop="dpi">
          <el-radio-group v-model="form.dpi">
            <el-radio :value="203">203</el-radio>
            <el-radio :value="300">300</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import LabelPreview from '@/components/label/LabelPreview.vue'
import { useLabelsStore, type LabelTemplate } from '@/stores/labels'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const labelsStore = useLabelsStore()
const loading = ref(false)
const saving = ref(false)
const templates = ref<LabelTemplate[]>([])
const companyName = ref('昆山佰泰胜精密加工')
const dialogVisible = ref(false)
const editing = ref<LabelTemplate | null>(null)
const formRef = ref<FormInstance>()

const previewType = ref<'GD' | 'LZ' | 'SB' | 'WW' | 'WL'>('GD')
const previewQr = ref('GD-260614-001')
const previewLinesText = ref('GD-260614-001\n工单：WO20260614001\n工序：P03\n数量：50\n日期：2026-06-14')
const previewKey = ref(0)

const previewTypes = ['GD', 'LZ', 'SB', 'WW', 'WL'] as const

const previewLines = computed(() =>
  previewLinesText.value.split('\n').map((s) => s.trim()).filter(Boolean).slice(0, 6),
)

const form = ref({
  colorStrip: '#1E40AF',
  factoryName: '',
  dpi: 300,
  enabled: true,
})

const rules: FormRules = {
  colorStrip: [{ required: true, message: '请选择色条', trigger: 'blur' }],
  factoryName: [{ required: true, message: '厂名必填', trigger: 'blur' }],
}

async function reload() {
  loading.value = true
  try {
    await labelsStore.loadTemplates()
    templates.value = labelsStore.templates
    companyName.value = labelsStore.companyName
  } finally {
    loading.value = false
  }
}

function onPreviewTypeChange(type: string) {
  const t = templates.value.find((x) => x.type === type)
  if (t?.qrExample) previewQr.value = t.qrExample
  previewKey.value++
}

function openEdit(row: LabelTemplate) {
  if (row.type === 'SB') return
  editing.value = row
  form.value = {
    colorStrip: row.colorStrip,
    factoryName: row.factoryName || companyName.value,
    dpi: row.dpi ?? 300,
    enabled: row.enabled !== false,
  }
  dialogVisible.value = true
}

async function save() {
  if (!editing.value || editing.value.type === 'SB') return
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    unwrapResult(await useBaseStore().api.put(`/label-templates/${editing.value.type}`, {
      colorStrip: form.value.colorStrip,
      factoryName: form.value.factoryName,
      dpi: form.value.dpi,
      enabled: form.value.enabled ? 1 : 0,
    }))
    ElMessage.success('模板已更新')
    dialogVisible.value = false
    labelsStore.clearPreviewCache()
    previewKey.value++
    await reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.color-chip {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 2px;
  margin-right: 6px;
  vertical-align: middle;
  border: 1px solid #dcdfe6;
}
.preview-wrap {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
.muted {
  color: var(--erp-text-muted);
  font-size: 12px;
}
</style>
