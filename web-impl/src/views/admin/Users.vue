<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
      <h2 style="margin: 0">用户/角色</h2>
      <el-button type="primary" @click="openCreate">新建用户</el-button>
    </div>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="用户名">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="deptFilter" clearable placeholder="全部" :loading="deptsLoading" style="width: 180px" @change="onSearch">
          <el-option v-for="d in flatOptions" :key="d.id" :label="d.label" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column label="部门" min-width="120">
        <template #default="{ row }">{{ deptName(row.deptId) || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="登录名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="≥8 位含大小写与数字" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" clearable placeholder="选择部门" style="width: 100%" :loading="deptsLoading">
            <el-option v-for="d in flatOptions" :key="d.id" :label="d.label" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="form.roleCode" style="width: 100%">
            <el-option label="管理员 ADMIN" value="ADMIN" />
            <el-option label="总经理 GM" value="GM" />
            <el-option label="销售 SALES" value="SALES" />
            <el-option label="销售经理 SALES_MGR" value="SALES_MGR" />
            <el-option label="生产经理 PROD_MGR" value="PROD_MGR" />
            <el-option label="操作工 OPERATOR" value="OPERATOR" />
            <el-option label="仓管 WAREHOUSE" value="WAREHOUSE" />
            <el-option label="品检 QC" value="QC" />
            <el-option label="采购 BUYER" value="BUYER" />
            <el-option label="财务 FINANCE" value="FINANCE" />
            <el-option label="人事 HR" value="HR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import { useDepartments } from '@/composables/useDepartments'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const api = useBaseStore().api
const keyword = ref('')
const deptFilter = ref<number | null>(null)
const dialogVisible = ref(false)
const dialogTitle = ref('新建用户')
const saving = ref(false)
const formRef = ref<FormInstance>()

const { flatOptions, loading: deptsLoading, load: loadDepts, deptName } = useDepartments()

const form = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  realName: '',
  deptId: null as number | null,
  phone: '',
  roleCode: 'OPERATOR',
})

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: form.id ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
}))

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  api.get('/users', {
    params: {
      keyword: keyword.value || undefined,
      deptId: deptFilter.value ?? undefined,
      ...params,
    },
  }),
)

function filters() {
  return {
    keyword: keyword.value || undefined,
    deptId: deptFilter.value ?? undefined,
  }
}

function onSearch() {
  pageNum.value = 1
  reload(filters())
}
function onPageChange() {
  reload(filters())
}

function openCreate() {
  dialogTitle.value = '新建用户'
  Object.assign(form, {
    id: undefined,
    username: '',
    password: '',
    realName: '',
    deptId: null,
    phone: '',
    roleCode: 'OPERATOR',
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    deptId: row.deptId ?? null,
    phone: row.phone ?? '',
    roleCode: 'OPERATOR',
  })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      username: form.username.trim(),
      realName: form.realName.trim(),
      deptId: form.deptId ?? undefined,
      phone: form.phone || undefined,
      roleCodes: [form.roleCode],
    }
    if (form.id) {
      await api.put(`/users/${form.id}`, payload)
      ElMessage.success('已更新')
    } else {
      await api.post('/users', { ...payload, password: form.password })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    onSearch()
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadDepts()
  onSearch()
})
</script>
