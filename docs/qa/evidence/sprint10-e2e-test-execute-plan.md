# Sprint 10 · Story 10.2 · QA test-execute 计划

> **作者**：QA 商鞅（Test Engineer & Quality Architect）
> **日期**：2026-06-14
> **范围**：Sprint 10 集成 E 验证 · QA 委派#1
> **关联**：[→ 10.2 dev log](../../../dev/logs/10.2-dev-log.md) · [→ 10.2 QA test-design](../reviews/10.2-playwright-e2e.md) · [→ Sprint 10 集成 E 验证报告](./sprint10-integration-test-report.md) · [→ 1.5 test-execute 模板](./1.5-test-execute.md)
> **截止**：**2026-06-15**（集成 E 验证 CONDITIONAL GO 收口前置）
> **执行环境说明**：本沙箱 bash 受限，QA 商鞅在沙箱内**撰写结构化 test-execute 计划 + 判定标准 + 失败处理流程**；**实机执行由 PM 范蠡 / DevOps 张良 / dev agent Opus 4.8 在工作站完成**（与 10.2 dev log §5.2 / §5.4 一致）。

---

## 1. 委派上下文

| 项 | 值 |
|----|------|
| **委派来源** | Sprint 10 集成 E 验证报告 §3.1 委派 1（SM 萧何 · 2026-06-14）|
| **委派范围** | 14 E2E 测例 test-execute + 9.2 4 happy path 回归 |
| **责任** | QA 商鞅（计划 + 判定 + 流程）· PM 范蠡 / DevOps 张良 / dev agent Opus 4.8（实机执行）|
| **前置依赖** | (1) PM 决策#1 web-impl git remote 接入 · (2) PM 决策#3 7 状态机 enum drift 修复（10.5 dev 已修 4 typecheck 错误）· (3) DevOps `.gitlab-ci.yml` 接入 GitLab repo · (4) V1.3.7 seed 数据一致性（含 14 spec 所需 mock id：quote id=100 / rework id=200 / outsourceNo 等）|
| **截止时间** | **2026-06-15**（24h 内 · 与集成 E 收口同日）|
| **判定等级** | 🟢 PASS / 🟡 CONDITIONAL PASS / 🔴 FAIL（详见 §6）|

---

## 2. 执行环境与前置检查清单

### 2.1 执行环境规格

| 维度 | 要求 |
|------|------|
| **OS** | Windows 10/11 / macOS / Linux（web-impl 仓跨平台）|
| **Node** | 18.x 或 20.x（package.json `engines` 约束）|
| **npm** | 9.x+（lockfile v3）|
| **浏览器** | Chromium 120+（Playwright 1.49.0 默认）|
| **JDK** | 17（仅 backend 启动时需要，E2E 实机跑不需要）|
| **Docker** | 可选（testcontainers 备选 · V1.3.7 默认直连 dev DB）|
| **web-impl 仓** | 已 git clone + 已 `git pull origin main`（PM 决策#1 完成后 untracked → tracked）|
| **backend 服务** | 启动中（端口 8080）· V1.3.7.1 seed 数据已加载 |

### 2.2 前置检查清单（执行前 30min 必做）

| # | 检查项 | 命令 | 期望 |
|---|--------|------|------|
| 1 | web-impl 仓已 git clone | `cd web-impl && git remote -v` | 输出 origin URL（PM 决策#1 完成后）|
| 2 | 当前分支最新 | `git pull origin main && git log -1 --oneline` | 10.2 dev commit + 10.5 typecheck 修复 commit |
| 3 | backend 服务健康 | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| 4 | V1.3.7.1 seed 已加载 | `curl -X POST http://localhost:8080/api/v1/test/reset-db` | 200 + reset 成功 · 含 mock id（quote=100 / rework=200 / outsourceNo=WW-）|
| 5 | Node 依赖就位 | `node -v && npm -v` | Node 18+/20+ · npm 9+ |
| 6 | 浏览器二进制 | `npx playwright --version` | 1.49.0+ |
| 7 | generated/ 目录存在 | `ls web-impl/src/api/generated/services/ \| wc -l` | ≥ 40（10.1 IMPL 后）|
| 8 | typecheck:ci 通过 | `npm run typecheck:ci` | 退出 0（PM 决策#3 修复 4 错误后）|

