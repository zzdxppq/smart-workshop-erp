import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

/**
 * V1.3.7 Story 1.48-1.49 · 报表 Pinia store
 * 涵盖：Sales Ranking + Sales Trend + Customer Analysis
 */

function api() {
  return useBaseStore().api
}

export const useReportStore = defineStore('report', {
  state: () => ({
    salesRanking: [] as any[],
    salesTrend: [] as any[],
    customerAnalysis: [] as any[],
  }),
  actions: {
    async loadSalesRanking(query: any) {
      return await api().get('/reports/sales-ranking', { params: query })
    },
    async loadSalesTrend(query: any) {
      return await api().get('/reports/sales-trend', { params: query })
    },
    async loadCustomerAnalysis(query: any) {
      return await api().get('/reports/customer-analysis', { params: query })
    },
    async exportReport(type: string, query: any) {
      return await api().get(`/reports/${type}/export`, { params: query, responseType: 'blob' })
    },
  },
})