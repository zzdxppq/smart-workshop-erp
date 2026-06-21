/**
 * 标签 store（V1.3.9 Sprint 12 · Story 12.3 · web-impl）
 *
 * <p>封装 /label-templates + /label-templates/preview
 * <p>5 类型缓存 + 预览请求去抖
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/utils/http'

export interface LabelTemplate {
  type: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL'
  name: string
  prefix: string
  colorStrip: string
  reuseFrom?: string | null
  layout?: any
  dpi?: number
  enabled?: boolean
  factoryName?: string
  qrExample?: string
}

export interface LabelPreviewRequest {
  type: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL'
  qrContent: string
  lines?: string[]
  factoryName?: string
  format?: 'PNG' | 'PDF'
}

export interface LabelPreviewResponse {
  type: string
  format: string
  base64: string
  contentType: string
  sizeBytes: number
  renderedAt: string
}

export const useLabelsStore = defineStore('labels', () => {
  const templates = ref<LabelTemplate[]>([])
  const companyName = ref<string>('昆山佰泰胜精密加工')
  const loading = ref<boolean>(false)
  const previewCache = ref<Map<string, LabelPreviewResponse>>(new Map())

  async function loadTemplates(tenantId = 1): Promise<void> {
    loading.value = true
    try {
      const res: any = await http.get('/label-templates', { params: { tenantId } })
      if (res.code === 0 && res.data) {
        templates.value = res.data.templates || []
        companyName.value = res.data.companyName || '昆山佰泰胜精密加工'
      }
    } finally {
      loading.value = false
    }
  }

  async function preview(req: LabelPreviewRequest, tenantId = 1): Promise<LabelPreviewResponse | null> {
    const key = `${req.type}|${req.qrContent}|${(req.lines || []).join('|')}|${req.factoryName || ''}`
    if (previewCache.value.has(key)) return previewCache.value.get(key)!
    loading.value = true
    try {
      const res: any = await http.post('/label-templates/preview', {
        type: req.type,
        data: {
          qrContent: req.qrContent,
          lines: req.lines || [],
          factoryName: req.factoryName,
        },
        format: req.format || 'PNG',
      }, { params: { tenantId } })
      if (res.code === 0 && res.data) {
        previewCache.value.set(key, res.data)
        return res.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  function clearPreviewCache(): void {
    previewCache.value.clear()
  }

  return {
    templates,
    companyName,
    loading,
    loadTemplates,
    preview,
    clearPreviewCache,
  }
})