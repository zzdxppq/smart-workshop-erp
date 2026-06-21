import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.35 · 成本核算 composable
 */
export function useCostAccounting() {
  const period = ref<string>('2026-06')
  const materialCost = ref(0)
  const laborCost = ref(0)
  const outsourceCost = ref(0)
  const overhead = ref(0)

  const totalCost = computed(() => materialCost.value + laborCost.value + outsourceCost.value + overhead.value)

  function setCosts(m: number, l: number, o: number, h: number) {
    materialCost.value = m
    laborCost.value = l
    outsourceCost.value = o
    overhead.value = h
  }

  function reset() {
    materialCost.value = 0
    laborCost.value = 0
    outsourceCost.value = 0
    overhead.value = 0
  }

  return { period, materialCost, laborCost, outsourceCost, overhead, totalCost, setCosts, reset }
}