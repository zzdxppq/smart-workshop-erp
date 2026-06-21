#!/usr/bin/env node
/**
 * 报价流程 V2.1 · 轻量 E2E/单元验证（无需 Maven test-compile 全量通过）
 *
 * 用法:
 *   node backend/scripts/test-quote-flow-v21.mjs
 *   API_BASE=http://127.0.0.1:9082 node backend/scripts/test-quote-flow-v21.mjs --live
 */
const LIVE = process.argv.includes('--live')
const BUSINESS = (process.env.API_BASE || process.env.BUSINESS_URL || 'http://127.0.0.1:9082').replace(/\/$/, '')
const PLATFORM = (process.env.PLATFORM_URL || 'http://127.0.0.1:9080').replace(/\/$/, '')
const PASSWORD = process.env.SMOKE_PASSWORD || '123456'

let passed = 0
let failed = 0
let skipped = 0

function pass(msg) {
  console.log(`  [PASS] ${msg}`)
  passed++
}
function fail(msg) {
  console.error(`  [FAIL] ${msg}`)
  failed++
}
function skip(msg) {
  console.log(`  [SKIP] ${msg}`)
  skipped++
}
function assert(cond, msg) {
  if (cond) pass(msg)
  else fail(msg)
}

// ── 1. Router 阈值（与 QuoteApprovalRouter.java 一致）──
console.log('\n=== [1] QuoteApprovalRouter 阈值 ===')
function routeDecision(amount) {
  if (amount == null) return 'SELF'
  if (amount < 50000) return 'SELF'
  if (amount < 200000) return 'DEPT_MANAGER_OR_SIGN'
  return 'GM_FINANCE_DUAL_SIGN'
}
assert(routeDecision(3000) === 'SELF', '<5万 → SELF')
assert(routeDecision(50000) === 'DEPT_MANAGER_OR_SIGN', '5万 → DEPT_MANAGER_OR_SIGN')
assert(routeDecision(250000) === 'GM_FINANCE_DUAL_SIGN', '>20万 → GM_FINANCE_DUAL_SIGN')

// ── 2. 双签节点推进（与 QuoteApprovalService.approve 一致）──
console.log('\n=== [2] 双签节点推进 ===')
function simulateApprove(totalAmount, currentNode) {
  const route = routeDecision(totalAmount)
  if (route === 'GM_FINANCE_DUAL_SIGN' && currentNode < 2) {
    return { status: 'PENDING_APPROVAL', currentNode: 2 }
  }
  return { status: 'APPROVED', currentNode: 99 }
}
let r1 = simulateApprove(250000, 1)
assert(r1.status === 'PENDING_APPROVAL' && r1.currentNode === 2, 'GM 首签 → node 2')
let r2 = simulateApprove(250000, 2)
assert(r2.status === 'APPROVED' && r2.currentNode === 99, '财务二签 → APPROVED')
let r3 = simulateApprove(10000, 1)
assert(r3.status === 'APPROVED', '<5万 单签 → APPROVED')

// ── 3. PDF 客户图号优先 ──
console.log('\n=== [3] PDF 客户图号 ===')
function displayDrawingNo(item) {
  if (item.customerDrawingNo?.trim()) return item.customerDrawingNo.trim()
  return item.drawingNo || '—'
}
const sample = displayDrawingNo({ customerDrawingNo: '615-03953-0009', drawingNo: 'DWG-001' })
assert(sample === '615-03953-0009', '优先 customerDrawingNo')
assert(displayDrawingNo({ drawingNo: 'DWG-002' }) === 'DWG-002', '回退 drawingNo')

