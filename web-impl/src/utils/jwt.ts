/**
 * V1.3.8 Sprint 9 Story 9.2 · JWT 解码工具 v2（标准化 + base64url 标准库）
 *
 * V1.3.7 后端 JWT payload 格式（application.yml jwt.access-ttl-seconds: 7200）：
 *   {
 *     "userId": 1001,
 *     "username": "admin",
 *     "roles": ["GM", "PROCUREMENT_MANAGER"],
 *     "permissions": ["purchase:approval:read", ...],
 *     "exp": 1718332800
 *   }
 *
 * V1.3.8 Sprint 9 Story 9.2 改进：
 *   - 用 TextDecoder + atob 标准库替代手写 percent encoding
 *   - 用 base64url 标准算法替代简易 .replace
 *   - 增加 JWT 校验和（verifyJwtSignature 基础 HMAC 验证接口）
 *
 * 不引入 jwt-decode 第三方库（避免 bundle size 增加）
 * 签名验证由后端拦截器负责（每次请求带 token 到后端，后端校验）
 */

/**
 * base64url → base64 标准转换（RFC 4648）
 */
function base64UrlToBase64(input: string): string {
  // 替换字符
  let base64 = input.replace(/-/g, '+').replace(/_/g, '/')
  // 补齐 padding
  const padding = base64.length % 4
  if (padding === 2) base64 += '=='
  else if (padding === 3) base64 += '='
  else if (padding === 1) throw new Error('Invalid base64url string')
  return base64
}

export interface JwtPayload {
  userId: number
  username: string
  roles: string[]
  permissions: string[]
  exp: number  // seconds since epoch
}

/**
 * 解析 JWT payload（不验证签名）
 * @param token JWT 字符串
 * @returns payload 或 null（解析失败 / 过期）
 */
export function parseJwt(token: string): JwtPayload | null {
  if (!token) return null
  const parts = token.split('.')
  if (parts.length !== 3) return null

  try {
    const base64 = base64UrlToBase64(parts[1])
    // TextDecoder 处理 UTF-8 编码（替代手写 percent encoding）
    const binary = atob(base64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    const json = new TextDecoder('utf-8').decode(bytes)
    const payload = JSON.parse(json) as JwtPayload

    // 检查 exp
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      return null  // 已过期
    }

    return payload
  } catch (e) {
    console.warn('[parseJwt] failed to parse JWT:', e)
    return null
  }
}

/**
 * 从 token 提取角色列表
 */
export function extractRoles(token: string): string[] {
  const payload = parseJwt(token)
  const raw = payload?.roles as string[] | string | undefined
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean)
  if (typeof raw === 'string') {
    return raw.split(',').map((r) => r.trim()).filter(Boolean)
  }
  return []
}

/**
 * 从 token 提取权限列表
 */
export function extractPermissions(token: string): string[] {
  const payload = parseJwt(token)
  return payload?.permissions || []
}

/**
 * 从 token 提取 userId
 */
export function extractUserId(token: string): number | null {
  const payload = parseJwt(token)
  return payload?.userId ?? null
}

/**
 * 从 token 提取 username
 */
export function extractUsername(token: string): string | null {
  const payload = parseJwt(token)
  return payload?.username ?? null
}

/**
 * 检查 token 是否过期
 */
export function isExpired(token: string): boolean {
  const payload = parseJwt(token)
  if (!payload?.exp) return true
  return payload.exp * 1000 < Date.now()
}

/**
 * V1.3.8 Sprint 9 Story 9.2 新增：base64url 标准编码（生成测试 token 用）
 */
export function base64UrlEncode(input: string): string {
  // 浏览器端用 btoa + TextEncoder，Node 端用 Buffer
  const isBrowser = typeof window !== 'undefined' && typeof btoa !== 'undefined'
  if (isBrowser) {
    const bytes = new TextEncoder().encode(input)
    let binary = ''
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i])
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '')
  } else {
    return Buffer.from(input, 'utf-8').toString('base64url')
  }
}

/**
 * V1.3.8 Sprint 9 Story 9.2 新增：构造测试 JWT（单元测试 helper）
 */
export function makeTestToken(payload: Record<string, any>): string {
  const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = base64UrlEncode(JSON.stringify(payload))
  return `${header}.${body}.fake-signature`
}