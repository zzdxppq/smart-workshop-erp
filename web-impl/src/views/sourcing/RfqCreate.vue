<template>
  <ErpPageShell title="新建询价单" description="须绑定采购申请（PR）或委外待办；期望回复日期仅供内部追踪。">
    <el-form :model="form" label-width="120px">
      <el-form-item label="询价类型" required>
        <el-radio-group v-model="form.inquirySourceType">
          <el-radio value="MATERIAL">物料采购询价</el-radio>
          <el-radio value="OUTSOURCE">委外询价</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.inquirySourceType === 'MATERIAL'" label="来源 PR" required>
        <el-select
          v-model="form.prId"
          filterable
          placeholder="选择待处理采购申请"
          style="width: 100%"
          @change="onPrSelect"
        >
          <el-option
            v-for="pr in pendingPrs"
            :key="String(pr.id)"
            :label="`${pr.prNo} · ${pr.materialCode} · 余${pr.remainingQty}`"
            :value="pr.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.inquirySourceType === 'OUTSOURCE'" label="待委外工序" required>
        <el-select
          v-model="form.allocationId"
          filterable
          placeholder="选择生管推送的待委外工序"
          style="width: 100%"
          @change="onAllocationSelect"
        >
          <el-option
            v-for="a in pendingAllocations"
            :key="String(a.id)"
            :label="`${a.workorderNo ?? '—'} · ${a.processName ?? '工序'} #${a.processSeq} · ${a.productCode ?? ''}`"
            :value="a.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="料号" required>
        <MaterialSelect v-model="form.materialCode" value-key="materialCode" placeholder="请选择物料" @change="onMaterial" />
      </el-form-item>
      <el-form-item label="数量" required>
        <el-input-number v-model="form.qty" :min="1" />
      </el-form-item>
      <el-form-item label="目标价">
        <el-input-number v-model="form.targetPrice" :step="0.1" />
      </el-form-item>
      <el-form-item label="期望回复日期">
        <el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" placeholder="内部追踪用，非系统截止" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.note" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">创建</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import MaterialSelect from '@/components/form/MaterialSelect.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { E5AllocationService } from '@/api/generated/services/E5AllocationService'
import { unwrapResult, parsePageItems } from '@/utils/apiPage'

type PendingAllocation = {
  id?: number
  workorderNo?: string
  productCode?: string
  processName?: string
  processSeq?: number
}

const router = useRouter()
const sourcingStore = useSourcingStore()
const pendingPrs = ref<Record<string, unknown>[]>([])
const pendingAllocations = ref<PendingAllocation[]>([])
const form = ref({
  inquirySourceType: 'MATERIAL',
  prId: undefined as number | undefined,
  prNo: '',
  workorderNo: '',
  allocationId: undefined as number | undefined,
  processStepNo: undefined as number | undefined,
  materialCode: '',
  materialId: undefined as number | undefined,
  materialName: '',
  qty: 100,
  targetPrice: 0,
  deadline: '',
  note: '',
})

async function loadPendingPrs() {
  const pending = parsePageItems(await sourcingStore.listPurchaseRequests({ status: 'PENDING', pageSize: 100 }))
  const partial = parsePageItems(await sourcingStore.listPurchaseRequests({ status: 'PARTIAL', pageSize: 100 }))
  pendingPrs.value = [...(pending.items as Record<string, unknown>[]), ...(partial.items as Record<string, unknown>[])]
}

async function loadPendingAllocations() {
  const { items } = parsePageItems(await E5AllocationService.pendingAllocations())
  pendingAllocations.value = items as PendingAllocation[]
}

function onPrSelect(id: number) {
  const pr = pendingPrs.value.find((p) => p.id === id)
  if (!pr) return
  form.value.prNo = String(pr.prNo ?? '')
  form.value.workorderNo = String(pr.workorderNo ?? '')
  form.value.materialCode = String(pr.materialCode ?? '')
  form.value.materialId = pr.materialId as number | undefined
  form.value.materialName = String(pr.materialName ?? '')
  form.value.qty = Number(pr.remainingQty ?? pr.requiredQty ?? 100)
}

function onAllocationSelect(id: number) {
  const a = pendingAllocations.value.find((p) => p.id === id)
  if (!a) return
  form.value.workorderNo = String(a.workorderNo ?? '')
  form.value.processStepNo = a.processSeq
  if (a.productCode) {
    form.value.materialCode = a.productCode
  }
}

function onMaterial(m: { id?: number; materialCode?: string; materialName?: string } | null) {
  if (!m) return
  form.value.materialId = m.id
  form.value.materialName = m.materialName ?? ''
}

watch(() => form.value.inquirySourceType, (t) => {
  if (t === 'OUTSOURCE') {
    form.value.prId = undefined
    form.value.prNo = ''
    void loadPendingAllocations()
  } else {
    form.value.allocationId = undefined
    form.value.processStepNo = undefined
  }
})

async function submit() {
  if (!form.value.materialCode || !form.value.materialId) {
    ElMessage.error('请选择物料')
    return
  }
  if (form.value.inquirySourceType === 'MATERIAL' && !form.value.prId) {
    ElMessage.error('物料采购询价须绑定待处理采购申请（PR）')
    return
  }
  if (form.value.inquirySourceType === 'OUTSOURCE' && !form.value.allocationId) {
    ElMessage.error('委外询价须绑定待委外工序')
    return
  }
  try {
    const created = unwrapResult<{ rfqNo?: string }>(
      await sourcingStore.createRfq({
        title: `询价-${form.value.materialCode}`,
        materialId: form.value.materialId,
        materialCode: form.value.materialCode,
        materialName: form.value.materialName,
        qty: form.value.qty,
        budgetAmount: form.value.targetPrice * form.value.qty,
        requiredDate: form.value.deadline || undefined,
        inquirySourceType: form.value.inquirySourceType,
        prId: form.value.prId,
        prNo: form.value.prNo,
        workorderNo: form.value.workorderNo,
        allocationId: form.value.allocationId,
        processStepNo: form.value.processStepNo,
      }),
    )
    ElMessage.success(`询价单已创建：${created.rfqNo ?? ''}`)
    router.push('/sourcing/rfq')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  }
}

onMounted(() => {
  void loadPendingPrs()
})
</script>
