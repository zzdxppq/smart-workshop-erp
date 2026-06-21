<template>
  <div v-loading="loading" class="customer-detail">
    <el-page-header @back="$router.push('/sales/customers')">
      <template #content>客户详情 (E2-S1)</template>
    </el-page-header>
    <el-descriptions v-if="customer && !editing" :column="2" border style="margin-top: 16px">
      <el-descriptions-item label="编码">{{ customer.customerCode }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ customer.name ?? customer.customerName }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ customer.contactName ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ customer.contactPhone ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="联系邮箱" :span="2">{{ customer.contactEmail ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="行业">{{ customer.industry }}</el-descriptions-item>
      <el-descriptions-item label="信用额度">
        <MoneyAmount :model-value="Number(customer.creditLimit ?? 0)" display-only />
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <ErpStatusTag :status="String(customer.status ?? '')" :label="customerStatusLabel(String(customer.status ?? ''))" />
      </el-descriptions-item>
      <el-descriptions-item label="负责人">{{ customer.ownerId ?? '未领用' }}</el-descriptions-item>
      <el-descriptions-item label="保护期">{{ customer.protectUntil ?? '—' }}</el-descriptions-item>
    </el-descriptions>

    <el-form v-if="customer && editing" :model="editForm" label-width="100px" style="margin-top: 16px; max-width: 520px">
      <el-form-item label="客户名称">
        <el-input v-model="editForm.name" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="editForm.contactName" placeholder="客户对接人" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="editForm.contactPhone" />
      </el-form-item>
      <el-form-item label="联系邮箱">
        <el-input v-model="editForm.contactEmail" placeholder="报价 PDF 将发送至此邮箱" />
      </el-form-item>
      <el-form-item label="行业">
        <el-input v-model="editForm.industry" />
      </el-form-item>
      <el-form-item label="信用额度">
        <el-input-number v-model="editForm.creditLimit" :min="0" :step="10000" style="width: 100%" />
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" show-icon title="「领用客户」是什么？"
      description="公海/未归属客户需先被业务员领用，领用后 30 天内其他业务员不可抢单；领用成功后该客户会出现在报价单等下拉列表中。"
      style="margin-top: 16px" />
    <div style="margin-top: 12px; display: flex; gap: 8px">
      <el-button v-if="!editing" @click="startEdit">编辑档案</el-button>
      <template v-else>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
        <el-button @click="cancelEdit">取消</el-button>
      </template>
      <el-button type="primary" :loading="claiming" @click="claim">领用客户</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { useMasterData } from '@/composables/useMasterData'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { customerStatusLabel } from '@/utils/statusLabels'

const route = useRoute()
const loading = ref(false)
const claiming = ref(false)
const saving = ref(false)
const editing = ref(false)
const customer = ref<Record<string, unknown> | null>(null)
const editForm = ref({
  name: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  industry: '',
  creditLimit: 0,
})
const api = useBaseStore().api
const { invalidateCustomersCache } = useMasterData()

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    customer.value = unwrapResult(await api.get(`/customers/${id}`)) as Record<string, unknown>
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function startEdit() {
  if (!customer.value) return
  editForm.value = {
    name: String(customer.value.name ?? ''),
    contactName: String(customer.value.contactName ?? ''),
    contactPhone: String(customer.value.contactPhone ?? ''),
    contactEmail: String(customer.value.contactEmail ?? ''),
    industry: String(customer.value.industry ?? ''),
    creditLimit: Number(customer.value.creditLimit ?? 0),
  }
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveEdit() {
  if (!customer.value?.id) return
  saving.value = true
  try {
    customer.value = unwrapResult(
      await api.put(`/customers/${customer.value.id}`, editForm.value),
    ) as Record<string, unknown>
    invalidateCustomersCache()
    editing.value = false
    ElMessage.success('客户档案已更新')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function claim() {
  if (!customer.value?.id) return
  claiming.value = true
  try {
    customer.value = unwrapResult(
      await api.post(`/customers/${customer.value.id}/claim`),
    ) as Record<string, unknown>
    invalidateCustomersCache()
    ElMessage.success('领用成功，30 天保护期生效，报价单客户下拉已可刷新选用')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '领用失败')
  } finally {
    claiming.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.customer-detail { padding: 16px; }
</style>
