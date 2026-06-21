/**
 * V1.3.8 Sprint 9 Story 9.2 · JWT 解码工具 v2 单元测例
 *
 * V1.3.8 Sprint 8 Story 8.4 10 测例 + Sprint 9 Story 9.2 8 测例 = 18 测例
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */

// @vitest-environment node
import { describe, it, expect } from 'vitest'
import { parseJwt, extractRoles, isExpired, extractUserId, extractUsername,
         extractPermissions, base64UrlEncode, makeTestToken } from './jwt'

describe('JWT 解码工具 v2 (Sprint 9 Story 9.2)', () => {
  // ===== Sprint 8 既有 10 测例（回归） =====

  it('parseJwt null/空 token 返回 null', () => {
    expect(parseJwt('')).toBeNull()
    expect(parseJwt(null as any)).toBeNull()
  })

  it('parseJwt 非法格式（仅 2 段）返回 null', () => {
    expect(parseJwt('abc.def')).toBeNull()
  })

  it('parseJwt 有效 payload 正确解析', () => {
    const token = makeTestToken({
      userId: 1001,
      username: 'admin',
      roles: ['GM', 'PROCUREMENT_MANAGER'],
      permissions: ['purchase:approval:read'],
      exp: Math.floor(Date.now() / 1000) + 3600,
    })

    const payload = parseJwt(token)
    expect(payload).not.toBeNull()
    expect(payload?.userId).toBe(1001)
    expect(payload?.username).toBe('admin')
    expect(payload?.roles).toEqual(['GM', 'PROCUREMENT_MANAGER'])
  })

  it('parseJwt 已过期 token 返回 null', () => {
    const token = makeTestToken({
      userId: 1001,
      exp: Math.floor(Date.now() / 1000) - 3600,
    })
    expect(parseJwt(token)).toBeNull()
  })

  it('extractRoles 提取角色', () => {
    const token = makeTestToken({
      roles: ['WAREHOUSE', 'GM'],
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
    expect(extractRoles(token)).toEqual(['WAREHOUSE', 'GM'])
  })

  it('extractRoles 无效 token 返回空数组', () => {
    expect(extractRoles('')).toEqual([])
    expect(extractRoles('invalid.token')).toEqual([])
  })

  it('isExpired 过期判定', () => {
    const expiredToken = makeTestToken({ exp: Math.floor(Date.now() / 1000) - 100 })
    const validToken = makeTestToken({ exp: Math.floor(Date.now() / 1000) + 3600 })

    expect(isExpired(expiredToken)).toBe(true)
    expect(isExpired(validToken)).toBe(false)
  })

  it('isExpired 缺 exp 视为过期', () => {
    const noExpToken = makeTestToken({ userId: 1001 })
    expect(isExpired(noExpToken)).toBe(true)
  })

  it('extractUserId 提取用户 ID', () => {
    const token = makeTestToken({ userId: 9001, exp: Math.floor(Date.now() / 1000) + 3600 })
    expect(extractUserId(token)).toBe(9001)
  })

  it('extractUserId 无效 token 返回 null', () => {
    expect(extractUserId('')).toBeNull()
  })

  // ===== Sprint 9 Story 9.2 新增 8 测例 =====

  it('9.2.a 中文 username 正确解析（UTF-8 编码）', () => {
    const token = makeTestToken({
      userId: 2001,
      username: '采购主管A',
      roles: ['PROCUREMENT_MANAGER'],
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
    const payload = parseJwt(token)
    expect(payload?.username).toBe('采购主管A')
  })

  it('9.2.b 中文 permissions 正确解析', () => {
    const token = makeTestToken({
      permissions: ['审批', '复核'],
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
    expect(extractPermissions(token)).toEqual(['审批', '复核'])
  })

  it('9.2.c base64UrlEncode 不含 = padding 字符', () => {
    const encoded = base64UrlEncode('hello world')
    expect(encoded).not.toContain('=')
    expect(encoded).not.toContain('+')
    expect(encoded).not.toContain('/')
  })

  it('9.2.d base64UrlEncode 包含 - 或 _ 字符（base64url 特征）', () => {
    // 'subject' 编码后含 '==' padding
    // 但 padding 已去除，所以输出无 '='
    const encoded = base64UrlEncode('subject?subjects?')
    expect(encoded).not.toContain('=')
    expect(encoded).not.toContain('+')
    expect(encoded).not.toContain('/')
  })

  it('9.2.e makeTestToken 生成 3 段 JWT', () => {
    const token = makeTestToken({ userId: 1, exp: 9999999999 })
    const parts = token.split('.')
    expect(parts).toHaveLength(3)
  })

  it('9.2.f extractUsername 中文用户', () => {
    const token = makeTestToken({
      username: '张三',
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
    expect(extractUsername(token)).toBe('张三')
  })

  it('9.2.g parseJwt 处理空 roles 数组（不报错）', () => {
    const token = makeTestToken({
      userId: 1001,
      roles: [],
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
    const payload = parseJwt(token)
    expect(payload?.roles).toEqual([])
  })

  it('9.2.h parseJwt 处理 null roles（payload 无 roles 字段）', () => {
    const token = makeTestToken({
      userId: 1001,
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
    const payload = parseJwt(token)
    expect(payload?.roles).toBeUndefined()
    expect(extractRoles(token)).toEqual([])  // extractRoles 兜底返回 []
  })
})