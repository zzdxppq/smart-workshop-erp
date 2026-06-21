<template>
  <div class="signed-scans-page">
    <h2>签字扫描件财务档案</h2>
    <el-alert type="info" :closable="false" title="对账厂商签字扫描件 · AES-256 加密存储 · 5 年保留 · 下载写入 sys_download_log" />

    <el-form :inline="true" style="margin-top: 16px">
      <el-form-item label="关键词">
        <el-input v-model="keyword" placeholder="对账单号 / 厂商 / 签字人" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="reconcileNo" label="对账单号" min-width="140" />
      <el-table-column prop="vendorName" label="厂商" min-width="120" />
      <el-table-column label="账期" width="100">
        <template #default="{ row }">{{ row.periodYear }}-{{ String(row.periodMonth).padStart(2, '0') }}</template>
      </el-table-column>
      <el-table-column prop="signerName" label="签字人" width="120" />
      <el-table-column prop="signedAt" label="签字时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" :loading="downloadingId === (row as SignedScanRow).id" @click="download(row as SignedScanRow)">
            解密下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      @current-change="reload"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useFinanceStore } from '@/stores/finance'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { resolveGatewayUrl } from '@/utils/serviceRoute'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'

interface SignedScanRow {
  id: number
  reconcileId: number
  reconcileNo: string
  vendorName: string
  periodYear: number
  periodMonth: number
  signerName: string
  signedAt: string
}

const financeStore = useFinanceStore()
const keyword = ref('')
const items = ref<SignedScanRow[]>([])
const loading = ref(false)
const downloadingId = ref<number | null>(null)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function reload() {
  loading.value = true
  try {
    const r = await financeStore.listSignedScans({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    const page = unwrapResult<Record<string, unknown>>(r)
    const parsed = parsePageItems(page)
    items.value = parsed.items as SignedScanRow[]
    total.value = parsed.total
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  reload()
}

async function download(row: SignedScanRow) {
  downloadingId.value = row.id
  try {
    const token = localStorage.getItem('token')
    const url = resolveGatewayUrl(`/finance/signed-scans/${row.id}/download`)
    const resp = await axios.get(url, {
      responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    const blob = resp.data as Blob
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${row.reconcileNo}_${row.signerName}.png`
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('下载完成（已审计）')
  } catch {
    ElMessage.error('下载失败')
  } finally {
    downloadingId.value = null
  }
}

onMounted(reload)
</script>

<style scoped>
.signed-scans-page { padding: 16px; }
</style>
