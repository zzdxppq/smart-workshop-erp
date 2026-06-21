import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.27-1.31 · 品质 Pinia store
 * 涵盖：Inspection (IQC/IPQC/OQC) + FA + CMM + Defect + Pickup
 */
export const useQualityStore = defineStore('quality', {
  state: () => ({
    inspections: [] as any[],
    currentInspection: null as any,
    fas: [] as any[],
    currentFa: null as any,
    cmms: [] as any[],
    currentCmm: null as any,
    defects: [] as any[],
    currentDefect: null as any,
    pickups: [] as any[],
    currentPickup: null as any,
  }),
  actions: {
    // === Inspection (IQC/IPQC/OQC) ===
    async listInspections(query: any) {
      return await api().get('/quality/inspections', { params: query })
    },
    async getInspection(id: number) {
      return await api().get(`/quality/inspections/${id}`)
    },
    async createInspection(payload: any) {
      return await api().post('/quality/inspections', payload)
    },
    async submitInspection(id: number, payload: any) {
      return await api().post(`/quality/inspections/${id}/submit`, payload)
    },
    async approveConcession(id: number, payload: { approverRole: string; action: string; comment?: string }) {
      return await api().post(`/quality/inspections/${id}/approve-concession`, payload)
    },
    async getConcessionApprovals(id: number) {
      return await api().get(`/quality/inspections/${id}/concession-approvals`)
    },
    async generateInspectionReport(id: number) {
      return await api().get(`/quality/inspections/${id}/report`)
    },
    async listOutsourceInspections(query: any) {
      return await api().get('/outsource-quality', { params: query })
    },
    // === FA 首件 ===
    async listFas(query: any) {
      return await api().get('/quality/fa', { params: query })
    },
    async getFa(id: number) {
      return await api().get(`/quality/fa/${id}`)
    },
    async submitFa(id: number, payload: any) {
      return await api().post(`/quality/fa/${id}/submit`, payload)
    },
    async getFaReport(id: number) {
      return await api().get(`/quality/fa/${id}/report`)
    },
    // V2.1 FA 双签
    async inspectorSignFa(id: number) {
      return await api().post(`/quality/fa/${id}/inspector-sign`)
    },
    async engineerSignFa(id: number) {
      return await api().post(`/quality/fa/${id}/engineer-sign`)
    },
    async reworkFa(id: number, reason?: string) {
      return await api().post(`/quality/fa/${id}/rework`, null, { params: { reason } })
    },
    async resubmitFa(id: number) {
      return await api().post(`/quality/fa/${id}/resubmit`)
    },
    // === CMM 三次元 ===
    async listCmms(query: any) {
      return await api().get('/quality/cmm', { params: query })
    },
    async getCmm(id: number) {
      return await api().get(`/quality/cmm/${id}`)
    },
    async submitCmm(id: number, payload: any) {
      return await api().post(`/quality/cmm/${id}/submit`, payload)
    },
    async getCmmReport(id: number) {
      return await api().get(`/quality/cmm/${id}/report`)
    },
    // === Defect 不良品 ===
    async listDefects(query: any) {
      return await api().get('/quality/defects', { params: query })
    },
    async getDefect(id: number) {
      return await api().get(`/quality/defects/${id}`)
    },
    async createDefect(payload: any) {
      return await api().post('/quality/defects', payload)
    },
    async getDefectReport(query: any) {
      return await api().get('/quality/defects/report', { params: query })
    },
    // V2.1 不良品处置
    async addDefectAction(defectId: number, payload: any) {
      return await api().post(`/quality/defects/${defectId}/action`, payload)
    },
    async createReworkWo(defectId: number) {
      return await api().post(`/quality/defects/${defectId}/create-rework-wo`)
    },
    async scrapInventory(defectId: number) {
      return await api().post(`/quality/defects/${defectId}/scrap-inventory`)
    },
    async concessionApprove(defectId: number, approved: boolean) {
      return await api().post(`/quality/defects/${defectId}/concession-approve`, null, { params: { approved } })
    },
    // === Pickup 提货检 ===
    async listPickups(query: any) {
      return await api().get('/quality/pickups', { params: query })
    },
    async getPickup(id: number) {
      return await api().get(`/quality/pickups/${id}`)
    },
    async inspectPickup(id: number, payload: any) {
      return await api().post(`/quality/pickups/${id}/inspect`, payload)
    },
    // === 检验方案模板 ===
    async listTemplates(query?: { status?: string; drawingNo?: string; inspectionType?: string; materialCode?: string }) {
      return await api().get('/quality/inspection-templates', { params: query ?? {} })
    },
    async getTemplate(id: number) {
      return await api().get(`/quality/inspection-templates/${id}`)
    },
    async createTemplate(payload: any) {
      return await api().post('/quality/inspection-templates', payload)
    },
    async updateTemplate(id: number, payload: any) {
      return await api().put(`/quality/inspection-templates/${id}`, payload)
    },
    async deleteTemplate(id: number) {
      return await api().delete(`/quality/inspection-templates/${id}`)
    },
    async publishTemplate(id: number) {
      return await api().post(`/quality/inspection-templates/${id}/publish`)
    },
    async archiveTemplate(id: number) {
      return await api().post(`/quality/inspection-templates/${id}/archive`)
    },
  },
})