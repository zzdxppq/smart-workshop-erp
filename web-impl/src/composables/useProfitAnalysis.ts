import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.36 · 利润分析 composable
 */
export function useProfitAnalysis() {
  const period = ref<string>('2026-06')
  const revenue = ref(0)
  const cost = ref(0)
  const tax = ref(0)
  const exportFormat = ref<'XLSX' | 'CSV' | 'PDF'>('XLSX')

  const grossProfit = computed(() => revenue.value - cost.value)
  const netProfit = computed(() => grossProfit.value - tax.value)
  const margin = computed(() => revenue.value === 0 ? 0 : Math.round((netProfit.value / revenue.value) * 10000) / 100)

  function setValues(r: number, c: number, t: number) {
    revenue.value = r
    cost.value = c
    tax.value = t
  }

  function setExportFormat(f: typeof exportFormat.value) {
    exportFormat.value = f
  }

  return { period, revenue, cost, tax, exportFormat, grossProfit, netProfit, margin, setValues, setExportFormat }
}