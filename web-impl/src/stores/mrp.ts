import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.17 · MRP Pinia store
 */
export const useMrpStore = defineStore('mrp', {
  state: () => ({
    runs: [] as any[],
    currentRun: null as any,
  }),
  actions: {
    async runMrp(payload: any) {
      return await api().post('/mrp/run', payload)
    },
    async getMrpResult(runId: number) {
      return await api().get('/mrp/results', { params: { runId } })
    },
    async listShortages(runId: number) {
      return await api().get('/mrp/shortages', { params: { runId } })
    },
    async listRuns(query: any) {
      return await api().get('/mrp/runs', { params: query })
    },
    async exportToPurchase(runId: number) {
      return await api().post('/mrp/export-to-purchase', null, { params: { runId } })
    },
  },
})
