# typecheck:ci 校验计划 · Sprint 10 集成 E · QA 委派 #2

> **作者**：QA 商鞅
> **日期**：2026-06-14
> **Sprint**：V1.3.8 S10 · 集成 E 验证 · QA 委派 #2
> **来源**：集成 E 验证报告 §3.1 委派 2（10.1 dev log §9 + 10.5 dev log §9）
> **前置**：PM 决策 #1 ✅（web-impl 接入 git remote）+ PM 决策 #3 ✅（7 状态机 enum drift 对齐）
> **截止**：2026-06-15
> **目标**：`typecheck:ci` 退出码 0 + 0 错误 + 0 diff · V1.3.8 FAT 准入关键路径

---

## 1. 校验目标与范围

### 1.1 目标

Sprint 10 集成 E 验证委派 #2 · 对 web-impl 仓执行 10.1 门禁（typecheck:ci）全量校验，确认 PM 决策 #3（7 状态机 enum drift）修复后整体 type-check 零错误、generated/ 产物零未 commit 变更、构建成功。

### 1.2 范围

| 维度 | 范围 |
|------|------|
| 入口 | web-impl 仓根目录（与 backend 同仓·多仓布局） |
| TypeScript 检查 | 全仓 vue-tsc --noEmit（含 `src/api/generated/**`） |
| 契约 gate | `git diff --exit-code src/api/generated` |
| 构建 | vite build（含 typecheck -b） |
| 期望结果 | 4 步骤全部退出码 0 · 0 TS 错误 · 0 未 commit diff · dist/ 生成 |

### 1.3 不在范围

- backend 仓 typecheck（Maven/Spring · 10.3 已自验证 8/8 PASS）
- android-impl 仓（10.4 独立 · V3/V4/V7 委派 #3 单独跑）
- 14 E2E test-execute（委派 #1 单独跑）
- 9.2 4 测例 Playwright 回归（委派 #1 内嵌）
- InspectionDTO schema 决策（PM 决策 #2 · 不阻塞 typecheck:ci）

---

## 2. 命令链（开发工作站可复制粘贴）

### 2.1 一键执行脚本

```bash
# ================================================================
# QA 商鞅 · typecheck:ci 全量校验 · Sprint 10 集成 E 委派 #2
# 前置：PM 决策 #1（git remote 接入）+ PM 决策 #3（enum drift 对齐）已完成
# 截止：2026-06-15
# ================================================================

cd web-impl

# 前置 0：拉取最新 main（确保 PM 决策 #3 commit 已落地）
git pull origin main

# 步骤 1：codegen 二次 regen（PM 决策 #3 修完 → openapi.yaml 可能更新 → 重新生成）
rm -rf src/api/generated
npm run gen:api
# 期望：退出码 0
# 期望：40 services + 100 models + index.ts + core/ + v138.ts shim 重新生成
# 期望：终端输出无 ERROR 关键字

# 步骤 2：typecheck（vue-tsc --noEmit · 不输出文件）
npm run typecheck
# 期望：退出码 0
# 期望：0 错误（0 TS 错误 + 0 vue-tsc 错误）
# 期望：终端输出无 `error TS` 关键字
# 期望：终端输出无 `error TS2xxx` 关键字

# 步骤 3：typecheck:ci（typecheck + git diff gate）
npm run typecheck:ci
# 期望：退出码 0
# 期望：vue-tsc --noEmit 退出 0（前置步骤 2）
# 期望：git diff --exit-code src/api/generated 在 tracked 文件下退出 0
# 期望：src/api/generated/ 无未 commit 变更

# 步骤 4：build（含 typecheck -b）
npm run build
# 期望：退出码 0
# 期望：dist/ 目录生成
# 期望：终端输出无 ERROR 关键字
# 期望：终端输出含 `built in` 关键字（vite 成功标志）
```

### 2.2 验证产物的命令（可选 · 步骤间 sanity check）

