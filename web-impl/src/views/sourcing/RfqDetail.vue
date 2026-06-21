<template>
  <div v-loading="loading">
    <div class="head">
      <h2>询价单详情</h2>
      <el-button
        v-if="showConvertPo"
        type="primary"
        :loading="converting"
        @click="openConvertPo"
      >
        转采购单
      </el-button>
      <el-button
        v-if="showConvertOutsource"
        type="primary"
        :loading="converting"
        @click="openConvertOutsource"
      >
        转委外单
      </el-button>
    </div>
    <el-card v-if="rfq">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="询价单号">{{ rfq.rfqNo }}</el-descriptions-item>
        <el-descriptions-item label="询价类型">{{ sourceTypeLabel }}</el-descriptions-item>
        <el-descriptions-item v-if="rfq.inquirySourceType !== 'OUTSOURCE'" label="来源 PR">{{ rfq.prNo || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="rfq.inquirySourceType === 'OUTSOURCE'" label="待委外工序">#{{ rfq.processStepNo ?? '—' }} · ID {{ rfq.allocationId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="关联工单">{{ rfq.workorderNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ rfq.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ rfq.qty }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="rfq.status" /></el-descriptions-item>
        <el-descriptions-item label="期望回复日期">{{ rfq.expectedReplyDate || rfq.deadline || '—' }}</el-descriptions-item>
        <el-descriptions-item label="转单状态">{{ convertLabel }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>报价录入（收到厂商回传后补录，非必填）</template>
      <el-form :inline="true" :model="quoteForm">
        <el-form-item label="供应商">
          <el-input v-model="quoteForm.vendorName" placeholder="厂商名称" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="quoteForm.unitPrice" :min="0" :step="0.01" />
        </el-form-item>
        <el-form-item label="交期(天)">
          <el-input-number v-model="quoteForm.leadTimeDays" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="quoting" @click="submitQuote">录入报价</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <h3 style="margin-top: 16px">已录入报价</h3>
    <el-table :data="rfq?.quotes || []" stripe border>
      <el-table-column prop="vendorName" label="供应商" />
      <el-table-column prop="unitPrice" label="单价" />
      <el-table-column prop="totalPrice" label="总价" />
      <el-table-column prop="leadTime" label="交期(天)" />
      <el-table-column prop="quality" label="质量等级" />
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>

    <el-dialog v-model="convertPoVisible" title="转采购单确认" width="480px">
      <p>即将根据定标结果生成采购订单，并扣减采购申请 <strong>{{ rfq?.prNo }}</strong> 数量。</p>
      <p v-if="rfq?.workorderNo">关联工单：<strong>{{ rfq.workorderNo }}</strong></p>
      <template #footer>
        <el-button @click="convertPoVisible = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="confirmConvertPo">确认转单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="convertOutsourceVisible" title="转委外单确认" width="480px">
      <p>即将根据定标结果生成委外单（WW-），工序归属不可修改。</p>
      <p v-if="rfq?.workorderNo">关联工单：<strong>{{ rfq.workorderNo }}</strong></p>
      <p v-if="rfq?.allocationId">待委外工序 ID：<strong>{{ rfq.allocationId }}</strong></p>
      <template #footer>
        <el-button @click="convertOutsourceVisible = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="confirmConvertOutsource">确认转委外单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSourcingStore } from '@/stores/sourcing'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { unwrapResult } from '@/utils/apiPage'

const route = useRoute()
const router = useRouter()
const sourcingStore = useSourcingStore()
const { data: rfq, loading, load } = useDetailLoad<any>((id) => sourcingStore.getRfq(id))

const quoteForm = ref({ vendorName: '', unitPrice: 0, leadTimeDays: 7 })
const quoting = ref(false)
const converting = ref(false)
const convertPoVisible = ref(false)
const convertOutsourceVisible = ref(false)

const sourceTypeLabel = computed(() => {
  const t = rfq.value?.inquirySourceType
  if (t === 'OUTSOURCE') return '委外询价'
  if (t === 'NO_ORDER') return '无订单采购'
  return '物料采购询价'
})

const convertLabel = computed(() => {
  if (!rfq.value) return '—'
  if (rfq.value.convertStatus === 'PO_CONVERTED') return `已转 PO ${rfq.value.convertedOrderNo ?? ''}`
  if (rfq.value.convertStatus === 'OUTSOURCE_CONVERTED') return `已转委外 ${rfq.value.convertedOrderNo ?? ''}`
  return '未转单'
})

const showConvertPo = computed(() =>
  rfq.value?.status === 'AWARDED'
  && rfq.value?.convertStatus !== 'PO_CONVERTED'
  && rfq.value?.convertStatus !== 'OUTSOURCE_CONVERTED'
  && rfq.value?.inquirySourceType !== 'OUTSOURCE',
)

const showConvertOutsource = computed(() =>
  rfq.value?.status === 'AWARDED'
  && rfq.value?.convertStatus !== 'OUTSOURCE_CONVERTED'
  && rfq.value?.convertStatus !== 'PO_CONVERTED'
  && rfq.value?.inquirySourceType === 'OUTSOURCE',
)

async function submitQuote() {
  if (!quoteForm.value.vendorName.trim()) {
    ElMessage.warning('请填写供应商名称')
    return
  }
  quoting.value = true
  try {
    const qty = Number(rfq.value?.qty ?? 1)
    await sourcingStore.quoteRfq(Number(route.params.id), {
      vendorId: 1,
      vendorName: quoteForm.value.vendorName.trim(),
      unitPrice: quoteForm.value.unitPrice,
      totalAmount: quoteForm.value.unitPrice * qty,
      leadTimeDays: quoteForm.value.leadTimeDays,
      qualityScore: 80,
    })
    ElMessage.success('报价已录入')
    await load()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '录入失败')
  } finally {
    quoting.value = false
  }
}

function openConvertPo() {
  convertPoVisible.value = true
}

function openConvertOutsource() {
  convertOutsourceVisible.value = true
}

async function confirmConvertPo() {
  converting.value = true
  try {
    const r = unwrapResult<Record<string, unknown>>(
      await sourcingStore.convertRfqToPo(Number(route.params.id)),
    )
    ElMessage.success(`已生成采购单 ${r.poNo}`)
    convertPoVisible.value = false
    await load()
    if (r.poId) router.push(`/sourcing/po-detail/${r.poId}`)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转单失败')
  } finally {
    converting.value = false
  }
}

async function confirmConvertOutsource() {
  converting.value = true
  try {
    const r = unwrapResult<Record<string, unknown>>(
      await sourcingStore.convertRfqToOutsource(Number(route.params.id)),
    )
    ElMessage.success(`已生成委外单 ${r.outsourceNo ?? r.convertedOrderNo ?? ''}`)
    convertOutsourceVisible.value = false
    await load()
    router.push('/sourcing/outsub-order')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '转委外单失败')
  } finally {
    converting.value = false
  }
}
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.head h2 { margin: 0; }
</style>
