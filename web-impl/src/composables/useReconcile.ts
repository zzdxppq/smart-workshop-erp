import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.25 · 月度对账 composable
 *
 * V1.3.7 AD-2 红线：本 composable 严格禁止暴露任何"线下"操作入口
 * （如"采购带纸去厂商处"、"线下签字"等），4 步流程：建单 → 厂商确认 → 财务审核 → 双方签
 */
export function useReconcile() {
  const reconcileId = ref<number | null>(null)
  const step = ref<'CREATE' | 'VENDOR_CONFIRM' | 'FINANCE_AUDIT' | 'SIGN'>('CREATE')
  const reconcileNo = ref<string>('')
  const vendorSigned = ref(false)
  const financeSigned = ref(false)

  const canSubmit = computed(() => step.value === 'CREATE')
  const canVendorConfirm = computed(() => step.value === 'CREATE' && reconcileNo.value !== '')
  const canFinanceAudit = computed(() => step.value === 'VENDOR_CONFIRM' && vendorSigned.value)
  const canSign = computed(() => step.value === 'FINANCE_AUDIT' && financeSigned.value)

  // 红线检查：禁止"线下"
  const FORBIDDEN_OFFLINE_ACTIONS = [
    'offline-sign',
    'paper-reconcile',
    'visit-vendor',
    'manual-print',
    '线下签字',
    '带纸去厂商',
  ]

  function isForbidden(action: string): boolean {
    return FORBIDDEN_OFFLINE_ACTIONS.some((f) => action.includes(f) || f.includes(action))
  }

  function setStep(s: typeof step.value) {
    step.value = s
  }

  function markVendorSigned() {
    vendorSigned.value = true
    step.value = 'VENDOR_CONFIRM'
  }

  function markFinanceSigned() {
    financeSigned.value = true
    step.value = 'FINANCE_AUDIT'
  }

  return {
    reconcileId, step, reconcileNo, vendorSigned, financeSigned,
    canSubmit, canVendorConfirm, canFinanceAudit, canSign,
    isForbidden, setStep, markVendorSigned, markFinanceSigned,
  }
}