```bash
# 步骤 1 验证：codegen 产物清单
ls src/api/generated/ | wc -l
# 期望：5（core/ index.ts models/ services/ v138.ts）

ls src/api/generated/models/*.ts | wc -l
# 期望：100（10.1 IMPL 后基线 · 或 PM 决策 #3 后更新数）

ls src/api/generated/services/*.ts | wc -l
# 期望：40（10.1 IMPL 后基线 · 按 tag 分组）

# 步骤 1 验证：5 目标 DTO 存在（10.5 替换基线）
for dto in Quote Order OrderProfit OutsourceStateHistory OutsourceStateAdvanceRequest MaterialBarcodeParseResponse; do
  test -f "src/api/generated/models/${dto}.ts" && echo "$dto: OK" || echo "$dto: MISSING"
done
# 期望：6/6 OK

# 步骤 3 验证：git diff gate 单独跑（调试用）
cd web-impl
git status --porcelain src/api/generated | head -20
# 期望：空（无未 commit 变更）

git diff --exit-code src/api/generated
# 期望：$? == 0

# 步骤 4 验证：dist/ 产物
ls -la dist/
# 期望：assets/ + index.html 存在

test -f dist/index.html && echo "build OK" || echo "build FAILED"
```

---

## 3. 校验项清单（4 步骤 · 0 错误期望）

| 步骤 | 命令 | 期望退出码 | 期望产出 | 期望关键字 | 失败处理流程 |
|------|------|-----------|----------|-----------|--------------|
| **1** | `rm -rf src/api/generated && npm run gen:api` | 0 | 5 个目录/文件（`core/`+`index.ts`+`models/`+`services/`+`v138.ts`） | 无 ERROR · 无 Cannot parse · 无 Reference error | A · codegen 失败 → 见 §4.1 |
| **2** | `npm run typecheck` | 0 | 0 错误（0 TS + 0 vue-tsc） | 无 `error TS` · 无 `error TS2xxx` · 无 `Object is possibly` · 无 `Property 'X' does not exist` | B · typecheck 4 错误 → 见 §4.2<br/>C · typecheck 其他错误 → 见 §4.3 |
| **3** | `npm run typecheck:ci` | 0 | vue-tsc 退出 0 + git diff 退出 0 | 步骤 2 + 步骤 3 合并输出均无 error | D · typecheck:ci diff → 见 §4.4 |
| **4** | `npm run build` | 0 | dist/ 目录（含 assets/ + index.html） | 无 ERROR · 含 `built in` | E · build 失败 → 见 §4.5 |

### 3.1 关键断言

- **退出码**：每步骤 `$? == 0`
- **错误数**：步骤 2 必须 0 TS 错误（PM 决策 #3 已修完 4 错误）
- **diff 数**：步骤 3 必须 0 uncommitted file in `src/api/generated/`
- **build 产物**：步骤 4 必须 `dist/index.html` 存在 + `dist/assets/` 非空

### 3.2 顺序约束（与 PM 决策 #1/#3 协同）

```
PM 决策 #1 (web-impl git remote 接入)
    ↓
PM 决策 #3 (7 状态机 enum drift 对齐 · openapi.yaml 或 composable)
    ↓
QA 商鞅执行 typecheck:ci 校验（本计划）
    ↓
V1.3.8 FAT 准入（PO 范蠡 + 客户）
```

---

## 4. 失败处理流程（5 类失败 · 对应 5 类修复路径）

### 4.1 codegen 失败（步骤 1）

**症状**：
- `npm run gen:api` 退出码非 0
- 终端输出 `Cannot parse openapi.yaml` / `ERROR` / `Reference error`

**根因**：
- openapi.yaml 字段漂移（PM 决策 #3 修复过程中可能引入 schema 结构错误）
- openapi.yaml 重复键（`/drawings` + `components:`/`schemas:` 块）
- 路径错误（`-i ../backend/spec/openapi.yaml` 相对路径）

**修复流程**：
1. QA 商鞅保留 codegen 错误输出（完整 + 含行号）
2. 通过 Slack/IM 通知 **PO 范蠡 + backend dev agent**
3. PO 范蠡决策是否 rollback PM 决策 #3 commit · 或紧急修复 openapi.yaml
4. backend dev agent 修复 openapi.yaml 结构错误（参考 10.1 dev log §1.2 修复模式）
5. 修复后 QA 商鞅重跑步骤 1

