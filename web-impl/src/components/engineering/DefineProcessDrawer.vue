<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    direction="rtl"
    size="720px"
    :destroy-on-close="true"
    @closed="emit('closed')"
  >
    <el-alert type="info" show-icon :closable="false" style="margin-bottom: 12px">
      {{ scenarioHint }}
    </el-alert>

    <div v-if="loading" v-loading="true" style="min-height: 120px" />

    <template v-else>
      <el-form label-width="96px" size="small" style="margin-bottom: 12px">
        <el-form-item label="单号">{{ refNo }}</el-form-item>
        <el-form-item v-if="mode === 'quote' && quoteItems.length > 1" label="明细行">
          <el-select v-model="activeItemId" style="width: 100%" @change="loadQuoteItemProcess">
            <el-option
              v-for="item in quoteItems"
              :key="item.id"
              :label="`${item.drawingNo || '明细'} · ${item.material || ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-tag :type="phaseTagType">{{ ENGINEER_TASK_PHASE_LABEL[phase] }}</el-tag>
        </el-form-item>
      </el-form>

      <div class="section-title">工艺路线 · 从工艺库选择并排序</div>
      <div class="library-row">
        <el-select
          v-model="pickCode"
          filterable
          placeholder="从工艺库选择工序（车床/CNC/放电/线割…）"
          style="flex: 1"
        >
          <el-option
            v-for="p in DEFAULT_PROCESS_LIBRARY"
            :key="p.processCode"
            :label="`${p.processName} · ${p.machineType}`"
            :value="p.processCode"
          />
        </el-select>
        <el-button type="primary" :disabled="!pickCode" @click="addStep">加入路线</el-button>
      </div>

      <el-table :data="steps" border stripe size="small" row-key="id" style="margin-top: 8px">
        <el-table-column label="#" width="48">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="processName" label="工序" min-width="100" />
        <el-table-column prop="machineType" label="设备" min-width="110" />
        <el-table-column label="预估工时(min)" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.estimatedMinutes" :min="1" :step="5" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <template v-if="mode === 'order'">
          <el-table-column label="转速" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.spindleRpm" :min="0" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="进给" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.feedRate" :min="0" :step="0.1" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="切深" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.cutDepth" :min="0" :step="0.01" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="刀具号" width="90">
            <template #default="{ row }">
              <el-input v-model="row.toolNo" size="small" />
            </template>
          </el-table-column>
        </template>
        <el-table-column label="顺序" width="100" fixed="right">
          <template #default="{ $index }">
            <el-button link :disabled="$index === 0" @click="move($index, -1)">↑</el-button>
            <el-button link :disabled="$index >= steps.length - 1" @click="move($index, 1)">↓</el-button>
            <el-button link type="danger" @click="steps.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template v-if="mode === 'order'">
        <div class="section-title" style="margin-top: 16px">BOM 编制</div>
        <el-table :data="bomLines" border stripe size="small">
          <el-table-column label="物料编码" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.materialCode" size="small" placeholder="WL- / 原材料" />
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.materialName" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.qty" :min="0" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="80">
            <template #default="{ row }">
              <el-input v-model="row.unit" size="small" />
            </template>
          </el-table-column>
          <el-table-column width="60">
            <template #default="{ $index }">
              <el-button link type="danger" @click="bomLines.splice($index, 1)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="small" style="margin-top: 8px" @click="addBomLine">+ 添加 BOM 行</el-button>
      </template>

      <template v-if="mode === 'quote'">
        <div class="section-title" style="margin-top: 16px">表处面积（㎡）</div>
        <el-form :inline="true" size="small">
          <el-form-item label="阳极氧化">
            <el-input-number v-model="surfaceAreas.anodizeArea" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="固溶">
            <el-input-number v-model="surfaceAreas.solidSolutionArea" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
          <el-form-item label="整形">
            <el-input-number v-model="surfaceAreas.formingArea" :min="0" :step="0.1" controls-position="right" />
          </el-form-item>
        </el-form>
        <div v-if="quoteItems.length > 1" class="item-nav">
          <el-button size="small" :disabled="!hasPrevItem" @click="switchItem(-1)">上一个图号</el-button>
          <el-button size="small" :disabled="!hasNextItem" @click="switchItem(1)">下一个图号</el-button>
        </div>
      </template>

      <div v-if="mode === 'quote'" class="quote-summary">
        预估总工时：<strong>{{ totalMinutes }}</strong> min · 保存后调用后端计算报价
      </div>
    </template>

    <div class="drawer-actions">
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="saving" :disabled="loading" @click="saveDraft">保存草稿</el-button>
      <el-button v-if="mode === 'quote'" type="primary" :loading="saving" :disabled="loading" @click="completeQuote">
        保存并计算报价
      </el-button>
      <el-button v-else type="primary" :loading="saving" :disabled="loading" @click="submitConversion">
        提交工程转化
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DEFAULT_PROCESS_LIBRARY,
  ENGINEER_TASK_PHASE_LABEL,
  type EngineerProcessStep,
  type EngineerTaskPhase,
} from '@/utils/engineeringTask'
import { useEngineeringStore, type QuoteItemRow } from '@/stores/engineering'

