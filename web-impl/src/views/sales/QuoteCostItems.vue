<template>
  <ErpPageShell title="报价范本管理" description="维护各类成本项的计费基础和利润率，报价时自动套用">
    <div style="margin-bottom: 12px">
      <el-button type="primary" @click="openEdit()">+ 新增成本项</el-button>
    </div>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="itemName" label="成本项" min-width="120" />
      <el-table-column label="计费方式" min-width="120">
        <template #default="{ row }">{{ billingLabel(row.billingMethod) }}</template>
      </el-table-column>
      <el-table-column label="单价(元)" width="110" align="right">
        <template #default="{ row }">{{ row.unitPrice ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="利润率" width="90" align="right">
        <template #default="{ row }">{{ formatPercent(row.profitMargin) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'" size="small">{{ row.isActive ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogOpen" :title="editForm.id ? '编辑成本项' : '新增成本项'" width="480px">
      <el-form label-width="96px">
        <el-form-item label="编码" required>
          <el-input v-model="editForm.itemCode" :disabled="!!editForm.id" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="editForm.itemName" />
        </el-form-item>
        <el-form-item label="计费方式">
          <el-select v-model="editForm.billingMethod" style="width: 100%">
            <el-option label="按重量(kg)" value="BY_WEIGHT" />
            <el-option label="按工时(h)" value="BY_HOUR" />
            <el-option label="按面积(㎡)" value="BY_AREA" />
            <el-option label="按百分比" value="BY_PERCENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="editForm.unit" placeholder="kg / h / ㎡ / %" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="editForm.unitPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="利润率">
          <el-input-number v-model="editForm.profitMargin" :min="0" :max="1" :step="0.01" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工序编码">
          <el-input v-model="editForm.processCode" placeholder="工时类关联 LATHE/CNC…" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useQuoteStore } from '@/stores/quote'

type CostItem = {
  id?: number
  itemCode?: string
  itemName?: string
  billingMethod?: string
  unit?: string
  unitPrice?: number
  profitMargin?: number
  processCode?: string
  isActive?: number
}

const quoteStore = useQuoteStore()
const loading = ref(false)
const saving = ref(false)
const items = ref<CostItem[]>([])
const dialogOpen = ref(false)
const editForm = ref<CostItem>({ isActive: 1, profitMargin: 0.15, billingMethod: 'BY_HOUR' })

function billingLabel(m?: string) {
  const map: Record<string, string> = {
    BY_WEIGHT: '按重量(kg)',
    BY_HOUR: '按工时(h)',
    BY_AREA: '按面积(㎡)',
    BY_PERCENT: '按百分比',
  }
  return map[m ?? ''] ?? m ?? '—'
}

function formatPercent(v?: number) {
  if (v == null) return '—'
  return `${(Number(v) * 100).toFixed(1)}%`
}

async function reload() {
  loading.value = true
  try {
    items.value = await quoteStore.listCostItems()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openEdit(row?: CostItem) {
  editForm.value = row
    ? { ...row, profitMargin: Number(row.profitMargin ?? 0), unitPrice: Number(row.unitPrice ?? 0) }
    : { isActive: 1, profitMargin: 0.15, billingMethod: 'BY_HOUR' }
  dialogOpen.value = true
}

async function save() {
  saving.value = true
  try {
    await quoteStore.saveCostItem(editForm.value as Record<string, unknown>)
    ElMessage.success('已保存')
    dialogOpen.value = false
    await reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: CostItem) {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除「${row.itemName}」？`, '确认')
  try {
    await quoteStore.deleteCostItem(row.id)
    ElMessage.success('已删除')
    await reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '删除失败')
  }
}

onMounted(reload)
</script>
