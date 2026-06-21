<template>
  <div v-loading="loading" class="scan-page">
    <h2>扫码中心</h2>
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="扫码入库" name="inbound">
          <el-form :model="inboundForm" label-width="120px">
            <el-form-item label="条码号">
              <el-input v-model="inboundForm.barcodeNo" placeholder="BC{yyyyMMdd}{seq:4}" />
            </el-form-item>
            <el-form-item label="库位">
              <el-select v-model="inboundForm.locationCode" placeholder="选择库位">
                <el-option v-for="l in locations" :key="l.locationCode" :label="l.locationCode" :value="l.locationCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="inboundForm.qty" :min="1" />
            </el-form-item>
            <el-form-item label="批次号">
              <el-input v-model="inboundForm.batchNo" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doInbound">扫码入库</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="扫码出库" name="outbound">
          <el-form :model="outboundForm" label-width="120px">
            <el-form-item label="条码号">
              <el-input v-model="outboundForm.barcodeNo" />
            </el-form-item>
            <el-form-item label="工单号">
              <el-input v-model="outboundForm.workorderNo" />
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="outboundForm.qty" :min="1" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doOutbound">扫码出库</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useWarehouseStore } from '@/stores/warehouse'
import { parsePageItems } from '@/utils/apiPage'

const activeTab = ref('inbound')
const warehouseStore = useWarehouseStore()
const loading = ref(false)

const inboundForm = ref({ barcodeNo: '', locationCode: '', qty: 1, batchNo: '' })
const outboundForm = ref({ barcodeNo: '', workorderNo: '', qty: 1 })
const locations = ref<{ locationCode: string }[]>([])

onMounted(async () => {
  loading.value = true
  try {
    locations.value = parsePageItems(await warehouseStore.listLocations()).items as { locationCode: string }[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '库位加载失败')
    locations.value = []
  } finally {
    loading.value = false
  }
})

const doInbound = async () => {
  try {
    await warehouseStore.scanInbound(inboundForm.value)
    ElMessage.success('入库成功')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '入库失败')
  }
}

const doOutbound = async () => {
  try {
    await warehouseStore.scanOutbound(outboundForm.value)
    ElMessage.success('出库成功（触发 1.17 MRP 钩子）')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '出库失败')
  }
}
</script>
