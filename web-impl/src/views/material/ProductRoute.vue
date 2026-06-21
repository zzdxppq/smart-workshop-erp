<template>
  <div v-loading="loading" class="product-route-page">
    <div class="page-header">
      <div>
        <h2>📐 工艺路线维护</h2>
        <p v-if="contextLabel" class="context">{{ contextLabel }}</p>
      </div>
      <div class="actions">
        <el-tag v-if="routeStatus" :type="routeStatus === 'RELEASED' ? 'success' : 'info'">
          {{ routeStatus === 'RELEASED' ? '已发布' : '草稿' }}
        </el-tag>
        <el-button :loading="saving" @click="saveRoute">保存草稿</el-button>
        <el-button type="primary" :loading="publishing" @click="publishRoute">发布</el-button>
      </div>
    </div>

    <el-form :inline="true" class="toolbar">
      <el-form-item label="图号">
        <el-input v-model="drawingNo" readonly style="width: 180px" />
      </el-form-item>
      <el-form-item label="物料">
        <el-select v-model="productId" filterable placeholder="WL 料号" style="width: 220px" @change="loadRoute">
          <el-option v-for="p in productOptions" :key="p.id" :label="`${p.code} · ${p.name}`" :value="String(p.id)" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="loadRoute">刷新</el-button>
        <el-button @click="$router.push('/material/process')">工艺库</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="routeMeta.processCode" type="info" show-icon :closable="false" style="margin-bottom: 12px">
      已关联工艺 {{ routeMeta.processCode }} · 总工时 {{ totalMinutes }} min
    </el-alert>

    <el-table :data="processRows" border stripe row-key="processSeq">
      <el-table-column label="序号" width="70">
        <template #default="{ row }">{{ row.processSeq }}</template>
      </el-table-column>
      <el-table-column label="工序名称/编码" min-width="160">
        <template #default="{ row }">
          <el-select v-model="row.processCode" filterable style="width: 140px">
            <el-option v-for="c in mdmProcessCodes" :key="c" :label="processLabel(c)" :value="c" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="标准工时(min)" width="140">
        <template #default="{ row }">
          <el-input-number v-model="row.stdTimeMin" :min="0" :step="5" controls-position="right" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="设备类型" width="100">
        <template #default="{ row }">{{ deviceType(row.processCode) }}</template>
      </el-table-column>
      <el-table-column label="委外" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isOutsource" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ $index }">
          <el-button link :disabled="$index === 0" @click="moveRow($index, -1)">↑</el-button>
          <el-button link :disabled="$index >= processRows.length - 1" @click="moveRow($index, 1)">↓</el-button>
          <el-button link type="danger" @click="processRows.splice($index, 1); reseq()">删</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button style="margin-top: 12px" @click="addRow">+ 从工艺库添加工序</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { E3DrawingService } from '@/api/generated/services/E3DrawingService'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

interface RouteRow {
  processSeq: number
  processCode: string
  stdTimeMin: number
  isOutsource: boolean
}

interface ProductRouteResponse {
  routes?: Array<{ processSeq?: number; processCode?: string; stdTimeMin?: number; isOutsource?: boolean }>
  processCode?: string
  processId?: number
  routeStatus?: string
  process?: { processCode?: string }
  steps?: Array<{ stepNo?: number; estimatedHours?: number; stepName?: string; machineType?: string }>
}

const PROCESS_NAMES: Record<string, string> = {
  P00: '下料', P01: 'CNC粗加工', P02: 'CNC精加工', P03: '热处理', P04: '表面处理', P05: '检验',
  P06: '车削', P07: '铣削', P08: '磨削', P09: '装配',
}
const PROCESS_DEVICE: Record<string, string> = {
  P00: '锯床', P01: 'CNC', P02: 'CNC', P03: '热处理炉', P04: '表处线', P05: '三次元',
}

const vueRoute = useRoute()
const api = useBaseStore().api
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const productId = ref('')
const productCode = ref('')
const drawingNo = ref('')
const drawingId = ref<number | undefined>()
const processRows = ref<RouteRow[]>([])
const routeMeta = ref<{ processId?: number; processCode?: string }>({})
const routeStatus = ref('DRAFT')
const productOptions = ref<{ id: number; code: string; name: string }[]>([])

const mdmProcessCodes = ['P00', 'P01', 'P02', 'P03', 'P04', 'P05', 'P06', 'P07', 'P08', 'P09']
const totalMinutes = computed(() => processRows.value.reduce((s, r) => s + (r.stdTimeMin ?? 0), 0))
const contextLabel = computed(() => {
  if (!drawingNo.value) return ''
  return `图号 ${drawingNo.value} · 物料 ${productCode.value || '—'}`
})

function processLabel(code: string) {
  return `${code} ${PROCESS_NAMES[code] ?? ''}`.trim()
}
function deviceType(code: string) {
  return PROCESS_DEVICE[code] ?? 'CNC'
}

function defaultRows(): RouteRow[] {
  return [
    { processSeq: 1, processCode: 'P00', stdTimeMin: 5, isOutsource: false },
    { processSeq: 2, processCode: 'P01', stdTimeMin: 20, isOutsource: false },
    { processSeq: 3, processCode: 'P02', stdTimeMin: 15, isOutsource: false },
  ]
}

