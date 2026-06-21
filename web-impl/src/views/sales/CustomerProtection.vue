<template>
  <div v-loading="loading" class="customer-protection">
    <h2>客户保护管理</h2>

    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      <template #title>
        <strong>功能说明</strong>
      </template>
      <ul style="margin: 8px 0 0 0; padding-left: 18px; font-size: 13px; line-height: 1.8">
        <li>客户保护用于限制哪些业务员可以查看和跟进特定客户，防止撞单和资源冲突。</li>
        <li>保护后的客户只有指定的保护人（业务员）可以报价和创建订单，其他人员无法操作。</li>
        <li>保护期限到期后，客户自动释放为公共资源，所有业务员均可跟进。</li>
        <li>保护人可随时手动解除保护，将客户释放给其他业务员。</li>
      </ul>
    </el-alert>

    <el-table :data="rows" stripe border>
      <el-table-column prop="customerCode" label="客户编码" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="ownerUserId" label="保护人" />
      <el-table-column prop="protectUntil" label="保护至" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '保护中' : '已过期' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { E2CrmService } from '@/api/generated/services/E2CrmService'
import type { Customer } from '@/api/generated/models/Customer'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import { useBaseStore } from '@/stores/_base'

const loading = ref(false)
const rows = ref<(Customer & { protectUntil?: string; active?: boolean })[]>([])
const api = useBaseStore().api

async function reload() {
  loading.value = true
  try {
    const r = await E2CrmService.listCustomers(1, 50, undefined, 'ACTIVE')
    rows.value = parsePageItems(r).items as typeof rows.value
    try {
      const prot = unwrapResult(await api.get('/customers/protection')) as typeof rows.value
      if (Array.isArray(prot) && prot.length) rows.value = prot
    } catch { /* list fallback */ }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.customer-protection { padding: 16px; }
</style>
