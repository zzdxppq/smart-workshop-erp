<template>
  <ErpPageShell title="员工列表" description="员工档案（考勤打卡请在 Android APP）。">
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="employeeNo" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="department" label="部门" min-width="120" />
      <el-table-column label="岗位" min-width="160">
        <template #default="{ row }">
          <el-tag v-if="positionLabel(row.position)" size="small" type="info">
            {{ positionLabel(row.position) }}
          </el-tag>
          <span v-else>{{ row.position }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !items.length" description="暂无员工数据" />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useBaseStore } from '@/stores/_base'
import { parsePageItems } from '@/utils/apiPage'

const api = useBaseStore().api
const loading = ref(false)
const items = ref<Record<string, unknown>[]>([])
// V94 · EMPLOYEE_POSITION 字典（POS-* → label）
const positionMap = ref<Record<string, string>>({})

function positionLabel(code: unknown): string {
  if (typeof code !== 'string' || !code) return ''
  return positionMap.value[code] ?? ''
}

async function loadPositions() {
  try {
    const r: any = await api.get('/dicts', { params: { type: 'EMPLOYEE_POSITION' } })
    const list: any[] = Array.isArray(r) ? r : Array.isArray(r?.data) ? r.data : []
    positionMap.value = Object.fromEntries(
      list
        .filter((d: any) => d && d.dictCode)
        .map((d: any) => [d.dictCode, d.dictLabel ?? '']),
    )
  } catch {
    positionMap.value = {}
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadPositions()
    const r = await api.get('/hr/employees', { params: { pageNum: 1, pageSize: 50 } })
    items.value = parsePageItems(r).items as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    items.value = []
  } finally {
    loading.value = false
  }
})
</script>
