<template>
  <ErpPageShell
    title="无订单采购"
    description="在无销售订单情况下创建采购单，需选择采购理由并由主管审批。"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      v-loading="submitting"
    >
      <el-form-item label="采购理由" prop="purchaseReason">
        <el-select v-model="form.purchaseReason" placeholder="选择采购理由" style="width: 100%">
          <el-option
            v-for="r in reasons"
            :key="r.code"
            :label="r.name"
            :value="r.code"
          >
            <el-tag :type="reasonColorType(r.color)" size="small">{{ r.name }}</el-tag>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="供应商" prop="supplierId">
        <VendorSelect v-model="form.supplierId" placeholder="请选择供应商" />
      </el-form-item>

      <el-form-item label="物料清单" prop="items">
        <el-button type="primary" plain @click="addItem">+ 添加物料</el-button>
        <el-table :data="form.items" border style="margin-top: 12px">
          <el-table-column label="料号" min-width="260">
            <template #default="{ row }">
              <MaterialSelect v-model="row.materialId" placeholder="选择物料" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="预估单价" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.estimatedPrice" :min="0.01" :precision="2" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              ¥{{ (row.quantity * row.estimatedPrice).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="采购理由详细说明..." />
      </el-form-item>

      <el-form-item label="预估总金额">
        <el-tag size="large" type="warning">¥{{ totalAmount.toFixed(2) }}</el-tag>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submit" :loading="submitting">提交</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-dialog v-model="resultDialog.visible" title="创建结果" width="500px">
      <div v-if="resultDialog.data">
        <p>PO 编号：{{ resultDialog.data.poNo }}</p>
        <p>来源：<ErpStatusTag :status="resultDialog.data.sourceType" :label="sourceTypeLabel(resultDialog.data.sourceType)" /></p>
        <p>采购理由：{{ resultDialog.data.purchaseReason }}</p>
        <p>审批路由：<el-tag type="warning">{{ resultDialog.data.approvalRoute }}</el-tag></p>
        <p>预估金额：¥{{ resultDialog.data.estimatedTotal }}</p>
      </div>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import VendorSelect from '@/components/form/VendorSelect.vue'
import MaterialSelect from '@/components/form/MaterialSelect.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { sourceTypeLabel } from '@/utils/statusLabels'
import { useBaseStore } from '@/stores/_base'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import type { NoOrderPurchaseResponse } from '@/api/generated/models/NoOrderPurchaseResponse'

interface PurchaseReason {
  code: string
  name: string
  color: string
}

interface Item {
  materialId?: number
  quantity: number
  estimatedPrice: number
}

const formRef = ref<FormInstance>()
const submitting = ref(false)
const reasonsLoading = ref(false)
const reasons = ref<PurchaseReason[]>([])

const form = ref({
  purchaseReason: '',
  supplierId: undefined as number | undefined,
  remark: '',
  items: [] as Item[],
})

const rules = {
  purchaseReason: [{ required: true, message: '请选择采购理由', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  items: [{ required: true, type: 'array', min: 1, message: '至少 1 行物料' }],
}

const totalAmount = computed(() =>
  form.value.items.reduce((sum, it) => sum + it.quantity * it.estimatedPrice, 0),
)

function reasonColorType(color: string): 'danger' | 'warning' | 'primary' | 'info' {
  switch (color) {
    case 'red': return 'danger'
    case 'orange': return 'warning'
    case 'blue': return 'primary'
    default: return 'info'
  }
}

function addItem() {
  form.value.items.push({ materialId: undefined, quantity: 1, estimatedPrice: 0.01 })
}

async function fetchReasons() {
  reasonsLoading.value = true
  try {
    reasons.value = parsePageItems(await useBaseStore().api.get('/purchase/reasons')).items as typeof reasons.value
  } catch (e: unknown) {
    ElMessage.error(`加载采购理由失败：${(e as { message?: string }).message ?? '未知'}`)
    reasons.value = []
  } finally {
    reasonsLoading.value = false
  }
}

const resultDialog = ref<{
  visible: boolean
  data: NoOrderPurchaseResponse | null
}>({
  visible: false,
  data: null,
})

async function submit() {
  await formRef.value?.validate()
  if (form.value.items.some((it) => !it.materialId)) {
    ElMessage.warning('请为每行选择物料')
    return
  }
  submitting.value = true
  try {
    resultDialog.value.data = unwrapResult<NoOrderPurchaseResponse>(
      await useBaseStore().api.post('/purchase/no-order', form.value),
    )
    resultDialog.value.visible = true
    ElMessage.success('无订单 PO 创建成功')
  } catch (e: unknown) {
    const err = e as { message?: string; response?: { data?: { msg?: string } } }
    ElMessage.error(err?.response?.data?.msg ?? err.message ?? '提交失败')
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.value = { purchaseReason: '', supplierId: undefined, remark: '', items: [] }
  addItem()
}

onMounted(() => {
  fetchReasons()
  addItem()
})
</script>
