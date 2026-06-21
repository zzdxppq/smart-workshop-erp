import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.18 · 委外 composable
 */
export function useOutsource() {
  const orders = ref<any[]>([])
  const view = ref<'production' | 'purchase'>('production')

  const canShowVendorDropdown = computed(() => view.value === 'purchase')
  const canEditProcess = computed(() => view.value === 'production')

  function setOrders(data: any[]) {
    orders.value = data
  }

  function setView(v: 'production' | 'purchase') {
    view.value = v
  }

  return { orders, view, canShowVendorDropdown, canEditProcess, setOrders, setView }
}
