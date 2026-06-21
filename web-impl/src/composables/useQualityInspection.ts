import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.27 · 品质 3 检 composable
 */
export type InspectionType = 'IQC' | 'IPQC' | 'OQC'

export function useQualityInspection() {
  const currentType = ref<InspectionType>('IQC')
  const inspectionNo = ref<string>('')
  const items = ref<any[]>([])
  const passCount = ref(0)
  const failCount = ref(0)

  const passRate = computed(() => {
    const total = passCount.value + failCount.value
    if (total === 0) return 0
    return Math.round((passCount.value / total) * 10000) / 100
  })

  const isComplete = computed(() => {
    return items.value.length > 0 && items.value.every((i) => i.result !== undefined)
  })

  function setType(t: InspectionType) {
    currentType.value = t
  }

  function setItemResult(idx: number, result: 'PASS' | 'FAIL') {
    if (items.value[idx]) items.value[idx].result = result
  }

  function recount() {
    passCount.value = items.value.filter((i) => i.result === 'PASS').length
    failCount.value = items.value.filter((i) => i.result === 'FAIL').length
  }

  return {
    currentType, inspectionNo, items, passCount, failCount,
    passRate, isComplete, setType, setItemResult, recount,
  }
}