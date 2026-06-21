#!/usr/bin/env node
/**
 * 全路由 API 冒烟：登录后遍历 platform / business / production 关键端点
 *
 * 用法:
 *   node backend/scripts/api-route-smoke.mjs
 *   API_BASE=http://localhost:9080 node backend/scripts/api-route-smoke.mjs
 *   API_BASE=https://bts.51xiaoping.com ERP_INTERNAL_TOKEN=xxx node backend/scripts/api-route-smoke.mjs
 */
const BASE = (process.env.API_BASE || 'http://localhost:9080').replace(/\/$/, '')
const PASSWORD = process.env.SMOKE_PASSWORD || '123456'
const INTERNAL_TOKEN = process.env.ERP_INTERNAL_TOKEN || process.env.APP_INTERNAL_TOKEN || ''
const SMOKE_EMAIL_TO = process.env.SMOKE_EMAIL_TO || 'zzdxpq@163.com'

function trendRangeQuery() {
  const now = new Date()
  const to = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const fromDate = new Date(now.getFullYear(), now.getMonth() - 5, 1)
  const from = `${fromDate.getFullYear()}-${String(fromDate.getMonth() + 1).padStart(2, '0')}`
  return `from=${from}&to=${to}`
}

/** @typedef {{ method?: string, path: string, user?: string, body?: object, optional?: boolean, headers?: Record<string,string>, expectStatus?: number[] }} RouteCase */

/** @type {RouteCase[]} */
const ROUTES = [
  // ── Platform · admin ──
  { path: '/erp-platform/email/config', user: 'admin' },
  { path: '/erp-platform/email/logs?pageNum=1&pageSize=5', user: 'admin' },
  { path: '/erp-platform/printers', user: 'admin' },
  { path: '/erp-platform/audit/logs?pageNum=1&pageSize=5', user: 'admin' },
  {
    method: 'POST',
    path: '/erp-platform/email/test',
    user: 'admin',
    body: {
      toAddress: SMOKE_EMAIL_TO,
      subject: 'ERP 冒烟测试',
      body: 'api-route-smoke 自动测试邮件',
      ...(process.env.SMTP_AUTH_CODE || process.env.EMAIL_163_AUTH_CODE
        ? { authCode: process.env.SMTP_AUTH_CODE || process.env.EMAIL_163_AUTH_CODE }
        : {}),
    },
    optional: true,
  },

  // ── Business · finance Web 路径 ──
  { path: '/erp-business/finance/receivables?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/payables?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/aging?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/cost?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/cost-accounting?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/payments?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/payment-plan?pageNum=1&pageSize=5', user: 'finance' },
  { path: '/erp-business/finance/profit?pageNum=1&pageSize=5', user: 'finance' },

  // ── Business · OpenAPI 财务根路径 ──
  { path: '/erp-business/cost-accounting/segment', user: 'finance' },
  { path: '/erp-business/receivable/aging', user: 'finance' },

  // ── Business · 物料 / HR / 报表 ──
  { path: '/erp-business/materials?categoryPrefix=WL&size=5', user: 'warehouse' },
  { path: '/erp-business/hr/attendance?pageNum=1&pageSize=5', user: 'hr' },
  { path: '/erp-business/hr/payroll?pageNum=1&pageSize=5', user: 'hr' },
  { path: '/erp-business/hr/performance?pageNum=1&pageSize=5', user: 'hr' },
  { path: '/erp-business/hr/recruitment?pageNum=1&pageSize=5', user: 'hr' },
  { path: '/erp-business/reports/sales-ranking', user: 'gm' },
  { path: `/erp-business/reports/sales-trend?${trendRangeQuery()}`, user: 'gm' },
  { path: '/erp-business/reports/customer-analysis', user: 'gm' },

  // ── Business · 品质 Web 路径 ──
  { path: '/erp-business/quality/fa?pageNum=1&pageSize=5', user: 'qc' },
  { path: '/erp-business/quality/cmm?pageNum=1&pageSize=5', user: 'qc' },
  { path: '/erp-business/quality/defects?pageNum=1&pageSize=5', user: 'qc' },
  { path: '/erp-business/quality/pickups?pageNum=1&pageSize=5', user: 'qc' },

  // ── Business · 销售 / 采购（读） ──
  { path: '/erp-business/customers?pageNum=1&pageSize=5', user: 'sales' },
  { path: '/erp-business/quotes?pageNum=1&pageSize=5', user: 'sales' },
  { path: '/erp-business/orders?pageNum=1&pageSize=5', user: 'sales' },
  { path: '/erp-business/rfq?pageNum=1&pageSize=5', user: 'buyer' },

  // ── Production ──
  { path: '/erp-production/workorders?pageNum=1&pageSize=5', user: 'prod_mgr' },

  // ── Internal（Feign · 可选 token） ──
  {
    path: '/erp-business/internal/product-route/routes/DEMO-PRODUCT',
    headers: INTERNAL_TOKEN ? { 'X-Internal-Token': INTERNAL_TOKEN } : {},
    optional: !INTERNAL_TOKEN,
    expectStatus: INTERNAL_TOKEN ? [200] : [200, 401],
  },
]