**任一检查未通过 → 阻塞 → 通知 SM 萧何 + PM 范蠡**。

---

## 3. test-execute 命令（工作站可复制粘贴）

### 3.1 一键脚本（推荐）

```bash
# 工作站 · 切到 web-impl 仓根目录
cd web-impl

# === Step 0: 拉最新 + 装依赖 ===
git pull origin main
npm ci
npx playwright install --with-deps chromium

# === Step 1: codegen 校验（10.1 协同）===
npm run gen:api
npm run typecheck:ci

# === Step 2: 14 E2E test-execute ===
npx playwright test e2e/sprint10/ \
  --project=chromium \
  --reporter=junit,html,list \
  --workers=4

# === Step 3: 9.2 4 happy path 回归 ===
npx playwright test \
  e2e/dashboard-production.spec.ts \
  e2e/salesperson-order-crud.spec.ts \
  e2e/salesperson-quote-crud.spec.ts \
  e2e/salesperson-50k-order-approve.spec.ts \
  --project=chromium \
  --reporter=junit,html,list
```

### 3.2 单测例调试（失败定位用）

```bash
# 失败测例 re-run with trace + video
npx playwright test e2e/sprint10/tc-10.2-a3-quote-approve.spec.ts \
  --project=chromium --trace=on --video=retain --headed

# 查看 trace
npx playwright show-trace test-results/tc-10.2-a3-quote-approve/trace.zip
```

### 3.3 产出物路径（CI artifact 同步）

| 路径 | 内容 | 保留期 |
|------|------|--------|
| `web-impl/playwright-report/index.html` | HTML 测试报告（可视）| 30 天 |
| `web-impl/test-results/` | 失败截图 + trace.zip + video | 30 天 |
| `web-impl/junit.xml` | JUnit XML（CI gate 消费）| 30 天 |
| `web-impl/test-output/.last-run.json` | 上次执行摘要 | 永久 |

---

## 4. 14 E2E 测例清单与断言（验收点）

| TC | 文件 | 业务域 | 端点 | 角色 | 期望 | 单测例预算 |
|----|------|--------|------|------|------|------------|
| **A1** | `tc-10.2-a1-login.spec.ts` | 认证 | POST /auth/login | sales | 200 + JWT (token.length > 50 + `/^eyJ/`) | < 3s |
| **A2** | `tc-10.2-a2-quote-submit.spec.ts` | 报价 | POST /quotes + /submit | sales | DRAFT → SUBMITTED + 4 通道 audit log | < 4s |
| **A3** | `tc-10.2-a3-quote-approve.spec.ts` | 审批 | GET /approvals/pending + POST /approve | manager | SUBMITTED → APPROVED + 无二次密码弹窗（< 20万）| < 4s |
| **A4** | `tc-10.2-a4-quote-to-order.spec.ts` | 转单 | POST /convert-to-order | sales | 订单号生成 `/^SO-\d{8}-\d{4}$/` | < 3s |
| **B1** | `tc-10.2-b1-order-list.spec.ts` | 订单 | GET /orders | sales/manager/gm × 3 | 权限矩阵 sales ≤ manager ≤ gm | < 4s |
| **B2** | `tc-10.2-b2-order-approve.spec.ts` | 审批 | POST /orders/{id}/approve | manager | CONFIRMED → PRODUCING + 4 阈值路由 | < 4s |
| **B3** | `tc-10.2-b3-order-to-production.spec.ts` | 生产 | POST /convert-to-production | production_manager | 工单号 + 库存预扣 `reservedQty > 0` | < 5s |
| **B4** | `tc-10.2-b4-order-profit.spec.ts` | 利润 | GET /orders/{id}/profit | gm | 收入/成本/毛利 + 委外占比 `%` 格式 | < 5s |
| **C1** | `tc-10.2-c1-outsource-create.spec.ts` | 委外 | POST /convert-to-outsource | purchaser | WW- 单号 + PENDING 初始 | < 4s |
| **C2** | `tc-10.2-c2-outsource-advance.spec.ts` | 状态 | POST /outsource-states/advance | warehouse | PENDING → SENT → IN_PROGRESS | < 5s |
| **C3** | `tc-10.2-c3-rework-create.spec.ts` | 返修 | POST /reworks | qa | NOTIFIED_REPAIR 衍生态 | < 5s |
| **C4** | `tc-10.2-c4-rework-finish.spec.ts` | 返修 | POST /reworks/{id}/finish | vendor_portal | 状态回 IN_PROGRESS | < 4s |
| **D1** | `tc-10.2-d1-stock-in.spec.ts` | 库存 | POST /stock/in | warehouse | 物料码校验 + 批次写入 | < 4s |
| **D2** | `tc-10.2-d2-kanban.spec.ts` | 看板 | GET /reports/kanban | gm | 9 维度 + 端到端 < 5s | < 5s |

