<template>
  <div class="field-encryption-page">
    <h2>字段加密管理</h2>
    <el-alert type="warning" :closable="false" title="AES-256-GCM · DEK 路径 · 敏感字段白名单（手机/身份证/银行卡）" />

    <el-form v-loading="loading" :model="form" label-width="140px" style="margin-top: 16px; max-width: 720px">
      <el-form-item label="算法">
        <el-input v-model="form.algorithm" disabled />
      </el-form-item>
      <el-form-item label="DEK 路径">
        <el-input v-model="form.dekPath" placeholder="/etc/erp/dek.key" />
      </el-form-item>
      <el-form-item label="加密字段白名单">
        <el-select v-model="form.whitelistFields" multiple filterable allow-create style="width: 100%">
          <el-option label="mobile（手机）" value="mobile" />
          <el-option label="idCard（身份证）" value="idCard" />
          <el-option label="bankCard（银行卡）" value="bankCard" />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="form.note" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { unwrapResult } from '@/utils/apiPage'

interface FieldEncryptionConfig {
  algorithm: string
  dekPath: string
  whitelistFields: string[]
  note?: string
}

const adminStore = useAdminStore()
const loading = ref(false)
const saving = ref(false)
const form = ref<FieldEncryptionConfig>({
  algorithm: 'AES-256-GCM',
  dekPath: '/etc/erp/dek.key',
  whitelistFields: ['mobile', 'idCard', 'bankCard'],
  note: '',
})

async function load() {
  loading.value = true
  try {
    const cfg = unwrapResult<FieldEncryptionConfig>(await adminStore.getFieldEncryption())
    form.value = { ...form.value, ...cfg }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await adminStore.updateFieldEncryption(form.value)
    ElMessage.success('配置已保存')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.field-encryption-page { padding: 16px; }
</style>
