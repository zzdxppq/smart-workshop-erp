import { defineStore } from 'pinia'
import { useBaseStore } from './_base'

function api() {
  return useBaseStore().api
}

/**
 * V1.3.7 Story 1.11 · 物料条码 Pinia store
 */
export const useMaterialStore = defineStore('material', {
  state: () => ({
    materials: [] as any[],
    barcodes: [] as any[],
    categories: [] as any[],
  }),
  actions: {
    async listMaterials() {
      const r = await api().get('/materials/barcode/materials')
      return r.data || []
    },
    async listDrawings(query?: Record<string, unknown>) {
      return await api().get('/drawings', { params: query })
    },
    async generateBarcode(payload: any) {
      return await api().post('/materials/barcode/generate', payload)
    },
    async batchGenerate(payload: any) {
      return await api().post('/materials/barcode/batch-generate', payload)
    },
    async parseBarcode(barcodeNo: string) {
      return await api().get(`/materials/barcode/${barcodeNo}`)
    },
    async listBarcodes(query: any) {
      return await api().get('/materials/barcodes', { params: query })
    },
    async regenerateBarcode(barcodeNo: string) {
      return await api().post(`/materials/barcode/${barcodeNo}/regenerate`)
    },
    async listCategories() {
      return await api().get('/materials/barcode/categories')
    },
    async listAnnotations(drawingId: number, version?: string) {
      return await api().get(`/drawings/${drawingId}/annotations`, { params: version ? { version } : {} })
    },
    async addAnnotation(drawingId: number, payload: Record<string, unknown>) {
      return await api().post(`/drawings/${drawingId}/annotations`, payload)
    },
    async convertDrawing(drawingId: number, payload: Record<string, unknown>) {
      return await api().post(`/drawings/${drawingId}/convert`, payload)
    },
    async listDrawingVersions(drawingId: number) {
      return await api().get(`/drawings/${drawingId}/versions`)
    },
    async listDrawingAttachments(drawingId: number) {
      return await api().get(`/drawings/${drawingId}/attachments`)
    },
    async uploadDrawingAttachment(drawingId: number, file: File, operatorUserId = 1001) {
      const form = new FormData()
      form.append('file', file)
      return await api().post(`/drawings/${drawingId}/attachments`, form, {
        params: { operatorUserId },
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
  },
})
