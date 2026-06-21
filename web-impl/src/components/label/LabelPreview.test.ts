/**
 * LabelPreview 组件测例（V1.3.9 Sprint 12 · Story 12.3 · TC-12.3.4.1）
 *
 * <p>验证 web-impl LabelPreview 调 /label-templates/preview 拿 base64 渲染
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// mock http util
vi.mock('@/utils/http', () => ({
  default: {
    get: vi.fn().mockResolvedValue({
      code: 0,
      data: {
        templates: [{
          type: 'GD', name: '工单码', prefix: 'GD-', colorStrip: '#1E40AF',
          reuseFrom: null, dpi: 300, enabled: true, qrExample: 'GD-260614-001',
        }],
        companyName: '昆山佰泰胜精密加工',
      },
    }),
    post: vi.fn().mockResolvedValue({
      code: 0,
      data: {
        type: 'GD',
        format: 'PNG',
        base64: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=',
        contentType: 'image/png',
        sizeBytes: 70,
        renderedAt: '2026-06-14T10:00:00',
      },
    }),
  },
}))

import LabelPreview from './LabelPreview.vue'

describe('LabelPreview · web-impl 跨仓一致性 (TC-12.3.4.1)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mounts and renders GD label preview', async () => {
    const wrapper = mount(LabelPreview, {
      props: {
        type: 'GD',
        qrContent: 'GD-260614-001',
        lines: ['GD-260614-001', '工单：WO20260614001', '工序：P03', '数量：50', '日期：2026-06-14'],
      },
    })
    await flushPromises()
    await flushPromises()

    const img = wrapper.find('[data-testid="label-preview-img"]')
    expect(img.exists()).toBe(true)
    const src = img.attributes('src') || ''
    expect(src).toMatch(/^data:image\/png;base64,/)
    expect(src).toContain('iVBORw0KGgo')
  })

  it('fetches meta on mount (colorStrip #1E40AF for GD)', async () => {
    const wrapper = mount(LabelPreview, {
      props: { type: 'GD', qrContent: 'GD-260614-001', lines: [] },
    })
    await flushPromises()
    const http = (await import('@/utils/http')).default
    expect(http.get).toHaveBeenCalledWith('/label-templates',
      expect.objectContaining({ params: expect.objectContaining({ type: 'GD' }) }))
  })

  it('passes qrContent through to /preview (no encoding, plain text)', async () => {
    const wrapper = mount(LabelPreview, {
      props: {
        type: 'LZ',
        qrContent: 'LZ-260613-001-P03', // 含 -P03 工序后缀
        lines: ['LZ-260613-001-P03'],
      },
    })
    await flushPromises()
    const http = (await import('@/utils/http')).default
    const postCalls = (http.post as any).mock.calls
    const previewCall = postCalls.find((c: any[]) => c[0] === '/label-templates/preview')
    expect(previewCall).toBeTruthy()
    expect(previewCall[1].data.data.qrContent).toBe('LZ-260613-001-P03')
    expect(previewCall[1].data.data.qrContent).not.toContain('base64')
  })

  it('renders error state when qrContent empty', async () => {
    const wrapper = mount(LabelPreview, {
      props: { type: 'GD', qrContent: '', lines: [] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('qrContent 不能为空')
  })
})