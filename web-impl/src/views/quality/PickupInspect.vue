<template>
  <div v-loading="loading">
    <h2>提货检验</h2>
    <el-card v-if="pickup">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="提货单号">{{ pickup.pickupNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ pickup.customer }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ pickup.qty }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ pickup.result }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <h3 style="margin-top: 16px">检验明细</h3>
    <el-table :data="pickup?.items || []" stripe border>
      <el-table-column prop="itemName" label="项目" />
      <el-table-column prop="result" label="结果">
        <template #default="{ row }">
          <el-radio-group v-model="row.result">
            <el-radio-button value="PASS">合格</el-radio-button>
            <el-radio-button value="FAIL">不合格</el-radio-button>
          </el-radio-group>
        </template>
      </el-table-column>
    </el-table>
    <el-button style="margin-top: 16px" type="primary" :loading="submitting" @click="submit">提交检验</el-button>
    <el-button @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'

const router = useRouter()
const qualityStore = useQualityStore()
const submitting = ref(false)

const { data: pickup, loading, id } = useDetailLoad<any>((pid) => qualityStore.getPickup(pid))

async function submit() {
  submitting.value = true
  try {
    await qualityStore.inspectPickup(id(), { items: pickup.value?.items || [] })
    ElMessage.success('检验结果已提交')
    router.push('/quality/pickup')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
