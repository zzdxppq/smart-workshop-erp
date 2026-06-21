import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.11 · 条码生成 composable
 */
export function useBarcode() {
  const barcodeNo = ref<string>('')
  const payload = ref<string>('')

  const isValidFormat = computed(() =>
    /^(.+)-BATCH-\d{8}-\d{4}$/.test(barcodeNo.value) || /^BC\d{8}-\d{4}$/.test(barcodeNo.value),
  )

  function generate(materialCode: string) {
    const ts = new Date()
    const ymd = `${ts.getFullYear()}${String(ts.getMonth() + 1).padStart(2, '0')}${String(ts.getDate()).padStart(2, '0')}`
    const seq = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
    barcodeNo.value = `${materialCode}-BATCH-${ymd}-${seq}`
    payload.value = `enc:v1:${materialCode}`
  }

  function parse(barcode: string) {
    barcodeNo.value = barcode
  }

  return { barcodeNo, payload, isValidFormat, generate, parse }
}
