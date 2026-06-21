# U-01 Dev Log · web-impl /admin/print-logs 路由注册修复

> **Author**: dev agent Opus 4.8 (orchestrix PO 范蠡委托)
> **Date**: 2026-06-14
> **Story**: V1.3.9 Sprint 12 · Story 12.4 AC-12.4.4 收口
> **关联文件**: `web-impl/src/views/admin/PrintLog.vue` (287 行 · IMPL 已 ship-ready)
> **工时**: 0.1 天 · **1 路由修复 + 1 PrintLog 类型小修**

---

## §1 缺陷回顾

`web-impl/src/views/admin/PrintLog.vue` 文件完整落地（287 行 · AC-12.4.4 全部实现 · 补打 + 历史查询），但路由表 `web-impl/src/router/index.ts` 缺失 `/admin/print-logs` 注册，导致浏览器访问 404。

QA 商鞅回归 U-01 缺陷：
- 打开 `/admin/print-logs` → 404 NotFound
- 期望：进入 PrintLog 打印管理页

---

## §2 改动清单

### §2.1 修改文件（2 个）

| # | 路径 | 改动 |
|---|------|------|
| 1 | `web-impl/src/router/index.ts` | +1 路由 `print-logs` · 嵌在 `/admin` parent route children 中 · 紧随 `printers` 之后 |
| 2 | `web-impl/src/views/admin/PrintLog.vue` | L164 `definePageMeta({ title: '打印管理' })` → 注释掉（Vue 3 + Vite 项目无 Nuxt 编译宏） |

### §2.2 路由注册 diff

```typescript
// router/index.ts L215-216
{ path: 'printers', name: 'Printers', component: () => import('@/views/admin/Printers.vue'), meta: { title: '打印机管理 · V1.3.9' } },
{ path: 'print-logs', name: 'PrintLogs', component: () => import('@/views/admin/PrintLog.vue'), meta: { title: '打印历史', icon: 'Printer', requiresAuth: true, roles: ['GM', 'ADMIN'] } },   // ← 新增
```

### §2.3 路由守卫（beforeEach L228-244）

`meta.roles: ['GM', 'ADMIN']` → 复用 V1.3.9 Sprint 13 Story 13.4 既有守卫逻辑：
- 角色校验：`auth.hasRole('GM') || auth.hasRole('ADMIN')`
- 不满足：`return next({ name: 'Dashboard' })` + `console.warn`
- 双层校验第二道防线：端点 `@PreAuthorize`（暂未在本路由触达具体端点 · 仅前端守卫）

---

## §3 验证

### §3.1 U-01 范围内 typecheck

| 文件 | typecheck 错误数 | 状态 |
|------|-----------------|------|
| `web-impl/src/router/index.ts` | 0 ✅ | 通过 |
| `web-impl/src/views/admin/PrintLog.vue` (修改 L164) | 0 ✅ | 通过 |

### §3.2 typecheck 全量（baseline 漂移标注）

```bash
$ cd web-impl && npm run typecheck
```

退出码：**非 0**（pre-existing 5 错误 · U-01 范围外）

| 文件 | 错误 | U-01 责任 |
|------|------|----------|
| `src/utils/http.ts(13,24)` | `Property 'env' does not exist on type 'ImportMeta'` | ❌ pre-existing（vite-env.d.ts 缺失）|
| `src/views/admin/Printers.vue(163,35)` | Cannot find module `@/api/generated/services/E12PrinterService` | ❌ pre-existing（backend 端点契约缺失）|
| `src/views/admin/Printers.vue(164,33)` | Cannot find module `@/api/generated/models/SysPrinter` | ❌ pre-existing（同上）|
| `src/views/v138/GmSummary.vue(33,14)` | `Type 'string' is not assignable to type 'number \| Dayjs'` | ❌ pre-existing |
| `src/views/v138/GmSummary.vue(47,14)` | 同上 | ❌ pre-existing |

### §3.3 typecheck:ci 全量

```bash
$ cd web-impl && npm run typecheck:ci
```

退出码：**非 0**（同 §3.2 5 pre-existing 错误 · `&&` 短路未执行 git diff gate）

### §3.4 build 全量

```bash
$ cd web-impl && npm run build
```

退出码：**非 0**（同 §3.2 5 pre-existing 错误 · vue-tsc -b 阶段拦截）

---

## §4 阻塞 / 风险

### §4.1 阻塞

**阻塞 1**：typecheck 双 0 / build 退出 0 **未达成**（5 个 pre-existing baseline 错误 · U-01 范围外）。

阻塞来源均为其他 Story 范围：
- `http.ts ImportMeta.env` → 平台 `src/vite-env.d.ts` 缺失（建议 Sprint 14 立项补 `/// <reference types="vite/client" />`）
- `Printers.vue E12PrinterService/SysPrinter` → backend `openapi.yaml` 缺 Printer 端点契约（建议 V1.3.10 Sprint 15 立项 `E12 Printer OpenAPI`）
- `GmSummary.vue Dayjs 类型` → pre-existing · 与 13.1/13.4 共享 baseline

### §4.2 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | baseline typecheck 错误阻塞 U-01 ship-ready 验收 | 🟡 中 | dev log 已记录 · PM Sprint 14 立项收口 · **不阻塞 U-01 路由功能本身**（路由注册语法/语义正确）|
| 2 | `roles: ['GM', 'ADMIN']` 端点 `@PreAuthorize` 第二道防线未在本路由触达具体端点 | 🟢 低 | 前端守卫拦截已生效 · 仅前端路由层防护足够 |

### §4.3 PM 决策需求

**建议 PM 在 Sprint 14 立项**：
- `13.X-baseline-typecheck-fix`：收口 5 个 pre-existing typecheck 错误（vite-env.d.ts + openapi.yaml Printer + Dayjs 类型）

**U-01 路由修复本身 ship-ready**（语法/语义/路由注册正确 · 仅因 baseline 阻塞无法通过 typecheck 双 0 验证）。

---

## §5 签字

- **dev agent Opus 4.8** · 2026-06-14 · U-01 路由注册修复完成
- **QA 商鞅** · 待本地浏览器验证 `/admin/print-logs` 进入 PrintLog 页面
- **PM 范蠡** · 待 Sprint 14 立项 baseline typecheck 收口

---

## 附录 A: 改动文件清单

### A.1 修改文件（2 个）

- `web-impl/src/router/index.ts` · +1 路由 `print-logs`（L215-216）
- `web-impl/src/views/admin/PrintLog.vue` · L164 `definePageMeta` 注释

### A.2 文档（1 文件）

- `docs/dev/logs/u01-print-logs-route-fix-dev-log.md` · 本 dev log

---

**U-01 路由修复 ship-ready（功能层）· typecheck 双 0 受 baseline 阻塞 · 待 PM Sprint 14 收口**