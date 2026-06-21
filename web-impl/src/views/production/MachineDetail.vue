<template>
  <div v-loading="loading">
    <el-page-header @back="$router.back()" content="设备详情" />

    <!-- 操作按钮行 -->
    <div style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap;">
      <el-button type="primary" @click="openEdit">编辑</el-button>
      <el-button type="warning" @click="openChangeStatus">变更状态</el-button>
      <el-button type="success" @click="openAddMaintenance">添加维保记录</el-button>
    </div>

    <!-- 基本信息卡片 -->
    <el-card v-if="machine" style="margin-top: 12px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备码">{{ machine.machineCode ?? machine.code }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <ErpStatusTag :status="String(machine.status ?? '')" />
        </el-descriptions-item>
        <el-descriptions-item label="名称">{{ machine.machineName ?? machine.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ machine.machineType }}</el-descriptions-item>
        <el-descriptions-item label="负荷率">{{ machine.loadPercent ?? 0 }}%</el-descriptions-item>
        <el-descriptions-item label="OEE">{{ machine.oee ?? 0 }}%</el-descriptions-item>
      </el-descriptions>
      <MachineLoadBar :percent="Number(machine.loadPercent ?? 0)" :machine-name="String(machine.machineName ?? machine.name ?? '')" style="margin-top: 12px" />
    </el-card>

    <!-- 关联排产工单 -->
    <el-card style="margin-top: 16px">
      <template #header>关联排产工单</template>
      <el-table :data="schedules" stripe border size="small">
        <el-table-column prop="scheduleNo" label="排产号" min-width="130" />
        <el-table-column prop="workorderId" label="工单ID" width="90" />
        <el-table-column prop="planStart" label="计划开始" min-width="160" />
        <el-table-column prop="planEnd" label="计划结束" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!schedules.length" description="暂无关联排产" />
    </el-card>

    <!-- 维保记录 -->
    <el-card style="margin-top: 16px">
      <template #header>
        <span>维保记录</span>
        <el-button type="success" size="small" style="float: right" @click="openAddMaintenance">+ 添加维保记录</el-button>
      </template>
      <el-table :data="maintenance" stripe border size="small">
        <el-table-column prop="maintenanceType" label="类型" width="120">
          <template #default="{ row }">{{ maintenanceTypeLabel(row.maintenanceType) }}</template>
        </el-table-column>
        <el-table-column prop="performedAt" label="执行时间" min-width="160" />
        <el-table-column prop="nextDue" label="下次到期" min-width="160" />
        <el-table-column prop="executor" label="执行人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="onDeleteMaintenance(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!maintenance.length" description="暂无维保记录" />
    </el-card>

    <!-- 编辑设备弹窗 -->
    <el-dialog v-model="editVisible" title="编辑设备" width="520px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="设备名称" prop="machineName">
          <el-input v-model="editForm.machineName" maxlength="100" />
        </el-form-item>
        <el-form-item label="设备类型" prop="machineType">
          <el-select v-model="editForm.machineType" style="width: 100%">
            <el-option v-for="t in machineTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="机台号" prop="machineNo">
          <el-input v-model="editForm.machineNo" maxlength="32" />
        </el-form-item>
        <el-form-item label="维护周期" prop="maintenanceCycleDays">
          <el-input-number v-model="editForm.maintenanceCycleDays" :min="1" :max="365" />
          <span style="margin-left: 8px; color: #909399">天</span>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="onEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 变更状态弹窗 -->
    <el-dialog v-model="statusVisible" title="变更设备状态" width="480px" destroy-on-close>
      <el-form :model="statusForm" label-width="110px">
        <el-form-item label="当前状态">
          <el-input :model-value="statusLabel(String(machine?.status ?? ''))" disabled />
        </el-form-item>
        <el-form-item label="目标状态" prop="status">
          <el-radio-group v-model="statusForm.status">
            <el-radio value="IDLE">空闲</el-radio>
            <el-radio value="RUNNING">运行中</el-radio>
            <el-radio value="MAINTENANCE">维修中</el-radio>
            <el-radio value="FAULT">故障</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="statusForm.reason" maxlength="200" placeholder="选填" />
        </el-form-item>
        <el-form-item label="预计恢复日期">
          <el-date-picker v-model="statusForm.estimatedRecoveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="warning" :loading="statusLoading" @click="onChangeStatus">确认变更</el-button>
      </template>
    </el-dialog>

    <!-- 添加维保记录弹窗 -->
    <el-dialog v-model="maintVisible" title="添加维保记录" width="480px" destroy-on-close>
      <el-form ref="maintFormRef" :model="maintForm" :rules="maintRules" label-width="110px">
        <el-form-item label="维保类型" prop="maintenanceType">
          <el-select v-model="maintForm.maintenanceType" style="width: 100%">
            <el-option label="例行保养" value="ROUTINE" />
            <el-option label="预防性保养" value="PREVENTIVE" />
            <el-option label="故障维修" value="REPAIR" />
            <el-option label="大修" value="OVERHAUL" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时间" prop="performedAt">
          <el-date-picker v-model="maintForm.performedAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次到期" prop="nextDue">
          <el-date-picker v-model="maintForm.nextDue" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="执行人">
          <el-input v-model="maintForm.executor" maxlength="50" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="maintForm.remark" type="textarea" :rows="2" maxlength="255" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintVisible = false">取消</el-button>
        <el-button type="success" :loading="maintLoading" @click="onAddMaintenance">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import MachineLoadBar from '@/components/erp/MachineLoadBar.vue'

const route = useRoute()
const loading = ref(false)
const machine = ref<Record<string, unknown> | null>(null)
const schedules = ref<any[]>([])
const maintenance = ref<any[]>([])

const machineTypes = ['CNC', '车床', '铣床', '磨床', '电火花', '激光切割', '冲床', '折弯机', '焊接机', '其他']

function statusLabel(s?: string) {
  const map: Record<string, string> = { IDLE: '空闲', RUNNING: '运行中', MAINTENANCE: '维修中', FAULT: '故障' }
  return s ? (map[s] ?? s) : '—'
}

function maintenanceTypeLabel(t?: string) {
  const map: Record<string, string> = { ROUTINE: '例行保养', PREVENTIVE: '预防性保养', REPAIR: '故障维修', OVERHAUL: '大修' }
  return t ? (map[t] ?? t) : '—'
}

async function reload() {
  const id = route.params.id
  loading.value = true
  try {
    const data = unwrapResult(await useBaseStore().api.get(`/machines/${id}/detail`)) as Record<string, unknown>
    machine.value = data
    schedules.value = (data.activeSchedules as any[]) ?? []
    maintenance.value = (data.maintenanceRecords as any[]) ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 编辑设备
const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref()
const editForm = ref({ machineName: '', machineType: '', machineNo: '', maintenanceCycleDays: 90, remark: '' })
const editRules = {
  machineName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  machineType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
}

function openEdit() {
  editForm.value = {
    machineName: String(machine.value?.machineName ?? ''),
    machineType: String(machine.value?.machineType ?? ''),
    machineNo: String(machine.value?.machineNo ?? ''),
    maintenanceCycleDays: Number(machine.value?.maintenanceCycleDays ?? 90),
    remark: String(machine.value?.remark ?? ''),
  }
  editVisible.value = true
}

async function onEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  editLoading.value = true
  try {
    await useBaseStore().api.put(`/machines/${route.params.id}`, editForm.value)
    ElMessage.success('设备更新成功')
    editVisible.value = false
    await reload()
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  } finally {
    editLoading.value = false
  }
}

// 变更状态
const statusVisible = ref(false)
const statusLoading = ref(false)
const statusForm = ref({ status: '', reason: '', estimatedRecoveryDate: '' })

function openChangeStatus() {
  statusForm.value = { status: '', reason: '', estimatedRecoveryDate: '' }
  statusVisible.value = true
}

async function onChangeStatus() {
  if (!statusForm.value.status) {
    ElMessage.warning('请选择目标状态')
    return
  }
  statusLoading.value = true
  try {
    await useBaseStore().api.put(`/machines/${route.params.id}/status`, statusForm.value)
    ElMessage.success('状态变更成功')
    statusVisible.value = false
    await reload()
  } catch (e: any) {
    ElMessage.error(e?.message || '变更失败')
  } finally {
    statusLoading.value = false
  }
}

// 添加维保记录
const maintVisible = ref(false)
const maintLoading = ref(false)
const maintFormRef = ref()
const maintForm = ref({ maintenanceType: '', performedAt: '', nextDue: '', executor: '', remark: '' })
const maintRules = {
  maintenanceType: [{ required: true, message: '请选择维保类型', trigger: 'change' }],
  performedAt: [{ required: true, message: '请选择执行时间', trigger: 'change' }],
  nextDue: [{ required: true, message: '请选择下次到期时间', trigger: 'change' }],
}

function openAddMaintenance() {
  maintForm.value = { maintenanceType: '', performedAt: '', nextDue: '', executor: '', remark: '' }
  maintVisible.value = true
}

async function onAddMaintenance() {
  const valid = await maintFormRef.value?.validate().catch(() => false)
  if (!valid) return
  maintLoading.value = true
  try {
    await useBaseStore().api.post(`/machines/${route.params.id}/maintenance`, maintForm.value)
    ElMessage.success('维保记录添加成功')
    maintVisible.value = false
    await reload()
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    maintLoading.value = false
  }
}

// 删除维保记录
async function onDeleteMaintenance(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该维保记录？', '提示', { type: 'warning' })
    await useBaseStore().api.delete(`/machines/${route.params.id}/maintenance/${row.id}`)
    ElMessage.success('删除成功')
    await reload()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(reload)
</script>