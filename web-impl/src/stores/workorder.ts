import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.15 · 工单 Pinia store
 */
export const useWorkorderStore = defineStore('workorder', {
  state: () => ({
    workorders: [] as any[],
    schedules: [] as any[],
  }),
  actions: {
    async createWorkorder(payload: any) {
      return await api().post('/workorders', payload)
    },
    async scheduleWorkorder(id: number, payload: any) {
      return await api().put(`/workorders/${id}/schedule`, payload)
    },
    async startProduction(id: number) {
      return await api().post(`/workorders/${id}/start`)
    },
    async finishProduction(id: number) {
      return await api().post(`/workorders/${id}/finish`)
    },
    async listWorkorders(query: any) {
      return await api().get('/workorders', { params: query })
    },
    async getWorkorder(id: number) {
      return await api().get(`/workorders/${id}`)
    },
    /** 按工单号（GD-XXXX）查工单，用于路由跳转兼容 */
    async getWorkorderByNo(workorderNo: string) {
      return await api().get(`/workorders/by-no/${encodeURIComponent(workorderNo)}`)
    },
    async listSteps(id: number) {
      return await api().get(`/workorders/${id}/steps`)
    },
  },
})