const tokenCache = new Map()

async function login(username) {
  if (tokenCache.has(username)) return tokenCache.get(username)
  const res = await fetch(`${BASE}/erp-platform/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: PASSWORD }),
  })
  const json = await res.json().catch(() => ({}))
  const token = json?.data?.accessToken
  if (!token) {
    throw new Error(`登录失败 ${username}: HTTP ${res.status} ${JSON.stringify(json)}`)
  }
  tokenCache.set(username, token)
  return token
}

async function hitRoute(route) {
  const method = (route.method || 'GET').toUpperCase()
  const url = `${BASE}${route.path}`
  const headers = { Accept: 'application/json', ...(route.headers || {}) }
  if (route.user) {
    headers.Authorization = `Bearer ${await login(route.user)}`
  }
  const init = { method, headers }
  if (route.body && method !== 'GET') {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(route.body)
  }
  const res = await fetch(url, init)
  let body = null
  const ct = res.headers.get('content-type') || ''
  if (ct.includes('application/json')) {
    body = await res.json().catch(() => null)
  }
  const okStatuses = route.expectStatus || [200]
  const httpOk = okStatuses.includes(res.status)
  const bizOk = body == null || body.code === undefined || body.code === 0
  return { httpOk, bizOk, status: res.status, code: body?.code, message: body?.message }
}

async function main() {
  console.log(`\n=== API Route Smoke ===`)
  console.log(`BASE=${BASE}`)
  console.log(`ROUTES=${ROUTES.length}\n`)

  const results = []
  for (const route of ROUTES) {
    const label = `${route.method || 'GET'} ${route.path}${route.user ? ` [${route.user}]` : ''}`
    try {
      const r = await hitRoute(route)
      const pass = r.httpOk && r.bizOk
      results.push({ label, pass, optional: route.optional, ...r })
      const icon = pass ? '✅' : route.optional ? '⚠️' : '❌'
      const detail = pass ? 'OK' : `HTTP ${r.status} code=${r.code} ${r.message || ''}`.trim()
      console.log(`${icon} ${label} → ${detail}`)
    } catch (e) {
      results.push({ label, pass: false, optional: route.optional, error: String(e) })
      const icon = route.optional ? '⚠️' : '❌'
      console.log(`${icon} ${label} → ${e.message || e}`)
    }
  }

  const hard = results.filter((r) => !r.optional)
  const failed = hard.filter((r) => !r.pass)
  const optionalFailed = results.filter((r) => r.optional && !r.pass)

  console.log('\n--- 汇总 ---')
  console.log(`必测: ${hard.length - failed.length}/${hard.length} 通过`)
  if (failed.length) {
    console.log(`失败 ${failed.length}:`)
    failed.forEach((f) => console.log(`  · ${f.label}`))
  }
  if (optionalFailed.length) {
    console.log(`可选/依赖环境 ${optionalFailed.length} 条未通过（SMTP、Internal Token 等）`)
  }

  process.exit(failed.length ? 1 : 0)
}

main()
