<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
      <h2 style="margin: 0">部门管理</h2>
      <el-button type="primary" @click="openCreate(null)">新建部门</el-button>
    </div>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="状态">
        <el-select v-model="statusFilter" clearable placeholder="全部" style="width: 120px" @change="load">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>
    <el-table
      v-loading="loading"
      :data="treeData"
      row-key="id"
      stripe
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="deptName" label="部门名称" min-width="180" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCreate(row as DeptNode)">子部门</el-button>
          <el-button link type="primary" @click="openEdit(row as DeptNode)">编辑</el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            link
            type="danger"
            @click="onDisable(row as DeptNode)"
          >停用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="上级部门">
          <el-select v-model="form.parentId" clearable placeholder="无（顶级）" style="width: 100%">
            <el-option
              v-for="d in parentOptions"
              :key="d.id"
              :label="d.deptName"
              :value="d.id"
              :disabled="d.id === form.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" maxlength="100" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useBaseStore } from '@/stores/_base'

interface DeptNode {
  id?: number
  parentId?: number | null
  deptName: string
  sort: number
  status: string
  children?: DeptNode[]
}

const api = useBaseStore().api
const loading = ref(false)
const saving = ref(false)
const statusFilter = ref<string | undefined>()
const treeData = ref<DeptNode[]>([])
const flatDepts = ref<DeptNode[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建部门')
const formRef = ref<FormInstance>()
const form = reactive<DeptNode>({ deptName: '', sort: 0, status: 'ACTIVE' })

const rules: FormRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

const parentOptions = computed(() =>
  flatDepts.value.filter((d) => d.status === 'ACTIVE' && d.id !== form.id),
)

async function load() {
  loading.value = true
  try {
    const [treeRes, flatRes] = await Promise.all([
      api.get('/depts', { params: { tree: true, status: statusFilter.value || undefined } }),
      api.get('/depts', { params: { status: 'ACTIVE' } }),
    ])
    treeData.value = treeRes?.data ?? treeRes ?? []
    flatDepts.value = flatRes?.data ?? flatRes ?? []
  } finally {
    loading.value = false
  }
}

function openCreate(parent: DeptNode | null) {
  dialogTitle.value = parent ? `新建子部门 · ${parent.deptName}` : '新建部门'
  Object.assign(form, { id: undefined, parentId: parent?.id ?? null, deptName: '', sort: 0, status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(row: DeptNode) {
  dialogTitle.value = '编辑部门'
  Object.assign(form, { ...row, parentId: row.parentId ?? null })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      parentId: form.parentId ?? null,
      deptName: form.deptName,
      sort: form.sort,
      status: form.status,
    }
    if (form.id) {
      await api.put(`/depts/${form.id}`, payload)
      ElMessage.success('已更新')
    } else {
      await api.post('/depts', payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDisable(row: DeptNode) {
  await ElMessageBox.confirm(`确定停用「${row.deptName}」？`, '停用部门', { type: 'warning' })
  try {
    await api.delete(`/depts/${row.id}`)
    ElMessage.success('已停用')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '停用失败')
  }
}

onMounted(load)
</script>
