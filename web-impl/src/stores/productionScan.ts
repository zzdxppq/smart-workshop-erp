import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.16 · 生产扫码 Pinia store
 */
export const useProductionScanStore = defineStore('productionScan', {
  state: () => ({
    pending: null as any,
  }),
  actions: {
    async scanStart(payload: any) {
      return await api().post('/app/production/scan/start', payload)
    },
    async scanReport(payload: any) {
      return await api().post('/app/production/scan/report', payload)
    },
    async scanStation(payload: any) {
      return await api().post('/app/production/scan/station', payload)
    },
    async listPending() {
      return await api().get('/app/production/scan/pending')
    },
    async getScanHistory(workorderNo: string) {
      return await api().get(`/app/production/scan/history/${workorderNo}`)
    },
    async getReportHistory(workorderNo: string) {
      return await api().get(`/app/production/scan/reports/${workorderNo}`)
    },
    async getStationHistory(workorderNo: string) {
      return await api().get(`/app/production/scan/stations/${workorderNo}`)
    },
  },
})
