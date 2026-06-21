<template>
  <div>
    <h2>RFQ 定标</h2>
    <el-form :model="form" label-width="120px">
      <el-form-item label="询价单号">
        <el-input v-model="form.rfqNo" disabled />
      </el-form-item>
      <el-form-item label="中标供应商" required>
        <el-input v-model="form.winnerName" />
      </el-form-item>
      <el-form-item label="定标金额">
        <el-input-number v-model="form.amount" :step="0.1" />
      </el-form-item>
      <el-form-item label="定标理由">
        <el-input v-model="form.reason" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">确认定标</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSourcingStore } from '@/stores/sourcing'

const route = useRoute()
const router = useRouter()
const sourcingStore = useSourcingStore()
const form = ref({ rfqNo: '', winnerName: '', amount: 0, reason: '' })

onMounted(() => {
  form.value.rfqNo = String(route.params.id || '')
})

async function submit() {
  try {
    await sourcingStore.awardRfq(Number(route.params.id), form.value)
    ElMessage.success('定标成功')
    router.push(`/sourcing/rfq-detail/${route.params.id}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '定标失败')
  }
}
</script>