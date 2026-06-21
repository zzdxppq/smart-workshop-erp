import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.33-1.34 · 应收应付 composable
 *
 * V1.3.7 红线 5：作业人员自助只读金额
 */
export function useReceivablePayable() {
  const role = ref<'FINANCE' | 'SALES' | 'OPERATOR'>('FINANCE')
  const items = ref<any[]>([])
  const totals = ref<{ receivable: number; payable: number }>({ receivable: 0, payable: 0 })

  // 红线 5：作业人员自助视图金额只读
  const canEditAmount = computed(() => role.value !== 'OPERATOR')
  const amountReadonly = computed(() => role.value === 'OPERATOR')

  function setRole(r: typeof role.value) {
    role.value = r
  }

  function addItem(it: any) {
    items.value.push(it)
  }

  return { role, items, totals, canEditAmount, amountReadonly, setRole, addItem }
}