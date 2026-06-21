<template>
  <ErpPageShell title="补录工单" description="例外补录入口；正常流程请从「待转产订单」由销售订单转工单。">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="主路径：销售订单 CONFIRMED → 生产 · 待转产订单 · 转工单（AC-5.1.1）"
      style="margin-bottom: 16px"
    />
    <el-form v-loading="submitting" :model="form" label-width="120px">
      <el-form-item label="图号">
        <DrawingPicker v-model="form.drawingNo" :allow-upload="false" style="max-width: 420px" @select="onDrawingSelect" />
      </el-form-item>
      <el-form-item label="料号">
        <MaterialSelect v-model="form.materialCode" value-key="materialCode" @change="onMaterialChange" />
      </el-form-item>
      <el-form-item label="产品名称">
        <el-input v-model="form.productName" />
      </el-form-item>
      <el-form-item label="数量">
        <el-input-number v-model="form.qty" :min="1" />
      </el-form-item>
      <el-form-item label="优先级">
        <el-input-number v-model="form.priority" :min="1" :max="10" />
      </el-form-item>
      <el-form-item label="机台类型">
        <el-select v-model="form.equipmentType" placeholder="选择机台类型" clearable style="width: 100%">
          <el-option label="CNC 加工中心" value="CNC" />
          <el-option label="车床 LATHE" value="LATHE" />
          <el-option label="铣床 MILLING" value="MILLING" />
        </el-select>
      </el-form-item>
      <el-form-item label="预计工时">
        <el-input-number v-model="form.estimatedHours" :min="0" :step="0.5" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="create">创建</el-button>
      </el-form-item>
    </el-form>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import MaterialSelect from '@/components/form/MaterialSelect.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import type { Drawing } from '@/api/generated/models/Drawing'
import { useWorkorderStore } from '@/stores/workorder'
import { unwrapResult } from '@/utils/apiPage'
import type { MaterialOption } from '@/composables/useMasterData'

const workorderStore = useWorkorderStore()
const submitting = ref(false)
const form = ref({
  drawingNo: '',
  materialCode: '',
  productName: '',
  qty: 1,
  priority: 5,
  equipmentType: '',
  estimatedHours: 0,
})

function onDrawingSelect(d: Drawing) {
  form.value.drawingNo = d.drawingNo ?? ''
  if (d.materialCode) form.value.materialCode = d.materialCode
  if (d.title && !form.value.productName) form.value.productName = d.title
}

function onMaterialChange(m?: MaterialOption) {
  if (m?.materialName && !form.value.productName) {
    form.value.productName = m.materialName
  }
}

const create = async () => {
  if (!form.value.materialCode) {
    ElMessage.warning('请选择物料')
    return
  }
  submitting.value = true
  try {
    const created = unwrapResult<{ workorderNo?: string }>(await workorderStore.createWorkorder(form.value))
    ElMessage.success(`工单已创建：${created.workorderNo ?? ''}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>
