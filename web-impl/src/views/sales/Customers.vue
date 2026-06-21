<template>
  <ErpPageShell title="客户档案" description="管理客户基本信息、信用额度与跟进归属。">
    <template #actions>
      <el-button type="primary" @click="showCreate = true">新增客户</el-button>
    </template>

    <el-form :inline="true" class="filter-form">
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" @change="reload">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
          <el-option label="黑名单" value="BLACKLIST" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="customers" stripe border>
      <el-table-column prop="customerCode" label="客户编码" min-width="120" />
      <el-table-column prop="name" label="客户名称" min-width="160" />
      <el-table-column prop="contactName" label="联系人" min-width="100" />
      <el-table-column prop="contactPhone" label="联系电话" min-width="120" />
      <el-table-column prop="contactEmail" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column prop="industry" label="行业" min-width="100" />
      <el-table-column prop="creditLimit" label="信用额度" width="120" />
      <el-table-column prop="ownerId" label="创建人" width="100">
        <template #default="{ row }">
          {{ row.ownerId ? '用户' + row.ownerId : '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          {{ customerStatusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" :disabled="!row.id" @click="viewDetail(row.id)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      @current-change="reload"
      @size-change="reload"
    />

    <el-dialog v-model="showCreate" title="新增客户" width="520px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="客户名称" required>
          <el-input v-model="createForm.name" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="客户编码">
          <el-input v-model="createForm.customerCode" placeholder="留空则自动生成" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="createForm.industry" placeholder="如：机械制造" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="createForm.contactName" placeholder="客户对接人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="createForm.contactPhone" placeholder="手机或座机" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="createForm.contactEmail" placeholder="报价 PDF 将发送至此邮箱" />
        </el-form-item>
        <el-form-item label="信用额度">
          <el-input-number v-model="createForm.creditLimit" :min="0" :step="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { E2CrmService } from '@/api/generated/services/E2CrmService'
import { useBaseStore } from '@/stores/_base'
import type { Customer } from '@/api/generated/models/Customer'
import { useMasterData } from '@/composables/useMasterData'
import { parsePageItems } from '@/utils/apiPage'
import { customerStatusLabel } from '@/utils/statusLabels'

const router = useRouter()
const { invalidateCustomersCache } = useMasterData()
const customers = ref<Customer[]>([])
const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const status = ref<string>()
const createForm = ref({
  name: '',
  customerCode: '',
  industry: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  creditLimit: 0,
  status: 'ACTIVE',
})
async function reload() {
  loading.value = true
  try {
    const r = await E2CrmService.listCustomers(pageNum.value, pageSize.value, undefined, status.value)
    const { items, total: t } = parsePageItems(r)
    customers.value = items as Customer[]
    total.value = t
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    customers.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请填写客户名称')
    return
  }
  creating.value = true
  try {
    const api = useBaseStore().api
    await api.post('/customers', {
      name: createForm.value.name.trim(),
      customerCode: createForm.value.customerCode.trim() || undefined,
      industry: createForm.value.industry || undefined,
      contactName: createForm.value.contactName.trim() || undefined,
      contactPhone: createForm.value.contactPhone.trim() || undefined,
      contactEmail: createForm.value.contactEmail.trim() || undefined,
      creditLimit: createForm.value.creditLimit,
      status: createForm.value.status,
    })
    ElMessage.success('客户创建成功')
    invalidateCustomersCache()
    showCreate.value = false
    createForm.value = {
      name: '', customerCode: '', industry: '', contactName: '', contactPhone: '',
      contactEmail: '', creditLimit: 0, status: 'ACTIVE',
    }
    await reload()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function viewDetail(id?: number) {
  if (id != null) router.push(`/sales/customers/${id}`)
}

onMounted(reload)
</script>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
}
</style>