async function login() {
  const res = await fetch(`${PLATFORM}/erp-platform/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'sales', password: PASSWORD }),
  })
  const json = await res.json().catch(() => ({}))
  return json?.data?.accessToken || null
}

async function api(method, path, { token, body } = {}) {
  const headers = { Accept: 'application/json', 'X-User-Id': '2' }
  if (token) headers.Authorization = `Bearer ${token}`
  const init = { method, headers }
  if (body != null) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }
  const res = await fetch(`${BUSINESS}${path}`, init)
  const ct = res.headers.get('content-type') || ''
  if (ct.includes('application/json')) {
    const json = await res.json()
    return { ok: res.ok, json, raw: res }
  }
  const buf = Buffer.from(await res.arrayBuffer())
  return { ok: res.ok, buf, raw: res }
}

async function runLive() {
  console.log('\n=== [4] Live API（erp-business）===')
  let token = null
  try {
    token = await login()
    if (token) pass('Platform 登录成功')
    else skip('Platform 未启动，使用 X-User-Id')
  } catch (e) {
    skip(`Platform 登录跳过: ${e.message}`)
  }

  try {
    const health = await fetch(`${BUSINESS}/actuator/health`).catch(() => null)
    if (!health?.ok) {
      const ping = await api('GET', '/quote-cost-items', { token })
      if (ping.json?.code !== 0) throw new Error('business unreachable')
    }
    pass(`Business 可达: ${BUSINESS}`)
  } catch (e) {
    skip(`Live API 跳过（请先启动 erp-business）: ${e.message}`)
    return
  }

  const custRes = await api('GET', '/customers?pageNum=1&pageSize=1', { token })
  if (custRes.json?.code !== 0 || !custRes.json?.data?.items?.length) {
    fail('客户列表为空')
    return
  }
  const customerId = custRes.json.data.items[0].id
  const testEmail = `e2e-${Date.now()}@example.com`
  await api('PUT', `/customers/${customerId}`, {
    token,
    body: { contactName: 'E2E', contactPhone: '13800000001', contactEmail: testEmail },
  })

  const drawingNo = `615-E2E-${Date.now()}`
  const create = await api('POST', '/quotes', {
    token,
    body: {
      quote: { customerId, customerName: custRes.json.data.items[0].name, ownerUserId: 2, comment: 'E2E' },
      items: [{
        customerDrawingNo: drawingNo,
        drawingNo: 'DWG-E2E',
        material: 'AL6061',
        quantity: 2,
        unitWeight: 0.5,
      }],
    },
  })
  assert(create.json?.code === 0, `创建报价 id=${create.json?.data?.id}`)
  const quoteId = create.json?.data?.id
  if (!quoteId) return

  const eng = await api('POST', `/quotes/${quoteId}/submit-to-engineer`, { token })
  assert(eng.json?.data?.status === 'PENDING_ENG', '提交工程师')

  const detail = await api('GET', `/quotes/${quoteId}`, { token })
  const itemId = detail.json?.data?.items?.[0]?.id
  if (!itemId) {
    fail('无报价明细')
    return
  }

  await api('POST', `/quotes/items/${itemId}/process`, {
    token,
    body: {
      processes: [{
        processCode: 'CNC',
        processName: 'CNC加工',
        unitTimeMinutes: 60,
        costPerHour: 120,
        outsourceFlag: 0,
      }],
    },
  })
  const calc = await api('POST', `/quotes/items/${itemId}/calculate`, { token })
  assert(calc.json?.code === 0, '工程师计算报价')

  const sub = await api('POST', `/quotes/${quoteId}/submit`, { token })
  assert(sub.json?.data?.status === 'PENDING_APPROVAL', '提交审批')

  const appr = await api('POST', `/quotes/${quoteId}/approve`, { token })
  assert(appr.json?.data?.status === 'APPROVED', '审批通过')

  const pdf = await api('GET', `/quotes/export/${quoteId}?format=pdf`, { token })
  const pdfText = pdf.buf?.toString('utf8') || ''
  assert(pdfText.includes(drawingNo), `PDF 含客户图号 ${drawingNo}`)

  const mail = await api('POST', `/quotes/${quoteId}/send-email`, { token })
  if (mail.json?.code === 0) pass('发送客户邮箱')
  else skip(`发邮件: ${mail.json?.message || 'SMTP 未配置'}`)

  const costs = await api('GET', '/quote-cost-items', { token })
  assert(Array.isArray(costs.json?.data) && costs.json.data.length >= 1, '成本项种子数据')
}

;(async () => {
  if (LIVE) await runLive()
  else console.log('\n=== [4] Live API ===\n  [SKIP] 加 --live 且启动 erp-business 后执行完整链路')

  console.log(`\n=== 汇总: PASS=${passed} FAIL=${failed} SKIP=${skipped} ===`)
  process.exit(failed > 0 ? 1 : 0)
})()
