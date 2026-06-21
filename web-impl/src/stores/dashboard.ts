import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

/**
 * V1.3.7 Story 1.46-1.51 · 工作台/看板 Pinia store
 * 涵盖：Production + Sales + Finance + Quality + Outsource + Material
 */

// Helper: get base api client
function api() {
  return useBaseStore().api
}

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    productionStats: null as any,
    productionAlerts: [] as any[],
    productionWorkorders: [] as any[],
    salesStats: null as any,
    financeStats: null as any,
    qualityStats: null as any,
    outsourceStats: null as any,
    materialPrice: [] as any[],
    materialCostTrend: [] as any[],
    materialVendorCompare: [] as any[],
  }),
  actions: {
    // === Production 看板 ===
    async loadProductionStats() {
      return await api().get('/dashboard/production')
    },
    async loadProductionAlerts() {
      return await api().get('/dashboard/production/alerts')
    },
    async loadProductionEvents() {
      return await api().get('/dashboard/events')
    },
    async loadProductionWorkorders() {
      return await api().get('/dashboard/production/workorders')
    },
    // === Sales 看板 ===
    async loadSalesStats(query?: { period?: string; dept?: string }) {
      return await api().get('/dashboard/sales', { params: query })
    },
    async loadFinanceStats(query?: { period?: string }) {
      return await api().get('/dashboard/finance', { params: query })
    },
    async loadQualityStats(query?: { period?: string }) {
      return await api().get('/dashboard/quality', { params: query })
    },
    async loadGmCockpit() {
      return await api().get('/dashboard/gm')
    },
    // === Outsource 看板 ===
    async loadOutsourceStats() {
      return await api().get('/dashboard/outsource')
    },
    async loadOutsourceQuality() {
      return await api().get('/dashboard/outsource/quality')
    },
    async loadOutsourceCost() {
      return await api().get('/dashboard/outsource/cost')
    },
    // === Material 物料看板 ===
    async loadMaterialPrice(query: any) {
      return await api().get('/dashboard/material/price', { params: query })
    },
    async loadMaterialCostTrend(query: any) {
      return await api().get('/dashboard/material/cost-trend', { params: query })
    },
    async loadMaterialVendorCompare(query: any) {
      return await api().get('/dashboard/material/vendor-compare', { params: query })
    },
  },
})