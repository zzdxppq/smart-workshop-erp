# PM 决策书#1 · web-impl 仓接入 git remote

> 决策人：PO 范蠡
> 日期：2026-06-13
> Sprint：V1.3.8 S10
> 关联：Sprint 10 集成 E 验证 CONDITIONAL GO · PM 决策#1
> 截止：2026-06-15
> 委派：DevOps 张良 · web-impl dev agent Opus 4.8

---

## 1. 背景

Sprint 10 集成 E 验证报告（`docs/qa/evidence/sprint10-integration-test-report.md` §4.3 PM 决策 #1）识别 web-impl 仓处于 **untracked 状态**：

- 10.1 dev log §6 #3 标注：`web-impl/ 整个仓 untracked · git diff --exit-code src/api/generated gate 失效`
- 10.2 dev log §8 #4 标注：`web-impl/ untracked · git diff gate 失效`
- 10.5 dev log §8 #7 标注：`web-impl 仓 untracked · 5 commit 准备就绪等 git remote`

**当前状态**：

- web-impl 本地仓 `.git/` 目录 **不存在**（ls 验证：`ls -la web-impl/.git` → `No such file or directory`）· 即未做 `git init`，更未关联任何 remote
- 3 Story 产出全部仅存在于文件系统，**0 commit 落地**，共 **13+ commit 待 push**
- `npm run typecheck:ci` gate 失效（untracked dir 下 `git diff --exit-code src/api/generated` 永远返回 0）
- 集成 E 验证中风险项 #3 标记 🟡 中 · 阻塞 Sprint 10 FAT 准入

**Sprint 8.4 已定 multi-repo 独立 web-impl 仓**，本次决策是 multi-repo 架构的首次落地（web-impl 从此成为独立仓）。

---

## 2. 选项分析

### 选项 A：内部 GitLab（推荐）

- DevOps 张良在内部 GitLab 创建 `web-impl` 空仓
- web-impl 本地仓 `git init` + `git remote add origin <gitlab-url>`
- 一次性 push 全部 13+ commit（10.1 + 10.2 + 10.5 产出，按 Story 顺序）
- CI 接入 `.gitlab-ci.yml`（10.2 已生成 · 3 stage · 4 worker parallel）

### 选项 B：GitHub 公开仓

- GitHub 创建公开仓 `web-impl`
- **不推荐**（V1.3.7 + V1.3.8 是商业合同产品 · OpenAPI 端点契约 / codegen / E2E 配置公开暴露有泄露风险）

### 选项 C：单仓多分支（multi-repo 反模式）

- 把 web-impl 仓并入 backend 仓的 `web-impl/` 子目录
- **不推荐**（破坏 multi-repo 架构 · Sprint 8.4 architect 鲁班已定 multi-repo 独立 web-impl）

---

## 3. 决策

**选项 A · 内部 GitLab**

依据 multi-repo 架构规范（8.4）+ 商业合同保密性 + CI gate 前置需求，内部 GitLab 是唯一可行路径。

---

## 4. 依据

### 4.1 multi-repo 规范（Sprint 8.4 architect 鲁班定）

- web-impl 是 V1.3.8 五大仓之一（`backend/` + `web-impl/` + `android-impl/` + `mobile-impl/` + `docs/`），与 backend 仓**解耦**
- 独立仓意味着独立的 `git init` + 独立 remote + 独立 CI pipeline
- 反模式选项 C 直接违反 multi-repo 架构

### 4.2 商业合同保密性

- V1.3.7 + V1.3.8 是商业合同产品（合同附录 A PRD · 附录 B 交付物 · 附录 C 硬件 · 附录 D 安全）
- 端点契约（148 ops）+ codegen 模型（100 models）+ E2E 配置属于商业机密
- GitHub 公开仓有泄露风险 · **选项 B 排除**

### 4.3 CI gate 前置需求

- `npm run typecheck:ci` 含 `git diff --exit-code src/api/generated`（10.1 dev log §1.5）
- 在 untracked dir 下永远返回 0 → CI 失效 → 端点契约变更无前端回归拦截
- 必须先有 tracked 文件 + commit，gate 才有意义
- Sprint 10 FAT 准入前置条件

### 4.4 与已有实践一致

- backend 仓已使用内部 GitLab（V1.3.7 已 ship · CI pipeline 运转良好）
- android-impl 仓已使用内部 GitLab（10.4 验证清单 V1/V2/V5/V6 已落地）
- web-impl 沿用同一模式 · DevOps 张良已有成熟流程