const props = defineProps<{
  modelValue: boolean
  mode: 'quote' | 'order'
  refId: number
  refNo: string
  title?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  saved: []
  closed: []
}>()

const eng = useEngineeringStore()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const loading = ref(false)
const saving = ref(false)
const pickCode = ref('')
const steps = ref<EngineerProcessStep[]>([])
const bomLines = ref<{ materialCode: string; materialName: string; qty: number; unit: string }[]>([])
const phase = ref<EngineerTaskPhase>('PENDING')

const quoteItems = ref<QuoteItemRow[]>([])
const activeItemId = ref<number | null>(null)
const activeItemIndex = computed(() =>
  quoteItems.value.findIndex((i) => i.id === activeItemId.value),
)
const hasPrevItem = computed(() => activeItemIndex.value > 0)
const hasNextItem = computed(() => activeItemIndex.value >= 0 && activeItemIndex.value < quoteItems.value.length - 1)
const surfaceAreas = ref({ anodizeArea: 0, solidSolutionArea: 0, formingArea: 0 })
const workbenchId = ref<number | null>(null)

const drawerTitle = computed(() =>
  props.mode === 'quote' ? '定义工艺 · 报价工艺定义' : '定义工艺 · 订单工程转化',
)
const scenarioHint = computed(() =>
  props.mode === 'quote'
    ? '场景 A：收到报价单待办 → 基于客户图纸定义工艺路线、填写预估工时 → 计算报价'
    : '场景 B：销售订单确认后 → 细化工艺参数、编制 BOM → 提交后进入待转产池',
)
const totalMinutes = computed(() => steps.value.reduce((s, r) => s + (r.estimatedMinutes || 0), 0))
const phaseTagType = computed(() =>
  phase.value === 'COMPLETED' ? 'success' : phase.value === 'IN_PROGRESS' ? 'warning' : 'info',
)

async function loadQuoteItemProcess() {
  if (!activeItemId.value) return
  const { steps: loaded, item } = await eng.getQuoteItemProcess(activeItemId.value)
  steps.value = loaded.length ? loaded.map((s) => ({ ...s })) : []
  phase.value = steps.value.length ? 'IN_PROGRESS' : 'PENDING'
  const it = item as { anodizeArea?: number; solidSolutionArea?: number; formingArea?: number } | undefined
  surfaceAreas.value = {
    anodizeArea: Number(it?.anodizeArea ?? 0),
    solidSolutionArea: Number(it?.solidSolutionArea ?? 0),
    formingArea: Number(it?.formingArea ?? 0),
  }
}

async function switchItem(delta: number) {
  const next = activeItemIndex.value + delta
  if (next < 0 || next >= quoteItems.value.length) return
  activeItemId.value = quoteItems.value[next].id
  await loadQuoteItemProcess()
}

async function loadQuoteData() {
  const { items } = await eng.getQuoteDetail(props.refId)
  quoteItems.value = items
  if (!items.length) {
    ElMessage.warning('该报价单无明细行')
    return
  }
  activeItemId.value = items[0].id
  await loadQuoteItemProcess()
}

