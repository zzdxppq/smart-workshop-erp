<template>
  <div v-loading="loading" class="barcode-generate">
    <h2>物料条码生成</h2>
    <el-card>
      <el-form :model="form" label-width="120px">
        <el-form-item label="料号">
          <el-select v-model="form.materialCode" placeholder="选择物料">
            <el-option v-for="m in materials" :key="m.materialCode" :label="`${m.materialCode} - ${m.materialName}`" :value="m.materialCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.qty" :min="1" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="form.batchNo" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="generate">生成条码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMaterialStore } from '@/stores/material'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

const materialStore = useMaterialStore()
const form = ref({ materialCode: '', qty: 1, batchNo: '' })
const materials = ref<{ materialCode: string; materialName?: string }[]>([])
const loading = ref(false)
const submitting = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    materials.value = parsePageItems(await materialStore.listMaterials()).items as { materialCode: string; materialName?: string }[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '物料加载失败')
    materials.value = []
  } finally {
    loading.value = false
  }
})

const generate = async () => {
  if (!form.value.materialCode) {
    ElMessage.warning('请选择物料')
    return
  }
  submitting.value = true
  try {
    const created = unwrapResult<{ barcodeNo?: string }>(await materialStore.generateBarcode(form.value))
    ElMessage.success(`生成成功：${created.barcodeNo ?? ''}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '生成失败')
  } finally {
    submitting.value = false
  }
}
</script>
