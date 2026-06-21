import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.32-1.37 · 财务 Pinia store
 * 涵盖：Receivable + Payable + Cost + Payment + Profit + Aging
 */
export const useFinanceStore = defineStore('finance', {
  state: () => ({
    receivables: [] as any[],
    currentReceivable: null as any,
    payables: [] as any[],
    currentPayable: null as any,
    costs: [] as any[],
    currentCost: null as any,
    payments: [] as any[],
    currentPayment: null as any,
    profits: [] as any[],
    agings: [] as any[],
    currentAging: null as any,
  }),
  actions: {
    // === Receivable 应收 ===
    async listReceivables(query: any) {
      return await api().get('/finance/receivables', { params: query })
    },
    async getReceivable(id: number) {
      return await api().get(`/finance/receivables/${id}`)
    },
    async createReceivable(payload: any) {
      return await api().post('/finance/receivables', payload)
    },
    async recordReceivableReceipt(id: number, payload: any) {
      return await api().post(`/finance/receivables/${id}/receipt`, payload)
    },
    // === Payable 应付 ===
    async listPayables(query: any) {
      return await api().get('/finance/payables', { params: query })
    },
    async getPayable(id: number) {
      return await api().get(`/finance/payables/${id}`)
    },
    // === Cost 成本核算 ===
    async listCosts(query: any) {
      return await api().get('/finance/cost', { params: query })
    },
    async getCost(id: number) {
      return await api().get(`/finance/cost/${id}`)
    },
    async runCostCalc(payload: any) {
      return await api().post('/finance/cost/run', payload)
    },
    // === Payment 付款 ===
    async listPayments(query: any) {
      return await api().get('/finance/payments', { params: query })
    },
    async getPayment(id: number) {
      return await api().get(`/finance/payments/${id}`)
    },
    async applyPayment(payload: any) {
      return await api().post('/finance/payments/apply', payload)
    },
    async approvePayment(id: number) {
      return await api().post(`/finance/payments/${id}/approve`)
    },
    // === Profit 利润分析 ===
    async listProfits(query: any) {
      return await api().get('/finance/profit', { params: query })
    },
    async exportProfit(query: any) {
      return await api().get('/finance/profit/export', { params: query, responseType: 'blob' })
    },
    // === Aging 账龄分析 ===
    async listAgings(query: any) {
      return await api().get('/finance/aging', { params: query })
    },
    async getAging(id: number) {
      return await api().get(`/finance/aging/${id}`)
    },
    async listSignedScans(query: Record<string, unknown>) {
      return await api().get('/finance/signed-scans', { params: query })
    },
  },
})