**SLA**：24h 内完成（PM 决策 #3 回退/修复）+ 通知 QA 重跑

---

### 4.2 typecheck 4 错误（步骤 2 · PM 决策 #3 修复不完整）

**症状**：
- `npm run typecheck` 退出码非 0
- 终端输出 4 处 `error TS2xxx` · 与 10.1 dev log §9 列出的 4 错误同形态：
  - `error TS2339: Property 'supplierName' does not exist on type 'OutsourceStateHistory'`
  - `error TS2339: Property 'workorderNo' does not exist on type 'OutsourceStateHistory'`
  - `error TS2345: Argument of type 'OutsourceState' is not assignable to parameter of type '"REJECTED" | "DRAFT" | ...'`
  - `error TS2345: Argument of type '"REJECTED" | "DRAFT" | ...' is not assignable to parameter of type 'OutsourceState'`

**根因**：
- PM 决策 #3 修复不完整（10.5 dev 路径修复但遗漏 1-2 处）
- openapi.yaml schema 与 composable 内部状态枚举未完全对齐
- `currentOrder` 类型 / `stateSteps` / `useOutsourceStateMachine.transition` 签名三处之一仍漂移

**修复流程**：
1. QA 商鞅记录 4 错误的精确行号 + 列号 + TS 错误码
2. 通过 Slack/IM 通知 **PM 范蠡 + 10.5 dev agent Opus 4.8**
3. PM 范蠡确认 PM 决策 #3 修复路径（10.5 dev log §1 路径选择）
4. 10.5 dev 修复路径：
   - (a) `currentOrder` 类型 `OutsourceStateHistory` → `OutsourceOrder`（supplierName/workorderNo 属于 OutsourceOrder）
   - (b) 7 状态机 `stateSteps` 与 generated 联合类型对齐（要么改 openapi.yaml 要么改 composable 内部）
   - (c) `useOutsourceStateMachine` composable 与 codegen 一致
5. 修复后 QA 商鞅重跑步骤 2

**SLA**：12h 内完成修复 + 通知 QA 重跑（关键路径 · V1.3.8 FAT 准入阻塞）

---

### 4.3 typecheck 其他错误（步骤 2 · 5 文件 any 替换未完）

**症状**：
- `npm run typecheck` 退出码非 0
- 终端输出非 §4.2 列出的 4 错误 · 而是其他 TS 错误（如 `any` 残留、`strict: true` 引发的 null 报错等）

**根因**：
- 10.5 dev 5 文件 any 替换遗漏（10.5 dev log §3 V1-V3 验证通过后 · 但 V4 typecheck 委托 QA · 实际跑可能暴露 1-2 处遗漏）
- 10.5 dev log §2.5 InspectionCreate.vue 用 `Promise<unknown>`（隐式）· 若 store 实际返回 Promise<typed> 可能报类型不匹配
- Sprint 8.4 / 9.2 既有代码与 10.1 codegen 类型收紧冲突

**修复流程**：
1. QA 商鞅记录所有非 §4.2 列出的 4 错误的精确行号 + 列号 + TS 错误码
2. 通过 Slack/IM 通知 **10.5 dev agent Opus 4.8 + PM 范蠡**
3. 10.5 dev 修复（针对自己 5 文件的遗漏）
4. 若涉及 Sprint 8.4 / 9.2 既有代码 · 升级为 PM 决策（独立于决策 #3）
5. 修复后 QA 商鞅重跑步骤 2

**SLA**：12-24h 内完成修复 + 通知 QA 重跑（依据错误范围）

---

### 4.4 typecheck:ci diff 失败（步骤 3 · generated/ 未 commit）

**症状**：
- `npm run typecheck:ci` 退出码非 0
- `vue-tsc --noEmit` 通过 · 但 `git diff --exit-code src/api/generated` 失败
- 终端输出：`src/api/generated/services/X.ts` 标注 modified / untracked

**根因**：
- 步骤 1 regen 后 generated/ 有未 commit 变更
- PM 决策 #1（web-impl git remote 接入）不完整（web-impl/ 仍 untracked dir）
- codegen 产物漂移（每次 regen 因 openapi.yaml 行号/格式微调导致产物不一致）

