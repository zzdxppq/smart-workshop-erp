<!--
  V1.3.9 Sprint 12 Story 12.4 · AC-12.4.4
  /admin/print-logs · 打印管理页
  - 历史查询（分页 + 多维过滤）
  - 选记录补打
  - 红色"补打" tag · FAILED 不可补打
-->
<template>
  <div class="print-log-page">
    <h2 style="margin-bottom: 12px">打印管理</h2>
    <el-alert
      type="info"
      :closable="false"
      title="支持 ZPL/TSPL 直连打印与 A4 PDF 打印，可对失败记录发起补打"
      style="margin-bottom: 12px"
    />

    <!-- 工具栏：多维过滤 -->
    <el-row :gutter="12" align="middle" style="margin-bottom: 12px">
      <el-col :span="3">
        <el-select v-model="filter.codeType" placeholder="码类型" clearable @change="reload">
          <el-option label="GD 工单" value="GD" />
          <el-option label="LZ 流转" value="LZ" />
          <el-option label="SB 设备" value="SB" />
          <el-option label="WW 委外" value="WW" />
          <el-option label="WL 物料" value="WL" />
          <el-option label="DRAWING 图纸" value="DRAWING" />
        </el-select>
      </el-col>
      <el-col :span="3">
        <el-select v-model="filter.mode" placeholder="模式" clearable @change="reload">
          <el-option label="ZPL_DIRECT" value="ZPL_DIRECT" />
          <el-option label="PDF_BROWSER" value="PDF_BROWSER" />
        </el-select>
      </el-col>
      <el-col :span="3">
        <el-select v-model="filter.status" placeholder="状态" clearable @change="reload">
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="FAILED" value="FAILED" />
          <el-option label="PENDING" value="PENDING" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-input v-model="filter.codeValue" placeholder="搜索业务编码" clearable @keyup.enter="reload" />
      </el-col>
      <el-col :span="3">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          @change="reload"
        />
      </el-col>
      <el-col :span="8" style="text-align: right">
        <el-button type="primary" @click="reload">刷新</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table :data="rows" stripe border v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="logNo" label="日志号" width="160" />
      <el-table-column label="打印时间" width="160">
        <template #default="{ row }">
          {{ row.printedAt ? formatTime(row.printedAt) : '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" width="100" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.codeType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="codeValue" label="业务编码" width="160" />
      <el-table-column prop="copies" label="份数" width="60" />
      <el-table-column label="打印机" width="200">
        <template #default="{ row }">
          <span v-if="row.printMode === 'PDF_BROWSER'">普通浏览器</span>
          <span v-else>{{ row.printerNameSnapshot }} ({{ row.printerIpSnapshot }})</span>
        </template>
      </el-table-column>
      <el-table-column label="模式" width="120">
        <template #default="{ row }">
          <el-tag :type="row.printMode === 'ZPL_DIRECT' ? 'warning' : 'info'" size="small">
            {{ row.printMode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="补打标记" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.referenceLogId" type="danger" size="small">
            补打 #{{ row.referenceLogId }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            :disabled="row.status !== 'SUCCESS' || !!row.referenceLogId"
            :title="replayTooltip(row)"
            @click="onReplay(row)"
            :data-testid="`replay-btn-${row.id}`"
          >
            补打
          </el-button>
          <el-button size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="filter.page"
      v-model:page-size="filter.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 12px"
      @size-change="reload"
      @current-change="reload"
    />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="`打印日志详情 #${currentLog?.id}`" size="500">
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="日志号">{{ currentLog.logNo }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.operatorName }} (#{{ currentLog.operatorUserId }})</el-descriptions-item>
        <el-descriptions-item label="打印时间">{{ formatTime(currentLog.printedAt) }}</el-descriptions-item>
        <el-descriptions-item label="码类型">{{ currentLog.codeType }}</el-descriptions-item>
        <el-descriptions-item label="业务编码">{{ currentLog.codeValue }}</el-descriptions-item>
        <el-descriptions-item label="份数">{{ currentLog.copies }}</el-descriptions-item>
        <el-descriptions-item label="打印机">{{ currentLog.printerNameSnapshot }} ({{ currentLog.printerIpSnapshot }})</el-descriptions-item>
        <el-descriptions-item label="模式">{{ currentLog.printMode }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="currentLog.status" /></el-descriptions-item>
        <el-descriptions-item v-if="currentLog.errorMsg" label="错误信息">{{ currentLog.errorMsg }}</el-descriptions-item>
        <el-descriptions-item v-if="currentLog.referenceLogId" label="补打自">#{{ currentLog.referenceLogId }}</el-descriptions-item>
        <el-descriptions-item v-if="currentLog.remark" label="备注">{{ currentLog.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/utils/http'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

/**
 * PrintLog 打印管理页 · V1.3.9 Sprint 12 Story 12.4
 *
 * <p>AC-12.4.4 打印历史查询与补打
 * <p>权限：admin（看 / 补打）· 操作员（看自己的）
 */
// Vue 3 + Vite 项目未启用 Nuxt 编译宏 · 标题以 router meta.title 为准
// definePageMeta({ title: '打印管理' })

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const currentLog = ref<any | null>(null)

const filter = reactive({
  codeType: '',
  mode: '',
  status: '',
  codeValue: '',
  page: 1,
  size: 20,
  tenantId: 1,
})

function formatTime(t: string): string {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}

function replayTooltip(row: any): string {
  if (row.status !== 'SUCCESS') return 'FAILED 状态不可补打 · 请重新创建打印任务'
  if (row.referenceLogId) return '补打记录不可再次补打（防递归）'
  return '点击重新打印'
}

function resetFilter() {
  filter.codeType = ''
  filter.mode = ''
  filter.status = ''
  filter.codeValue = ''
  dateRange.value = null
  filter.page = 1
  reload()
}

async function reload() {
  loading.value = true
  try {
    const params: any = {
      page: filter.page,
      size: filter.size,
      tenantId: filter.tenantId,
    }
    if (filter.codeType) params.codeType = filter.codeType
    if (filter.mode) params.mode = filter.mode
    if (filter.status) params.status = filter.status
    if (filter.codeValue) params.codeValue = filter.codeValue
    if (dateRange.value) {
      params.dateFrom = dateRange.value[0]
      params.dateTo = dateRange.value[1]
    }
    const pageData = parsePageItems(await http.get('/print/logs', { params }))
    rows.value = pageData.items
    total.value = pageData.total
  } catch (e: any) {
    ElMessage.error(`查询失败: ${e?.message || '未知错误'}`)
  } finally {
    loading.value = false
  }
}

async function openDetail(row: any) {
  try {
    currentLog.value = unwrapResult(await http.get(`/print/logs/${row.id}`))
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(`查询详情失败: ${e?.message || '未知错误'}`)
  }
}

async function onReplay(row: any) {
  try {
    const { value: targetMode } = await ElMessageBox({
      title: '选择补打模式',
      message: '请选择补打目标模式',
      showInput: true,
      inputValue: 'SAME',
      inputPlaceholder: 'SAME / ZPL_DIRECT / PDF_BROWSER',
      showCancelButton: true,
    })
    const replay = unwrapResult<{ printLogId?: number }>(
      await http.post(`/print/logs/${row.id}/replay`, { targetMode: targetMode || 'SAME' }),
    )
    ElMessage.success(`补打成功 · 新日志 #${replay.printLogId}`)
    reload()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(`补打失败: ${e?.message || '未知错误'}`)
  }
}

onMounted(() => {
  reload()
})
</script>

<style scoped>
.print-log-page {
  padding: 12px;
}
</style>
