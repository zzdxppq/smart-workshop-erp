/**
 * Gap 矩阵路由冒烟：校验 PRD Story 对应路由与 Vue 文件存在
 * 用法: node scripts/gap-matrix-smoke.mjs
 */
import { readFileSync, existsSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')
const routerPath = join(root, 'src/router/index.ts')
const routerSrc = readFileSync(routerPath, 'utf8')

/** Epic → Story → { route, component import path fragment } */
const MATRIX = [
  // E1
  { epic: 1, story: 'E1-S1', route: '/admin/users', file: 'views/admin/Users.vue' },
  { epic: 1, story: 'E1-S2', route: '/admin/workflows', file: 'views/admin/Workflows.vue' },
  { epic: 1, story: 'E1-S3', route: '/admin/dict', file: 'views/admin/Dict.vue' },
  { epic: 1, story: 'E1-S5', route: '/admin/email-templates', file: 'views/admin/EmailTemplates.vue' },
  { epic: 1, story: 'E1-S6', route: '/admin/field-encryption', file: 'views/admin/FieldEncryption.vue' },
  { epic: 1, story: 'E1-S4', route: 'android:ScanScreen', file: '../android-impl/app/src/main/kotlin/com/btsheng/erp/feature/scan/ScanScreen.kt' },
  // E2
  { epic: 2, story: 'E2-S1', route: '/sales/customers', file: 'views/sales/Customers.vue' },
  { epic: 2, story: 'E2-S1', route: '/sales/customers/:id', file: 'views/sales/CustomerDetail.vue' },
  { epic: 2, story: 'E2-S1', route: '/sales/customer/protection', file: 'views/sales/CustomerProtection.vue' },
  { epic: 2, story: 'E2-S2', route: '/sales/quotes', file: 'views/sales/Quotes.vue' },
  { epic: 2, story: 'E2-S2', route: '/sales/quotes/new', file: 'views/sales/QuoteForm.vue' },
  { epic: 2, story: 'E2-S2', route: '/sales/quotes/approval', file: 'views/sales/QuoteApproval.vue' },
  { epic: 2, story: 'E2-S3', route: '/sales/orders', file: 'views/sales/Orders.vue' },
  { epic: 2, story: 'E2-S3', route: '/sales/orders/:id/change', file: 'views/sales/OrderChange.vue' },
  { epic: 2, story: 'E2-S4', route: '/sales/contracts', file: 'views/sales/Contracts.vue' },
  // E3
  { epic: 3, story: 'E3-S1', route: '/material/drawings', file: 'views/material/Drawings.vue' },
  { epic: 3, story: 'E3-S2', route: '/material/drawings', file: 'components/material/DrawingConversionWizard.vue' },
  { epic: 3, story: 'E3-S3', route: '/material/boms', file: 'views/material/BOMs.vue' },
  { epic: 3, story: 'E3-S4', route: '/material/process', file: 'views/material/Process.vue' },
  // E4 — 扫码入/出库为 APP 专属（Web 仅 redirect → /app-only）
  { epic: 4, story: 'E4-S2', route: 'android:ScanScreen', file: '../android-impl/app/src/main/kotlin/com/btsheng/erp/feature/scan/ScanScreen.kt' },
  { epic: 4, story: 'E4-S3', route: '/warehouse/inventory', file: 'views/warehouse/Inventory.vue' },
  { epic: 4, story: 'E4-S4', route: '/warehouse/inventory-alert', file: 'views/warehouse/InventoryAlert.vue' },
  // E5
  { epic: 5, story: 'E5-S1', route: '/production/schedule-gantt', file: 'views/production/ScheduleGantt.vue' },
  { epic: 5, story: 'E5-S2', route: 'android:WorkorderProcessScan', file: '../android-impl/app/src/main/kotlin/com/btsheng/erp/feature/v139/WorkorderProcessScanFragment.kt' },
  { epic: 5, story: 'E5-S3', route: '/production/mrp', file: 'views/production/MRP.vue' },
  { epic: 5, story: 'E5-S4', route: '/production/allocation', file: 'views/production/Allocation.vue' },
  // E6
  { epic: 6, story: 'E6-S5', route: '/production/outsub-panel', file: 'views/production/OutsourceStateMachine.vue' },
  { epic: 6, story: 'E6-S1', route: '/sourcing/reconcile', file: 'views/sourcing/Reconcile.vue' },
  // E7
  { epic: 7, story: 'E7-S1', route: '/quality/inspection', file: 'views/quality/Inspection.vue' },
  { epic: 7, story: 'E7-S2', route: '/quality/fa', file: 'views/quality/FA.vue' },
  { epic: 7, story: 'E7-S3', route: '/quality/cmm', file: 'views/quality/CMM.vue' },
  { epic: 7, story: 'E7-S4', route: '/quality/defect', file: 'views/quality/Defect.vue' },
  // E8
  { epic: 8, story: 'E8-S1', route: '/sourcing/rfq', file: 'views/sourcing/RFQ.vue' },
  { epic: 8, story: 'E8-S2', route: '/sourcing/po', file: 'views/sourcing/PO.vue' },
  { epic: 8, story: 'E8-S2', route: '/sourcing/po-create', file: 'views/sourcing/PoCreate.vue' },
  { epic: 8, story: 'E8-S3', route: '/sourcing/incoming', file: 'views/sourcing/Incoming.vue' },
  { epic: 8, story: 'E8-S4', route: '/quality/inspection-create', file: 'views/quality/InspectionCreate.vue' },
  // E9
  { epic: 9, story: 'E9-S1', route: '/finance/receivables', file: 'views/finance/Receivables.vue' },
  { epic: 9, story: 'E9-S2', route: '/finance/cost', file: 'views/finance/Cost.vue' },
  { epic: 9, story: 'E9-S3', route: '/finance/payments', file: 'views/finance/Payments.vue' },
  { epic: 9, story: 'E9-S4', route: '/finance/profit', file: 'views/finance/Profit.vue' },
  { epic: 9, story: 'E9-S5', route: '/finance/signed-scans', file: 'views/finance/SignedScans.vue' },
  // E10
  { epic: 10, story: 'E10-S1', route: '/admin/hr', file: 'views/admin/HR.vue' },
  // E11
  { epic: 11, story: 'E11-S1', route: '/dashboard/production', file: 'views/dashboard/Production.vue' },
  { epic: 11, story: 'E11-S2', route: '/dashboard/multi', file: 'views/dashboard/MultiDashboard.vue' },
  { epic: 11, story: 'E11-S3', route: '/reports/sales-ranking', file: 'views/reports/SalesRanking.vue' },
  { epic: 11, story: 'E11-S3', route: '/reports/customer-analysis', file: 'views/reports/CustomerAnalysis.vue' },
  // E12 — 到货扫码为 APP 专属
  { epic: 12, story: 'E12-S2', route: 'android:BatchIncomingScan', file: '../android-impl/app/src/main/kotlin/com/btsheng/erp/feature/v138/BatchIncomingScanFragment.kt' },
]

const failures = []
const warnings = []

for (const row of MATRIX) {
  const filePath = row.file.startsWith('../')
    ? join(root, row.file)
    : join(root, 'src', row.file)

  if (!existsSync(filePath)) {
    failures.push(`[E${row.epic} ${row.story}] 文件缺失: ${row.file}`)
    continue
  }

  if (row.route.startsWith('android:')) continue

  const routeNorm = row.route.replace(/^\//, '').replace(/:[^/]+/g, ':param')
  const hasRoute =
    routerSrc.includes(`path: '${row.route.split('/').pop()?.replace(/:.*/, '')}'`) ||
    routerSrc.includes(row.route.replace(/^\//, '').split('/').slice(-1)[0]?.split(':')[0] || '') ||
    routerSrc.includes(row.file.replace('views/', '@/views/').replace('.vue', ''))

  if (!hasRoute) {
    warnings.push(`[E${row.epic} ${row.story}] 路由可能未注册: ${row.route}`)
  }
}

console.log(`\n=== Gap Matrix Smoke (${MATRIX.length} 条) ===\n`)
if (failures.length === 0) {
  console.log('✅ 全部 Vue/Kotlin 文件存在')
} else {
  console.log(`❌ ${failures.length} 个文件缺失:`)
  failures.forEach((f) => console.log('  ', f))
}
if (warnings.length) {
  console.log(`\n⚠️  ${warnings.length} 条路由警告:`)
  warnings.forEach((w) => console.log('  ', w))
}

const byEpic = new Map()
for (const row of MATRIX) {
  if (!byEpic.has(row.epic)) byEpic.set(row.epic, { ok: 0, fail: 0 })
  const stat = byEpic.get(row.epic)
  const filePath = row.file.startsWith('../') ? join(root, row.file) : join(root, 'src', row.file)
  if (existsSync(filePath)) stat.ok++
  else stat.fail++
}
console.log('\n按 Epic 汇总:')
for (const [epic, stat] of [...byEpic.entries()].sort((a, b) => a[0] - b[0])) {
  console.log(`  Epic ${epic}: ${stat.ok} OK / ${stat.fail} FAIL`)
}

process.exit(failures.length ? 1 : 0)
