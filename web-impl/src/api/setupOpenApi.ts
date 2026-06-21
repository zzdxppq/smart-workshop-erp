import { OpenAPI } from '@/api/generated/core/OpenAPI'

function readAuthToken(): string {
  if (typeof localStorage === 'undefined') return ''
  return localStorage.getItem('token') ?? ''
}

/** 生成客户端 BASE 为站点根；路径由 fetch 补丁自动加 erp- 服务前缀 */
export function setupOpenApiClient(): void {
  OpenAPI.BASE = (import.meta as ImportMeta & { env: Record<string, string> }).env.VITE_API_BASE || ''
  OpenAPI.TOKEN = async () => readAuthToken()
  OpenAPI.HEADERS = async () => {
    const token = readAuthToken()
    return token ? { Authorization: `Bearer ${token}` } : {}
  }
  OpenAPI.WITH_CREDENTIALS = false
}
