import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { unwrapResult } from '@/utils/apiPage'

/**
 * 详情页通用加载（按路由 :id 拉取单条记录）
 */
export function useDetailLoad<T = unknown>(fetcher: (id: number) => Promise<unknown>) {
  const route = useRoute()
  const data = ref<T | null>(null)
  const loading = ref(false)

  async function load() {
    const id = Number(route.params.id)
    if (!id) {
      ElMessage.error('无效的记录 ID')
      return
    }
    loading.value = true
    try {
      const r = await fetcher(id)
      data.value = unwrapResult<T>(r)
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '加载失败')
      data.value = null
    } finally {
      loading.value = false
    }
  }

  onMounted(load)

  return { data, loading, load, reload: load, id: () => Number(route.params.id) }
}

/**
 * 详情页通用加载（按路由字符串参数，如 :code、:batchNo）
 */
export function useParamLoad<T = unknown>(
  paramKey: string,
  fetcher: (value: string) => Promise<unknown>,
) {
  const route = useRoute()
  const data = ref<T | null>(null)
  const loading = ref(false)

  async function load() {
    const value = String(route.params[paramKey] ?? '')
    if (!value) {
      ElMessage.error('无效的参数')
      return
    }
    loading.value = true
    try {
      const r = await fetcher(value)
      data.value = unwrapResult<T>(r)
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '加载失败')
      data.value = null
    } finally {
      loading.value = false
    }
  }

  onMounted(load)

  return { data, loading, load, param: () => String(route.params[paramKey] ?? '') }
}
