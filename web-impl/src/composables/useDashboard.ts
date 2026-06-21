import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.46-1.51 · 工作台 composable
 */
export function useDashboard() {
  const refreshInterval = ref<number>(30_000)
  const lastRefresh = ref<Date>(new Date())
  const autoRefresh = ref(true)

  const needsRefresh = computed(() => autoRefresh.value && Date.now() - lastRefresh.value.getTime() > refreshInterval.value)

  function tick() {
    lastRefresh.value = new Date()
  }

  function setAutoRefresh(v: boolean) {
    autoRefresh.value = v
  }

  function setInterval(ms: number) {
    refreshInterval.value = ms
  }

  return { refreshInterval, lastRefresh, autoRefresh, needsRefresh, tick, setAutoRefresh, setInterval }
}