<template>
  <div class="email-templates-page">
    <h2>邮件模板管理</h2>
    <el-alert type="info" :closable="false" title="PRD 5 套模板：委外下单 / 发货提醒 / 质检不合格 / 返修单 / 月度对账 · 占位符如 {{outsourceNo}}" />

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>模板列表</template>
          <el-menu :default-active="activeKey" @select="onSelect">
            <el-menu-item v-for="t in templates" :key="t.key" :index="t.key">
              {{ t.name }}
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card v-loading="loading" shadow="never">
          <template #header>{{ current?.name || '编辑模板' }}</template>
          <el-form v-if="current" label-width="80px">
            <el-form-item label="主题">
              <el-input v-model="form.subject" />
            </el-form-item>
            <el-form-item label="正文">
              <el-input v-model="form.body" type="textarea" :rows="14" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </el-form-item>
          </el-form>
          <el-empty v-else description="请选择模板" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { unwrapResult } from '@/utils/apiPage'

interface EmailTemplate {
  key: string
  name: string
  subject: string
  body: string
  description?: string
}

const adminStore = useAdminStore()
const templates = ref<EmailTemplate[]>([])
const activeKey = ref('')
const current = ref<EmailTemplate | null>(null)
const loading = ref(false)
const saving = ref(false)
const form = ref({ subject: '', body: '' })

async function loadList() {
  loading.value = true
  try {
    const list = unwrapResult<EmailTemplate[]>(await adminStore.listEmailTemplates())
    templates.value = list
    if (list.length && !activeKey.value) {
      onSelect(list[0].key)
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSelect(key: string) {
  activeKey.value = key
  loading.value = true
  try {
    const t = unwrapResult<EmailTemplate>(await adminStore.getEmailTemplate(key))
    current.value = t
    form.value = { subject: t.subject, body: t.body }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载模板失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!activeKey.value) return
  saving.value = true
  try {
    const t = unwrapResult<EmailTemplate>(await adminStore.updateEmailTemplate(activeKey.value, form.value))
    current.value = t
    ElMessage.success('模板已保存')
    await loadList()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.email-templates-page { padding: 16px; }
</style>
