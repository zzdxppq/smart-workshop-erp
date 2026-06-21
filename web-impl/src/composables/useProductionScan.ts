import { ref } from 'vue'

/**
 * V1.3.7 Story 1.16 · 生产扫码 composable
 */
export function useProductionScan() {
  const scanType = ref<'START' | 'REPORT' | 'STATION'>('START')
  const workorderNo = ref<string>('')
  const stepNo = ref<number>(1)
  const qty = ref<number>(1)
  const isAbnormal = ref<boolean>(false)

  function reset() {
    workorderNo.value = ''
    stepNo.value = 1
    qty.value = 1
    isAbnormal.value = false
  }

  return { scanType, workorderNo, stepNo, qty, isAbnormal, reset }
}
