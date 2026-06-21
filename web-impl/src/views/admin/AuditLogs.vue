<template>
  <ErpPageShell title="审计日志" description="全系统增删改操作留痕 · 操作人 / 时间 / IP / 前后值">
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="模块">
        <el-select v-model="filters.module" clearable placeholder="全部" style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="采购" value="po" />
          <el-option label="报价" value="quote" />
          <el-option label="仓库" value="warehouse" />
          <el-option label="工作流" value="workflow" />
          <el-option label="用户" value="user" />
          <el-option label="角色" value="role" />
          <el-option label="打印" value="SYS_PRINT_LOG" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人ID">
        <el-input v-model="filters.operatorId" clearable placeholder="用户ID" style="width: 120px" />
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="module" label="模块" width="130">
        <template #default="{ row }">
          <el-tag size="small">{{ row.module ?? '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="action" label="操作" width="160">
        <template #default="{ row }">{{ row.action ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="userId" label="操作人ID" width="100" align="center" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="ts" label="时间" width="170">
        <template #default="{ row }">{{ formatTs(row.ts) }}</template>
      </el-table-column>
      <el-table-column label="变更内容" min-width="280">
        <template #default="{ row }">
          <div v-if="row.beforeJson || row.afterJson" class="audit-diff">
            <div v-if="row.beforeJson" class="diff-row diff-before">
              <span class="diff-label">前</span>
              <span class="diff-val">{{ truncate(row.beforeJson) }}</span>
            </div>
            <div v-if="row.afterJson" class="diff-row diff-after">
              <span class="diff-label">后</span>
              <span class="diff-val">{{ truncate(row.afterJson) }}</span>
            </div>
          </div>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="erp-pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const api = useBaseStore().api
const loading = ref(false)
const items = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ module: '', operatorId: '' })

async function load() {
  loading.value = true
  try {
    const params: any = { pageNum: page.value, pageSize: size.value }
    if (filters.module) params.bizType = filters.module
    if (filters.operatorId) params.operatorId = filters.operatorId
    const data = unwrapResult<any>(await api.get('/audit/logs', { params }))
    items.value = data?.records ?? data?.list ?? []
    total.value = data?.total ?? items.value.length
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function formatTs(ts: string | null): string {
  if (!ts) return '—'
  try {
    return ts.replace('T', ' ').substring(0, 19)
  } catch {
    return ts
  }
}

function truncate(val: string | null): string {
  if (!val) return '—'
  return val.length > 120 ? val.substring(0, 120) + '…' : val
}

onMounted(load)
</script>

<style scoped>
.audit-diff {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.diff-row {
  display: flex;
  gap: 6px;
  font-size: 12px;
  line-height: 1.4;
  font-family: 'Courier New', monospace;
}
.diff-label {
  flex-shrink: 0;
  font-weight: 600;
  width: 18px;
}
.diff-before {
  color: #f56c6c;
}
.diff-after {
  color: #67c23a;
}
.diff-val {
  word-break: break-all;
}
.muted {
  color: var(--erp-text-secondary, #9ca3af);
}
</style>
