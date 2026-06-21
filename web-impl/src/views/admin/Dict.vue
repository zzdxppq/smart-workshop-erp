<template>
  <div class="dict-page">
    <!-- 左侧：分类列表 -->
    <div class="dict-types-panel">
      <div class="panel-header">
        <span>字典分类</span>
        <el-button type="primary" size="small" @click="onAddType">新增分类</el-button>
      </div>
      <el-scrollbar class="type-scroll">
        <div
          v-for="t in types"
          :key="t.typeCode"
          class="type-item"
          :class="{ active: selectedType === t.typeCode }"
          @click="selectType(t.typeCode)"
        >
          <div class="type-main">
            <span class="type-name">{{ t.typeName }}</span>
            <el-tag v-if="t.isBuiltin" size="small" type="info">内置</el-tag>
          </div>
          <div class="type-desc">{{ t.description || '—' }}</div>
          <div class="type-code">{{ t.typeCode }}</div>
        </div>
        <el-empty v-if="types.length === 0 && !loadingTypes" description="暂无分类" />
      </el-scrollbar>
    </div>

    <!-- 右侧：字典项 -->
    <div class="dict-items-panel">
      <div class="panel-header">
        <span>{{ selectedTypeName || '请选择分类' }}</span>
        <el-button
          v-if="selectedType"
          type="primary"
          size="small"
          :disabled="isBuiltinType"
          @click="onAddItem"
        >新增项</el-button>
      </div>

      <!-- 分类操作栏（内置分类不可增删分类） -->
      <div v-if="selectedType && !isBuiltinType" class="type-actions">
        <el-button size="small" @click="onEditType">编辑分类</el-button>
        <el-button size="small" type="danger" @click="onDeleteType">删除分类</el-button>
      </div>

      <el-table v-loading="loadingItems" :data="items" stripe border style="flex:1">
        <el-table-column prop="dictCode" label="键编码" min-width="160" />
        <el-table-column prop="dictLabel" label="显示名称" min-width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :disabled="isBuiltinType" @click="onEditItem(row as Dict)">编辑</el-button>
            <el-button
              v-if="!isBuiltinType"
              size="small"
              type="danger"
              @click="onDeleteItem(row as Dict)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!selectedType" description="请从左侧选择一个分类" />
    </div>

    <!-- 新增/编辑分类弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeForm.id ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="typeForm" label-width="100px">
        <el-form-item label="分类编码" required>
          <el-input v-model="typeForm.typeCode" :disabled="!!typeForm.id" placeholder="如 MACHINE_TYPE" />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="typeForm.typeName" placeholder="如 设备类型" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" :rows="2" placeholder="分类用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeDialogLoading" @click="onSaveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑字典项弹窗 -->
    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑字典项' : '新增字典项'" width="480px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item label="键编码" required>
          <el-input v-model="itemForm.dictCode" :disabled="!!itemForm.id" placeholder="如 CNC_MILL" />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="itemForm.dictLabel" placeholder="如 CNC铣床" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemDialogLoading" @click="onSaveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useBaseStore } from '@/stores/_base'
import { ElMessage, ElMessageBox } from 'element-plus'

const api = useBaseStore().api

// ----- 分类 -----
interface DictType {
  id: number
  typeCode: string
  typeName: string
  description: string
  isBuiltin: number
  status: string
}
interface Dict {
  id: number
  dictType: string
  dictCode: string
  dictLabel: string
  sort: number
  status: string
}

const types = ref<DictType[]>([])
const loadingTypes = ref(false)
const selectedType = ref<string | null>(null)
const selectedTypeName = ref('')
const typeDialogVisible = ref(false)
const typeDialogLoading = ref(false)
const typeForm = ref({ id: 0, typeCode: '', typeName: '', description: '' })

const isBuiltinType = computed(() => {
  const t = types.value.find(x => x.typeCode === selectedType.value)
  return t?.isBuiltin === 1
})

async function loadTypes() {
  loadingTypes.value = true
  try {
    const r = await api.get('/dicts/types')
    types.value = ((r as any)?.data ?? []) as DictType[]
  } catch {
    ElMessage.error('加载分类失败')
  } finally {
    loadingTypes.value = false
  }
}

function selectType(code: string) {
  selectedType.value = code
  const t = types.value.find(x => x.typeCode === code)
  selectedTypeName.value = t?.typeName ?? code
  loadItems()
}

function onAddType() {
  typeForm.value = { id: 0, typeCode: '', typeName: '', description: '' }
  typeDialogVisible.value = true
}

