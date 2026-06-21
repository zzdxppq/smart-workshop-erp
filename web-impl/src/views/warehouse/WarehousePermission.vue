<template>
  <div v-loading="loading">
    <h2>仓库权限管理</h2>
    <el-form label-width="120px">
      <el-form-item label="用户 ID">
        <el-input v-model="userId" placeholder="用户ID" />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="userName" placeholder="仓管姓名" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="email" placeholder="可选" />
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="warehouseId">
          <el-option label="主仓" :value="1" />
          <el-option label="线边仓" :value="2" />
          <el-option label="成品仓" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="权限">
        <el-checkbox v-model="permInbound">入库</el-checkbox>
        <el-checkbox v-model="permOutbound">出库</el-checkbox>
        <el-checkbox v-model="permInventory">盘点</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="save">授予扫码权限</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="lastPermissionNo" type="success" :closable="false" :title="`权限单号：${lastPermissionNo}`" style="margin-top: 12px" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const userId = ref('')
const userName = ref('')
const email = ref('')
const warehouseId = ref<number>(1)
const permInbound = ref(true)
const permOutbound = ref(false)
const permInventory = ref(false)
const loading = ref(false)
const lastPermissionNo = ref('')

async function save() {
  const uid = Number(userId.value)
  if (!uid) {
    ElMessage.warning('请填写用户 ID')
    return
  }
  if (!userName.value.trim()) {
    ElMessage.warning('请填写用户名')
    return
  }
  loading.value = true
  try {
    const data = unwrapResult<{ permission?: { permissionNo?: string } }>(
      await useBaseStore().api.post('/warehouse/permission/grant', null, {
        params: {
          userId: uid,
          userName: userName.value.trim(),
          role: 'WAREHOUSE',
          grantedBy: 'admin',
          email: email.value || undefined,
        },
      }),
    )
    lastPermissionNo.value = data.permission?.permissionNo ?? ''
    ElMessage.success(`权限已授予${lastPermissionNo.value ? `：${lastPermissionNo.value}` : ''}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    loading.value = false
  }
}
</script>
