<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>库位树</h2>
      <el-button type="primary" @click="showCreate = true">新建库位</el-button>
    </div>
    <el-card>
      <el-tree
        v-if="treeData.length"
        :data="treeData"
        :props="defaultProps"
        default-expand-all
        node-key="id"
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span>{{ node.label }}</span>
            <el-button
              v-if="data.locationCode"
              size="small"
              link
              type="primary"
              @click.stop="openEdit(data)"
            >
              编辑
            </el-button>
          </span>
        </template>
      </el-tree>
      <el-empty v-else description="暂无库位，请先创建仓库与库位" />
    </el-card>

    <el-dialog v-model="showCreate" title="新建库位" width="480px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库" required>
          <el-select v-model="form.warehouse" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.warehouseCode" :label="`${w.warehouseName} (${w.warehouseCode})`" :value="w.warehouseCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="库区" required>
          <el-input v-model="form.zone" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="库位" required>
          <el-input v-model="form.position" placeholder="如 01" />
        </el-form-item>
        <el-form-item label="库位编码" required>
          <el-input v-model="form.locationCode" placeholder="LOC-A01-01-01" />
        </el-form-item>
        <el-form-item label="库容">
          <el-input-number v-model="form.capacity" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitLocation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEdit" title="编辑库位" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="库位编码">
          <el-input v-model="editForm.locationCode" disabled />
        </el-form-item>
        <el-form-item label="库区" required>
          <el-input v-model="editForm.zone" />
        </el-form-item>
        <el-form-item label="库位" required>
          <el-input v-model="editForm.position" />
        </el-form-item>
        <el-form-item label="库容">
          <el-input-number v-model="editForm.capacity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useWarehouseStore } from '@/stores/warehouse'
import { unwrapResult } from '@/utils/apiPage'

interface TreeNode {
  id: string
  label?: string
  locationCode?: string
  zoneCode?: string
  zone?: string
  position?: string
  capacity?: number
  isActive?: number
  children?: TreeNode[]
}

const route = useRoute()
const warehouseStore = useWarehouseStore()
const treeData = ref<TreeNode[]>([])
const warehouses = ref<{ warehouseCode: string; warehouseName?: string }[]>([])
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const showEdit = ref(false)
const defaultProps = { children: 'children', label: 'label' }

const form = ref({
  warehouse: '',
  zone: 'A01',
  position: '01',
  locationCode: '',
  capacity: 100,
})

const editForm = ref({
  locationCode: '',
  zone: '',
  position: '',
  capacity: 100,
  isActive: 1,
})

function normalizeTree(nodes: Record<string, unknown>[]): TreeNode[] {
  return (nodes || []).map((wh) => ({
    id: String(wh.warehouseCode ?? ''),
    label: String(wh.label ?? `${wh.warehouseName} (${wh.warehouseCode})`),
    children: Array.isArray(wh.children)
      ? (wh.children as Record<string, unknown>[]).map((zone) => ({
          id: `${wh.warehouseCode}-${zone.zoneCode}`,
          label: String(zone.label ?? `库区 ${zone.zoneCode}`),
          zoneCode: String(zone.zoneCode ?? ''),
          children: Array.isArray(zone.children)
            ? (zone.children as Record<string, unknown>[]).map((loc) => ({
                id: String(loc.locationCode ?? ''),
                label: String(loc.label ?? loc.locationCode ?? ''),
                locationCode: String(loc.locationCode ?? ''),
                zone: String(zone.zoneCode ?? ''),
                position: String(loc.position ?? ''),
                capacity: Number(loc.capacity ?? 0),
                isActive: Number(loc.isActive ?? 1),
              }))
            : [],
        }))
      : [],
  }))
}

function openEdit(node: TreeNode) {
  if (!node.locationCode) return
  editForm.value = {
    locationCode: node.locationCode,
    zone: node.zone ?? '',
    position: node.position ?? '',
    capacity: node.capacity ?? 100,
    isActive: node.isActive ?? 1,
  }
  showEdit.value = true
}

async function loadTree() {
  loading.value = true
  try {
    const r = await warehouseStore.getLocationTree()
    treeData.value = normalizeTree(unwrapResult<Record<string, unknown>[]>(r) || [])
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    treeData.value = []
  } finally {
    loading.value = false
  }
}

async function loadWarehouses() {
  try {
    const r = await warehouseStore.listWarehouses()
    warehouses.value = unwrapResult<{ warehouseCode: string; warehouseName?: string }[]>(r) || []
    const preset = String(route.query.warehouse || '')
    if (preset) form.value.warehouse = preset
    else if (warehouses.value.length) form.value.warehouse = warehouses.value[0].warehouseCode
  } catch {
    warehouses.value = []
  }
}

async function submitLocation() {
  if (!form.value.warehouse || !form.value.zone || !form.value.position || !form.value.locationCode) {
    ElMessage.warning('请填写完整库位信息')
    return
  }
  saving.value = true
  try {
    await warehouseStore.createLocation({ ...form.value })
    ElMessage.success('库位已创建')
    showCreate.value = false
    await loadTree()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

async function submitEdit() {
  if (!editForm.value.zone || !editForm.value.position) {
    ElMessage.warning('请填写库区与库位')
    return
  }
  saving.value = true
  try {
    await warehouseStore.updateLocation(editForm.value.locationCode, {
      zone: editForm.value.zone,
      position: editForm.value.position,
      capacity: editForm.value.capacity,
      isActive: editForm.value.isActive,
    })
    ElMessage.success('库位已更新')
    showEdit.value = false
    await loadTree()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadWarehouses()
  await loadTree()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding-right: 8px;
}
</style>
