import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.45 · 仓库到货扫码 composable
 */
export function useWarehouseIncomingScan() {
  const scans = ref<any[]>([])
  const scanning = ref(false)
  const lastBarcode = ref<string>('')

  const count = computed(() => scans.value.length)
  const qtyTotal = computed(() => scans.value.reduce((acc, s) => acc + (s.qty ?? 0), 0))

  function addScan(barcode: string, qty: number) {
    scans.value.push({ barcode, qty, at: new Date().toISOString() })
    lastBarcode.value = barcode
  }

  function clear() {
    scans.value = []
    lastBarcode.value = ''
  }

  return { scans, scanning, lastBarcode, count, qtyTotal, addScan, clear }
}