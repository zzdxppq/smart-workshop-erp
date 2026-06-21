import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

export const useAdminStore = defineStore('admin', {
  state: () => ({}),
  actions: {
    async listEmailTemplates() {
      return await api().get('/admin/email-templates')
    },
    async getEmailTemplate(key: string) {
      return await api().get(`/admin/email-templates/${key}`)
    },
    async updateEmailTemplate(key: string, payload: { subject?: string; body?: string; name?: string }) {
      return await api().put(`/admin/email-templates/${key}`, payload)
    },
    async getFieldEncryption() {
      return await api().get('/admin/field-encryption')
    },
    async updateFieldEncryption(payload: Record<string, unknown>) {
      return await api().put('/admin/field-encryption', payload)
    },
  },
})
