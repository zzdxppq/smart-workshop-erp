/**
 * V1.3.8 Sprint 8 Story 8.4 · JWT 工具单元测例（Node.js 内置 test runner）
 *
 * V1.3.8 实施说明：web-impl vitest 配置依赖 jsdom，但 node_modules 未装 jsdom
 * 切换到 Node.js 内置 test runner（Node 18+）— 纯函数测试无 DOM 依赖
 *
 * 运行：node scripts/run-jwt-tests.mjs
 */

import { test } from 'node:test'
import { strict as assert } from 'node:assert'
import { parseJwt, extractRoles, isExpired, extractUserId, extractUsername, extractPermissions } from '../src/utils/jwt.ts'

/**
 * 构造测试 JWT：header.payload.signature（payload base64url 编码 JSON）
 */
function makeTestToken(payload) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    .toString('base64').replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
  const body = Buffer.from(JSON.stringify(payload))
    .toString('base64').replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `${header}.${body}.fake-signature`
}

test('parseJwt null/空 token 返回 null', () => {
  assert.equal(parseJwt(''), null)
  assert.equal(parseJwt(null), null)
  assert.equal(parseJwt(undefined), null)
})

test('parseJwt 非法格式（仅 2 段）返回 null', () => {
  assert.equal(parseJwt('abc.def'), null)
  assert.equal(parseJwt('only-one-segment'), null)
})

test('parseJwt 有效 payload 正确解析', () => {
  const token = makeTestToken({
    userId: 1001,
    username: 'admin',
    roles: ['GM', 'PROCUREMENT_MANAGER'],
    permissions: ['purchase:approval:read'],
    exp: Math.floor(Date.now() / 1000) + 3600,
  })

  const payload = parseJwt(token)
  assert.ok(payload)
  assert.equal(payload.userId, 1001)
  assert.equal(payload.username, 'admin')
  assert.deepEqual(payload.roles, ['GM', 'PROCUREMENT_MANAGER'])
})

test('parseJwt 已过期 token 返回 null', () => {
  const token = makeTestToken({
    userId: 1001,
    exp: Math.floor(Date.now() / 1000) - 3600,
  })
  assert.equal(parseJwt(token), null)
})

test('extractRoles 提取角色', () => {
  const token = makeTestToken({
    roles: ['WAREHOUSE', 'GM'],
    exp: Math.floor(Date.now() / 1000) + 3600,
  })
  assert.deepEqual(extractRoles(token), ['WAREHOUSE', 'GM'])
})

test('extractRoles 无效 token 返回空数组', () => {
  assert.deepEqual(extractRoles(''), [])
  assert.deepEqual(extractRoles('invalid.token'), [])
})

test('extractPermissions 提取权限', () => {
  const token = makeTestToken({
    permissions: ['purchase:approval:read', 'purchase:no-order:create'],
    exp: Math.floor(Date.now() / 1000) + 3600,
  })
  assert.deepEqual(extractPermissions(token), [
    'purchase:approval:read',
    'purchase:no-order:create',
  ])
})

test('isExpired 过期判定', () => {
  const expired = makeTestToken({ exp: Math.floor(Date.now() / 1000) - 100 })
  const valid = makeTestToken({ exp: Math.floor(Date.now() / 1000) + 3600 })

  assert.equal(isExpired(expired), true)
  assert.equal(isExpired(valid), false)
})

test('isExpired 缺 exp 视为过期', () => {
  const noExp = makeTestToken({ userId: 1001 })
  assert.equal(isExpired(noExp), true)
})

test('extractUserId 提取用户 ID', () => {
  const token = makeTestToken({ userId: 9001, exp: Math.floor(Date.now() / 1000) + 3600 })
  assert.equal(extractUserId(token), 9001)
})

test('extractUserId 无效 token 返回 null', () => {
  assert.equal(extractUserId(''), null)
})

test('extractUsername 提取用户名', () => {
  const token = makeTestToken({
    username: 'admin',
    exp: Math.floor(Date.now() / 1000) + 3600,
  })
  assert.equal(extractUsername(token), 'admin')
})

test('JWT 含中文 username 正确解析', () => {
  const token = makeTestToken({
    userId: 2001,
    username: '采购主管A',
    roles: ['PROCUREMENT_MANAGER'],
    exp: Math.floor(Date.now() / 1000) + 3600,
  })
  const payload = parseJwt(token)
  assert.equal(payload.username, '采购主管A')
})

console.log('✅ JWT 工具 Node.js 内置测试已加载（13 测例）')
console.log('   运行：node scripts/run-jwt-tests.mjs')