---

## 5. 委派

### 5.1 DevOps 张良（截止 2026-06-15）

**任务清单**：

1. **创建 GitLab 空仓**
   - 内部 GitLab 创建 `web-impl` 空仓（不勾选 README / .gitignore · 空仓状态）
   - 记录 GitLab URL（`<gitlab-url>` 占位）· 通知 PM + web-impl dev agent

2. **通知 web-impl dev**
   - 通知 web-impl dev agent Opus 4.8 仓已创建
   - 提供 GitLab URL + 访问凭证（dev 凭据 + CI service account）

3. **CI 接入协调**
   - `.gitlab-ci.yml`（10.2 已生成）接入 GitLab repo
   - typecheck stage 加 `rm -rf node_modules/.cache`（10.1 architect review §6 IMPL 注意事项 2）
   - 与 backend 仓共用 Nexus 镜像（如 `https://nexus.internal/gradle-8.7-bin.zip` · 10.4 沿用）

### 5.2 web-impl dev agent Opus 4.8（截止 2026-06-15）

**任务清单**：

1. **本地仓初始化**
   - `cd web-impl`
   - `git init` · 创建 `.gitignore`（排除 `node_modules/` / `dist/` / `playwright-report/` / `test-results/` / `.env.local` 等）
   - `git config user.name` + `git config user.email`（按 multi-repo 规范）

2. **关联 origin**
   - `git remote add origin <gitlab-url>`（DevOps 提供的 GitLab URL）

3. **commit 拆分（13 commit · 按 Story + 文件粒度）**
   - **Story 10.1（5 commit）**：
     - `feat(codegen): npm scripts gen:api/typecheck/typecheck:ci + 依赖升级 (openapi-typescript-codegen 0.29.0 / vue-tsc 2.1.10)`（`web-impl/package.json`）
     - `feat(codegen): tsconfig.json include src/api/generated/**/*.ts`（`web-impl/tsconfig.json`）
     - `feat(codegen): v138.ts re-export shim 替换手写 stub`（`web-impl/src/api/generated/v138.ts`）
     - `chore(codegen): generated/ 全量 codegen 输出 (40 service stubs + 100 models + 148 ops)`（`web-impl/src/api/generated/`）
     - `refactor(types): GmSummary/BatchIncoming/MaterialDetail/CustomerAnalysis 收紧 unknown 类型`（4 个 v138.vue / reports 文件）
   - **Story 10.2（2 commit）**：
     - `feat(e2e): Playwright 14 spec + helpers (sprint10/)`（`web-impl/e2e/sprint10/` 14 spec + helpers.ts）
     - `ci: .gitlab-ci.yml 3 stage (lint/typecheck/e2e) + playwright.config chromium project`（`.gitlab-ci.yml` + `playwright.config.ts`）
   - **Story 10.5（5 commit）**：
     - `refactor(types): sales/Quotes.vue any → Quote[]`（`Quotes.vue`）
     - `refactor(types): sales/Orders.vue any → Order[]`（`Orders.vue`）
     - `refactor(types): finance/Profit.vue any → OrderProfit[]`（`Profit.vue`）
     - `refactor(types): production/OutsourceStateMachine.vue any → OutsourceState* (含 as unknown cast)`（`OutsourceStateMachine.vue`）
     - `refactor(types): quality/InspectionCreate.vue InspectionFormPayload + QualityStatus enum (Option A)`（`InspectionCreate.vue`）
   - **backend 协同修复（1 commit · 10.1）**：
     - `fix(openapi): merge duplicate /drawings keys + components/schemas 块 (含 10.3 WorkflowEventStats/StatsPeriod)`（`backend/spec/openapi.yaml`）

   **注**：backend openapi.yaml 修复是 backend 仓提交，由 backend dev 负责，**不在 web-impl push 范围**。

4. **push**
   - `git push -u origin master`（或 `main` · 按 GitLab 默认分支名）
   - 验证 13+ commit 全部就位

### 5.3 QA 商鞅（前置依赖 · 截止 2026-06-15）

- 等 commit push + CI 接入后执行 typecheck:ci gate 验证
- 14 E2E 测例 test-execute + 9.2 4 测例回归（与本决策正交 · 集成 E 委派 1）
- gradle wrapper V3/V4/V7 实机（10.4 · 与本决策正交）

