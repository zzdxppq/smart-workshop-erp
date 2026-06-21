import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.34 · 物料成本 composable
 */
export function useMaterialCost() {
  const materialCode = ref<string>('')
  const priceTrend = ref<any[]>([])
  const vendorCompare = ref<any[]>([])

  const avgPrice = computed(() => {
    if (priceTrend.value.length === 0) return 0
    const sum = priceTrend.value.reduce((acc, p) => acc + (p.price ?? 0), 0)
    return Math.round((sum / priceTrend.value.length) * 100) / 100
  })

  function setPriceTrend(data: any[]) {
    priceTrend.value = data
  }

  function setVendorCompare(data: any[]) {
    vendorCompare.value = data
  }

  return { materialCode, priceTrend, vendorCompare, avgPrice, setPriceTrend, setVendorCompare }
}