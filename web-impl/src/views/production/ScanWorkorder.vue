<template>
  <div v-loading="loading" class="scan-workorder">
    <h2>扫码三码</h2>
    <el-steps :active="step" finish-status="success" align-center style="margin-bottom: 24px">
      <el-step title="GD- 工单码" description="开工/报工" />
      <el-step title="LZ- 流转码" description="过站" />
      <el-step title="SB- 设备码" description="选机台" />
    </el-steps>

    <el-card>
      <p class="step-label">{{ stepLabels[step] }}</p>
      <ScanTrigger v-model="scanCode" @scan="onScan" />
      <el-form v-if="step === 1 && gdCode" style="margin-top: 16px" label-width="100px">
        <el-form-item label="工单">{{ gdCode }}</el-form-item>
        <el-form-item label="合格数">
          <el-input-number v-model="reportQty.qtyOk" :min="0" />
        </el-form-item>
        <el-form-item label="报废数">
          <el-input-number v-model="reportQty.qtyScrap" :min="0" />
        </el-form-item>
        <el-form-item label="投入数">
          <el-input-number v-model="reportQty.qtyDone" :min="0" />
        </el-form-item>
      </el-form>
      <div style="margin-top: 16px">
        <el-button v-if="step > 0" @click="step--">上一步</el-button>
        <el-button type="primary" @click="nextStep">{{ step < 2 ? '下一步' : '提交' }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { E5ScanService } from '@/api/generated/services/E5ScanService'
const loading = ref(false)
const step = ref(0)
const scanCode = ref('')
const gdCode = ref('')
const lzCode = ref('')
const sbCode = ref('')
const reportQty = ref({ qtyDone: 1, qtyOk: 1, qtyScrap: 0 })

const stepLabels = ['请扫描 GD- 工单码', '请扫描 LZ- 流转码（或跳过报工）', '请扫描 SB- 设备码（可选）并提交开工']

function parsePrefix(code: string) {
  const u = code.trim().toUpperCase()
  if (u.startsWith('GD-')) return 'GD'
  if (u.startsWith('LZ-')) return 'LZ'
  if (u.startsWith('SB-')) return 'SB'
  return null
}

function onScan(code: string) {
  const p = parsePrefix(code)
  if (step.value === 0 && p === 'GD') gdCode.value = code
  else if (step.value === 1 && p === 'LZ') lzCode.value = code
  else if (step.value === 2 && p === 'SB') sbCode.value = code
  else ElMessage.warning('码型与当前步骤不匹配')
}

async function nextStep() {
  loading.value = true
  try {
    if (step.value === 0) {
      if (!gdCode.value) {
        ElMessage.warning('请先扫描 GD- 工单码')
        return
      }
      step.value = 1
    } else if (step.value === 1) {
      if (lzCode.value) {
        await E5ScanService.scanTransfer(lzCode.value, {
          workorderNo: gdCode.value,
          fromStepNo: 1,
          toStepNo: 2,
        })
      } else {
        await E5ScanService.scanReport(gdCode.value, reportQty.value)
      }
      step.value = 2
    } else {
      await E5ScanService.scanStart(gdCode.value, {
        machineBarcode: sbCode.value || undefined,
      })
      ElMessage.success('扫码三码完成')
      step.value = 0
      gdCode.value = lzCode.value = sbCode.value = scanCode.value = ''
    }
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.scan-workorder { padding: 16px; max-width: 720px; }
.step-label { font-size: 16px; margin-bottom: 12px; color: var(--erp-text-secondary); }
</style>
