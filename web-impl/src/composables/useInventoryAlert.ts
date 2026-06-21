import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.14 · 库存预警 composable
 */
export function useInventoryAlert() {
  const alerts = ref<any[]>([])

  const levelCounts = computed(() => {
    const result: any = { INFO: 0, WARN: 0, ERROR: 0, CRITICAL: 0 }
    for (const a of alerts.value) {
      if (a.status === 'OPEN') {
        result[a.alertLevel] = (result[a.alertLevel] || 0) + 1
      }
    }
    return result
  })

  function addAlert(alert: any) {
    alerts.value.push(alert)
  }

  function resolveAlert(id: number) {
    const a = alerts.value.find(x => x.id === id)
    if (a) a.status = 'RESOLVED'
  }

  return { alerts, levelCounts, addAlert, resolveAlert }
}
