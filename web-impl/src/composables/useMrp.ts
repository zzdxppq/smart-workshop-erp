import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.17 · MRP composable
 */
export function useMrp() {
  const dateRangeStart = ref<string>('2026-06-01')
  const dateRangeEnd = ref<string>('2026-07-01')
  const warehouseIds = ref<number[]>([1, 2, 3])
  const runType = ref<'FULL' | 'INCREMENTAL'>('FULL')
  const result = ref<any>(null)

  const isValid = computed(() => {
    return dateRangeStart.value && dateRangeEnd.value
        && dateRangeEnd.value >= dateRangeStart.value
        && warehouseIds.value.length > 0
  })

  function reset() {
    result.value = null
  }

  return { dateRangeStart, dateRangeEnd, warehouseIds, runType, result, isValid, reset }
}
