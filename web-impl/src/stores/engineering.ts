import { defineStore } from 'pinia'
import { useBaseStore } from './_base'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import type { EngineerProcessStep, EngineerTaskPhase, EngineerTaskRecord } from '@/utils/engineeringTask'

function api() {
  return useBaseStore().api
}

export interface QuoteItemRow {
  id: number
  quoteId?: number
  drawingNo?: string
  material?: string
  quantity?: number
  processRoute?: string
  totalHours?: number
}

export interface WorkbenchDetail {
  workbench: { id: number; status?: string; processStatus?: string; bomStatus?: string; orderId?: number }
  processes?: BackendProcess[]
  bomItems?: BackendBomItem[]
}

interface BackendProcess {
  processCode?: string
  processName?: string
  machineType?: string
  unitTimeMinutes?: number
  spindleSpeed?: number
  feedRate?: number
  cuttingDepth?: number
  toolNo?: string
  sequence?: number
}

interface BackendBomItem {
  materialCode?: string
  materialName?: string
  quantity?: number
  unit?: string
  sequence?: number
}

function toProcessSteps(processes: BackendProcess[] | null | undefined): EngineerProcessStep[] {
  if (!processes?.length) return []
  return processes.map((p, i) => ({
    id: `${p.processCode ?? 'P'}-${i}-${Date.now()}`,
    processCode: p.processCode ?? '',
    processName: p.processName ?? '',
    machineType: p.machineType ?? '',
    estimatedMinutes: p.unitTimeMinutes ?? 60,
    spindleRpm: p.spindleSpeed,
    feedRate: p.feedRate != null ? Number(p.feedRate) : undefined,
    cutDepth: p.cuttingDepth != null ? Number(p.cuttingDepth) : undefined,
    toolNo: p.toolNo,
  }))
}

function toBackendProcesses(steps: EngineerProcessStep[]): BackendProcess[] {
  return steps.map((s, i) => ({
    sequence: i,
    processCode: s.processCode,
    processName: s.processName,
    machineType: s.machineType,
    unitTimeMinutes: s.estimatedMinutes,
    spindleSpeed: s.spindleRpm,
    feedRate: s.feedRate,
    cuttingDepth: s.cutDepth,
    toolNo: s.toolNo,
  }))
}

function toBomLines(items: BackendBomItem[] | null | undefined) {
  if (!items?.length) {
    return [{ materialCode: '', materialName: '原材料', qty: 1, unit: 'PCS' }]
  }
  return items.map((b) => ({
    materialCode: b.materialCode ?? '',
    materialName: b.materialName ?? '',
    qty: Number(b.quantity ?? 1),
    unit: b.unit ?? 'PCS',
  }))
}

function mapWorkbenchPhase(wb?: { status?: string; processStatus?: string; bomStatus?: string }): EngineerTaskPhase {
  if (!wb) return 'PENDING'
  if (wb.status === 'COMPLETED') return 'COMPLETED'
  if (
    wb.status === 'IN_PROGRESS' ||
    wb.processStatus === 'IN_PROGRESS' ||
    wb.bomStatus === 'IN_PROGRESS'
  ) {
    return 'IN_PROGRESS'
  }
  return 'PENDING'
}

export const useEngineeringStore = defineStore('engineering', {
  actions: {
    async listTasks(phase?: EngineerTaskPhase) {
      const r = await api().get('/engineering/tasks', { params: phase ? { phase } : {} })
      return unwrapResult<EngineerTaskRecord[]>(r)
    },

    async listQuoteQueue(query: {
      pageNum?: number
      pageSize?: number
      phase?: EngineerTaskPhase
      customerId?: number
      dateFrom?: string
      dateTo?: string
    }) {
      const r = await api().get('/engineering/quote-queue', { params: query })
      return parsePageItems(r)
    },

    async listOrderQueue(query: { pageNum?: number; pageSize?: number; phase?: EngineerTaskPhase }) {
      const r = await api().get('/engineering/order-queue', { params: query })
      return parsePageItems(r)
    },

    async getQuoteDetail(quoteId: number) {
      const r = await api().get(`/quotes/${quoteId}`)
      const data = unwrapResult<{ quote?: Record<string, unknown>; items?: QuoteItemRow[] }>(r)
      return {
        quote: data?.quote ?? {},
        items: (data?.items ?? []) as QuoteItemRow[],
      }
    },

    async getQuoteItemProcess(itemId: number) {
      const r = await api().get(`/quotes/items/${itemId}/process`)
      const data = unwrapResult<{ processes?: BackendProcess[]; item?: QuoteItemRow & { anodizeArea?: number } }>(r)
      return {
        item: data?.item,
        steps: toProcessSteps(data?.processes),
      }
    },

    async saveQuoteItemProcess(itemId: number, steps: EngineerProcessStep[]) {
      const r = await api().post(`/quotes/items/${itemId}/process`, {
        processes: toBackendProcesses(steps),
      })
      return unwrapResult(r)
    },

    async saveQuoteItemSurfaceAreas(
      itemId: number,
      areas: { anodizeArea?: number; solidSolutionArea?: number; formingArea?: number },
    ) {
      const r = await api().post(`/quotes/items/${itemId}/surface-areas`, areas)
      return unwrapResult(r)
    },

    async calculateQuoteItem(itemId: number) {
      const r = await api().post(`/quotes/items/${itemId}/calculate`)
      return unwrapResult(r)
    },

    async ensureOrderWorkbench(orderId: number) {
      const r = await api().post(`/engineering-workbench/ensure/${orderId}`)
      return unwrapResult<{ id: number; status?: string }[]>(r)
    },

    async getOrderWorkbenches(orderId: number) {
      const r = await api().get(`/engineering-workbench/order/${orderId}`)
      return unwrapResult<{ id: number; status?: string; processStatus?: string; bomStatus?: string }[]>(r)
    },

    async getWorkbenchDetail(workbenchId: number) {
      const r = await api().get(`/engineering-workbench/${workbenchId}`)
      const data = unwrapResult<WorkbenchDetail>(r)
      return {
        workbench: data.workbench,
        steps: toProcessSteps(data.processes),
        bomLines: toBomLines(data.bomItems),
        phase: mapWorkbenchPhase(data.workbench),
      }
    },

    async startWorkbench(workbenchId: number) {
      const r = await api().post(`/engineering-workbench/${workbenchId}/start`)
      return unwrapResult(r)
    },

    async saveWorkbenchProcess(workbenchId: number, steps: EngineerProcessStep[]) {
      const r = await api().post(`/engineering-workbench/${workbenchId}/process`, {
        processes: toBackendProcesses(steps),
      })
      return unwrapResult(r)
    },

    async saveWorkbenchBom(
      workbenchId: number,
      bomLines: { materialCode: string; materialName: string; qty: number; unit: string }[],
    ) {
      const r = await api().post(`/engineering-workbench/${workbenchId}/bom`, {
        bomItems: bomLines.map((b, i) => ({
          sequence: i,
          materialCode: b.materialCode,
          materialName: b.materialName,
          quantity: b.qty,
          unit: b.unit,
        })),
      })
      return unwrapResult(r)
    },

    async submitWorkbench(workbenchId: number) {
      const r = await api().post(`/engineering-workbench/${workbenchId}/submit`)
      return unwrapResult(r)
    },

    async getOrderProgress(orderId: number) {
      const r = await api().get(`/engineering-workbench/order/${orderId}/progress`)
      return unwrapResult<{ engineerPhase?: EngineerTaskPhase; workbenches?: { id: number }[] }>(r)
    },
  },
})

export { toProcessSteps, toBackendProcesses, mapWorkbenchPhase }
