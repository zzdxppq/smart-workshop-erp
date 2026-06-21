<template>
  <ErpPageShell title="登记到货" description="选择采购单与物料，登记到货数量。">
    <el-form :model="form" label-width="120px">
      <el-form-item label="采购单" required>
        <PoSelect v-model="form.poId" @change="onPoChange" />
      </el-form-item>
      <el-form-item label="料号" required>
        <MaterialSelect v-model="form.materialCode" value-key="materialCode" />
      </el-form-item>
      <el-form-item label="数量" required>
        <el-input-number v-model="form.qty" :min="1" />
      </el-form-item>
      <el-form-item label="到货时间">
        <el-date-picker v-model="form.arrivedAt" type="datetime" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">登记</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import PoSelect from '@/components/form/PoSelect.vue'
import MaterialSelect from '@/components/form/MaterialSelect.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { unwrapResult } from '@/utils/apiPage'
import type { PoOption } from '@/composables/useMasterData'

const router = useRouter()
const sourcingStore = useSourcingStore()
const form = ref({
  poId: undefined as number | undefined,
  poNo: '',
  materialCode: '',
  qty: 1,
  arrivedAt: new Date(),
})

function onPoChange(po?: PoOption) {
  form.value.poNo = po?.poNo ?? ''
}

async function submit() {
  if (!form.value.poNo || !form.value.materialCode) {
    ElMessage.error('请选择采购单和物料')
    return
  }
  try {
    await sourcingStore.createIncoming({
      poNo: form.value.poNo,
      materialCode: form.value.materialCode,
      qty: form.value.qty,
      arrivedAt: form.value.arrivedAt instanceof Date
        ? form.value.arrivedAt.toISOString()
        : form.value.arrivedAt,
    })
    ElMessage.success('到货登记成功')
    router.push('/sourcing/incoming')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '登记失败')
  }
}
</script>
