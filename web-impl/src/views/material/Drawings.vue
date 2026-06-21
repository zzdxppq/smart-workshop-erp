<template>
  <ErpPageShell title="图纸档案" description="FR-3-1 图纸档案 · FR-3-2 工程转化（条码仅在仓储入库时生成）">
    <el-card>
      <el-tabs v-model="activeTab">
        <!-- FR-3-1 图纸列表 -->
        <el-tab-pane label="图纸列表" name="drawings">
          <el-form :inline="true" style="margin-bottom: 12px">
            <el-form-item label="图号">
              <DrawingPicker v-model="drawingKeyword" style="width: 280px" @select="onDrawingPick" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="loading" :data="items" stripe border>
            <el-table-column prop="drawingNo" label="图号" min-width="130">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPreview(row)">{{ row.drawingNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="version" label="版本" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="openVersions(row)">{{ row.version }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="materialCode" label="料号" min-width="120">
              <template #default="{ row }">
                <el-button v-if="row.materialCode" link type="primary" @click="goMaterialDetail(row.materialCode)">
                  {{ row.materialCode }}
                </el-button>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="140" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <ErpStatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openPreview(row)">预览</el-button>
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
        </el-tab-pane>

        <!-- 工程数据页嵌入时隐藏条码 Tab -->
        <el-tab-pane v-if="canViewBarcodes && !engineeringContext" label="条码（只读）" name="barcodes">
          <el-alert type="warning" :closable="false" show-icon title="条码仅在「仓储 → 入库单 → 执行入库」时生成，格式：{料号}-BATCH-{日期}-{流水}" />
          <DrawingBarcodesPanel />
        </el-tab-pane>

        <!-- FR-3-2 工程转化 -->
        <el-tab-pane label="工程转化" name="conversion">
          <DrawingConversionWizard />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- FR-3-1-3 PDF 在线预览 -->
    <el-dialog v-model="previewVisible" :title="`图纸预览 · ${previewRow?.drawingNo ?? ''}`" width="90%" top="4vh" destroy-on-close>
      <DrawingViewer v-if="previewRow?.id" :drawing-id="previewRow.id" auto-preview />
      <DrawingCadAttachments v-if="previewRow?.id" :drawing-id="previewRow.id" />
    </el-dialog>

    <!-- FR-3-1-2 版本历史 -->
    <el-drawer v-model="versionVisible" :title="`版本历史 · ${versionRow?.drawingNo ?? ''}`" size="480px">
      <el-table v-loading="versionLoading" :data="versions" stripe border size="small">
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="changeReason" label="变更说明" min-width="160" />
        <el-table-column prop="changedAt" label="变更时间" min-width="150" />
      </el-table>
      <p class="muted">旧版本可查不可改（FR-3-1-2）</p>
    </el-drawer>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import DrawingViewer from '@/views/crm/drawing/DrawingViewer.vue'
import DrawingConversionWizard from '@/components/material/DrawingConversionWizard.vue'
import DrawingBarcodesPanel from '@/components/material/DrawingBarcodesPanel.vue'
import DrawingCadAttachments from '@/components/material/DrawingCadAttachments.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useMaterialStore } from '@/stores/material'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import { unwrapResult } from '@/utils/apiPage'
import { hasAnyRole } from '@/utils/roleAccess'
import { useAuthStore } from '@/stores/auth'

const props = withDefaults(defineProps<{ engineeringContext?: boolean }>(), {
  engineeringContext: false,
})

interface DrawingRow {
  id?: number
  drawingNo?: string
  version?: string
  materialCode?: string
  title?: string
  status?: string
}

interface VersionRow {
  version?: string
  changeReason?: string
  changedAt?: string
}

const route = useRoute()
const router = useRouter()
const api = useBaseStore().api
const auth = useAuthStore()
const activeTab = ref('drawings')
const materialStore = useMaterialStore()
const drawingKeyword = ref('')

// 仅仓管可见条码 Tab（工程师不应看到仓管作业面板）
const canViewBarcodes = computed(() =>
  hasAnyRole(auth.userRoles, ['WAREHOUSE', 'WAREHOUSE_LEAD', 'ADMIN', 'SYS_ADMIN', 'GM']),
)

const previewVisible = ref(false)
const previewRow = ref<DrawingRow | null>(null)
const versionVisible = ref(false)
const versionRow = ref<DrawingRow | null>(null)
const versionLoading = ref(false)
const versions = ref<VersionRow[]>([])

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<DrawingRow>((params) =>
  materialStore.listDrawings({ ...params, keyword: drawingKeyword.value || undefined }),
)

function onDrawingPick(d: { drawingNo?: string }) {
  drawingKeyword.value = d.drawingNo ?? ''
  onSearch()
}

function applyRouteQuery() {
  const tab = route.query.tab as string | undefined
  if (tab === 'conversion' || tab === 'barcodes' || tab === 'drawings') activeTab.value = tab
  const dn = route.query.drawingNo as string | undefined
  if (dn) {
    drawingKeyword.value = dn
    onSearch()
  }
}

watch(() => route.query, applyRouteQuery)

function onSearch() {
  pageNum.value = 1
  reload()
}
function onPageChange() {
  reload()
}

function openPreview(row: DrawingRow) {
  if (!row.id) return
  previewRow.value = row
  previewVisible.value = true
}

async function openVersions(row: DrawingRow) {
  if (!row.id) return
  versionRow.value = row
  versionVisible.value = true
  versionLoading.value = true
  try {
    const list = unwrapResult<VersionRow[]>(await materialStore.listDrawingVersions(row.id))
    versions.value = Array.isArray(list) ? list : []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载版本失败')
    versions.value = []
  } finally {
    versionLoading.value = false
  }
}

async function goMaterialDetail(materialCode: string) {
  try {
    const data = unwrapResult<{ id?: number }>(await api.get('/materials/lookup', { params: { code: materialCode } }))
    if (!data?.id) {
      ElMessage.warning('物料主数据不存在，请先执行工程转化或联系工程师建档')
      return
    }
    router.push({ name: 'MaterialDetail', params: { id: String(data.id) } })
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '料号查询失败')
  }
}

onMounted(() => {
  applyRouteQuery()
  onSearch()
})
</script>

<style scoped>
.muted { margin-top: 12px; font-size: 12px; color: var(--erp-text-muted); }
</style>
