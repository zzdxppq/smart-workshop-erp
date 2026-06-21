import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.12/1.13 · 仓储 Pinia store
 */
export const useWarehouseStore = defineStore('warehouse', {
  state: () => ({
    locations: [] as any[],
    warehouses: [] as any[],
    batches: [] as any[],
  }),
  actions: {
    async scanInbound(payload: any) {
      return await api().post('/app/scan/inbound', payload)
    },
    async scanOutbound(payload: any) {
      return await api().post('/app/scan/outbound', payload)
    },
    async syncOffline(payload: any) {
      return await api().post('/app/scan/sync', payload)
    },
    async listScans(query: any) {
      return await api().get('/app/scan/list', { params: query })
    },
    async listLocations(warehouse?: string) {
      return await api().get('/app/scan/locations', { params: { warehouse } })
    },
    async listWarehouses() {
      return await api().get('/warehouses')
    },
    async createWarehouse(payload: Record<string, unknown>) {
      return await api().post('/warehouses', payload)
    },
    async createLocation(payload: Record<string, unknown>) {
      return await api().post('/warehouses/locations', payload)
    },
    async updateWarehouse(code: string, payload: Record<string, unknown>) {
      return await api().put(`/warehouses/${encodeURIComponent(code)}`, payload)
    },
    async updateLocation(code: string, payload: Record<string, unknown>) {
      return await api().put(`/warehouses/locations/${encodeURIComponent(code)}`, payload)
    },
    async getLocationTree() {
      return await api().get('/warehouses/locations/tree')
    },
    async getLocation(code: string) {
      return await api().get(`/app/scan/locations`, { params: { warehouse: code } })
    },
    async listBatchesFefo(query: Record<string, unknown>) {
      return await api().get('/warehouses/batches', { params: query })
    },
    async getBatchTrace(batchNo: string) {
      return await api().get(`/warehouses/batches/${batchNo}`)
    },
  },
})
