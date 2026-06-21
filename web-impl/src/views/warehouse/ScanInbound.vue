<template>
  <div v-loading="loading" class="scan-page">
    <h2>扫码入库</h2>
    <ScanTrigger v-model="form.barcodeNo" @scan="onScan" />
    <el-form :model="form" label-width="120px" style="margin-top: 16px">
      <el-form-item label="库位">
        <el-input v-model="form.locationCode" />
      </el-form-item>
      <el-form-item label="数量">
        <el-input-number v-model="form.qty" :min="1" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交入库</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useWarehouseStore } from '@/stores/warehouse'

const warehouseStore = useWarehouseStore()
const loading = ref(false)
const form = ref({ barcodeNo: '', locationCode: '', qty: 1 })

function onScan(code: string) {
  form.value.barcodeNo = code
  if (code.toUpperCase().startsWith('WL-')) submit()
}

const submit = async () => {
  if (!form.value.barcodeNo) {
    ElMessage.warning('请先扫描料号 WL-')
    return
  }
  loading.value = true
  try {
    await warehouseStore.scanInbound(form.value)
    ElMessage.success('入库成功')
    form.value = { barcodeNo: '', locationCode: form.value.locationCode, qty: 1 }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '入库失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.scan-page { padding: 16px; max-width: 640px; }
</style>
