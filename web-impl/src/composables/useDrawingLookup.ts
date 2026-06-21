import { E3DrawingService } from '@/api/generated/services/E3DrawingService'
import type { Drawing } from '@/api/generated/models/Drawing'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

export interface DrawingLookupInput {
  drawingId?: number | null
  drawingNo?: string | null
  materialCode?: string | null
}

/** 按图号 / 物料编码解析图纸 ID（列表精确匹配优先） */
export async function resolveDrawingId(input: DrawingLookupInput): Promise<number | null> {
  if (input.drawingId) return input.drawingId
  const kw = (input.drawingNo || input.materialCode || '').trim()
  if (!kw) return null
  try {
    const r = await E3DrawingService.listDrawings(kw, undefined, undefined, undefined, undefined, undefined, 0, 20)
    const { items } = parsePageItems(r)
    const list = items as Drawing[]
    if (!list.length) return null
    const byNo = input.drawingNo
      ? list.find((d) => d.drawingNo === input.drawingNo)
      : undefined
    if (byNo?.id) return byNo.id
    const byMat = input.materialCode
      ? list.find((d) => d.materialCode === input.materialCode)
      : undefined
    if (byMat?.id) return byMat.id
    return list[0]?.id ?? null
  } catch {
    return null
  }
}

export async function fetchDrawingById(id: number): Promise<Drawing | null> {
  try {
    return unwrapResult<Drawing>(await E3DrawingService.getDrawing(id))
  } catch {
    return null
  }
}
