<template>
  <div class="boms-page">
    <el-row :gutter="16">
      <el-col :span="14">
        <h2>BOM 物料清单</h2>
        <el-form :inline="true" style="margin-bottom: 12px">
          <el-form-item label="产品编码">
            <DrawingPicker v-model="drawingNo" style="width: 280px" @select="onDrawingPick" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table
          v-loading="loading"
          :data="items"
          stripe
          border
          highlight-current-row
          @current-change="onSelectRow"
          @row-dblclick="openEditor"
        >
          <el-table-column prop="bomNo" label="BOM号" min-width="140" />
          <el-table-column prop="materialCode" label="产品编码" min-width="120" />
          <el-table-column prop="drawingNo" label="图号" min-width="130" />
          <el-table-column prop="bomVersion" label="版本" width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <ErpStatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="openEditor(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          style="margin-top: 12px"
          @current-change="onPageChange"
          @size-change="onSearch"
        />
      </el-col>
      <el-col :span="10">
        <el-card header="BOM 预览">
          <p class="hint-text">双击列表行或点击「编辑」进入 BOM 编辑器</p>
          <BomItemTable
            v-if="selectedBomId"
            :bom-id="selectedBomId"
            :items="previewItems"
            :editable="false"
            :status="selectedStatus"
          />
          <el-empty v-else description="请选择 BOM 行" :image-size="64" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import BomItemTable, { type BomRow } from '@/components/erp/BomItemTable.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { unwrapResult } from '@/utils/apiPage'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const drawingNo = ref('')
const previewItems = ref<BomRow[]>([])
const selectedBomId = ref<number | undefined>()
const selectedStatus = ref('DRAFT')
const api = useBaseStore().api

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  api.get('/boms', {
    params: { keyword: keyword.value || undefined, drawingNo: drawingNo.value || undefined, ...params },
  }),
)

function onDrawingPick(d: { drawingNo?: string; materialCode?: string }) {
  drawingNo.value = d.drawingNo ?? ''
  keyword.value = d.materialCode ?? d.drawingNo ?? ''
  onSearch()
}

async function onSelectRow(row: { id?: number; status?: string } | null) {
  if (!row?.id) return
  selectedBomId.value = row.id
  selectedStatus.value = row.status ?? 'DRAFT'
  try {
    const tree = unwrapResult<{ items?: BomRow[] }>(await api.get(`/boms/${row.id}/tree`))
    previewItems.value = (tree.items ?? []).map((it) => ({
      id: it.id,
      itemLevel: it.itemLevel ?? 1,
      materialCode: it.materialCode ?? '',
      materialName: it.materialName ?? '',
      qty: Number(it.qty ?? 1),
      unit: it.unit ?? '件',
      scrapRate: 0,
    }))
  } catch {
    previewItems.value = []
  }
}

function openEditor(row?: { id?: number }) {
  const id = row?.id ?? selectedBomId.value
  if (!id) {
    ElMessage.info('请先选择 BOM')
    return
  }
  router.push({ path: '/material/boms/edit', query: { bomId: String(id) } })
}

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

onMounted(async () => {
  onSearch()
  const bomNo = route.query.bomNo as string | undefined
  if (bomNo) {
    keyword.value = bomNo
    await onSearch()
    const hit = items.value.find((b: { bomNo?: string }) => b.bomNo === bomNo)
    if (hit) openEditor(hit)
  }
})
</script>

<style scoped>
.boms-page { padding: 16px; }
.hint-text { font-size: 12px; color: var(--erp-text-muted); margin-bottom: 8px; }
</style>
