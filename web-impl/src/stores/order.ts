import { defineStore } from 'pinia'
import { useBaseStore } from './_base'
import { unwrapResult } from '@/utils/apiPage'
import type { OrderFormItem, OrderFormOrder } from '@/utils/orderPayload'
import { buildOrderCreatePayload } from '@/utils/orderPayload'

function api() {
  return useBaseStore().api
}

export const useOrderStore = defineStore('order', {
  actions: {
    async createDraft(form: { order: OrderFormOrder; items: OrderFormItem[] }) {
      const payload = buildOrderCreatePayload(form)
      const r = await api().post('/orders', payload)
      return unwrapResult<{ id?: number }>(r)
    },

    async saveDraft(id: number, form: { order: OrderFormOrder; items: OrderFormItem[] }) {
      const payload = buildOrderCreatePayload(form)
      const r = await api().put(`/orders/${id}/draft`, payload)
      return unwrapResult(r)
    },

    async submitOrder(id: number) {
      const r = await api().post(`/orders/${id}/submit`)
      return unwrapResult<Record<string, unknown>>(r)
    },

    async checkMaterialNos(drawingNos: string[]) {
      if (!drawingNos.length) return {} as Record<string, string>
      const r = await api().get('/orders/check-material-nos', { params: { drawingNos } })
      return unwrapResult<Record<string, string>>(r)
    },
  },
})
