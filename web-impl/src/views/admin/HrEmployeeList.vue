<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="dialogVisible = true">新建员工</el-button>
    </div>
    <el-form :inline="true" class="erp-filter-bar">
      <el-form-item label="员工姓名">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="deptFilter" clearable placeholder="全部" :loading="deptsLoading" @change="onSearch">
          <el-option v-for="d in flatOptions" :key="d.id" :label="d.label" :value="d.deptName" />
        </el-select>
      </el-form-item>
      <el-form-item label="岗位">
        <el-select v-model="positionFilter" clearable placeholder="全部" :loading="positionsLoading" @change="onSearch">
          <el-option v-for="p in positions" :key="p.code" :label="p.label" :value="p.code" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column label="工号" min-width="120">
        <template #default="{ row }">{{ row.employeeNo ?? row.employee_no ?? '—' }}</template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column prop="department" label="部门" min-width="100" />
      <el-table-column prop="position" label="岗位" min-width="120" />
      <el-table-column prop="phone" label="电话" width="120" />
      <el-table-column prop="hireDate" label="入职日期" width="120" />
      <el-table-column label="登录账号" min-width="120">
        <template #default="{ row }">
          <template v-if="row.loginUsername || row.userId">
            <el-tag type="success" size="small">{{ row.loginUsername ?? `UID:${row.userId}` }}</el-tag>
          </template>
          <el-tag v-else type="info" size="small">未关联</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/hr/employee/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="erp-pagination"
      @current-change="onPageChange"
      @size-change="onSearch"
    />

    <el-dialog v-model="dialogVisible" title="新建员工档案" width="520px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="工号">
          <el-input v-model="form.employeeNo" placeholder="留空则系统自动生成" clearable maxlength="64" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="部门" required>
          <el-select v-model="form.deptId" placeholder="选择部门" style="width: 100%" :loading="deptsLoading">
            <el-option v-for="d in flatOptions" :key="d.id" :label="d.label" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select
            v-model="form.position"
            placeholder="选择岗位（字典 POS-*）"
            style="width: 100%"
            filterable
            allow-create
            default-first-option
            :loading="positionsLoading"
          >
            <el-option v-for="p in positions" :key="p.code" :label="`${p.label}（${p.code}）`" :value="p.code" />
          </el-select>
          <span class="hint">必选 POS-* 编码；如需自定义请先在「数据字典 EMPLOYEE_POSITION」中新增</span>
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="开通登录">
          <el-switch v-model="form.createLoginAccount" />
          <span class="hint">开启后将自动创建系统账号并与员工档案关联；登录名/初始密码留空则由系统生成（姓名全拼+随机2位数字，或 bts+工号；密码默认与登录名相同）</span>
        </el-form-item>
        <template v-if="form.createLoginAccount">
          <el-form-item label="登录名">
            <el-input v-model="form.loginUsername" placeholder="留空自动生成" />
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="form.loginPassword" placeholder="留空则与登录名相同" show-password />
          </el-form-item>
          <el-form-item label="系统角色">
            <el-select v-model="form.roleCode" style="width: 100%">
              <el-option label="操作工 OPERATOR" value="OPERATOR" />
              <el-option label="仓管 WAREHOUSE" value="WAREHOUSE" />
              <el-option label="品检 QC" value="QC" />
              <el-option label="采购 BUYER" value="BUYER" />
              <el-option label="销售 SALES" value="SALES" />
              <el-option label="人事 HR" value="HR" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useHrStore } from '@/stores/hr'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import { useDepartments } from '@/composables/useDepartments'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const { flatOptions, loading: deptsLoading, load: loadDepts, deptName } = useDepartments()

const hrStore = useHrStore()
const baseStore = useBaseStore()
const keyword = ref('')
const deptFilter = ref<string | null>(null)
const positionFilter = ref<string | null>(null)
const positions = ref<Array<{ code: string; label: string }>>([])
const positionsLoading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)

/** V94 · 加载 EMPLOYEE_POSITION 字典（POS-* 编码 + 岗位名称） */
async function loadPositions() {
  positionsLoading.value = true
  try {
    const r: any = await baseStore.api.get('/dicts', { params: { type: 'EMPLOYEE_POSITION' } })
    const list: any[] = Array.isArray(r) ? r : Array.isArray(r?.data) ? r.data : []
    positions.value = list
      .filter((d: any) => d && (d.status == null || d.status === 'ACTIVE'))
      .map((d: any) => ({ code: d.dictCode ?? d.code, label: d.dictLabel ?? d.label }))
      .sort((a, b) => a.code.localeCompare(b.code))
  } catch {
    positions.value = []
  } finally {
    positionsLoading.value = false
  }
}

const form = reactive({
  employeeNo: '',
  name: '',
  deptId: null as number | null,
  position: '',
  hireDate: '',
  phone: '',
  createLoginAccount: true,
  loginUsername: '',
  loginPassword: '',
  roleCode: 'OPERATOR',
})

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  hrStore.listEmployees({
    keyword: keyword.value || undefined,
    department: deptFilter.value ?? undefined,
    position: positionFilter.value ?? undefined,
    ...params,
  }),
)

function filters() {
  return {
    keyword: keyword.value || undefined,
    department: deptFilter.value ?? undefined,
    position: positionFilter.value ?? undefined,
  }
}

function onSearch() {
  pageNum.value = 1
  reload(filters())
}
function onPageChange() {
  reload(filters())
}

async function submitCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (!form.deptId) {
    ElMessage.warning('请选择部门')
    return
  }
  submitting.value = true
  try {
    const res: any = await hrStore.createEmployee({
      employeeNo: form.employeeNo.trim() || undefined,
      name: form.name.trim(),
      deptId: form.deptId,
      department: deptName(form.deptId),
      position: form.position || undefined,
      hireDate: form.hireDate || undefined,
      phone: form.phone || undefined,
      createLoginAccount: form.createLoginAccount,
      loginUsername: form.loginUsername || undefined,
      loginPassword: form.loginPassword || undefined,
      roleCode: form.roleCode || undefined,
    })
    const emp = res?.data ?? res
    const no = emp?.employeeNo ?? emp?.employee_no
    ElMessage.success(
      '员工档案已创建' + (no ? `，工号 ${no}` : '') + (form.createLoginAccount ? '，登录账号已开通' : ''),
    )
    dialogVisible.value = false
    Object.assign(form, {
      employeeNo: '',
      name: '',
      deptId: null,
      position: '',
      hireDate: '',
      phone: '',
      createLoginAccount: true,
      loginUsername: '',
      loginPassword: '',
      roleCode: 'OPERATOR',
    })
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDepts()
  loadPositions()
  onSearch()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--erp-text-muted);
}
</style>
