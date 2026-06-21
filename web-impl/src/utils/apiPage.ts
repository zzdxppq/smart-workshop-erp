/**
 * 统一解析后端 Result + 分页结构
 */
export function unwrapResult<T = unknown>(r: unknown): T {
  const body = r as { code?: number; message?: string; data?: T }
  if (body && typeof body.code === 'number' && body.code !== 0) {
    throw new Error(body.message || '请求失败')
  }
  return (body?.data ?? r) as T
}

export function parsePageItems(r: unknown): { items: unknown[]; total: number } {
  const data = unwrapResult<Record<string, unknown>>(r)
  const items = (data?.records || data?.items || data?.list || (Array.isArray(data) ? data : [])) as unknown[]
  const total = Number(data?.total ?? data?.totalCount ?? items.length)
  return { items, total }
}
