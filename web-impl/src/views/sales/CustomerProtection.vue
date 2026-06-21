<template>
  <div v-loading="loading" class="customer-protection">
    <h2>客户保护管理</h2>
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
