import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.14 · 库存预警 Pinia store
 */
export const useInventoryStore = defineStore('inventory', {
  state: () => ({
    safetyConfigs: [] as any[],
    alerts: [] as any[],
  }),
  actions: {
    async listSafetyConfigs(query?: Record<string, unknown>) {
      return await api().get('/inventory/safety', { params: query })
    },
    async updateSafety(payload: any) {
      return await api().post('/inventory/safety', payload)
    },
    async checkAlert(materialCode: string, currentQty: number) {
      return await api().post('/inventory/check-alert', null, { params: { materialCode, currentQty } })
    },
    async listAlerts(query: any) {
      return await api().get('/inventory/alerts', { params: query })
    },
    async resolveAlert(alertId: number, payload: any) {
      return await api().post(`/inventory/alerts/${alertId}/resolve`, payload)
    },
    async archiveAlert(alertId: number) {
      return await api().post(`/inventory/alerts/${alertId}/archive`)
    },
    async alertStats() {
      // 简化：从 listAlerts 聚合
      return {}
    },
  },
})
