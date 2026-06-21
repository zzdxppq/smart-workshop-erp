<template>
  <div v-loading="loading">
    <h2>应收单详情</h2>
    <el-card v-if="recv">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="应收单号">{{ recv.receivableNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ recv.customerName }}</el-descriptions-item>
        <el-descriptions-item label="销售订单号">{{ recv.orderNo ?? recv.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="订单总额">
          <MoneyAmount :model-value="Number(recv.totalAmount ?? 0)" display-only />
        </el-descriptions-item>
        <el-descriptions-item label="已收">
          <MoneyAmount :model-value="Number(recv.paidAmount ?? 0)" display-only />
        </el-descriptions-item>
        <el-descriptions-item label="未收">
          <MoneyAmount :model-value="Number(recv.unpaidAmount ?? 0)" display-only />
        </el-descriptions-item>
        <el-descriptions-item label="到期日">{{ recv.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="recv.status" /></el-descriptions-item>
      </el-descriptions>
    </el-card>

    <h3 style="margin-top: 16px">收款记录</h3>
    <el-table :data="recv?.receipts || []" stripe border>
      <el-table-column prop="receiptNo" label="收款单号" min-width="140" />
      <el-table-column label="金额" width="140" align="right">
        <template #default="{ row }">
          <MoneyAmount :model-value="Number(row.amount ?? 0)" display-only />
        </template>
      </el-table-column>
      <el-table-column prop="receivedAt" label="收款时间" width="170" />
      <el-table-column prop="method" label="收款方式" width="120" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
    </el-table>

    <div style="margin-top: 16px">
      <el-button
        v-if="canRecord"
        type="primary"
        :loading="submitting"
        @click="openDialog"
      >登记收款</el-button>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-dialog v-model="dialogVisible" title="登记收款" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="实收日" required>
          <el-date-picker
            v-model="form.paidDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFutureDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实收金额" required>
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :max="maxAmount"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="form.method" style="width: 100%">
            <el-option label="银行转账" value="BANK" />
            <el-option label="承兑汇票" value="CHECK" />
            <el-option label="现金" value="CASH" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReceipt">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useFinanceStore } from '@/stores/finance'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import MoneyAmount from '@/components/erp/MoneyAmount.vue'

const financeStore = useFinanceStore()
const submitting = ref(false)
const dialogVisible = ref(false)
const { data: recv, loading, load, id } = useDetailLoad<any>((rid) => financeStore.getReceivable(rid))

const form = ref({
  paidDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  method: 'BANK',
  remark: '',
})

const maxAmount = computed(() => Number(recv.value?.unpaidAmount ?? 0))
const canRecord = computed(() => maxAmount.value > 0)

function disableFutureDate(date: Date) {
  return date.getTime() > Date.now()
}

function openDialog() {
  form.value = {
    paidDate: new Date().toISOString().slice(0, 10),
    amount: maxAmount.value,
    method: 'BANK',
    remark: '',
  }
  dialogVisible.value = true
}

async function submitReceipt() {
  if (!form.value.paidDate || !form.value.amount || form.value.amount <= 0) {
    ElMessage.warning('请填写实收日与实收金额')
    return
  }
  if (form.value.amount > maxAmount.value) {
    ElMessage.warning('实收金额不能大于未收金额')
    return
  }
  submitting.value = true
  try {
    await financeStore.recordReceivableReceipt(id(), {
      amount: form.value.amount,
      method: form.value.method,
      paidDate: form.value.paidDate,
      remark: form.value.remark || undefined,
    })
    ElMessage.success('收款已登记')
    dialogVisible.value = false
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '登记失败')
  } finally {
    submitting.value = false
  }
}
</script>
