<template>
  <div>
    <div class="page-header">
      <h2>工艺库</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreate = true">新建工艺</el-button>
        <el-button type="primary" @click="openProductRoute">产品工艺路线编辑 →</el-button>
      </div>
    </div>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="工艺名称">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="code" label="工艺编码" min-width="140" />
      <el-table-column prop="name" label="工艺名称" min-width="160" />
      <el-table-column prop="standardTime" label="总工时(h)" width="100" />
      <el-table-column prop="equipmentType" label="类型" min-width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row as ProcessRowView)">编辑</el-button>
          <el-button size="small" link type="primary" @click="viewRoute(row as ProcessRowView)">工序</el-button>
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
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />

    <el-dialog v-model="showEdit" title="编辑工艺" width="560px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="工艺编码">
          <el-input v-model="editForm.processCode" disabled />
        </el-form-item>
        <el-form-item label="工艺名称" required>
          <el-input v-model="editForm.processName" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editForm.processType" placeholder="全部" style="width: 100%">
            <el-option label="标准 STANDARD" value="STANDARD" />
            <el-option label="首件 FA" value="FA" />
            <el-option label="试制 PROTOTYPE" value="PROTOTYPE" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.comment" />
        </el-form-item>
        <el-form-item label="可复用">
          <el-switch v-model="editForm.isReusable" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreate" title="新建工艺" width="560px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="工艺名称" required>
          <el-input v-model="createForm.processName" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="createForm.processType" placeholder="全部" style="width: 100%">
            <el-option label="标准 STANDARD" value="STANDARD" />
            <el-option label="首件 FA" value="FA" />
            <el-option label="试制 PROTOTYPE" value="PROTOTYPE" />
          </el-select>
        </el-form-item>
        <el-form-item label="工序名称" required>
          <el-input v-model="createForm.stepName" placeholder="如 CNC 粗加工" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="createForm.machineType" placeholder="请选择设备类型" style="width: 100%">
            <el-option v-for="t in machineTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="工时(h)">
          <el-input-number v-model="createForm.estimatedHours" :min="0.1" :step="0.5" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import { normalizeProcessList, type ProcessRowView } from '@/utils/productionApi'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'

const api = useBaseStore().api
const router = useRouter()
const keyword = ref('')
const showCreate = ref(false)
const showEdit = ref(false)
const creating = ref(false)
const editing = ref(false)
const editForm = ref({
  id: 0,
  processCode: '',
  processName: '',
  processType: 'STANDARD',
  description: '',
  comment: '',
  isReusable: true,
  isActive: 1,
})
const createForm = ref({
  processName: '',
  processType: 'STANDARD',
  stepName: 'CNC 粗加工',
  machineType: '',
  estimatedHours: 1.5,
})
const machineTypes = ref<string[]>([])

async function loadMachineTypes() {
  try {
    const r = await api.get('/machines/types')
    const data = unwrapResult(r) as string[]
    if (Array.isArray(data)) {
      machineTypes.value = data
    }
  } catch { /* 不影响主流程 */ }
}

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<ProcessRowView>(async (params) => {
  const r = await api.get('/processes', {
    params: { keyword: keyword.value || undefined, ...params },
  })
  const { items: list, total: t } = parsePageItems(r)
  const filtered = keyword.value
    ? (list as Record<string, unknown>[]).filter((row) => {
        const kw = keyword.value.toLowerCase()
        return String(row.processName ?? row.name ?? '').toLowerCase().includes(kw)
          || String(row.processCode ?? row.code ?? '').toLowerCase().includes(kw)
      })
    : list
  return { code: 0, data: { list: normalizeProcessList(filtered as unknown[]), total: keyword.value ? filtered.length : t } }
})

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

async function submitCreate() {
  if (!createForm.value.processName || !createForm.value.stepName) {
    ElMessage.warning('请填写工艺名称和工序名称')
    return
  }
  creating.value = true
  try {
    unwrapResult(await api.post('/processes', {
      processName: createForm.value.processName,
      processType: createForm.value.processType,
      steps: [{
        stepNo: 1,
        stepName: createForm.value.stepName,
        segment: '粗加工',
        machineType: createForm.value.machineType,
        estimatedHours: createForm.value.estimatedHours,
      }],
    }))
    ElMessage.success('工艺已创建')
    showCreate.value = false
    createForm.value.processName = ''
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function openEdit(row: ProcessRowView) {
  if (!row.id) {
    ElMessage.warning('缺少工艺 ID')
    return
  }
  editing.value = true
  try {
    const data = unwrapResult<Record<string, unknown>>(await api.get(`/processes/${row.id}`))
    editForm.value = {
      id: row.id,
      processCode: String(data.processCode ?? row.code ?? ''),
      processName: String(data.processName ?? row.name ?? ''),
      processType: String(data.processType ?? row.processType ?? 'STANDARD'),
      description: String(data.description ?? ''),
      comment: String(data.comment ?? ''),
      isReusable: data.isReusable === 1 || data.isReusable === true,
      isActive: Number(data.isActive ?? 1),
    }
    showEdit.value = true
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载工艺失败')
  } finally {
    editing.value = false
  }
}

async function submitEdit() {
  if (!editForm.value.processName) {
    ElMessage.warning('请填写工艺名称')
    return
  }
  editing.value = true
  try {
    unwrapResult(await api.put(`/processes/${editForm.value.id}`, {
      processName: editForm.value.processName,
      processType: editForm.value.processType,
      description: editForm.value.description || undefined,
      comment: editForm.value.comment || undefined,
      isReusable: editForm.value.isReusable,
      isActive: editForm.value.isActive,
    }))
    ElMessage.success('工艺已更新')
    showEdit.value = false
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

async function openProductRoute() {
  try {
    const r = unwrapResult<{ list?: { id?: number; materialCode?: string; materialName?: string }[] }>(
      await api.get('/materials', { params: { categoryPrefix: 'CP', size: 50 } }),
    )
    const products = (r.list ?? []).filter((m) => String(m.materialCode ?? '').startsWith('CP-'))
    if (products.length === 1 && products[0].id) {
      router.push(`/material/product-route/${products[0].id}`)
      return
    }
    router.push('/material/product-route')
  } catch {
    router.push('/material/product-route')
  }
}

async function viewRoute(row: ProcessRowView & { id?: number }) {
  if (!row.id) {
    ElMessage.warning('缺少工艺 ID')
    return
  }
  try {
    const data = unwrapResult<{ steps?: unknown[] }>(await api.get(`/processes/${row.id}/route`))
    const count = Array.isArray(data?.steps) ? data.steps.length : 0
    ElMessage.info(`工艺 ${row.code || row.name} 共 ${count} 道工序`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载工序失败')
  }
}

onMounted(() => { onSearch(); loadMachineTypes() })
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
