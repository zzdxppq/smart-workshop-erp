<template>
  <div class="conversion-wizard">
    <el-alert type="info" :closable="false" title="客户图 → 厂内图：选图纸 → 添加标注 → 触发工程转化（Story 1.8）" />

    <el-steps :active="step" finish-status="success" align-center style="margin: 20px 0">
      <el-step title="选择图纸" />
      <el-step title="添加标注" />
      <el-step title="工程转化" />
    </el-steps>

    <!-- Step 1 -->
    <div v-show="step === 0">
      <el-form :inline="true">
        <el-form-item label="图号">
          <DrawingPicker v-model="keyword" style="width: 280px" @select="onPickDrawing" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loadingDrawings" @click="searchDrawings">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="drawingOptions" highlight-current-row stripe border @current-change="onDrawingSelect">
        <el-table-column prop="drawingNo" label="图号" min-width="130" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 12px">
        <el-button type="primary" :disabled="!selectedDrawing" @click="step = 1">下一步：标注</el-button>
      </div>
    </div>

    <!-- Step 2 -->
    <div v-show="step === 1">
      <p v-if="selectedDrawing">当前图纸：<strong>{{ selectedDrawing.drawingNo }}</strong> · {{ selectedDrawing.version }}</p>
      <el-form :model="annoForm" label-width="100px" style="max-width: 640px">
        <el-form-item label="版本" required>
          <el-input v-model="annoForm.version" placeholder="v1" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="annoForm.type">
            <el-option label="尺寸 DIMENSION" value="DIMENSION" />
            <el-option label="公差 TOLERANCE" value="TOLERANCE" />
            <el-option label="工艺要求 PROCESS_REQ" value="PROCESS_REQ" />
            <el-option label="技术说明 TECH_NOTE" value="TECH_NOTE" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="annoForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="addingAnno" @click="addAnnotation">添加标注</el-button>
          <el-button @click="loadAnnotations">刷新列表</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="annotations" stripe border>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="content" label="内容" min-width="200" />
        <el-table-column prop="color" label="颜色" width="80" />
      </el-table>
      <el-divider content-position="left">CAD/CAM 附件（FR-3-2-2）</el-divider>
      <DrawingCadAttachments v-if="selectedDrawing?.id" :drawing-id="selectedDrawing.id" />
      <div style="margin-top: 12px">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" @click="step = 2">下一步：转化</el-button>
      </div>
    </div>

    <!-- Step 3 -->
    <div v-show="step === 2">
      <el-form :model="convertForm" label-width="120px" style="max-width: 480px">
        <el-form-item label="BOM 类型">
          <el-select v-model="convertForm.bomType">
            <el-option label="标准 STANDARD" value="STANDARD" />
            <el-option label="FA" value="FA" />
            <el-option label="样机 PROTOTYPE" value="PROTOTYPE" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标数量">
          <el-input-number v-model="convertForm.targetQty" :min="1" />
        </el-form-item>
        <el-form-item label="工程师">
          <el-input v-model="convertForm.engineerName" placeholder="PDF 水印用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="convertForm.comment" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button @click="step = 1">上一步</el-button>
          <el-button type="primary" :loading="converting" @click="runConvert">触发工程转化</el-button>
        </el-form-item>
      </el-form>
      <el-result v-if="convertResult" icon="success" title="转化成功！">
        <template #sub-title>
          厂内图号：{{ convertResult.factoryDrawingNo || convertResult.materialCode || '—' }}
          · BOM 号：{{ convertResult.bomNo }}
          · 料号：{{ convertResult.materialCode }}
          · 状态：<ErpStatusTag :status="convertResult.status" />
        </template>
        <template #extra>
          <el-button type="primary" @click="goBomEditor">立即去维护 BOM</el-button>
          <el-button @click="$router.push('/material/boms')">查看 BOM 列表</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import DrawingCadAttachments from '@/components/material/DrawingCadAttachments.vue'
import type { Drawing } from '@/api/generated/models/Drawing'
import { useMaterialStore } from '@/stores/material'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const router = useRouter()

interface DrawingRow {
  id: number
  drawingNo: string
  version: string
  title?: string
  status?: string
}

