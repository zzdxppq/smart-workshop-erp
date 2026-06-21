<template>
  <div v-loading="loading" class="order-change-panel">
    <el-form :model="form" label-width="100px">
      <el-form-item label="变更原因">
        <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明变更原因" />
      </el-form-item>
      <el-form-item label="新交货日">
        <el-date-picker
          v-model="form.deliveryDate"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="金额调整">
        <MoneyAmount v-model="form.amountDelta" />
      </el-form-item>
    </el-form>
    <div v-if="showActions" class="order-change-panel__actions">
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交变更</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const props = withDefaults(defineProps<{
  orderId: number | string
  showActions?: boolean
}>(), {
  showActions: true,
})

const emit = defineEmits<{
  success: []
  cancel: []
}>()

const api = useBaseStore().api
const loading = ref(false)
const submitting = ref(false)
const form = ref({ reason: '', deliveryDate: '', amountDelta: 0 })

async function loadOrder() {
  if (!props.orderId) return
  loading.value = true
  try {
    const o = unwrapResult(await api.get(`/orders/${props.orderId}`))
    form.value.deliveryDate = (o as { deliveryDate?: string })?.deliveryDate ?? ''
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!props.orderId) return
  submitting.value = true
  try {
    await api.post(`/orders/${props.orderId}/change`, form.value)
    ElMessage.success('变更已提交')
    emit('success')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

watch(() => props.orderId, () => {
  form.value = { reason: '', deliveryDate: '', amountDelta: 0 }
  loadOrder()
}, { immediate: true })

defineExpose({ submit })
</script>

<style scoped>
.order-change-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
