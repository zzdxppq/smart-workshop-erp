<template>
  <div class="email-config-page">
    <h2>邮件配置 · 163 邮箱</h2>
    <el-alert type="warning" :closable="false" title="V1.3.7 AD-3：发件地址须为 @163.com · SMTP 默认 smtp.163.com:465 SSL" />

    <el-form v-loading="loading" :model="form" label-width="140px" style="margin-top: 16px; max-width: 640px">
      <el-form-item label="SMTP 服务器">
        <el-input v-model="form.smtpHost" placeholder="smtp.163.com" />
      </el-form-item>
      <el-form-item label="端口">
        <el-input-number v-model="form.smtpPort" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="SSL">
        <el-switch v-model="form.useSsl" />
      </el-form-item>
      <el-form-item label="发件地址">
        <el-input v-model="form.fromAddress" placeholder="example@163.com" />
      </el-form-item>
      <el-form-item label="授权码">
        <el-input v-model="form.authCode" type="password" show-password placeholder="163 客户端授权码" />
      </el-form-item>
      <el-form-item label="日配额">
        <el-input-number v-model="form.dailyQuota" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="配额预警阈值">
        <el-input-number v-model="form.quotaWarnThreshold" :min="0" :max="100" controls-position="right" />
      </el-form-item>
      <el-form-item label="日志保留(天)">
        <el-input-number v-model="form.logRetentionDays" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="附件上限(MB)">
        <el-input-number v-model="form.attachmentMaxSizeMb" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button :loading="testing" @click="openTest">测试发送</el-button>
      </el-form-item>
    </el-form>

    <el-divider />
    <h3>发送日志</h3>
    <el-table :data="logs" stripe border>
      <el-table-column prop="toAddress" label="收件人" />
      <el-table-column prop="subject" label="主题" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="sentAt" label="时间" width="180" />
    </el-table>
    <el-pagination
      v-model:current-page="logPage"
      :page-size="20"
      :total="logTotal"
      layout="prev, pager, next"
      style="margin-top: 12px"
      @current-change="loadLogs"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { E1EmailService } from '@/api/generated/services/E1EmailService'
import type { EmailConfig } from '@/api/generated/models/EmailConfig'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const logs = ref<Array<Record<string, unknown>>>([])
const logPage = ref(1)
const logTotal = ref(0)

const form = ref<EmailConfig>({
  smtpHost: 'smtp.163.com',
  smtpPort: 465,
  useSsl: true,
  fromAddress: '',
  authCode: '',
  dailyQuota: 500,
  quotaWarnThreshold: 80,
  logRetentionDays: 90,
  attachmentMaxSizeMb: 10,
})

function validate163() {
  if (!form.value.fromAddress?.endsWith('@163.com')) {
    ElMessage.error('V1.3.7 AD-3：发件地址必须使用 163 邮箱')
    return false
  }
  return true
}

async function loadConfig() {
  loading.value = true
  try {
    const loaded = unwrapResult<EmailConfig>(await E1EmailService.getEmailConfig())
    form.value = { ...form.value, ...loaded, authCode: loaded.authCode || '' }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载配置失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!validate163()) return
  saving.value = true
  try {
    unwrapResult(await E1EmailService.updateEmailConfig(form.value))
    ElMessage.success('已保存')
    await loadConfig()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function openTest() {
  if (!validate163()) return
  try {
    const { value } = await ElMessageBox.prompt('测试收件地址', '测试发送', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputPattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      inputErrorMessage: '请输入有效邮箱',
    })
    testing.value = true
    unwrapResult(await E1EmailService.testEmail({
      toAddress: value,
      subject: 'ERP 邮件配置测试',
      body: '这是一封来自智能车间 ERP 的测试邮件。',
    }))
    ElMessage.success('测试邮件已发送')
    loadLogs()
  } catch (e: unknown) {
    if (e === 'cancel') return
    ElMessage.error((e as { message?: string })?.message || '发送失败')
  } finally {
    testing.value = false
  }
}

async function loadLogs() {
  try {
    const pageData = parsePageItems(await E1EmailService.emailLogs(logPage.value, 20))
    logs.value = pageData.items as Array<Record<string, unknown>>
    logTotal.value = pageData.total
  } catch {
    logs.value = []
  }
}

onMounted(async () => {
  await loadConfig()
  await loadLogs()
})
</script>

<style scoped>
.email-config-page {
  padding: 16px;
}
</style>
