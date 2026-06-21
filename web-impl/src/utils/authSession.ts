import type { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { isExpired } from '@/utils/jwt'

const REFRESH_TOKEN_KEY = 'refresh_token'
const SESSION_EXPIRED_MSG = '登录已过期，请重新登录'

let handlingSessionExpired = false
let refreshingToken: Promise<string | null> | null = null

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(token: string) {
  if (token) localStorage.setItem(REFRESH_TOKEN_KEY, token)
  else localStorage.removeItem(REFRESH_TOKEN_KEY)
}

function isAuthEndpoint(url?: string): boolean {
  if (!url) return false
  const path = url.split('?')[0]
  return path.includes('/auth/login') || path.includes('/auth/refresh')
}

function extractUnauthorizedMessage(err: AxiosError): string {
  const data = err.response?.data as { message?: string; msg?: string } | undefined
  return data?.message || data?.msg || SESSION_EXPIRED_MSG
}

export function isSessionActive(token: string | null | undefined): boolean {
  return Boolean(token) && !isExpired(token)
}

export function handleSessionExpired(message = SESSION_EXPIRED_MSG): void {
  if (handlingSessionExpired) return
  handlingSessionExpired = true

  const auth = useAuthStore()
  auth.logout()
  setRefreshToken('')

  ElMessage.warning(message)

  const redirect = router.currentRoute.value.fullPath
  const isLogin = router.currentRoute.value.name === 'Login'
  if (!isLogin) {
    router.replace({ name: 'Login', query: { redirect, expired: '1' } })
  }

  window.setTimeout(() => {
    handlingSessionExpired = false
  }, 2000)
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return null

  if (!refreshingToken) {
    refreshingToken = (async () => {
      try {
        const auth = useAuthStore()
        return await auth.refreshAccessToken(refreshToken)
      } catch {
        return null
      } finally {
        refreshingToken = null
      }
    })()
  }

  return refreshingToken
}

async function handleUnauthorized(err: AxiosError, retry?: (token: string) => Promise<unknown>): Promise<unknown> {
  const config = err.config as (InternalAxiosRequestConfig & { _authRetried?: boolean }) | undefined
  const url = config?.url

  if (isAuthEndpoint(url)) {
    return Promise.reject(err)
  }

  if (config && !config._authRetried) {
    const newToken = await refreshAccessToken()
    if (newToken && retry) {
      config._authRetried = true
      config.headers.Authorization = `Bearer ${newToken}`
      return retry(newToken)
    }
  }

  handleSessionExpired(extractUnauthorizedMessage(err))
  return Promise.reject(err)
}

export function attachAuthResponseInterceptor(instance: AxiosInstance): void {
  instance.interceptors.response.use(
    (resp) => resp,
    async (err: AxiosError) => {
      if (err.response?.status !== 401) {
        return Promise.reject(err)
      }
      return handleUnauthorized(err, async () => instance.request(err.config!))
    },
  )
}

export function ensureActiveSession(token: string | null | undefined): boolean {
  if (isSessionActive(token)) return true
  handleSessionExpired(SESSION_EXPIRED_MSG)
  return false
}

export function installAuthFetchGuard(): void {
  if (typeof window === 'undefined' || (window as any).__erpAuthFetchGuard) return

  const rawFetch = window.fetch.bind(window)
  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const resp = await rawFetch(input, init)
    if (resp.status !== 401) return resp

    const url = typeof input === 'string'
      ? input
      : input instanceof URL
        ? input.toString()
        : input.url

    if (isAuthEndpoint(url)) return resp

    handleSessionExpired(SESSION_EXPIRED_MSG)
    return resp
  }

  ;(window as any).__erpAuthFetchGuard = true
}
