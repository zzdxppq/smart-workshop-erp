import { ref } from 'vue'

/**
 * V1.3.7 Story 1.12 · 扫码 composable
 */
export function useScanner() {
  const lastScan = ref<string>('')
  const scanHistory = ref<any[]>([])
  const isOffline = ref(false)
  const offlineCache = ref<any[]>([])

  function scan(barcode: string) {
    lastScan.value = barcode
    scanHistory.value.push({ barcode, at: Date.now() })
    if (isOffline.value) {
      offlineCache.value.push({ barcode, at: Date.now() })
    }
  }

  function setOffline(value: boolean) {
    isOffline.value = value
  }

  return { lastScan, scanHistory, isOffline, offlineCache, scan, setOffline }
}