interface AnnotationRow {
  version?: string
  type?: string
  content?: string
  color?: string
}

interface ConversionResult {
  bomNo?: string
  bomId?: number
  materialCode?: string
  factoryDrawingNo?: string
  status?: string
}

const materialStore = useMaterialStore()
const step = ref(0)
const keyword = ref('')
const loadingDrawings = ref(false)
const drawingOptions = ref<DrawingRow[]>([])
const selectedDrawing = ref<DrawingRow | null>(null)
const annotations = ref<AnnotationRow[]>([])
const addingAnno = ref(false)
const converting = ref(false)
const convertResult = ref<ConversionResult | null>(null)

const annoForm = ref({
  version: '',
  type: 'DIMENSION',
  content: '',
})

const convertForm = ref({
  bomType: 'STANDARD',
  targetQty: 1,
  engineerName: '',
  comment: '',
})

watch(selectedDrawing, (d) => {
  if (d?.version) annoForm.value.version = d.version
})

function onPickDrawing(d: Drawing) {
  if (!d.id) {
    ElMessage.warning('图纸数据不完整，请重新选择')
    return
  }
  keyword.value = d.drawingNo ?? ''
  selectedDrawing.value = {
    id: d.id,
    drawingNo: d.drawingNo ?? '',
    version: d.version ?? 'v1',
    title: d.title,
    status: d.status,
  }
  searchDrawings()
}

async function searchDrawings() {
  loadingDrawings.value = true
  try {
    const r = await materialStore.listDrawings({ pageNum: 1, pageSize: 20, keyword: keyword.value || undefined })
    const { items } = parsePageItems(r)
    drawingOptions.value = items as DrawingRow[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '查询失败')
  } finally {
    loadingDrawings.value = false
  }
}

function onDrawingSelect(row: DrawingRow | null) {
  selectedDrawing.value = row
}

async function loadAnnotations() {
  if (!selectedDrawing.value?.id) return
  try {
    const list = unwrapResult<AnnotationRow[]>(
      await materialStore.listAnnotations(selectedDrawing.value.id, annoForm.value.version || undefined),
    )
    annotations.value = Array.isArray(list) ? list : []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载标注失败')
  }
}

async function addAnnotation() {
  if (!selectedDrawing.value?.id) {
    ElMessage.warning('请先选择图纸')
    return
  }
  if (!annoForm.value.version || !annoForm.value.content?.trim()) {
    ElMessage.warning('版本与内容必填')
    return
  }
  addingAnno.value = true
  try {
    await materialStore.addAnnotation(selectedDrawing.value.id, annoForm.value)
    ElMessage.success('标注已添加')
    annoForm.value.content = ''
    await loadAnnotations()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '添加失败')
  } finally {
    addingAnno.value = false
  }
}

async function runConvert() {
  if (!selectedDrawing.value?.id) {
    ElMessage.warning('请先选择图纸')
    return
  }
  converting.value = true
  convertResult.value = null
  try {
    const result = unwrapResult<ConversionResult>(
      await materialStore.convertDrawing(selectedDrawing.value.id, convertForm.value),
    )
    convertResult.value = result
    ElMessage.success(`工程转化完成：${result.bomNo ?? ''}`)
    await ElMessageBox.confirm(
      '转化成功！是否立即去维护 BOM？',
      '工程转化',
      { confirmButtonText: '立即去维护 BOM', cancelButtonText: '稍后', type: 'success' },
    ).then(() => goBomEditor()).catch(() => { /* 用户选择稍后 */ })
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转化失败')
  } finally {
    converting.value = false
  }
}

function goBomEditor() {
  const bomId = convertResult.value?.bomId
  const bomNo = convertResult.value?.bomNo
  if (bomId) {
    router.push({ path: '/material/boms/edit', query: { bomId: String(bomId) } })
  } else if (bomNo) {
    router.push({ path: '/material/boms/edit', query: { bomNo } })
  } else {
    router.push('/material/boms')
  }
}

searchDrawings()
</script>

<style scoped>
.conversion-wizard { padding: 8px 0; }
</style>
