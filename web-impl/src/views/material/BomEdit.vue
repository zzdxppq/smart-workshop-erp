<template>
  <div v-loading="loading" class="bom-edit-page">
    <div class="page-header">
      <div>
        <h2>📦 BOM 物料清单</h2>
        <p v-if="bomMeta.bomNo" class="subtitle">
          {{ bomMeta.bomNo }}
          <el-tag :type="bomMeta.status === 'RELEASED' ? 'success' : 'info'" size="small">
            {{ bomMeta.status === 'RELEASED' ? '已发布' : '草稿' }}
          </el-tag>
          · 产品：{{ bomMeta.materialCode }} {{ bomMeta.drawingNo ? `(${bomMeta.drawingNo})` : '' }}
        </p>
      </div>
      <el-button @click="$router.push('/material/boms')">返回列表</el-button>
    </div>

    <el-card>
      <BomItemTable
        :bom-id="bomId"
        :items="bomItems"
        :editable="bomMeta.status !== 'RELEASED'"
        :status="bomMeta.status"
        @saved="loadBom"
        @released="loadBom"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import BomItemTable, { type BomRow } from '@/components/erp/BomItemTable.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

interface BomHeader {
  id?: number
  bomNo?: string
  materialCode?: string
  drawingNo?: string
  status?: string
}

interface BomItemRaw {
  id?: number
  itemLevel?: number
  materialCode?: string
  materialName?: string
  qty?: number
  unit?: string
  unitCost?: number
  substituteMaterials?: string
}

const route = useRoute()
const api = useBaseStore().api
const loading = ref(false)
const bomMeta = ref<BomHeader>({})
const bomItems = ref<BomRow[]>([])

const bomId = computed(() => {
  const q = route.query.bomId
  if (q) return Number(q)
  return bomMeta.value.id
})

async function resolveBomId(): Promise<number | undefined> {
  const fromQuery = route.query.bomId
  if (fromQuery) return Number(fromQuery)
  const bomNo = route.query.bomNo as string | undefined
  if (!bomNo) return undefined
  const list = unwrapResult<{ list?: BomHeader[] }>(await api.get('/boms', { params: { keyword: bomNo, size: 5 } }))
  const hit = (list.list ?? []).find((b) => b.bomNo === bomNo)
  return hit?.id
}

async function loadBom() {
  loading.value = true
  try {
    const id = await resolveBomId()
    if (!id) {
      ElMessage.warning('未找到 BOM，请从列表或工程转化入口进入')
      return
    }
    const tree = unwrapResult<{ bom?: BomHeader; items?: BomItemRaw[] }>(await api.get(`/boms/${id}/tree`))
    bomMeta.value = tree.bom ?? {}
    bomItems.value = (tree.items ?? []).map((it) => ({
      id: it.id,
      itemLevel: it.itemLevel ?? 1,
      materialCode: it.materialCode ?? '',
      materialName: it.materialName ?? '',
      qty: Number(it.qty ?? 1),
      unit: it.unit ?? '件',
      scrapRate: parseScrap(it.substituteMaterials),
      unitCost: Number(it.unitCost ?? 0),
    }))
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载 BOM 失败')
  } finally {
    loading.value = false
  }
}

function parseScrap(sub?: string): number {
  if (!sub?.startsWith('scrap:')) return 0
  const n = Number(sub.slice(6))
  return Number.isFinite(n) ? n : 0
}

watch(() => [route.query.bomId, route.query.bomNo], loadBom)
onMounted(loadBom)
</script>

<style scoped>
.bom-edit-page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.subtitle { margin: 4px 0 0; font-size: 14px; color: var(--erp-text-muted); display: flex; align-items: center; gap: 8px; }
</style>