function mapRouteRows(data: ProductRouteResponse): RouteRow[] {
  if (data.routes?.length) {
    return data.routes.map((r, i) => ({
      processSeq: r.processSeq ?? i + 1,
      processCode: r.processCode ?? 'P01',
      stdTimeMin: Number(r.stdTimeMin ?? 30),
      isOutsource: Boolean(r.isOutsource),
    }))
  }
  if (data.steps?.length) {
    return data.steps.map((s, i) => ({
      processSeq: s.stepNo ?? i + 1,
      processCode: 'P01',
      stdTimeMin: Math.round(Number(s.estimatedHours ?? 0.5) * 60),
      isOutsource: false,
    }))
  }
  return []
}

function syncRouteMeta(data: ProductRouteResponse) {
  routeMeta.value = {
    processId: data.processId,
    processCode: data.processCode ?? data.process?.processCode,
  }
  routeStatus.value = data.routeStatus ?? 'DRAFT'
}

function reseq() {
  processRows.value.forEach((r, i) => { r.processSeq = i + 1 })
}

function moveRow(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= processRows.value.length) return
  const rows = [...processRows.value]
  const tmp = rows[index]
  rows[index] = rows[target]
  rows[target] = tmp
  processRows.value = rows
  reseq()
}

async function loadProducts() {
  try {
    const r = unwrapResult<{ list?: { id?: number; materialCode?: string; materialName?: string }[] }>(
      await api.get('/materials', { params: { categoryPrefix: 'WL', size: 100 } }),
    )
    const list = r.list ?? []
    productOptions.value = list
      .filter((m) => m.materialCode?.startsWith('WL-'))
      .map((m) => ({ id: Number(m.id), code: m.materialCode!, name: m.materialName ?? m.materialCode! }))
    if (productCode.value) {
      const hit = productOptions.value.find((p) => p.code === productCode.value)
      if (hit) productId.value = String(hit.id)
    }
    if (!productId.value && productOptions.value.length) {
      productId.value = String(productOptions.value[0].id)
    }
  } catch {
    productOptions.value = []
  }
}

async function resolveProductFromQuery() {
  drawingNo.value = (vueRoute.query.drawingNo as string) ?? drawingNo.value
  productCode.value = (vueRoute.query.materialCode as string) ?? productCode.value
  const dId = vueRoute.query.drawingId as string | undefined
  drawingId.value = dId ? Number(dId) : undefined
  if (productCode.value) {
    try {
      const lookup = unwrapResult<{ id?: number }>(
        await api.get('/materials/lookup', { params: { code: productCode.value } }),
      )
      if (lookup.id) productId.value = String(lookup.id)
    } catch { /* ignore */ }
  }
}

async function loadRoute() {
  if (!productId.value) return
  loading.value = true
  try {
    const data = unwrapResult<ProductRouteResponse>(
      await E3DrawingService.getProductRoute(productId.value),
    )
    syncRouteMeta(data)
    const rows = mapRouteRows(data)
    processRows.value = rows.length ? rows : defaultRows()
    const sel = productOptions.value.find((p) => String(p.id) === productId.value)
    if (sel) productCode.value = sel.code
  } catch {
    if (!processRows.value.length) processRows.value = defaultRows()
  } finally {
    loading.value = false
  }
}

function addRow() {
  processRows.value.push({
    processSeq: processRows.value.length + 1,
    processCode: 'P01',
    stdTimeMin: 30,
    isOutsource: false,
  })
}

async function saveRoute() {
  if (!productId.value) {
    ElMessage.warning('请选择物料')
    return
  }
  saving.value = true
  try {
    const routeBody = {
      changeReason: 'Web 工艺路线编辑',
      drawingId: drawingId.value,
      drawingNo: drawingNo.value || undefined,
      processes: processRows.value,
    }
    const data = unwrapResult<ProductRouteResponse>(
      await E3DrawingService.createProductRoute(
        productId.value,
        routeBody as Parameters<typeof E3DrawingService.createProductRoute>[1],
      ),
    )
    syncRouteMeta(data)
    ElMessage.success('工艺路线已保存（草稿）')
    await loadRoute()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function publishRoute() {
  if (!productId.value) return
  await saveRoute()
  publishing.value = true
  try {
    const data = unwrapResult<ProductRouteResponse>(
      await api.post(`/products/${productId.value}/routes/publish`, null, {
        params: { drawingId: drawingId.value },
      }),
    )
    syncRouteMeta(data)
    ElMessage.success('工艺路线已发布，报价单可预览')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

watch(() => vueRoute.query, async () => {
  await resolveProductFromQuery()
  await loadProducts()
  if (productId.value) await loadRoute()
}, { immediate: true })

onMounted(async () => {
  await resolveProductFromQuery()
  await loadProducts()
  if (productId.value) await loadRoute()
  else processRows.value = defaultRows()
})
</script>

<style scoped>
.product-route-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.actions { display: flex; align-items: center; gap: 8px; }
.context { margin: 4px 0 0; font-size: 14px; color: var(--erp-text-muted); }
.toolbar { margin-bottom: 16px; }
</style>
