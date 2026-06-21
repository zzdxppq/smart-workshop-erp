import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.18 · 委外 Pinia store
 */
export const useOutsourceStore = defineStore('outsource', {
  state: () => ({
    orders: [] as any[],
    currentOrder: null as any,
  }),
  actions: {
    async createOrder(payload: any) {
      return await api().post('/outsource', payload)
    },
    async submitOrder(outsourceNo: string, note: string) {
      return await api().post(`/outsource/${outsourceNo}/submit`, { note })
    },
    async acceptOrder(outsourceNo: string) {
      return await api().post(`/outsource/${outsourceNo}/accept`)
    },
    async completeOrder(outsourceNo: string) {
      return await api().post(`/outsource/${outsourceNo}/complete`)
    },
    async reworkOrder(outsourceNo: string, note: string) {
      return await api().post(`/outsource/${outsourceNo}/rework`, null, { params: { note } })
    },
    async listOrders(query: any) {
      return await api().get('/outsource', { params: query })
    },
    async getOrder(outsourceNo: string) {
      return await api().get(`/outsource/${outsourceNo}`)
    },
    async listItems(outsourceNo: string) {
      return await api().get(`/outsource/${outsourceNo}/items`)
    },
    async listHistory(outsourceNo: string) {
      return await api().get(`/outsource/${outsourceNo}/history`)
    },
    async getPriceHistory(supplierId: number, materialCode: string) {
      return await api().get('/outsource/price-history', { params: { supplierId, materialCode } })
    },
    async getHistoryPrice(vendorId: number, processName: string) {
      return await api().get('/outsource/history-price', { params: { vendorId, processName } })
    },
    async getPriceSuggest(supplierId: number, processName?: string, materialCode?: string) {
      return await api().get('/outsource/price-suggest', {
        params: { supplierId, processName, materialCode },
      })
    },
  },
})
