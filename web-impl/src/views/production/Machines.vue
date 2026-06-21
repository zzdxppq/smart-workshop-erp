<template>
  <div>
    <h2>设备机台</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="机台号">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="openCreate">+ 新增设备</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="code" label="机台号" min-width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/production/machines/${row.id}`)">
            {{ row.code }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="oee" label="OEE" width="80">
        <template #default="{ row }">{{ row.oee ?? 0 }}%</template>
      </el-table-column>
      <el-table-column label="负荷" min-width="160">
        <template #default="{ row }">
          <MachineLoadBar :percent="Number(row.loadPercent ?? row.oee ?? 0)" :machine-name="row.code" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button size="small" link @click="$router.push(`/production/machines/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />

    <el-dialog v-model="createVisible" title="新增设备" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="设备名称" prop="machineName">
          <el-input v-model="createForm.machineName" maxlength="100" placeholder="如：CNC加工中心01" />
        </el-form-item>
        <el-form-item label="设备类型" prop="machineType">
          <el-select v-model="createForm.machineType" placeholder="请选择设备类型" style="width: 100%">
            <el-option v-for="t in machineTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="机台号" prop="machineNo">
          <el-input v-model="createForm.machineNo" maxlength="32" placeholder="如：SB-CNC-001" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="空闲" value="IDLE" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="维修中" value="MAINTENANCE" />
            <el-option label="故障" value="FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="维护周期" prop="maintenanceCycleDays">
          <el-input-number v-model="createForm.maintenanceCycleDays" :min="1" :max="365" />
          <span style="margin-left: 8px; color: #909399">天</span>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="onCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import MachineLoadBar from '@/components/erp/MachineLoadBar.vue'

const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/machines', {
    params: { keyword: keyword.value || undefined, ...params },
  }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

// 新增设备
const machineTypes = ['CNC', '车床', '铣床', '磨床', '电火花', '激光切割', '冲床', '折弯机', '焊接机', '其他']
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const createForm = ref({
  machineName: '',
  machineType: '',
  machineNo: '',
  status: 'IDLE',
  maintenanceCycleDays: 90,
  remark: '',
})
const createRules = {
  machineName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  machineType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
}

function openCreate() {
  createForm.value = { machineName: '', machineType: '', machineNo: '', status: 'IDLE', maintenanceCycleDays: 90, remark: '' }
  createVisible.value = true
}

async function onCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await useBaseStore().api.post('/machines', createForm.value)
    ElMessage.success('设备创建成功')
    createVisible.value = false
    onSearch()
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(onSearch)
</script>