**测例总数**：14 文件（含 B1 矩阵 4 test 共 18 个 test）· 业务角色 7 套（sales/manager/gm/production_manager/purchaser/warehouse/qa/vendor_portal mock）

---

## 5. 9.2 4 happy path 回归清单

| # | 文件 | 业务域 | 链路 | 期望 |
|---|------|--------|------|------|
| 1 | `e2e/dashboard-production.spec.ts` | 看板 | 登录 → 进生产看板 | dashboard 渲染 + 9 维度 tab |
| 2 | `e2e/salesperson-order-crud.spec.ts` | 订单 | 业务员 → CRUD | 4 操作（增/删/改/查）+ 状态推进 |
| 3 | `e2e/salesperson-quote-crud.spec.ts` | 报价 | 业务员 → CRUD | 4 操作 + 4 状态机 |
| 4 | `e2e/salesperson-50k-order-approve.spec.ts` | 审批 | 5万订单 OR 会签 | DEPT_MANAGER 路由 + 双签 OR |

**回归目的**：验证 10.2 改动（playwright.config.ts 新增 project + helpers.ts + 14 spec）**未破坏** 9.2 既有核心路径。

---

## 6. 性能基线

| 项 | 期望 | 实测容忍 | 超容忍处理 |
|----|------|----------|------------|
| 单测例 | < 5s | < 8s | 8-10s 标 🟡 CONDITIONAL（需分析）· > 10s 标 🔴 FAIL |
| 全量 14 测例（4 worker 并行）| ≤ 2min | ≤ 3min | 3-4min 标 🟡 · > 4min 标 🔴（架构层问题）|
| 二次密码弹窗显式等待 | < 5s | < 8s | 8s+ 修 helpers.ts `confirmHighAmountDialog` timeout |
| D2 看板端到端 | < 5s | < 8s | 8s+ 标 🟡（K6 baseline + 监控告警）|
| 9.2 4 回归 | ≤ 90s | ≤ 120s | 120s+ 拆 worker 数 |

**基线依据**：[→ 10.2 QA review §四](../reviews/10.2-playwright-e2e.md) · [→ 10.2 dev log §5.3](../../../dev/logs/10.2-dev-log.md) · 域总计 ~56s + setup 30s ≈ 1.5min

---

## 7. 失败处理流程

### 7.1 自动归档（CI 配置已就位）

```yaml
# .gitlab-ci.yml（10.2 dev log §6）
artifacts:
  when: always
  paths:
    - playwright-report/
    - test-results/
  reports:
    junit: junit.xml
  expire_in: 30 days
```

- 失败测例自动：截图 + trace.zip + video（`screenshot: 'only-on-failure'` + `video: 'retain-on-failure'` + `trace: 'on-first-retry'`）
- CI 失败 → 自动通知 PM 范蠡 + QA 商鞅 + dev agent Opus 4.8（GitLab notification settings）

### 7.2 失败分析矩阵（4 类型）

| 类型 | 症状 | 根因 | 修复责任 | 修复路径 |
|------|------|------|----------|----------|
| **A · 二次密码弹窗 flake** | TC-10.2.A3 偶发 timeout 5s | page object 显式等待不足 | dev agent | `helpers.ts` `confirmHighAmountDialog` timeout 5s → 8s + `await dialog.waitFor({ state: 'visible' })` |
| **B · DB state 污染** | 测例间数据串扰（quote id 不存在 / 库存预扣错）| `POST /test/reset-db` 钩子失败 / 未在 beforeAll 调 | dev agent | `helpers.ts` `resetDb` 加 retry 3 次 + 失败时强制 seed SQL 兜底 |
| **C · 后端 API 漂移** | 404 / 500 / 字段缺失 | 10.3/9.1/9.2 接口变（status code / path / 字段名）| dev + architect | 看 backend git log 近 7 天 diff · 修 e2e 端点或回滚 backend |
| **D · codegen 不一致** | `LoginResponse` 字段找不到 / typecheck 错误 | 10.1 regen 失败（PM 决策#3 修复不彻底）| dev | `npm run gen:api` 重跑 + 比对 openapi.yaml + 修 helpers.ts 引用 |

