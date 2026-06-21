<template>
  <div>
    <el-row :gutter="12" style="margin-bottom: 12px">
      <el-col :span="16">
        <el-input v-model="keyword" clearable placeholder="按物料编码筛选" @keyup.enter="reload" />
      </el-col>
      <el-col :span="8" style="text-align: right">
        <el-button type="primary" @click="$router.push('/material/barcode-list')">查看全部条码</el-button>
        <el-button @click="$router.push('/material/barcode-print')">批量打印</el-button>
      </el-col>
    </el-row>
    <el-table v-loading="loading" :data="items" stripe border size="small">
      <el-table-column prop="barcodeNo" label="条码号" min-width="150" />
      <el-table-column label="料号" min-width="120">
        <template #default="{ row }">
          <el-button v-if="row.materialCode" link type="primary" size="small" @click="lookupMaterial(row.materialCode)">
            {{ row.materialCode }}
          </el-button>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column prop="spec" label="规格" min-width="100" />
      <el-table-column prop="qty" label="数量" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" :label="barcodeStatusLabel(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="goDetail(row.barcodeNo)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 8px"
        @current-change="() => reload()"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useMaterialStore } from '@/stores/material'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import { unwrapResult } from '@/utils/apiPage'
import { bizStatusLabel, BARCODE_STATUS } from '@/utils/statusLabels'

function barcodeStatusLabel(s?: string) {
  return bizStatusLabel(s, BARCODE_STATUS)
}

const router = useRouter()
const materialStore = useMaterialStore()
const api = useBaseStore().api
const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  materialStore.listBarcodes({ ...params, materialCode: keyword.value || undefined }),
)

function goDetail(barcodeNo: string) {
  router.push(`/material/barcode-detail/${encodeURIComponent(barcodeNo)}`)
}

async function lookupMaterial(code: string) {
  try {
    const data = unwrapResult<{ id?: number }>(await api.get('/materials/lookup', { params: { code } }))
    if (data?.id) {
      router.push({ name: 'MaterialDetail', params: { id: String(data.id) } })
    } else {
      ElMessage.warning('未找到料号主数据')
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '料号查询失败')
  }
}

onMounted(reload)
</script>
