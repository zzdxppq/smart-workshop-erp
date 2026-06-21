<template>
  <div class="bom-item-table">
    <div v-if="editable" class="bom-toolbar">
      <el-button type="primary" size="small" @click="startAddRow">+ 添加子件</el-button>
      <el-button size="small" @click="expandHint = !expandHint">展开全部</el-button>
      <el-button size="small" @click="collapseAll">收起全部</el-button>
      <el-button size="small" type="success" :loading="saving" @click="saveAll">保存</el-button>
      <el-button v-if="canRelease" size="small" type="warning" :loading="releasing" @click="releaseBom">发布</el-button>
    </div>
    <p v-if="editable" class="hint-text">💡 点击行或双击可编辑 · 也可使用 F2 快捷键</p>

    <el-table
      :data="displayRows"
      stripe
      border
      size="small"
      @row-dblclick="(row: BomRow) => editable && startEditRow(row)"
    >
      <el-table-column prop="itemLevel" label="层级" width="60" />
      <el-table-column label="子件编码" min-width="130">
        <template #default="{ row }">
          <el-select
            v-if="row._editing && row._isNew"
            v-model="row.materialCode"
            filterable
            placeholder="选择物料"
            style="width: 100%"
            @change="onMaterialPick(row as BomRow)"
          >
            <el-option
              v-for="m in materialOptions"
              :key="m.code"
              :label="`${m.code} · ${m.name}`"
              :value="m.code"
            />
          </el-select>
          <span v-else>{{ row.materialCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="子件名称" min-width="120">
        <template #default="{ row }">
          <el-input v-if="row._editing" v-model="row.materialName" size="small" />
          <span v-else>{{ row.materialName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用量" width="100">
        <template #default="{ row }">
          <el-input-number v-if="row._editing" v-model="row.qty" :min="0.001" :step="1" size="small" controls-position="right" />
          <span v-else>{{ row.qty }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单位" width="90">
        <template #default="{ row }">
          <el-select v-if="row._editing" v-model="row.unit" size="small" style="width: 72px">
            <el-option label="件" value="件" />
            <el-option label="kg" value="kg" />
            <el-option label="PCS" value="PCS" />
            <el-option label="m" value="m" />
          </el-select>
          <span v-else>{{ row.unit }}</span>
        </template>
      </el-table-column>
      <el-table-column label="损耗" width="90">
        <template #default="{ row }">
          <el-input-number v-if="row._editing" v-model="row.scrapRate" :min="0" :max="100" size="small" controls-position="right" />
          <span v-else>{{ row.scrapRate }}%</span>
        </template>
      </el-table-column>
      <el-table-column v-if="editable" label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <template v-if="row._editing">
            <el-button link type="primary" size="small" @click="saveRow(row as BomRow)">保存</el-button>
            <el-button link size="small" @click="cancelRow(row as BomRow)">取消</el-button>
          </template>
          <template v-else>
            <el-button link type="primary" size="small" @click="startEditRow(row as BomRow)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteRow(row as BomRow)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <p v-if="rows.length" class="footer-hint">共 {{ rows.length }} 个子件</p>
    <p v-if="editable" class="hint-text shortcut">快捷键：F2 编辑当前行 · Enter 保存 · Esc 取消</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

export interface BomRow {
  id?: string | number
  itemLevel?: number
  materialCode: string
  materialName: string
  qty: number
  unit: string
  scrapRate: number
  unitCost?: number
  _editing?: boolean
  _isNew?: boolean
  _snapshot?: BomRow
}

const props = withDefaults(defineProps<{
  bomId?: number
  items?: BomRow[]
  editable?: boolean
  status?: string
}>(), {
  items: () => [],
  editable: true,
  status: 'DRAFT',
})

const emit = defineEmits<{
  saved: []
  released: []
}>()

const api = useBaseStore().api
const rows = ref<BomRow[]>([])
const materialOptions = ref<{ code: string; name: string }[]>([])
const saving = ref(false)
const releasing = ref(false)
const expandHint = ref(true)
const activeRowId = ref<string | number | null>(null)

const canRelease = computed(() => props.status === 'DRAFT' && props.bomId)
const displayRows = computed(() => rows.value)

watch(() => props.items, (items) => {
  rows.value = (items ?? []).map(normalizeRow)
}, { immediate: true, deep: true })

function normalizeRow(r: BomRow): BomRow {
  return {
    ...r,
    qty: Number(r.qty ?? 1),
    unit: r.unit ?? '件',
    scrapRate: Number(r.scrapRate ?? 0),
    itemLevel: r.itemLevel ?? 1,
    _editing: false,
    _isNew: false,
  }
}

async function loadMaterials() {
  try {
    const r = unwrapResult<{ list?: { materialCode?: string; materialName?: string }[] }>(
      await api.get('/materials', { params: { size: 100 } }),
    )
    materialOptions.value = (r.list ?? [])
      .filter((m) => m.materialCode)
      .map((m) => ({ code: m.materialCode!, name: m.materialName ?? m.materialCode! }))
  } catch {
    materialOptions.value = [
      { code: 'RM-001', name: '45#圆钢' },
      { code: 'RM-002', name: '铸件毛坯' },
      { code: 'WL-1003', name: '轴承座' },
    ]
  }
}

function onMaterialPick(row: BomRow) {
  const m = materialOptions.value.find((x) => x.code === row.materialCode)
  if (m) row.materialName = m.name
}

function startAddRow() {
  const id = `new-${Date.now()}`
  rows.value.push({
    id,
    itemLevel: 1,
    materialCode: '',
    materialName: '',
    qty: 1,
    unit: '件',
    scrapRate: 0,
    _editing: true,
    _isNew: true,
  })
  activeRowId.value = id
}

function startEditRow(row: BomRow) {
  cancelOtherEdits(row)
  row._snapshot = { ...row }
  row._editing = true
  activeRowId.value = row.id ?? row.materialCode
}

function cancelOtherEdits(except?: BomRow) {
  rows.value.forEach((r) => {
    if (r !== except && r._editing) cancelRow(r)
  })
}

function cancelRow(row: BomRow) {
  if (row._isNew) {
    rows.value = rows.value.filter((r) => r !== row)
  } else if (row._snapshot) {
    Object.assign(row, row._snapshot, { _editing: false, _snapshot: undefined })
  } else {
    row._editing = false
  }
}

async function saveRow(row: BomRow) {
  if (!row.materialCode?.trim()) {
    ElMessage.warning('请选择子件编码')
    return
  }
  row._editing = false
  row._isNew = false
  row._snapshot = undefined
  await persistRows()
  ElMessage.success('保存成功')
}

async function saveAll() {
  cancelOtherEdits()
  await persistRows()
  ElMessage.success('BOM 已保存')
  emit('saved')
}

async function persistRows() {
  if (!props.bomId) return
  saving.value = true
  try {
    const lines = rows.value
      .filter((r) => r.materialCode?.trim())
      .map((r, i) => ({
        itemLevel: r.itemLevel ?? 1,
        itemNo: i + 1,
        materialCode: r.materialCode,
        materialName: r.materialName,
        qty: r.qty,
        unit: r.unit,
        scrapRate: r.scrapRate,
        unitCost: r.unitCost ?? 0,
      }))
    await api.post('/boms/save-tree', { bomId: props.bomId, lines })
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
    throw e
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: BomRow) {
  try {
    await ElMessageBox.confirm('确定要删除该子件吗？', '删除确认', { type: 'warning' })
    rows.value = rows.value.filter((r) => r !== row)
    await persistRows()
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

async function releaseBom() {
  if (!props.bomId) return
  releasing.value = true
  try {
    await api.post(`/boms/${props.bomId}/publish`, {})
    ElMessage.success('BOM 已发布')
    emit('released')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '发布失败')
  } finally {
    releasing.value = false
  }
}

function collapseAll() {
  expandHint.value = false
}

function onKeydown(e: KeyboardEvent) {
  if (!props.editable) return
  const target = e.target as HTMLElement
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return
  if (e.key === 'F2') {
    const row = rows.value.find((r) => r.id === activeRowId.value) ?? rows.value[0]
    if (row) { e.preventDefault(); startEditRow(row) }
  }
  if (e.key === 'Escape') {
    const editing = rows.value.find((r) => r._editing)
    if (editing) cancelRow(editing)
  }
}

onMounted(() => {
  loadMaterials()
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.bom-toolbar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.hint-text { font-size: 12px; color: var(--erp-text-muted); margin: 8px 0; }
.footer-hint { font-size: 13px; margin-top: 12px; color: var(--erp-text-secondary); }
.shortcut { margin-top: 4px; }
</style>
