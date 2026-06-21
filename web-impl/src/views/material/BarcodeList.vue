<template>
  <ErpPageShell title="物料条码列表" description="管理物料条码，支持按料号查询并跳转料号详情。">
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="料号">
        <el-input v-model="materialCode" clearable placeholder="料号" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
        <el-button @click="$router.push('/material/lookup')">料号详情查询</el-button>
      </el-form-item>
    </el-form>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="items" stripe border>
        <el-table-column prop="barcodeNo" label="条码号" min-width="200" />
        <el-table-column label="料号" min-width="160">
          <template #default="{ row }">
            <el-button v-if="row.materialId" link type="primary" @click="goMaterialDetail(row.materialId)">
              {{ row.materialCode }}
            </el-button>
            <el-button v-else link type="primary" @click="lookupAndGo(row.materialCode)">
              {{ row.materialCode }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格" min-width="100" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <ErpStatusTag :status="row.status" :label="barcodeStatusLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="generatedAt" label="生成时间" min-width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goMaterialDetail(row)">料号详情</el-button>
            <el-button size="small" @click="goBarcodeDetail(row)">条码详情</el-button>
            <el-button size="small" type="danger" @click="regenerate(row)">重新生成</el-button>
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
    </el-card>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useMaterialStore } from '@/stores/material'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import { unwrapResult } from '@/utils/apiPage'

const router = useRouter()
const materialStore = useMaterialStore()
const api = useBaseStore().api
import { bizStatusLabel, BARCODE_STATUS } from '@/utils/statusLabels'

function barcodeStatusLabel(s?: string) {
  return bizStatusLabel(s, BARCODE_STATUS)
}


const materialCode = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  materialStore.listBarcodes({ materialCode: materialCode.value || undefined, ...params }),
)

function onSearch() {
  pageNum.value = 1
  reload({ materialCode: materialCode.value || undefined })
}
function onPageChange() {
  reload({ materialCode: materialCode.value || undefined })
}

function goMaterialDetail(row: { materialId?: number; materialCode?: string } | number) {
  const id = typeof row === 'number' ? row : row.materialId
  if (id) {
    router.push(`/material/detail/${id}`)
    return
  }
  lookupAndGo(typeof row === 'number' ? '' : row.materialCode)
}

async function lookupAndGo(code?: string) {
  if (!code) {
    ElMessage.warning('缺少料号')
    return
  }
  try {
    const data = unwrapResult<{ id?: number }>(await api.get('/materials/lookup', { params: { code } }))
    if (data?.id) router.push(`/material/detail/${data.id}`)
    else ElMessage.warning('未找到料号')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '查询料号失败')
  }
}

function goBarcodeDetail(row: { barcodeNo?: string }) {
  if (row.barcodeNo) router.push(`/material/barcode-detail/${encodeURIComponent(row.barcodeNo)}`)
}

async function regenerate(row: { barcodeNo?: string }) {
  try {
    await materialStore.regenerateBarcode(row.barcodeNo!)
    ElMessage.success('已重新生成')
    onPageChange()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '重新生成失败')
  }
}

onMounted(onSearch)
</script>