### 7.3 失败升级路径

```
失败发生
  ↓
[1] Playwright 自动：截图 + trace + video 上传 artifact
  ↓
[2] GitLab CI：发送 failed 通知给 PM 范蠡 + QA 商鞅 + dev agent
  ↓
[3] QA 商鞅：分析 trace.zip（npx playwright show-trace）→ 判定类型 A/B/C/D
  ↓
[4] 类型 A/B → dev agent 修 helpers.ts → re-run（不需 PM 决策）· < 30min
   类型 C → dev + architect 联合 review → 修 backend / 回滚 → re-run · < 2h
   类型 D → dev 重跑 regen → 修 codegen → re-run · < 1h
  ↓
[5] 修完后再跑一轮全量 → 14/14 + 9.2 4/4 PASS → 🟢 PASS
  ↓
[6] 仍 FAIL → 升级 SM 萧何 → 评估是否推迟 V1.3.8 FAT 准入
```

### 7.4 重试策略

- **同测例 flake**（首次 fail + 重试 pass）：标 🟡 CONDITIONAL，记录 trace · 不阻塞 PASS
- **同测例 hard fail**（重试 2 次仍 fail）：标 🔴 FAIL · 必须修
- **9.2 回归 hard fail**：标 🔴 FAIL · typecheck:ci 已过 ≠ 运行时对 · 立即通知 architect 鲁班

---

## 8. 通过/失败判定标准

### 8.1 三级判定

| 等级 | 14 E2E | 9.2 回归 | 性能 | flake | 处置 |
|------|--------|----------|------|-------|------|
| 🟢 **PASS** | 14/14 | 4/4 | 全量 ≤ 2min + 单测例 < 5s | 0 | 集成 E 验证收口 → V1.3.8 FAT 准入 |
| 🟡 **CONDITIONAL PASS** | 14/14 | 4/4 | 全量 ≤ 3min（容忍区）| 1-2（已记录）| 集成 E 验证收口（标 CONDITIONAL）· V1.3.9 修 |
| 🔴 **FAIL** | 任一 FAIL | 任一 FAIL | 全量 > 4min | > 2 | **阻塞 V1.3.8 FAT 准入** · 修完后重跑 |

### 8.2 附加硬性条件（任一不满足即降级）

- 14 测例必须全部 PASS（含 trace 完整）
- 9.2 4 回归必须全部 PASS（typecheck:ci 0 漂移的运行时验证）
- 性能基线达标（全量 ≤ 2min 期望 / ≤ 3min 容忍）
- 0 数据残留（test-results/ 失败 trace 已归档）
- 0 hard fail（任何 FAIL 即降级）

### 8.3 判定签字

- 🟢 PASS · QA 商鞅签字 + 14/14 + 9.2 4/4 + 性能达标 + 0 flake
- 🟡 CONDITIONAL PASS · QA 商鞅签字 + 1-2 flake 详情 + 性能容忍区
- 🔴 FAIL · QA 商鞅 + dev agent 联合签字 + 失败分析 + 修复 ETA

---

## 9. 数据需求（V1.3.7 seed 一致性）

### 9.1 seed 必含 mock id（10.2 dev log §8 #5）

| 测例 | 所需 seed 数据 | 验证命令 |
|------|---------------|----------|
| TC-10.2.A4 | quote id=100（DRAFT/SUBMITTED 状态均可）| `curl /api/v1/quotes/100` |
| TC-10.2.B2/B3/B4/C1 | order id=200（CONFIRMED 状态）| `curl /api/v1/orders/200` |
| TC-10.2.C2/C3/C4 | outsourceNo `WW-20260613-0001` | `curl /api/v1/outsource/WW-20260613-0001` |
| TC-10.2.C4 | rework id=200（IN_PROGRESS 状态）| `curl /api/v1/reworks/200` |
| TC-10.2.D1 | 物料码 `MAT-20260613-0001` | `curl /api/v1/stock/MAT-20260613-0001` |
| 全部 | 7 业务角色账号（sales/manager/gm/production_manager/purchaser/warehouse/qa）| `curl /api/v1/users?role=...` |

