import { defineStore } from 'pinia'
import { useBaseStore } from './_base'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

function api() {
  return useBaseStore().api
}

function parsePeriod(period?: string): { periodYear?: number; periodMonth?: number } {
  if (!period || !/^\d{4}-\d{2}$/.test(period)) return {}
  const [y, m] = period.split('-')
  return { periodYear: Number(y), periodMonth: Number(m) }
}

/**
 * V1.3.7 Story 1.21-1.26 · 采购 Pinia store
 * 涵盖：RFQ + PO + Incoming + Reconcile + Vendors
 */
export const useSourcingStore = defineStore('sourcing', {
  state: () => ({
    rfqs: [] as any[],
    currentRfq: null as any,
    pos: [] as any[],
    currentPo: null as any,
    incomings: [] as any[],
    reconciles: [] as any[],
    currentReconcile: null as any,
    vendors: [] as any[],
    currentVendor: null as any,
  }),
  actions: {
    // === RFQ 询比价 ===
    async createRfq(payload: any) {
      return await api().post('/rfq', payload)
    },
    async submitRfq(rfqId: number) {
      return await api().post(`/rfq/${rfqId}/submit`)
    },
    async quoteRfq(rfqId: number, payload: any) {
      return await api().post(`/rfq/${rfqId}/quotes`, payload)
    },
    async compareRfq(rfqId: number) {
      return await api().get(`/rfq/${rfqId}/compare`)
    },
    async awardRfq(rfqId: number, payload: any) {
      return await api().post(`/rfq/${rfqId}/award`, payload)
    },
    async listRfqs(query: any) {
      return await api().get('/rfq', { params: query })
    },
    async getRfq(rfqId: number) {
      return await api().get(`/rfq/${rfqId}`)
    },
    // === PO 采购订单 ===
    async createPo(payload: any) {
      return await api().post('/po', payload)
    },
    async confirmPo(poId: number) {
      return await api().post(`/po/${poId}/confirm`)
    },
    async closePo(poId: number) {
      return await api().post(`/po/${poId}/close`)
    },
    async listPos(query: any) {
      return await api().get('/po', { params: query })
    },
    async getPo(poId: number) {
      return await api().get(`/po/${poId}`)
    },
    // === 采购申请 PR（MRP → 转 PO）===
    async listPurchaseRequests(query: Record<string, unknown>) {
      return await api().get('/purchase-request', { params: query })
    },
    async getPurchaseRequest(id: number) {
      return await api().get(`/purchase-request/${id}`)
    },
    async convertPrToPo(prId: number, payload: Record<string, unknown>) {
      return await api().post(`/purchase-request/${prId}/convert-to-po`, payload)
    },
    async convertRfqToPo(rfqId: number) {
      return await api().post(`/rfq/${rfqId}/convert-to-po`)
    },
    async convertRfqToOutsource(rfqId: number) {
      return await api().post(`/rfq/${rfqId}/convert-to-outsource`)
    },
    // === Incoming 到货提醒（Story 1.34 · /incoming-alert）===
    async listIncoming(_query: any) {
      const r = await api().post('/incoming-alert/list')
      const alerts = unwrapResult<any[]>(r)
      return { data: { records: alerts, total: alerts.length } }
    },
    async getIncoming(id: number) {
      const r = await this.listIncoming({})
      const items = parsePageItems(r).items as any[]
      const found = items.find((a) => a.id === id)
      if (!found) throw new Error('到货提醒不存在')
      return { data: found }
    },
    async createIncoming(payload: any) {
      return await api().post('/incoming-alert', payload)
    },
    async markIncomingReceived(id: number) {
      return await api().post(`/incoming-alert/${id}/arrived`, { arrivedQty: 0 })
    },
    // === Reconcile 月度对账（V1.3.7 · /reconciles）===
    async listReconciles(query: any) {
      const { periodYear, periodMonth } = parsePeriod(query.period)
      const r = await api().get('/reconciles', {
        params: {
          vendorId: query.vendorId,
          periodYear,
          periodMonth,
          status: query.status,
          page: (query.pageNum ?? 1) - 1,
          size: query.pageSize ?? 20,
        },
      })
      const data = unwrapResult<{ list?: Record<string, unknown>[] }>(r)
      const list = (data?.list ?? []).map((row) => {
        const y = row.periodYear
        const m = row.periodMonth
        if (y && m) {
          row.period = `${y}-${String(m).padStart(2, '0')}`
        }
        return row
      })
      return { data: { list, records: list, total: list.length } }
    },
    async getReconcile(reconcileId: number) {
      const r = await api().get(`/reconciles/${reconcileId}`)
      const detail = unwrapResult<{ reconcile?: Record<string, unknown>; items?: unknown[]; signatures?: unknown[] }>(r)
      if (detail?.reconcile) {
        const rec = detail.reconcile as Record<string, unknown>
        const y = rec.periodYear
        const m = rec.periodMonth
        if (y && m) {
          rec.period = `${y}-${String(m).padStart(2, '0')}`
        }
        return { data: { ...rec, items: detail.items ?? [], signatures: detail.signatures ?? [] } }
      }
      return r
    },
    async createReconcile(payload: any) {
      const { periodYear, periodMonth } = parsePeriod(payload.period)
      return await api().post('/reconciles', {
        vendorId: payload.vendorId,
        vendorName: payload.vendorName,
        periodYear,
        periodMonth,
        items: payload.items,
      })
    },
    async vendorConfirmReconcile(reconcileId: number, payload: any) {
      return await api().post(`/reconciles/${reconcileId}/vendor-confirm`, payload)
    },
    async financeConfirmReconcile(reconcileId: number) {
      return await api().post(`/reconciles/${reconcileId}/finance-confirm`)
    },
    async uploadReconcileSignature(reconcileId: number, formData: FormData) {
      return await api().post(`/reconciles/${reconcileId}/upload-signature`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
    /** 预览委外单明细（E6-S1 · 从 production 拉取） */
    async previewReconcileItems(vendorId: number, period?: string) {
      const r = await api().get('/outsource', {
        params: { supplierId: vendorId, status: 'COMPLETED', size: 200, page: 0 },
      })
      const { items } = parsePageItems(r)
      const { periodYear, periodMonth } = parsePeriod(period)
      if (!periodYear || !periodMonth) return items
      return (items as any[]).filter((row) => {
        const ts = row.completedAt || row.createdAt
        if (!ts) return true
        const d = new Date(ts)
        return d.getFullYear() === periodYear && d.getMonth() + 1 === periodMonth
      })
    },
    // === Vendors 厂商资料 ===
    async listVendors(query: any) {
      return await api().get('/vendors', { params: query })
    },
    async getVendor(vendorId: number) {
      return await api().get(`/vendors/${vendorId}`)
    },
    async updateVendorNotifyPref(vendorId: number, payload: any) {
      return await api().put(`/vendors/${vendorId}/notify-pref`, payload)
    },
    async createVendor(payload: any) {
      return await api().post('/vendors', payload)
    },
    async updateVendor(vendorId: number, payload: any) {
      return await api().put(`/vendors/${vendorId}`, payload)
    },
    async uploadBusinessLicense(vendorId: number, formData: FormData) {
      return await api().post(`/vendors/${vendorId}/upload-license`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
  },
})
