<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>多仓库总览</h2>
      <el-button type="primary" @click="openCreate">新建仓库</el-button>
    </div>
    <el-row :gutter="20">
      <el-col v-for="w in warehouses" :key="w.warehouseCode" :span="8">
        <el-card>
          <h3>{{ w.warehouseName }}</h3>
          <p>编码：{{ w.warehouseCode }}</p>
          <p>类型：{{ w.warehouseType }}</p>
          <p>地址：{{ w.address || '—' }}</p>
          <p>状态：{{ w.isActive === 0 ? '停用' : '启用' }}</p>
          <div class="card-actions">
            <el-button type="primary" link @click="openEdit(w)">编辑</el-button>
            <el-button @click="$router.push(`/warehouse/locations?warehouse=${w.warehouseCode}`)">查看库位</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !warehouses.length" description="暂无仓库" />

    <el-dialog v-model="showCreate" title="新建仓库" width="480px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库编码" required>
          <el-input v-model="form.warehouseCode" placeholder="WH-D" maxlength="4" />
        </el-form-item>
        <el-form-item label="仓库名称" required>
          <el-input v-model="form.warehouseName" placeholder="演示仓 D" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.warehouseType" placeholder="全部" style="width: 100%">
            <el-option label="主仓 MAIN" value="MAIN" />
            <el-option label="副仓 SUB" value="SUB" />
            <el-option label="线边仓 LINE_SIDE" value="LINE_SIDE" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitWarehouse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEdit" title="编辑仓库" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="仓库编码">
          <el-input v-model="editForm.warehouseCode" disabled />
        </el-form-item>
        <el-form-item label="仓库名称" required>
          <el-input v-model="editForm.warehouseName" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="editForm.warehouseType" placeholder="全部" style="width: 100%">
            <el-option label="主仓 MAIN" value="MAIN" />
            <el-option label="副仓 SUB" value="SUB" />
            <el-option label="线边仓 LINE_SIDE" value="LINE_SIDE" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="editForm.address" />
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
import { ElMessage } from 'element-plus'
import { useWarehouseStore } from '@/stores/warehouse'
import { unwrapResult } from '@/utils/apiPage'

interface WarehouseRow {
  warehouseCode: string
  warehouseName?: string
  warehouseType?: string
  address?: string
  isActive?: number
}

const warehouseStore = useWarehouseStore()
const warehouses = ref<WarehouseRow[]>([])
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const showEdit = ref(false)
const form = ref({
  warehouseCode: 'WH-D',
  warehouseName: '演示仓 D',
  warehouseType: 'SUB',
  address: '',
})
const editForm = ref({
  warehouseCode: '',
  warehouseName: '',
  warehouseType: 'SUB',
  address: '',
  isActive: 1,
})

function openCreate() {
  showCreate.value = true
}

function openEdit(w: WarehouseRow) {
  editForm.value = {
    warehouseCode: w.warehouseCode,
    warehouseName: w.warehouseName ?? '',
    warehouseType: w.warehouseType ?? 'SUB',
    address: w.address ?? '',
    isActive: w.isActive ?? 1,
  }
  showEdit.value = true
}

async function load() {
  loading.value = true
  try {
    warehouses.value = unwrapResult<WarehouseRow[]>(await warehouseStore.listWarehouses()) || []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    warehouses.value = []
  } finally {
    loading.value = false
  }
}

async function submitWarehouse() {
  if (!form.value.warehouseCode || !form.value.warehouseName || !form.value.warehouseType) {
    ElMessage.warning('请填写完整仓库信息')
    return
  }
  saving.value = true
  try {
    await warehouseStore.createWarehouse(form.value)
    ElMessage.success('仓库已创建')
    showCreate.value = false
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

async function submitEdit() {
  if (!editForm.value.warehouseName || !editForm.value.warehouseType) {
    ElMessage.warning('请填写完整仓库信息')
    return
  }
  saving.value = true
  try {
    await warehouseStore.updateWarehouse(editForm.value.warehouseCode, {
      warehouseName: editForm.value.warehouseName,
      warehouseType: editForm.value.warehouseType,
      address: editForm.value.address || undefined,
      isActive: editForm.value.isActive,
    })
    ElMessage.success('仓库已更新')
    showEdit.value = false
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
