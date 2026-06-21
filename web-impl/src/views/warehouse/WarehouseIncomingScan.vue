<template>
  <div v-loading="loading">
    <h2>仓库到货扫码</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="权限单号"><el-input v-model="meta.permissionNo" placeholder="WP..." /></el-form-item>
      <el-form-item label="委外单号"><el-input v-model="meta.outsourceNo" /></el-form-item>
      <el-form-item label="厂商"><el-input v-model="meta.vendorName" /></el-form-item>
    </el-form>
    <el-form :inline="true">
      <el-form-item label="条码"><el-input v-model="barcode" @keyup.enter="doScan" /></el-form-item>
      <el-form-item label="数量"><el-input-number v-model="qty" :min="1" /></el-form-item>
      <el-form-item><el-button type="primary" @click="doScan">扫码入库</el-button></el-form-item>
    </el-form>
    <h3>本次到货 ({{ count }} 笔 · 共 {{ qtyTotal }} 件)</h3>
    <el-table :data="scans" stripe>
      <el-table-column prop="barcode" label="条码" />
      <el-table-column prop="qty" label="数量" />
      <el-table-column prop="at" label="时间" />
    </el-table>
    <el-button style="margin-top: 12px" type="success" :loading="loading" @click="submitAll">提交入库</el-button>
    <el-button @click="clear">清空</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { useWarehouseIncomingScan } from '@/composables/useWarehouseIncomingScan'
import { unwrapResult } from '@/utils/apiPage'

const barcode = ref('')
const qty = ref(1)
const loading = ref(false)
const meta = ref({ permissionNo: '', outsourceNo: '', vendorName: '' })
const { scans, count, qtyTotal, addScan, clear } = useWarehouseIncomingScan()

function doScan() {
  if (!barcode.value) return
  addScan(barcode.value, qty.value)
  barcode.value = ''
  qty.value = 1
}

async function submitAll() {
  if (!count.value) {
    ElMessage.warning('请先扫码')
    return
  }
  if (!meta.value.permissionNo || !meta.value.outsourceNo || !meta.value.vendorName) {
    ElMessage.warning('请填写权限单号、委外单号与厂商')
    return
  }
  loading.value = true
  try {
    const items = scans.value.map((s, i) => ({
      itemNo: String(i + 1),
      barcode: s.barcode,
      barcodeType: 'WL',
      quantity: s.qty,
    }))
    const created = unwrapResult<{ scanNo?: string }>(
      await useBaseStore().api.post('/warehouse/incoming-scan/create', items, {
        params: {
          permissionNo: meta.value.permissionNo,
          userId: 1,
          vendorName: meta.value.vendorName,
          outsourceNo: meta.value.outsourceNo,
          email: '',
        },
      }),
    )
    ElMessage.success(`已提交 ${count.value} 笔到货扫码${created.scanNo ? `：${created.scanNo}` : ''}`)
    clear()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>
