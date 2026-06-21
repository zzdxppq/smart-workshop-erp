import { defineStore } from 'pinia'
import { useBaseStore } from './_base'
import { unwrapResult } from '@/utils/apiPage'
import { E2QuoteExportService } from '@/api/generated/services/E2QuoteExportService'
import type { QuoteFormItem, QuoteFormQuote } from '@/utils/quotePayload'
import { buildQuoteCreatePayload } from '@/utils/quotePayload'

function api() {
  return useBaseStore().api
}

export const useQuoteStore = defineStore('quote', {
  actions: {
    async saveDraft(id: number, form: { quote: QuoteFormQuote; items: QuoteFormItem[] }) {
      const payload = buildQuoteCreatePayload(form)
      const r = await api().put(`/quotes/${id}/draft`, payload)
      return unwrapResult(r)
    },

    async createDraft(form: { quote: QuoteFormQuote; items: QuoteFormItem[] }) {
      const payload = buildQuoteCreatePayload(form)
      const r = await api().post('/quotes', payload)
      return unwrapResult<{ id?: number }>(r)
    },

    async submitToEngineer(id: number) {
      const r = await api().post(`/quotes/${id}/submit-to-engineer`)
      return unwrapResult(r)
    },

    async submitForApproval(id: number) {
      const r = await api().post(`/quotes/${id}/submit`)
      return unwrapResult(r)
    },

    async listCostItems() {
      const r = await api().get('/quote-cost-items')
      return unwrapResult<Array<Record<string, unknown>>>(r)
    },

    async saveCostItem(item: Record<string, unknown>) {
      if (item.id) {
        const r = await api().put(`/quote-cost-items/${item.id}`, item)
        return unwrapResult(r)
      }
      const r = await api().post('/quote-cost-items', item)
      return unwrapResult(r)
    },

    async deleteCostItem(id: number) {
      const r = await api().delete(`/quote-cost-items/${id}`)
      return unwrapResult(r)
    },

    async exportPdf(id: number) {
      return E2QuoteExportService.exportQuote(id, 'pdf')
    },

    async sendToCustomerEmail(id: number) {
      const r = await api().post(`/quotes/${id}/send-email`)
      return unwrapResult<Record<string, unknown>>(r)
    },
  },
})
