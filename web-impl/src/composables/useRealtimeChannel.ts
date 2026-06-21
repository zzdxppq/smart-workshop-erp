import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useBaseStore } from '@/stores/_base'
import { resolveGatewayUrl } from '@/utils/serviceRoute'

export type RealtimeChannel =
  | 'dashboard:kpi'
  | 'dashboard:kanban'
  | 'dashboard:events'
  | 'dashboard:outsource'
  | 'schedule:machine'
  | 'approval:new'
  | 'message:new'
  | 'scan:progress'
  | 'inventory:alert'
  | 'payment:remind'

export interface RealtimeOptions {
  /** WS 频道名 */
  channel: RealtimeChannel
  /** HTTP 轮询兜底 URL（相对业务路径，如 /dashboard/index） */
  pollUrl?: string
  pollIntervalMs?: number
  onMessage?: (data: unknown) => void
  enabled?: boolean
}

const WS_BASE = (import.meta as ImportMeta & { env: Record<string, string> }).env.VITE_WS_BASE
  || `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/erp-business/ws`

function buildWsUrl(channel: RealtimeChannel): string {
  return `${WS_BASE}?channel=${encodeURIComponent(channel)}`
}

/**
 * 原生 WebSocket + SSE + HTTP 轮询兜底（Spec 附录 B.2）
 */
export function useRealtimeChannel(options: RealtimeOptions) {
  const connected = ref(false)
  const lastPayload = ref<unknown>(null)
  const error = ref<string | null>(null)
  let ws: WebSocket | null = null
  let pollTimer: ReturnType<typeof setInterval> | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let backoffMs = 1000

  const api = useBaseStore().api

  async function pollOnce() {
    if (!options.pollUrl) return
    try {
      const data = await api.get(options.pollUrl)
      lastPayload.value = data
      options.onMessage?.(data)
      error.value = null
    } catch (e: unknown) {
      error.value = (e as { message?: string })?.message || 'poll failed'
    }
  }

  function startPolling() {
    if (!options.pollUrl) return
    const interval = options.pollIntervalMs ?? 5000
    pollOnce()
    pollTimer = setInterval(pollOnce, interval)
  }

  function stopPolling() {
    if (pollTimer) clearInterval(pollTimer)
    pollTimer = null
  }

  function connectWs() {
    if (options.channel === 'inventory:alert' || options.channel === 'payment:remind') {
      connectSse()
      return
    }
    try {
      ws = new WebSocket(buildWsUrl(options.channel))
      ws.onopen = () => {
        connected.value = true
        backoffMs = 1000
        error.value = null
        stopPolling()
      }
      ws.onmessage = (ev) => {
        try {
          const data = JSON.parse(ev.data)
          lastPayload.value = data
          options.onMessage?.(data)
        } catch {
          lastPayload.value = ev.data
          options.onMessage?.(ev.data)
        }
      }
      ws.onclose = () => {
        connected.value = false
        if (options.pollUrl && !pollTimer) startPolling()
        scheduleReconnect()
      }
      ws.onerror = () => {
        connected.value = false
        ws?.close()
        if (options.pollUrl && !pollTimer) startPolling()
      }
    } catch {
      if (options.pollUrl) startPolling()
    }
  }

  let sse: EventSource | null = null

  function connectSse() {
    const url = resolveGatewayUrl(`/sse/${options.channel.replace(':', '/')}`)
    try {
      sse = new EventSource(url)
      sse.onopen = () => { connected.value = true }
      sse.onmessage = (ev) => {
        try {
          const data = JSON.parse(ev.data)
          lastPayload.value = data
          options.onMessage?.(data)
        } catch {
          lastPayload.value = ev.data
        }
      }
      sse.onerror = () => {
        connected.value = false
        sse?.close()
        startPolling()
      }
    } catch {
      startPolling()
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      backoffMs = Math.min(backoffMs * 2, 30000)
      connectWs()
    }, backoffMs)
  }

  function disconnect() {
    stopPolling()
    if (reconnectTimer) clearTimeout(reconnectTimer)
    ws?.close()
    sse?.close()
    ws = null
    sse = null
    connected.value = false
  }

  onMounted(() => {
    if (options.enabled === false) return
    connectWs()
  })

  onUnmounted(disconnect)

  watch(() => options.enabled, (v) => {
    if (v === false) disconnect()
    else connectWs()
  })

  return { connected, lastPayload, error, disconnect, refresh: pollOnce }
}
