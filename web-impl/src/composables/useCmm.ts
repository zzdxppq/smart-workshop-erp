import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.29 · CMM 三次元 composable
 */
export function useCmm() {
  const cmmNo = ref<string>('')
  const deviceId = ref<string>('CMM-001')
  const measurements = ref<any[]>([])
  const tolerances = ref<{ upper: number; lower: number }>({ upper: 0.05, lower: -0.05 })

  const outOfTolCount = computed(() =>
    measurements.value.filter((m) => m.value > tolerances.value.upper || m.value < tolerances.value.lower).length
  )

  function setMeasurements(data: any[]) {
    measurements.value = data
  }

  function addMeasurement(m: any) {
    measurements.value.push(m)
  }

  return { cmmNo, deviceId, measurements, tolerances, outOfTolCount, setMeasurements, addMeasurement }
}