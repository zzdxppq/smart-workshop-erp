<template>
  <div v-loading="loading">
    <h2>对账单详情</h2>
    <el-alert type="warning" :closable="false" title="四步流程：建单 → 发送邮件 → 上传签字 → 财务确认（不含线下操作按钮）" style="margin-bottom: 12px" />

    <el-card v-if="reconcile">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="对账单号">{{ reconcile.reconcileNo }}</el-descriptions-item>
        <el-descriptions-item label="期间">{{ reconcile.period }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ reconcile.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="对账金额">{{ reconcile.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="当前步骤">{{ stepLabel(reconcile.step) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ reconcile.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 16px">
        <ApprovalChainRenderer :nodes="flowNodes" />
      </div>

      <div style="margin-top: 16px; display: flex; flex-wrap: wrap; gap: 8px">
        <el-button v-if="reconcile.step === 'CREATE'" type="primary" :loading="acting" disabled title="请先在列表页发送对账邮件">
          发送对账邮件（详情页待接）
        </el-button>
        <el-button v-if="canVendorConfirm" type="warning" :loading="acting" @click="vendorConfirm">
          厂商确认
        </el-button>
        <el-button v-if="canFinanceAudit" type="success" :loading="acting" @click="financeAudit">
          财务审核
        </el-button>
        <el-button type="primary" plain @click="$router.push(`/sourcing/reconcile-signature/${id()}`)">
          上传签字扫描件
        </el-button>
      </div>
    </el-card>

    <h3 style="margin-top: 16px">明细行</h3>
    <el-table :data="reconcile?.items || []" stripe border>
      <el-table-column prop="poNo" label="采购单号" />
      <el-table-column prop="materialCode" label="料号" />
      <el-table-column prop="qty" label="数量" />
      <el-table-column prop="unitPrice" label="单价" />
      <el-table-column prop="amount" label="金额" />
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useSourcingStore } from '@/stores/sourcing'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ApprovalChainRenderer from '@/components/erp/ApprovalChainRenderer.vue'
import type { ApprovalNode } from '@/components/erp/ApprovalChainRenderer.vue'

const sourcingStore = useSourcingStore()
const acting = ref(false)
const { data: reconcile, loading, load, id } = useDetailLoad<any>((rid) => sourcingStore.getReconcile(rid))

function stepLabel(s: string) {
  return ({ CREATE: '建单', VENDOR_CONFIRM: '厂商确认', FINANCE_AUDIT: '财务审核', SIGN: '双方签' } as Record<string, string>)[s] || s
}

const flowNodes = computed<ApprovalNode[]>(() => [
  { title: '建单', status: reconcile.value?.step === 'CREATE' ? 'PENDING' : 'APPROVED' },
  { title: '厂商确认', status: reconcile.value?.vendorSigned ? 'APPROVED' : 'PENDING' },
  { title: '财务审核', status: reconcile.value?.financeSigned ? 'APPROVED' : 'PENDING' },
  { title: '双方签', status: reconcile.value?.step === 'SIGN' ? 'APPROVED' : 'PENDING' },
])

const canVendorConfirm = computed(() => reconcile.value?.step === 'CREATE' || reconcile.value?.step === 'VENDOR_CONFIRM')
const canFinanceAudit = computed(() => reconcile.value?.step === 'VENDOR_CONFIRM' && reconcile.value?.vendorSigned)

async function vendorConfirm() {
  acting.value = true
  try {
    await sourcingStore.vendorConfirmReconcile(id(), { confirmedAmount: reconcile.value?.totalAmount })
    ElMessage.success('厂商已确认')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '确认失败')
  } finally {
    acting.value = false
  }
}

async function financeAudit() {
  acting.value = true
  try {
    await sourcingStore.financeConfirmReconcile(id())
    ElMessage.success('财务审核完成')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '审核失败')
  } finally {
    acting.value = false
  }
}
</script>