### 9.2 委派#5 seed 一致性验证（10.2 集成 E 报告 §4.2 #9）

**责任**：QA 商鞅在跑测前 30min 验证 · 命令：

```bash
# seed 一致性 check 脚本（QA 商鞅工作站实跑）
curl -s http://localhost:8080/api/v1/test/seed-check \
  | jq '.data | {quotes: .quoteIds, orders: .orderIds, outsource: .outsourceNos, reworks: .reworkIds, materials: .materialCodes}'
# 期望：quoteIds 包含 100 + orderIds 包含 200 + outsourceNos 包含 "WW-20260613-0001" + ...
```

**若 seed 缺失 mock id**：
- 通知 PM 范蠡 → 决策：补 seed SQL（V1.3.7.1 patch）或调整 14 测例 mock id 适配现有 seed
- **决策时效**：执行前 4h（2026-06-15 12:00 前）· 超时即视为 FAIL 阻塞

### 9.3 reset-db 钩子验证

```bash
# V1.3.7.1 端点存在性
curl -X POST http://localhost:8080/api/v1/test/reset-db
# 期望：200 OK + {"code":0,"message":"reset success"}
# 若 404 → V1.3.7.1 已退役 → 需用 seed SQL 兜底（10.2 dev log §4.2 fallback 已落实）
```

---

## 10. 执行责任分工与时序

### 10.1 责任分工

| 角色 | 责任 |
|------|------|
| **PM 范蠡** | 决策：web-impl git remote（决策#1）+ seed mock id 补齐决策 + 7 状态机 enum drift（决策#3）· 截止 2026-06-15 12:00 |
| **DevOps 张良** | `.gitlab-ci.yml` 接入 GitLab repo + Nexus 镜像确认 · 截止 2026-06-15 12:00 |
| **dev agent Opus 4.8** | (a) 修 4 typecheck 错误（PM 决策#3 修复路径已明确）· (b) 失败时按 §7.3 修 helpers.ts / backend / codegen |
| **QA 商鞅（沙箱）** | 计划 + 判定 + 流程（本文档）· 收到执行结果后填写 `sprint10-e2e-test-summary.json`（用 §11 模板）|
| **执行者（工作站）** | 跑 §3.1 命令 + 上传 CI artifact + 反馈结果给 QA 商鞅 |

### 10.2 时序（24h 内）

| 时间 | 节点 | 责任 | 备注 |
|------|------|------|------|
| 2026-06-14 18:00 | 本计划交付 | QA 商鞅 | 现在 |
| 2026-06-14 20:00 | PM 决策#1+#3 回复 | PM 范蠡 | git remote + enum drift 修复路径 |
| 2026-06-14 22:00 | 4 typecheck 错误修复完成 | dev agent | 与 PM 决策#3 协同 |
| 2026-06-15 09:00 | `.gitlab-ci.yml` 接入 | DevOps 张良 | GitLab repo + Nexus 镜像 |
| 2026-06-15 10:00 | seed 一致性验证（§9.2）| QA 商鞅（工作站）| mock id 齐备 |
| 2026-06-15 11:00 | 前置检查清单（§2.2）| 执行者 | 8 项全过 |
| 2026-06-15 12:00 | 14 E2E + 9.2 4 回归执行 | 执行者 | §3.1 一键脚本 |
| 2026-06-15 14:00 | 失败分析 + 修复（若有）| dev agent + 执行者 | §7.3 升级路径 |
| 2026-06-15 16:00 | 重跑（若有修复）| 执行者 | 全量 PASS |
| 2026-06-15 18:00 | test-summary.json 填写 + 签字 | QA 商鞅 | 用 §11 模板 |
| 2026-06-15 20:00 | 集成 E 收口 | SM 萧何 | 4/4 PM 决策 + 3/3 QA 委派 + 1/1 DevOps 接入 → GO |