function onEditType() {
  const t = types.value.find(x => x.typeCode === selectedType.value)
  if (!t) return
  typeForm.value = { id: t.id, typeCode: t.typeCode, typeName: t.typeName, description: t.description || '' }
  typeDialogVisible.value = true
}

async function onSaveType() {
  if (!typeForm.value.typeCode || !typeForm.value.typeName) {
    ElMessage.warning('编码和名称必填')
    return
  }
  typeDialogLoading.value = true
  try {
    if (typeForm.value.id) {
      await api.put(`/dict-types/${typeForm.value.id}`, {
        typeName: typeForm.value.typeName,
        description: typeForm.value.description,
      })
    } else {
      await api.post('/dict-types', typeForm.value)
    }
    typeDialogVisible.value = false
    await loadTypes()
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    typeDialogLoading.value = false
  }
}

async function onDeleteType() {
  if (!selectedType.value) return
  try {
    await ElMessageBox.confirm('删除分类将同时删除该分类下所有字典项，确认删除？', '警告', { type: 'warning' })
    await api.delete(`/dict-types/${selectedType.value}`)
    selectedType.value = null
    selectedTypeName.value = ''
    items.value = []
    await loadTypes()
    ElMessage.success('已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

// ----- 字典项 -----
const items = ref<Dict[]>([])
const loadingItems = ref(false)
const itemDialogVisible = ref(false)
const itemDialogLoading = ref(false)
const itemForm = ref({ id: 0, dictCode: '', dictLabel: '', sort: 0, status: 'ACTIVE' })

async function loadItems() {
  if (!selectedType.value) return
  loadingItems.value = true
  try {
    const r = await api.get('/dicts', { params: { type: selectedType.value } })
    items.value = ((r as any)?.data ?? []) as Dict[]
  } catch {
    ElMessage.error('加载字典项失败')
  } finally {
    loadingItems.value = false
  }
}

function onAddItem() {
  itemForm.value = { id: 0, dictCode: '', dictLabel: '', sort: 0, status: 'ACTIVE' }
  itemDialogVisible.value = true
}

function onEditItem(row: Dict) {
  itemForm.value = { id: row.id, dictCode: row.dictCode, dictLabel: row.dictLabel, sort: row.sort ?? 0, status: row.status }
  itemDialogVisible.value = true
}

async function onSaveItem() {
  if (!itemForm.value.dictCode || !itemForm.value.dictLabel) {
    ElMessage.warning('编码和名称必填')
    return
  }
  itemDialogLoading.value = true
  try {
    if (itemForm.value.id) {
      await api.put(`/dicts/${itemForm.value.id}`, {
        dictLabel: itemForm.value.dictLabel,
        sort: itemForm.value.sort,
        status: itemForm.value.status,
      })
    } else {
      await api.post('/dicts', {
        dictType: selectedType.value,
        dictCode: itemForm.value.dictCode,
        dictLabel: itemForm.value.dictLabel,
        sort: itemForm.value.sort,
        status: itemForm.value.status,
      })
    }
    itemDialogVisible.value = false
    await loadItems()
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    itemDialogLoading.value = false
  }
}

async function onDeleteItem(row: Dict) {
  try {
    await ElMessageBox.confirm('确认删除该字典项？', '提示', { type: 'warning' })
    await api.delete(`/dicts/${row.id}`)
    await loadItems()
    ElMessage.success('已删除')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(loadTypes)
</script>

<style scoped>
.dict-page {
  display: flex;
  height: calc(100vh - 120px);
  background: var(--erp-bg-page, #f0f2f5);
  gap: 12px;
  padding: 12px;
}

.dict-types-panel {
  width: 280px;
  background: #fff;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dict-items-panel {
  flex: 1;
  background: #fff;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--erp-border-color, #ebeef5);
  font-weight: 600;
  font-size: 15px;
  flex-shrink: 0;
}

.type-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.type-item {
  padding: 10px 12px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  border: 1px solid transparent;
  transition: all 0.15s;
}

.type-item:hover {
  background: #f5f7fa;
  border-color: #dcdfe6;
}

.type-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.type-main {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.type-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.type-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.type-code {
  font-size: 11px;
  color: #c0c4cc;
  font-family: monospace;
}

.type-actions {
  display: flex;
  gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--erp-border-color, #ebeef5);
  flex-shrink: 0;
}

.dict-items-panel :deep(.el-table) {
  flex: 1;
  border-radius: 0;
}
</style>