---

## 6. 验证

### 6.1 git 状态验证

```bash
cd web-impl
git status                    # 期望：clean working tree
git log --oneline | head -20  # 期望：13+ commit 顺序排列
git remote -v                 # 期望：origin 指向内部 GitLab URL
```

### 6.2 CI gate 验证

```bash
cd web-impl
npm run typecheck:ci          # 期望：退出码 0 + vue-tsc 0 错误 + git diff 退出 0
npx playwright test e2e/sprint10/ --project=chromium --workers=4  # 期望：14/14 PASS
```

### 6.3 集成 E 验证闭环

- ✅ git status 干净
- ✅ typecheck:ci 工作（gate 生效）
- ✅ 13+ commit 在 GitLab 上可访问
- ✅ CI pipeline 3 stage 运转（lint / typecheck / e2e）

---

## 7. 待 push commit 清单（13+ commit 范围）

来自 10.1 / 10.2 / 10.5 dev log 产出范围：

### 7.1 Story 10.1 · 5 commit

| # | commit message | 关键文件 | 来源 dev log |
|---|---------------|---------|--------------|
| 1 | `feat(codegen): npm scripts gen:api/typecheck/typecheck:ci + 依赖升级` | `web-impl/package.json` | 10.1 §1.5 |
| 2 | `feat(codegen): tsconfig.json include generated/**/*.ts` | `web-impl/tsconfig.json` | 10.1 §1.1 |
| 3 | `feat(codegen): v138.ts re-export shim 替换手写 stub` | `web-impl/src/api/generated/v138.ts` | 10.1 §1.1 |
| 4 | `chore(codegen): generated/ 全量 codegen 输出` | `web-impl/src/api/generated/`（40 services + 100 models + core + index） | 10.1 §1.1 + §2 |
| 5 | `refactor(types): GmSummary/BatchIncoming/MaterialDetail/CustomerAnalysis 收紧类型` | `web-impl/src/views/v138/GmSummary.vue` + `BatchIncoming.vue` + `MaterialDetail.vue` + `web-impl/src/views/reports/CustomerAnalysis.vue` | 10.1 §1.1 |

### 7.2 Story 10.2 · 2 commit

| # | commit message | 关键文件 | 来源 dev log |
|---|---------------|---------|--------------|
| 6 | `feat(e2e): Playwright 14 spec + helpers (sprint10/)` | `web-impl/e2e/sprint10/` 14 spec + `helpers.ts` | 10.2 §2.1 |
| 7 | `ci: .gitlab-ci.yml 3 stage + playwright.config chromium project` | `web-impl/.gitlab-ci.yml` + `web-impl/playwright.config.ts` | 10.2 §6 + §2.1 |

### 7.3 Story 10.5 · 5 commit

| # | commit message | 关键文件 | 来源 dev log |
|---|---------------|---------|--------------|
| 8 | `refactor(types): sales/Quotes.vue any → Quote[]` | `web-impl/src/views/sales/Quotes.vue` | 10.5 §2.1 |
| 9 | `refactor(types): sales/Orders.vue any → Order[]` | `web-impl/src/views/sales/Orders.vue` | 10.5 §2.2 |
| 10 | `refactor(types): finance/Profit.vue any → OrderProfit[]` | `web-impl/src/views/finance/Profit.vue` | 10.5 §2.3 |
| 11 | `refactor(types): production/OutsourceStateMachine.vue any → OutsourceState*` | `web-impl/src/views/production/OutsourceStateMachine.vue` | 10.5 §2.4 |
| 12 | `refactor(types): quality/InspectionCreate.vue InspectionFormPayload + QualityStatus enum (Option A)` | `web-impl/src/views/quality/InspectionCreate.vue` | 10.5 §2.5 |

### 7.4 协同项（不在 web-impl push 范围）

- backend 仓 `backend/spec/openapi.yaml` 修复 · backend dev 负责

**总计 web-impl 仓 12 commit · 实际验证后可能合并/拆分若干 commit · 估算 13+ commit**。

---

## 8. 风险 / 阻塞