---

## 11. test-summary 模板引用

执行完成后，QA 商鞅用以下模板填写实际结果（见配套文件 `sprint10-e2e-test-summary.json`）：

```json
{
  "story": "10.2",
  "sprint": "V1.3.8 S10",
  "level": "Comprehensive",
  "executed": { "e2e_sprint10": 14, "e2e_regression_9_2": 4 },
  "passed":   { "e2e_sprint10": 14, "e2e_regression_9_2": 4 },
  "flake":    { "e2e_sprint10": 0, "e2e_regression_9_2": 0 },
  "failed":   { "e2e_sprint10": 0, "e2e_regression_9_2": 0 },
  "performance": { "full_runtime_sec": 0, "single_max_sec": 0, "baseline_met": false },
  "failure_archive": { "traces": [], "screenshots": [] },
  "result": "PASS | CONDITIONAL_PASS | FAIL",
  "qa_signoff": "QA 商鞅",
  "signoff_date": "2026-06-15",
  "status": "ACCEPTED | CONDITIONAL | FAILED",
  "evidence_files": [
    "docs/qa/evidence/sprint10-e2e-test-execute-plan.md",
    "docs/qa/evidence/sprint10-e2e-test-summary.json",
    "web-impl/playwright-report/index.html",
    "web-impl/junit.xml"
  ]
}
```

---

## 12. 风险与阻塞

### 12.1 已知风险（与 10.2 集成 E 报告 §4.2 衔接）

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | PM 决策#1 web-impl git remote 未完成 → `git diff --exit-code` gate 失效（不影响实机跑，仅影响 CI gate）| 🟡 中 | 实机跑不需要 gate · PM 决策#1 决策时长 ≤ 2026-06-15 12:00 |
| 2 | PM 决策#3 enum drift 修复不彻底 → 4 typecheck 错误残留 → typecheck:ci 不过 | 🟡 中 | 修复路径已明确（10.5 dev log §9）· dev agent 在 2026-06-14 22:00 前完成 |
| 3 | seed mock id 缺失（10.2 dev log §8 #5）| 🟡 中 | §9.2 验证 + §9.3 reset-db fallback |
| 4 | 二次密码弹窗 flake（10.2 dev log §7 风险 2）| 🟡 中 | helpers.ts 已显式等待 5s · 容忍 8s |
| 5 | D2 看板 > 5s（10.2 QA review §五）| 🟡 中 | K6 baseline + 监控告警 · 容忍 8s |
| 6 | 执行者工作站 Playwright / Node / Chromium 缺失 | 🟢 低 | §2.1 环境规格 + §2.2 前置检查 |

### 12.2 阻塞条件（任一成立即 🔴 FAIL）

1. PM 决策#1/#3 任一未在 2026-06-15 12:00 前完成
2. DevOps `.gitlab-ci.yml` 未在 2026-06-15 12:00 前接入
3. seed mock id 缺失且 PM 决策未在 2026-06-15 10:00 前回复
4. backend 服务未启动或 V1.3.7.1 端点 404
5. 14 E2E 任何 1 个 hard fail（重试 2 次仍 FAIL）
6. 9.2 4 回归任何 1 个 hard fail

---

## 13. QA 商鞅签字与交接

**QA 商鞅 · 2026-06-14 · Sprint 10 集成 E 验证委派#1 计划交付**

- **计划路径**：`docs/qa/evidence/sprint10-e2e-test-execute-plan.md`
- **模板路径**：`docs/qa/evidence/sprint10-e2e-test-summary.json`
- **执行命令**：§3.1 一键脚本（工作站可复制粘贴）
- **失败处理流程**：§7.1-7.4（4 类型失败矩阵 + 重试 + 升级）
- **判定标准**：§8.1 三级（🟢 PASS / 🟡 CONDITIONAL PASS / 🔴 FAIL）
- **截止时间**：**2026-06-15 18:00**（test-summary 签字交付）
- **风险/阻塞**：§12（6 项已知风险 + 6 项阻塞条件）

**交接链**：QA 商鞅（计划）→ 执行者（工作站实机跑）→ QA 商鞅（test-summary 填写）→ SM 萧何（集成 E 收口）→ PO 范蠡（V1.3.8 FAT 准入）
