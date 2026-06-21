import { defineStore } from 'pinia'
import axios from 'axios'
import { resolveGatewayUrl } from '@/utils/serviceRoute'
import { attachAuthResponseInterceptor } from '@/utils/authSession'

/**
 * Base Pinia store with shared api client.
 * Gateway Nacos 路由：/erp-{service}/...
 */
function createApi() {
  const instance = axios.create({
    baseURL: (import.meta as any).env?.VITE_API_BASE || '',
    timeout: 30_000,
  })

  instance.interceptors.request.use((config) => {
    if (config.url) {
      config.url = resolveGatewayUrl(config.url)
    }
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    try {
      const user = JSON.parse(localStorage.getItem('user') || 'null')
      if (user?.id != null) {
        config.headers['X-User-Id'] = String(user.id)
      }
      const roles = user?.roles ?? user?.roleCodes
      if (Array.isArray(roles) && roles.length) {
        config.headers['X-User-Roles'] = roles.join(',')
      } else if (typeof roles === 'string' && roles) {
        config.headers['X-User-Roles'] = roles
      }
    } catch {
      /* ignore */
    }
    return config
  })

  attachAuthResponseInterceptor(instance)

  function unwrap(promise: Promise<any>): Promise<any> {
    return promise.then((r) => r?.data)
  }

  return {
    get: (url: string, config?: any) => unwrap(instance.get(url, config)),
    post: (url: string, data?: any, config?: any) => unwrap(instance.post(url, data, config)),
    put: (url: string, data?: any, config?: any) => unwrap(instance.put(url, data, config)),
    delete: (url: string, config?: any) => unwrap(instance.delete(url, config)),
  }
}

export const useBaseStore = defineStore('base', {
  state: () => ({
    api: createApi() as any,
  }),
})
