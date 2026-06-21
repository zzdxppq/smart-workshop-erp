import type { Order } from '@/api/generated/models/Order'
import type { OrderItem } from '@/api/generated/models/OrderItem'

export type OrderFlag = 0 | 1 | boolean | undefined
export type OrderFormOrder = Omit<Order, 'isFa' | 'isNew' | 'isUrgent'> & {
  isFa?: OrderFlag
  isNew?: OrderFlag
  isUrgent?: OrderFlag
}

export type OrderFormItem = Omit<OrderItem, 'isFa' | 'isNew'> & {
  isFa?: OrderFlag
  isNew?: OrderFlag
  customerDrawingNo?: string
  drawingId?: number
  productName?: string
  unitWeight?: number
  processRoute?: string
  materialNo?: string
  bomPreview?: {
    hasBom: boolean
    bomNo?: string
    bomVersion?: string
    totalCost?: number
    items: { materialCode: string; materialName?: string; spec?: string; qty: number; unit?: string; segment?: string }[]
  }
}

function flag(v: unknown): 0 | 1 {
  return v === 1 || v === true ? 1 : 0
}

export function buildOrderCreatePayload(form: { order: OrderFormOrder; items: OrderFormItem[] }) {
  return {
    order: {
      ...form.order,
      isFa: flag(form.order.isFa),
      isNew: flag(form.order.isNew),
      isUrgent: flag(form.order.isUrgent),
    },
    items: form.items
      .filter((item) => item.drawingId || item.drawingNo)
      .map((item) => ({
        ...item,
        isFa: flag(item.isFa),
        isNew: flag(item.isNew),
      })),
  }
}

/** 解析图纸 processRoute JSON 为可读预览 */
export function formatProcessRoutePreview(raw?: string | null): string {
  if (!raw) return ''
  try {
    const j = JSON.parse(raw)
    if (Array.isArray(j)) {
      return j
        .map((x: unknown) => {
          if (typeof x === 'string') return x
          if (x && typeof x === 'object') {
            const o = x as Record<string, unknown>
            return String(o.processName ?? o.name ?? o.stepName ?? '')
          }
          return String(x)
        })
        .filter(Boolean)
        .join(' → ')
    }
  } catch {
    /* plain text */
  }
  if (raw.includes('→')) return raw
  return raw.replace(/[,，]/g, ' → ')
}

export function disablePastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}
