import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.48-1.49 · 报表 composable
 */
export type ReportType = 'SALES_RANKING' | 'SALES_TREND' | 'CUSTOMER_ANALYSIS'

export function useReport() {
  const reportType = ref<ReportType>('SALES_RANKING')
  const dateRange = ref<[string, string]>(['2026-01-01', '2026-06-30'])
  const topN = ref<number>(10)

  function setType(t: ReportType) {
    reportType.value = t
  }

  function setRange(r: [string, string]) {
    dateRange.value = r
  }

  function setTopN(n: number) {
    topN.value = n
  }

  return { reportType, dateRange, topN, setType, setRange, setTopN }
}