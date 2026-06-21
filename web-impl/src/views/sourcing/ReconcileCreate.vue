<template>
  <ErpPageShell title="新建对账单" description="选对账月份与厂商 → 预览委外明细 → 生成对账单（E6-S1 AC-6.1.1）">
    <el-alert type="warning" :closable="false" title="系统内四步流程：生成 PDF → 发送邮件 → 上传签字 → 财务确认（不含线下操作按钮）" />

    <el-form :model="form" label-width="120px" style="margin-top: 16px">
      <el-form-item label="对账月份" required>
        <el-date-picker
          v-model="form.period"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          :disabled-date="disableFutureMonth"
        />
      </el-form-item>
      <el-form-item label="厂商" required>
        <el-select
          v-model="form.vendorId"
          filterable
          placeholder="选择厂商"
          style="width: 360px"
          @change="onVendorChange"
        >
          <el-option
            v-for="v in vendors"
            :key="v.id"
            :label="`${v.vendorName || v.name} (${v.vendorCode || v.code || v.id})`"
            :value="v.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="selectedVendor && !selectedVendor.defaultReconcileEmail && !selectedVendor.email">
        <el-alert type="error" :closable="false" title="该厂商未维护默认对账邮箱，请先到厂商资料维护后再发送邮件" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" plain :loading="previewLoading" :disabled="!form.vendorId" @click="loadPreview">
          预览汇总
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="previewLoading" :data="previewItems" stripe border style="margin-top: 8px">
      <el-table-column prop="outsourceNo" label="委外单号" min-width="140" />
      <el-table-column prop="processName" label="工序" min-width="120" />
      <el-table-column prop="materialCode" label="料号" width="120" />
      <el-table-column prop="qty" label="数量" width="80" align="right" />
      <el-table-column prop="unitPrice" label="单价" width="100" align="right" />
      <el-table-column prop="totalAmount" label="金额" width="110" align="right">
        <template #default="{ row }">
          <MoneyAmount :value="row.totalAmount ?? row.amount" />
        </template>
      </el-table-column>
    </el-table>
    <div v-if="previewItems.length" style="margin-top: 8px; text-align: right; font-weight: 600">
      合计：<MoneyAmount :value="previewTotal" />
    </div>
    <el-empty v-if="previewLoaded && !previewItems.length" description="该月份该厂商暂无已完工委外单" />

    <div style="margin-top: 20px">
      <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">生成对账单</el-button>
      <el-button @click="$router.back()">取消</el-button>
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import MoneyAmount from '@/components/erp/MoneyAmount.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

const router = useRouter()
const sourcingStore = useSourcingStore()

const form = ref({ period: '', vendorId: undefined as number | undefined, vendorName: '' })
const vendors = ref<any[]>([])
const previewItems = ref<any[]>([])
const previewLoading = ref(false)
const previewLoaded = ref(false)
const submitting = ref(false)

const selectedVendor = computed(() => vendors.value.find((v) => v.id === form.value.vendorId))

const previewTotal = computed(() =>
  previewItems.value.reduce((sum, row) => sum + Number(row.totalAmount ?? row.amount ?? 0), 0),
)

const canSubmit = computed(
  () => form.value.period && form.value.vendorId && form.value.vendorName && previewItems.value.length > 0,
)

function disableFutureMonth(d: Date) {
  const now = new Date()
  return d.getFullYear() > now.getFullYear()
    || (d.getFullYear() === now.getFullYear() && d.getMonth() > now.getMonth())
}

function onVendorChange(id: number) {
  const v = vendors.value.find((x) => x.id === id)
  form.value.vendorName = v?.vendorName || v?.name || ''
  previewItems.value = []
  previewLoaded.value = false
}

async function loadVendors() {
  try {
    const r = await sourcingStore.listVendors({ pageNum: 1, pageSize: 500 })
    vendors.value = parsePageItems(r).items as any[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载厂商失败')
  }
}

async function loadPreview() {
  if (!form.value.vendorId || !form.value.period) {
    ElMessage.warning('请先选择月份和厂商')
    return
  }
  previewLoading.value = true
  previewLoaded.value = false
  try {
    previewItems.value = (await sourcingStore.previewReconcileItems(form.value.vendorId, form.value.period)) as any[]
    previewLoaded.value = true
    if (!previewItems.value.length) {
      ElMessage.warning('该月份该厂商无已完工委外单')
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '预览失败')
    previewItems.value = []
  } finally {
    previewLoading.value = false
  }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const items = previewItems.value.map((row) => ({
      outsourceOrderId: row.id,
      outsourceOrderNo: row.outsourceNo,
      itemName: row.processName || row.materialCode || row.outsourceNo,
      quantity: Number(row.qty ?? 1),
      unitPrice: Number(row.unitPrice ?? 0),
    }))
    const created = unwrapResult<{ reconcileNo?: string }>(
      await sourcingStore.createReconcile({ ...form.value, items }),
    )
    ElMessage.success(`对账单已创建：${created.reconcileNo ?? ''}`)
    router.push('/sourcing/reconcile')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const now = new Date()
  form.value.period = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadVendors()
})
</script>
