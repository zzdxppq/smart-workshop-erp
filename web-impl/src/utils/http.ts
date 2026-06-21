/**
 * 通用 HTTP 工具（V1.3.9 Sprint 12 · Story 12.3）
 */
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'
import { resolveGatewayUrl } from '@/utils/serviceRoute'
import { attachAuthResponseInterceptor } from '@/utils/authSession'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

instance.interceptors.request.use((config) => {
  if (config.url) {
    config.url = resolveGatewayUrl(config.url)
  }
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

instance.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    const message = err?.response?.data?.message || err.message || '网络错误'
    return Promise.reject({ code: err?.response?.data?.code || 50001, message })
  },
)

attachAuthResponseInterceptor(instance)

const http = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config)
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config)
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config)
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config)
  },
}

export default http
