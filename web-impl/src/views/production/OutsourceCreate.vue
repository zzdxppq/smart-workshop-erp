<template>
  <ErpPageShell title="委外创建" description="生管提交委外工序（不含厂商选择；厂商由采购在「委外下单」选定）。">
    <el-alert type="warning" :closable="false" title="V1.3.7 红线：生管仅设置自制/委外，不可选择厂商。请采购前往「采购 → 委外下单」。" style="margin-bottom: 12px" />
    <el-button type="primary" plain style="margin-bottom: 12px" @click="$router.push('/sourcing/outsub-order')">
      前往委外下单（采购）
    </el-button>
    <el-form v-loading="submitting" :model="form" label-width="120px">
      <el-form-item label="工单号">
        <el-input v-model="form.workorderNo" placeholder="如 GD-20260615-0001" />
      </el-form-item>
      <el-form-item label="工序号">
        <el-input-number v-model="form.stepNo" :min="1" />
      </el-form-item>
      <el-form-item label="工序名称">
        <el-input v-model="form.processName" />
      </el-form-item>
      <el-form-item label="料号">
        <MaterialSelect v-model="form.materialCode" value-key="materialCode" />
      </el-form-item>
      <el-form-item label="数量">
        <el-input-number v-model="form.qty" :min="1" />
      </el-form-item>
      <el-form-item label="单价">
        <el-input-number v-model="form.unitPrice" :min="0" :step="0.5" />
      </el-form-item>
      <el-form-item label="交期">
        <el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="create">创建</el-button>
      </el-form-item>
    </el-form>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import MaterialSelect from '@/components/form/MaterialSelect.vue'

const router = useRouter()
const submitting = ref(false)
const form = ref({
  workorderNo: '',
  stepNo: 1,
  processName: '',
  materialCode: '',
  qty: 1,
  unitPrice: 0,
  deliveryDate: '2026-06-25',
})

const create = async () => {
  ElMessage.info('生管请先在「工序分配」勾选委外；采购在「委外下单」选厂商并下单')
  router.push('/production/allocation')
}
</script>