### 8.1 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | DevOps 张良 GitLab 仓创建延迟（内部审批） | 🟡 中 | 截止 2026-06-15（+1 day buffer）· 若延迟启动 typecheck:ci gate 失败时 fallback 到 `git status --porcelain src/api/generated \| grep -q .` |
| 2 | 13 commit 历史重写风险（首次 push 无 baseline） | 🟢 低 | 本次为首次 push，无历史重写 · 若需 squash 可在 Sprint 11 backlog 处理 |
| 3 | .gitignore 缺漏（node_modules / dist / playwright-report 等被误 commit） | 🟢 低 | web-impl dev 按 multi-repo 标准 `.gitignore` 模板创建 |
| 4 | backend openapi.yaml 修复未先 commit/push（web-impl codegen 依赖 backend openapi.yaml 当前内容） | 🟢 低 | 10.1 IMPL 时 backend 仓已有 openapi.yaml 最新版本（10.1 dev log §1.2 · 协同修复已就位） |
| 5 | 10.5 OutsourceStateMachine.vue 引入 4 typecheck 错误（10.1 dev log §9 · enum drift） | 🟡 中 | **前置条件**：10.5 dev 修复 4 错误后再 push（10.1 dev log §9 已指明路径）· 否则 typecheck:ci gate 会失败 |
| 6 | 14 spec mock id（100/200 等）需 V1.3.7 seed 配合（10.2 §8 #5） | 🟡 中 | 与本决策正交 · QA 商鞅验证 seed 数据一致性 · 若 seed 不含 mock id 需补 seed SQL |

### 8.2 阻塞

**无硬阻塞**。

### 8.3 前置依赖（必须先完成）

1. **10.5 OutsourceStateMachine.vue 4 typecheck 错误修复**（10.1 dev log §9）
   - (a) `currentOrder` 类型 `OutsourceStateHistory` → `OutsourceOrder`
   - (b) 7 状态机 `stateSteps` 与 codegen 联合类型对齐（PM 决策 #3 范围）
   - (c) `useOutsourceStateMachine` composable 的 `transition` 函数签名与 codegen 一致
   - **否则 typecheck:ci gate 失败 · commit push 后 CI 红**

---

## 9. 签字

**PO 范蠡** · 2026-06-13 · Sprint 10 PM 决策#1 · 选项 A 内部 GitLab · 委派 DevOps 张良 + web-impl dev agent Opus 4.8 · 截止 2026-06-15

**关联签字**（待补）：
- DevOps 张良 · 待执行（GitLab 仓创建 + CI 接入）
- web-impl dev agent Opus 4.8 · 待执行（本地仓 init + 13 commit 拆分 + push）
- PM 范蠡 · 2026-06-13 · 决策发出
- SM 萧何 · 已协调集成 E 验证报告 · 决策依据引用

---

## 10. 与集成 E 验证的衔接

| 项 | 决策前状态 | 决策后状态 |
|----|-----------|-----------|
| 集成点 1（10.1 + 10.3） | ✅ PASS | ✅ PASS（不变） |
| 集成点 2（10.1 + 10.5） | 🟡 CONDITIONAL（InspectionDTO） | 🟡 CONDITIONAL（InspectionDTO 决策 #2 仍待） |
| 集成点 3（10.1 + 10.2） | ✅ PASS | ✅ PASS（不变） |
| 集成点 4（10.3 + 10.2 D2） | 🟢 PASS | 🟢 PASS（不变） |
| 集成点 5（10.5 + 10.2） | 🟡 CONDITIONAL（运行时委托 QA） | 🟡 CONDITIONAL（不变） |
| 集成点 6（10.4 独立） | 🟡 CONDITIONAL（V3/V4/V7 委托 QA） | 🟡 CONDITIONAL（不变） |
| **风险 #3（web-impl untracked）** | 🟡 中 · typecheck:ci gate 失效 | 🟢 已决策 · DevOps 执行后 gate 生效 |
| **PM 决策 #1** | 🟡 待决策 | ✅ 已决策（本文档） |
| **PM 决策 #2**（InspectionDTO） | 🟡 待决策 | 🟡 仍待 |
| **PM 决策 #3**（7 状态机 enum drift） | 🟡 待决策 | 🟡 仍待 · 前置依赖 |

**判定**：本决策通过后，集成 E 验证从 **🟡 CONDITIONAL GO** 收敛到 **3 项 PM 决策中 1/3 已回复** · 决策 #2 + #3 仍待回复 · 集成 E 验证闭环预计 2026-06-16。

---

**PM 决策书#1 完 · web-impl 仓接入内部 GitLab · multi-repo 架构首次落地 · DevOps 张良 + web-impl dev 执行 · 截止 2026-06-15**
