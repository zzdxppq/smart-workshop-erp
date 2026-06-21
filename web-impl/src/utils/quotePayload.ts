import type { Quote } from '@/api/generated/models/Quote'
import type { QuoteItem } from '@/api/generated/models/QuoteItem'

export type QuoteFlag = 0 | 1 | boolean | undefined
export type QuoteFormQuote = Omit<Quote, 'isFa' | 'isNew'> & {
  isFa?: QuoteFlag
  isNew?: QuoteFlag
  engineerCompleted?: number
}
export type QuoteFormItem = Omit<QuoteItem, 'isFa' | 'isNew'> & {
  isFa?: QuoteFlag
  isNew?: QuoteFlag
  customerDrawingNo?: string
  drawingId?: number
  productName?: string
  unitWeight?: number
  processRoute?: string
}

function flag(v: unknown): 0 | 1 {
  return v === 1 || v === true ? 1 : 0
}

/** 后端 CrmQuote.isFa/isNew 为 Integer，避免 boolean 导致 500 */
export function buildQuoteCreatePayload(form: { quote: QuoteFormQuote; items: QuoteFormItem[] }) {
  return {
    quote: {
      ...form.quote,
      isFa: flag(form.quote.isFa),
      isNew: flag(form.quote.isNew),
    },
    items: form.items.map((item) => ({
      ...item,
      isFa: flag(item.isFa),
      isNew: flag(item.isNew),
    })),
  }
}

export function disablePastDate(date: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}
