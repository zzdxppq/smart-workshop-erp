import { defineStore } from 'pinia'
import { useBaseStore } from './_base'
import { hasAnyRole, normalizeRoles } from '@/utils/roleAccess'
import { extractRoles } from '@/utils/jwt'
import { setRefreshToken as persistRefreshToken } from '@/utils/authSession'
import { unwrapResult } from '@/utils/apiPage'

function parseStoredRoles(raw: unknown, token: string): string[] {
  if (Array.isArray(raw)) return normalizeRoles(raw)
  if (typeof raw === 'string') {
    return normalizeRoles(raw.split(',').map((r) => r.trim()).filter(Boolean))
  }
  if (token) return normalizeRoles(extractRoles(token))
  return []
}

function parseStoredMenuPaths(): string[] {
  try {
    const raw = localStorage.getItem('menuPaths')
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((p): p is string => typeof p === 'string' && !!p) : []
  } catch {
    return []
  }
}

function parseStoredPermissions(): string[] {
  try {
    const raw = localStorage.getItem('permissions')
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((p): p is string => typeof p === 'string' && !!p) : []
  } catch {
    return []
  }
}

/**
 * V1.3.7 鉴权 store
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    permissions: parseStoredPermissions(),
    menuPaths: parseStoredMenuPaths(),
  }),
  getters: {
    userRoles(state): string[] {
      const fromUser = state.user?.roles ?? state.user?.roleCodes
      return parseStoredRoles(fromUser, state.token)
    },
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
      if (this.user) {
        const roles = parseStoredRoles(this.user.roles ?? this.user.roleCodes, token)
        this.user = { ...this.user, roles }
        localStorage.setItem('user', JSON.stringify(this.user))
      }
    },
    setUser(user: any) {
      const roles = parseStoredRoles(user?.roles ?? user?.roleCodes, this.token)
      this.user = { ...user, roles }
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    setMenuAccess(menuPaths: string[] | undefined | null, permissions: string[] | undefined | null) {
      this.menuPaths = Array.isArray(menuPaths) ? menuPaths : []
      this.permissions = Array.isArray(permissions) ? permissions : []
      localStorage.setItem('menuPaths', JSON.stringify(this.menuPaths))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
    },
    async fetchMenus(): Promise<void> {
      if (!this.token) return
      try {
        const r = await useBaseStore().api.get('/auth/menus')
        const body = unwrapResult<{ menuPaths?: string[]; permissions?: string[] }>(r)
        if (body && 'menuPaths' in body) {
          this.setMenuAccess(body.menuPaths ?? [], body.permissions ?? [])
        }
      } catch {
        // 静默失败，保留本地缓存或回退 roleAccess
      }
    },
    saveRefreshToken(refreshToken: string) {
      persistRefreshToken(refreshToken)
    },
    async refreshAccessToken(refreshToken: string): Promise<string> {
      const r = await useBaseStore().api.post('/auth/refresh', JSON.stringify(refreshToken), {
        headers: { 'Content-Type': 'application/json' },
      })
      const body = unwrapResult<{
        accessToken?: string
        refreshToken?: string
        roles?: string[]
        menuPaths?: string[]
        permissions?: string[]
        user?: { username?: string; realName?: string; roleCodes?: string[] }
      }>(r)
      const accessToken = body?.accessToken
      if (!accessToken) {
        throw new Error('刷新登录状态失败')
      }
      this.setToken(accessToken)
      if (body.refreshToken) {
        this.saveRefreshToken(body.refreshToken)
      }
      if (body && 'menuPaths' in body) {
        this.setMenuAccess(body.menuPaths ?? [], body.permissions ?? [])
      }
      return accessToken
    },
    logout() {
      this.token = ''
      this.user = null
      this.menuPaths = []
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('menuPaths')
      localStorage.removeItem('permissions')
      persistRefreshToken('')
    },
    async logoutRemote() {
      try {
        await useBaseStore().api.post('/auth/logout')
      } catch {
        // 网络失败仍清除本地会话
      }
      this.logout()
    },
    hasRole(role: string): boolean {
      return hasAnyRole(this.userRoles, [role])
    },
  },
})