async function loadOrderData() {
  let wbs = await eng.getOrderWorkbenches(props.refId)
  if (!wbs.length) {
    wbs = await eng.ensureOrderWorkbench(props.refId)
  }
  if (!wbs.length) {
    ElMessage.warning('无法创建工程转化工作台')
    return
  }
  workbenchId.value = wbs[0].id
  const detail = await eng.getWorkbenchDetail(wbs[0].id)
  steps.value = detail.steps.map((s) => ({ ...s }))
  bomLines.value = detail.bomLines.map((b) => ({ ...b }))
  phase.value = detail.phase
}

async function loadExisting() {
  loading.value = true
  steps.value = []
  bomLines.value = []
  phase.value = 'PENDING'
  workbenchId.value = null
  quoteItems.value = []
  activeItemId.value = null
  try {
    if (props.mode === 'quote') {
      await loadQuoteData()
    } else {
      await loadOrderData()
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.refId, props.mode] as const,
  ([open]) => {
    if (open) void loadExisting()
  },
  { immediate: true },
)

function addStep() {
  const lib = DEFAULT_PROCESS_LIBRARY.find((p) => p.processCode === pickCode.value)
  if (!lib) return
  steps.value.push({
    id: `${lib.processCode}-${Date.now()}`,
    ...lib,
  })
  pickCode.value = ''
  phase.value = 'IN_PROGRESS'
}

function move(index: number, delta: number) {
  const next = index + delta
  if (next < 0 || next >= steps.value.length) return
  const arr = [...steps.value]
  ;[arr[index], arr[next]] = [arr[next], arr[index]]
  steps.value = arr
}

function addBomLine() {
  bomLines.value.push({ materialCode: '', materialName: '', qty: 1, unit: 'PCS' })
}

async function saveDraft() {
  if (!steps.value.length) {
    ElMessage.warning('请至少添加工序')
    return
  }
  saving.value = true
  try {
    if (props.mode === 'quote') {
      if (!activeItemId.value) return
      await eng.saveQuoteItemProcess(activeItemId.value, steps.value)
      await eng.saveQuoteItemSurfaceAreas(activeItemId.value, surfaceAreas.value)
    } else {
      if (!workbenchId.value) return
      if (phase.value === 'PENDING') {
        await eng.startWorkbench(workbenchId.value)
      }
      await eng.saveWorkbenchProcess(workbenchId.value, steps.value)
      if (bomLines.value.length) {
        await eng.saveWorkbenchBom(workbenchId.value, bomLines.value)
      }
    }
    phase.value = 'IN_PROGRESS'
    ElMessage.success('草稿已保存')
    emit('saved')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function completeQuote() {
  if (!steps.value.length || !activeItemId.value) {
    ElMessage.warning('请定义工艺路线后再计算报价')
    return
  }
  saving.value = true
  try {
    await eng.saveQuoteItemProcess(activeItemId.value, steps.value)
    await eng.saveQuoteItemSurfaceAreas(activeItemId.value, surfaceAreas.value)
    await eng.calculateQuoteItem(activeItemId.value)
    phase.value = 'COMPLETED'
    ElMessage.success(`工艺已定义（${totalMinutes.value} min），报价计算完成`)
    emit('saved')
    visible.value = false
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '计算失败')
  } finally {
    saving.value = false
  }
}

async function submitConversion() {
  if (!steps.value.length || !workbenchId.value) {
    ElMessage.warning('请完善工艺路线')
    return
  }
  saving.value = true
  try {
    if (phase.value === 'PENDING') {
      await eng.startWorkbench(workbenchId.value)
    }
    await eng.saveWorkbenchProcess(workbenchId.value, steps.value)
    await eng.saveWorkbenchBom(workbenchId.value, bomLines.value)
    await eng.submitWorkbench(workbenchId.value)
    phase.value = 'COMPLETED'
    ElMessage.success('工程转化已提交，订单将进入待转产池')
    emit('saved')
    visible.value = false
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.section-title {
  font-weight: 600;
  font-size: 14px;
  margin: 8px 0;
}
.library-row {
  display: flex;
  gap: 8px;
}
.quote-summary {
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--erp-bg-muted, #f6f8fa);
  border-radius: 6px;
  font-size: 13px;
}
.item-nav {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
.drawer-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
