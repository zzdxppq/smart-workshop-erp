import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.31 · 提货检 composable
 */
export function useQualityPickup() {
  const pickupNo = ref<string>('')
  const customer = ref<string>('')
  const items = ref<any[]>([])
  const result = ref<'PASS' | 'FAIL' | 'PARTIAL'>('PASS')

  const passCount = computed(() => items.value.filter((i) => i.result === 'PASS').length)
  const failCount = computed(() => items.value.filter((i) => i.result === 'FAIL').length)
  const allPass = computed(() => failCount.value === 0 && items.value.length > 0)

  function addItem(it: any) {
    items.value.push(it)
  }

  function finalize() {
    if (failCount.value === 0) result.value = 'PASS'
    else if (passCount.value === 0) result.value = 'FAIL'
    else result.value = 'PARTIAL'
  }

  return { pickupNo, customer, items, result, passCount, failCount, allPass, addItem, finalize }
}