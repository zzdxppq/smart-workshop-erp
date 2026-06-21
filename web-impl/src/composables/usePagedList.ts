import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { parsePageItems } from '@/utils/apiPage'

/**
 * 列表页通用分页加载（对接 store / generated API 的 Result 结构）
 */
export function usePagedList<T = unknown>(
  fetcher: (params: Record<string, unknown>) => Promise<unknown>,
) {
  const items = ref<T[]>([])
  const loading = ref(false)
  const pageNum = ref(1)
  const pageSize = ref(20)
  const total = ref(0)

  async function reload(filters: Record<string, unknown> = {}) {
    loading.value = true
    try {
      const r = await fetcher({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        page: pageNum.value - 1,
        size: pageSize.value,
        ...filters,
      })
      const { items: list, total: t } = parsePageItems(r)
      items.value = list as T[]
      total.value = t
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '加载失败')
      items.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  return { items, loading, pageNum, pageSize, total, reload }
}
