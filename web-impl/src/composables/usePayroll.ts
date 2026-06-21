import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.42 · 薪酬 composable
 *
 * V1.3.7 红线 5：作业人员自助只读金额
 */
export function usePayroll() {
  const period = ref<string>('2026-06')
  const items = ref<any[]>([])
  const currentRole = ref<'HR' | 'OPERATOR'>('HR')

  const totals = computed(() => {
    return {
      base: items.value.reduce((acc, i) => acc + (i.base ?? 0), 0),
      overtime: items.value.reduce((acc, i) => acc + (i.overtime ?? 0), 0),
      bonus: items.value.reduce((acc, i) => acc + (i.bonus ?? 0), 0),
      deduction: items.value.reduce((acc, i) => acc + (i.deduction ?? 0), 0),
      net: items.value.reduce((acc, i) => acc + (i.net ?? 0), 0),
    }
  })

  // 红线 5：作业人员自助只读
  const amountReadonly = computed(() => currentRole.value === 'OPERATOR')

  function setRole(r: typeof currentRole.value) {
    currentRole.value = r
  }

  function setItems(data: any[]) {
    items.value = data
  }

  return { period, items, currentRole, totals, amountReadonly, setRole, setItems }
}