**修复流程**：
1. QA 商鞅记录 diff 涉及的文件清单（`git status --porcelain src/api/generated`）
2. 若 web-impl/ 仍 untracked（PM 决策 #1 未生效）：
   - **升级为硬阻塞** · 通知 PO 范蠡 + PM 范蠡 + 10.1 dev agent Opus 4.8
   - PM 决策 #1 必须在 typecheck:ci 执行前完成（无法绕过）
   - 备选方案：改 gate 逻辑为 `git status --porcelain src/api/generated | grep -q .`（同时覆盖 untracked + modified · 增加复杂度 · 需 PM 二次决策）
3. 若 web-impl/ tracked 但 codegen 产物漂移：
   - 通知 10.1 dev agent Opus 4.8 · commit codegen 产物
   - 修复后 QA 商鞅重跑步骤 3
4. 修复后 QA 商鞅重跑步骤 3

**SLA**：6h 内完成（PM 决策 #1 关键路径 · 阻塞 V1.3.8 FAT 准入）

---

### 4.5 build 失败（步骤 4）

**症状**：
- `npm run build` 退出码非 0
- `vue-tsc -b`（build 内含 typecheck）通过 · 但 `vite build` 失败
- 终端输出：`RollupError` / `Could not resolve` / `ENOENT` 等 vite 错误

**根因**：
- vite 配置问题（10.1 dev log §6 已标注 `openapi` bin 短名规避 Windows PATH · 类似 PATH 问题）
- 资源引用错误（10.5 `OutsourceStateMachine.vue` 模板中字段引用缺失 · 但 TS 层不报错）
- 第三方库版本冲突（npm peer dep · 10.1 dev log §6 #6 已标注 eslint 9 vs @vue/eslint-config-typescript 13）

**修复流程**：
1. QA 商鞅保留 vite build 错误输出（完整 + 含文件路径 + 行号）
2. 通过 Slack/IM 通知 **10.1 dev agent Opus 4.8 + DevOps 张良**
3. 10.1 dev 修复 vite 配置或资源引用
4. 若是 peer dep 冲突 · DevOps 张良落实 `npm install --legacy-peer-deps` 或升级 eslint 8 → 9（V1.3.9 backlog）
5. 修复后 QA 商鞅重跑步骤 4

**SLA**：12-24h 内完成修复 + 通知 QA 重跑

---

## 5. 顺序约束（与 PM 决策 #1/#3 协同）

### 5.1 严格顺序

| # | 步骤 | 责任 | 前置 | 输出 |
|---|------|------|------|------|
| **A** | PM 决策 #1：web-impl 仓接入 git remote | PM 范蠡 | web-impl/ 当前 untracked | web-impl/ tracked + git remote 配通 |
| **B** | PM 决策 #3：7 状态机 enum drift 对齐 | PM 范蠡 + 10.5 dev | openapi.yaml + composable + stateSteps 三处对齐 | 4 typecheck 错误修复（10.1 dev log §9） |
| **C** | QA 商鞅执行 typecheck:ci 校验（本计划） | QA 商鞅 | A + B 完成 | 4 步骤全部退出 0 |
| **D** | V1.3.8 FAT 准入 | PO 范蠡 + 客户 | C 通过 + 14 E2E 通过 + DevOps 接入完成 | FAT 全量 2830 测例准入 |

### 5.2 关键依赖

```
PM 决策 #1 ──→ typecheck:ci git diff gate 生效
PM 决策 #3 ──→ typecheck 0 错误期望达成
PM 决策 #1 + PM 决策 #3 ──→ QA 委派 #2 可执行
QA 委派 #2 + QA 委派 #1 + DevOps 接入 ──→ V1.3.8 FAT 准入
```

### 5.3 时间窗口

- PM 决策 #1 + #3 截止：**2026-06-14 24:00**
- QA 商鞅委派 #2 截止：**2026-06-15 24:00**
- V1.3.8 FAT 准入：**2026-06-16+**（待客户服务器就绪）

---

## 6. 风险与阻塞

