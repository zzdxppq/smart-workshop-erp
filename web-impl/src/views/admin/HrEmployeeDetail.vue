<template>
  <div v-loading="loading">
    <h2>员工详情</h2>
    <el-card v-if="emp">
      <el-form :model="form" label-width="100px" style="max-width: 520px">
        <el-form-item label="工号">
          <el-input v-model="form.employeeNo" placeholder="留空则系统自动生成" clearable maxlength="64" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="岗位"><el-input v-model="form.position" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="考核人ID">
          <el-input-number v-model="form.reviewerUserId" :min="1" controls-position="right" style="width: 200px" />
          <span class="hint">对应 sys_user.id，用于绩效申诉处理人</span>
        </el-form-item>
        <el-form-item label="工资账套">
          <el-select v-model="form.salaryPackageId" clearable placeholder="选择账套" style="width: 240px">
            <el-option v-for="p in packages" :key="p.id" :label="p.packageName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考核方案">
          <el-select v-model="form.performanceSchemeId" clearable placeholder="选择方案" style="width: 240px">
            <el-option v-for="s in schemes" :key="s.id" :label="s.schemeName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useHrStore } from '@/stores/hr'
import { useDetailLoad } from '@/composables/useDetailLoad'

const route = useRoute()
const hrStore = useHrStore()
const saving = ref(false)
const packages = ref<any[]>([])
const schemes = ref<any[]>([])
const form = ref<any>({})

const { data: emp, loading } = useDetailLoad<any>((id) => hrStore.getEmployee(id))

watch(emp, (e) => {
  if (!e) return
  form.value = {
    employeeNo: e.employeeNo ?? e.employee_no ?? '',
    name: e.name,
    department: e.department ?? e.deptName,
    position: e.position,
    phone: e.phone,
    email: e.email,
    reviewerUserId: e.reviewerUserId,
    salaryPackageId: e.salaryPackageId,
    performanceSchemeId: e.performanceSchemeId,
  }
}, { immediate: true })

async function save() {
  saving.value = true
  try {
    await hrStore.updateEmployee(Number(route.params.id), form.value)
    ElMessage.success('已保存')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  packages.value = await hrStore.listSalaryPackages()
  schemes.value = await hrStore.listPerformanceSchemes()
})
</script>

<style scoped>
.hint { margin-left: 8px; font-size: 12px; color: var(--erp-text-muted); }
</style>
