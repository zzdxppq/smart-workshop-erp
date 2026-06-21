import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { unwrapResult, parsePageItems } from '@/utils/apiPage'

/**
 * 看板/工作台单对象加载
 */
export function useDashboardStat<T = Record<string, unknown>>(loader: () => Promise<unknown>) {
  const data = ref<T | null>(null)
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      data.value = unwrapResult<T>(await loader())
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '加载失败')
      data.value = null
    } finally {
      loading.value = false
    }
  }

  return { data, loading, load }
}

/**
 * 看板/工作台列表加载
 */
export function useDashboardList<T = unknown>(loader: () => Promise<unknown>) {
  const items = ref<T[]>([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      const r = await loader()
      items.value = parsePageItems(r).items as T[]
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '加载失败')
      items.value = []
    } finally {
      loading.value = false
    }
  }

  return { items, loading, load }
}