### 6.1 已识别风险（5 项）

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | PM 决策 #3 修复不完整（4 错误残留 1-2 处） | 🟡 中 | §4.2 修复流程 · 12h SLA |
| 2 | PM 决策 #1 未完成（web-impl/ 仍 untracked）· typecheck:ci gate 失效 | 🟡 中 | §4.4 修复流程 · 6h SLA · 升级为硬阻塞 |
| 3 | codegen 漂移（PM 决策 #3 修改 openapi.yaml → regen 后产物行号变化） | 🟢 低 | §4.4 修复流程 · 10.1 dev commit 产物 |
| 4 | 5 .vue any 替换遗漏（10.5 V4 typecheck 委托 QA · 实际跑可能暴露遗漏） | 🟡 中 | §4.3 修复流程 · 12-24h SLA |
| 5 | vite build 第三方库版本冲突（peer dep · eslint 9 vs @vue/eslint-config-typescript 13） | 🟢 低 | §4.5 修复流程 · V1.3.9 backlog |

### 6.2 硬阻塞（0 项）

✅ **本计划范围内无硬阻塞**。所有失败路径均有修复流程 + SLA + 责任归属。

### 6.3 跨委派依赖

- 本委派 #2 依赖 PM 决策 #1（git remote）+ PM 决策 #3（enum drift）
- 本委派 #2 与委派 #1（14 E2E + 9.2 回归）独立 · 可并行
- 本委派 #2 与委派 #4（DevOps `.gitlab-ci.yml` 接入 + Nexus 镜像）独立 · 可并行
- 本委派 #2 与委派 #3（gradle wrapper V3/V4/V7）独立 · Sprint 11 范畴

---

## 7. 执行与归档

### 7.1 执行清单

- [ ] **步骤 1**：codegen（regen `src/api/generated/`）
- [ ] **步骤 2**：typecheck（`vue-tsc --noEmit`）
- [ ] **步骤 3**：typecheck:ci（typecheck + git diff gate）
- [ ] **步骤 4**：build（`vue-tsc -b && vite build`）
- [ ] **归档**：命令输出保存到 `docs/qa/evidence/sprint10-typecheck-result-{timestamp}.log`

### 7.2 通过标准

| 维度 | 通过标准 |
|------|----------|
| 步骤 1 | 退出 0 + 5 个目录/文件重新生成 |
| 步骤 2 | 退出 0 + 0 `error TS` 关键字 |
| 步骤 3 | 退出 0 + `git diff --exit-code src/api/generated` 退出 0 |
| 步骤 4 | 退出 0 + `dist/index.html` 存在 + `dist/assets/` 非空 |
| 全流程 | 4/4 PASS · V1.3.8 FAT 准入关键路径解锁 |

### 7.3 输出物

1. **本计划**：`docs/qa/evidence/sprint10-typecheck-plan.md`（本文档）
2. **执行日志**：`docs/qa/evidence/sprint10-typecheck-result-{timestamp}.log`
3. **通过报告**：`docs/qa/evidence/sprint10-typecheck-pass-report.md`（4/4 PASS 后）
4. **失败报告**：`docs/qa/evidence/sprint10-typecheck-fail-report.md`（如有 · 含失败步骤 + 根因 + 修复责任）

---

## 8. 签字

- **QA 商鞅** · 2026-06-14 · 计划撰写完成 · 待 PM 决策 #1 + #3 通过后实机执行
- **SM 萧何** · 2026-06-14 · 集成 E 验证协调完成 · 委派 #2 计划归档
- **PM 范蠡** · 待决策 #1 + #3 通过（截止 2026-06-14 24:00）
- **10.5 dev agent Opus 4.8** · 待 PM 决策 #3 路径确定后修复 4 错误
- **10.1 dev agent Opus 4.8** · 待 PM 决策 #3 路径确定后修复 codegen 配置 / vite 配置
- **DevOps 张良** · 待 `.gitlab-ci.yml` 接入（独立委派 #4）

**QA 商鞅委派 #2 计划就绪 · 等待 PM 决策 #1 + #3 通过后实机执行 typecheck:ci · 截止 2026-06-15**