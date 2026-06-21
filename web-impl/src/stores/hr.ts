import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

/**
 * V1.3.7 Story 1.41-1.44 · 人事 Pinia store
 * 涵盖：Employee + Attendance + Payroll + Performance + Recruitment
 */

function api() {
  return useBaseStore().api
}

export const useHrStore = defineStore('hr', {
  state: () => ({
    employees: [] as any[],
    currentEmployee: null as any,
    attendances: [] as any[],
    payrolls: [] as any[],
    currentPayroll: null as any,
    performances: [] as any[],
    recruitments: [] as any[],
    currentRecruitment: null as any,
  }),
  actions: {
    // === Employee 员工 ===
    async listEmployees(query: any) {
      return await api().get('/hr/employees', { params: query })
    },
    async getEmployee(id: number) {
      return await api().get(`/hr/employees/${id}`)
    },
    async updateEmployee(id: number, payload: any) {
      return await api().put(`/hr/employees/${id}`, payload)
    },
    async createEmployee(payload: any) {
      return await api().post('/hr/employees', payload)
    },
    // === Attendance 考勤 ===
    async listAttendances(query: any) {
      return await api().get('/hr/attendance', { params: query })
    },
    async punch(payload: any) {
      return await api().post('/hr/attendance/punch', payload)
    },
    // === Payroll 薪资 ===
    async listPayrolls(query: any) {
      return await api().get('/hr/payroll', { params: query })
    },
    async getPayroll(id: number) {
      return await api().get(`/hr/payroll/${id}`)
    },
    async calculatePayroll(payload: any) {
      return await api().post('/hr/payroll/calculate', payload)
    },
    async calculatePayrollBatch(payload: { period: string }) {
      const r = await api().post('/hr/payroll/calculate-batch', null, { params: payload })
      return (r as any)?.data ?? r
    },
    async getPayrollSlip(id: number) {
      const r = await api().get(`/hr/payroll/${id}/slip`)
      return (r as any)?.data ?? r
    },
    async approvePayroll(id: number) {
      return await api().post(`/hr/payroll/${id}/approve`)
    },
    // === Performance 绩效 ===
    async listPerformances(query: any) {
      return await api().get('/hr/performance', { params: query })
    },
    async submitPerformance(payload: any) {
      return await api().post('/hr/performance', payload)
    },
    async calculatePerformance(payload: { period: string }) {
      const r = await api().post('/hr/performance/calculate', null, { params: payload })
      return (r as any)?.data ?? r
    },
    async listPerformanceSchemes() {
      const r = await api().get('/hr/performance-schemes')
      return (r as any)?.data?.list ?? (r as any)?.list ?? []
    },
    async savePerformanceScheme(payload: any) {
      return await api().post('/hr/performance-schemes', payload)
    },
    async listSalaryPackages() {
      const r = await api().get('/hr/salary-packages')
      return (r as any)?.data?.list ?? (r as any)?.list ?? []
    },
    async saveSalaryPackage(payload: any) {
      return await api().post('/hr/salary-packages', payload)
    },
    async listPerformanceAppeals(query: any = {}) {
      const r = await api().get('/hr/performance-appeals', { params: query })
      return (r as any)?.data?.list ?? (r as any)?.list ?? []
    },
    async resolvePerformanceAppeal(id: number, payload: { status: string; reply?: string }) {
      return await api().post(`/hr/performance-appeals/${id}/resolve`, payload)
    },
    payrollPdfUrl(id: number) {
      const base = (import.meta as any).env?.VITE_API_BASE || ''
      return `${String(base).replace(/\/$/, '')}/hr/payroll/${id}/slip.pdf`
    },
    // === Recruitment 招聘 ===
    async listRecruitments(query: any) {
      return await api().get('/hr/recruitment', { params: query })
    },
    async getRecruitment(id: number) {
      return await api().get(`/hr/recruitment/${id}`)
    },
    async createRecruitment(payload: any) {
      return await api().post('/hr/recruitment', payload)
    },
  },
})