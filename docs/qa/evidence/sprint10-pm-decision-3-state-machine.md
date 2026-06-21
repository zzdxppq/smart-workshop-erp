# PM 决策书#3 · 7 状态机 enum drift 对齐

> **决策人**：PO 范蠡（PM 范蠡委派）
> **日期**：2026-06-13
> **Sprint**：V1.3.8 S10
> **关联**：Sprint 10 集成 E 验证 CONDITIONAL GO（`docs/qa/evidence/sprint10-integration-test-report.md` §4.3 PM 决策 #3）· 10.1 dev log §9 · 10.5 dev log §9
> **截止**：2026-06-15（24h 内）
> **决策等级**：🟡 中 · typecheck:ci 门禁前置 · Sprint 10 FAT 准入关键路径

---

## 1. 背景

### 1.1 问题源

Sprint 10 Story 10.5（web-impl 5 .vue any → unknown）IMPL 时，dev agent 在 `web-impl/src/views/production/OutsourceStateMachine.vue` 引入 4 typecheck 错误（10.1 dev log §9 实测验证）：

```
src/views/production/OutsourceStateMachine.vue(12,61): error TS2339: Property 'supplierName' does not exist on type 'OutsourceStateHistory'
src/views/production/OutsourceStateMachine.vue(13,60): error TS2339: Property 'workorderNo' does not exist on type 'OutsourceStateHistory'
src/views/production/OutsourceStateMachine.vue(22,88): error TS2345: Argument of type 'OutsourceState' is not assignable to parameter of type '"REJECTED" | "DRAFT" | "SENT" | "CLOSED" | "ACCEPTED" | "IN_PRODUCTION" | "INSPECTED" | "COMPLETED" | "REWORK"'
src/views/production/OutsourceStateMachine.vue(44,16): error TS2345: Argument of type '"REJECTED" | "DRAFT" | "SENT" | "CLOSED" | "ACCEPTED" | "IN_PRODUCTION" | "INSPECTED" | "COMPLETED" | "REWORK"' is not assignable to parameter of type 'OutsourceState'
```

### 1.2 enum drift 根因

三方 enum 不一致：

| 来源 | 7 状态机定义 | 枚举项数 |
|------|-------------|----------|
| **V1.3.7 1.22 合同定义**（10.5 引用） | `DRAFT / SUBMITTED / ACCEPTED / IN_PROGRESS / COMPLETED / REWORK / CLOSED` | **7** |
| **10.5 `OutsourceStateMachine.vue`** | `['DRAFT', 'SUBMITTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'REWORK', 'CLOSED']` | **7** |
| **openapi.yaml 当前 enum**（lines 473-478 + 494-495） | `[DRAFT, SENT, ACCEPTED, IN_PRODUCTION, INSPECTED, COMPLETED, CLOSED, REWORK, REJECTED]` | **9** |
| **codegen 联合类型**（derived from openapi.yaml） | `'DRAFT' \| 'SENT' \| 'ACCEPTED' \| 'IN_PRODUCTION' \| 'INSPECTED' \| 'COMPLETED' \| 'CLOSED' \| 'REWORK' \| 'REJECTED'` | **9** |

**根因**：openapi.yaml enum 是早期 IMPL 阶段（V1.3.7 1.22 IMPL 启动前）落地的 9 状态扩展版（含 `SENT/INSPECTED/REJECTED`），与 V1.3.7 1.22 Story 正式合同定义的 7 状态机不一致 → codegen 派生联合类型 → 10.5 引用 V1.3.7 1.22 真实状态机时 typecheck 失败。

### 1.3 影响

- **typecheck:ci 门禁失效**：4 错误阻塞全量 `vue-tsc --noEmit` 退出码 0
- **10.1 + 10.2 + 10.5 协同断裂**：codegen 输出与 10.5 .vue 实际消费的状态机不一致
- **后续 Sprint 11 backlog 受影响**：9.1 触发逻辑（10.1 dev log §9）也以 enum 为输入
- **风险等级**：🟡 中 · 不影响业务运行时（10.5 用 `as unknown as OutsourceStateHistory` cast 兜底）· 但影响类型层契约一致性

---

## 2. 选项分析

### 选项 A：openapi.yaml 改 V1.3.7 1.22 定义（**推荐**）

- **做法**：
  1. `backend/spec/openapi.yaml` 修改 `OutsourceStateHistory.fromState` / `OutsourceStateHistory.toState` / `OutsourceStateAdvanceRequest.targetState` 三个 enum 段
  2. 改为 7 状态：`[DRAFT, SUBMITTED, ACCEPTED, IN_PROGRESS, COMPLETED, REWORK, CLOSED]`
  3. 10.5 dev 同步修 `OutsourceStateMachine.vue`：用 `as const` 字面量类型替代 `OutsourceState` 联合类型别名 → 4 typecheck 错误逐个修复
  4. 10.1 dev 二次 regen（`npm run gen:api`）→ codegen 输出联合类型与 V1.3.7 1.22 对齐
- **优势**：
  - V1.3.7 1.22 是合同定义 · codegen 是 IMPL 派生 → 一致性归一化符合「合同为源 · 派生跟随」原则
  - 10.5 已用 7 状态作为业务语义 → 改动最小（10.5 不用改 stateSteps）
  - typecheck:ci 门禁一次通过 · 0 typecheck 错误
- **风险**：🟢 低
  - codegen 输出影响 10.5 / 10.2 / 9.2 既有代码 → 但 10.2/9.2 不直接消费 OutsourceState 联合类型（10.1 dev log §3 验证）· 10.5 用 `as const` 字面量隔离影响
  - openapi.yaml 是 backend 契约源 · 改动需 backend dev（10.1）同步 regen
  - 不破坏 V1.3.7 已部署业务（V1.3.7 实际 7 状态业务路径已上线）· 改的是 enum 而非业务流转

### 选项 B：V1.3.7 1.22 7 状态机改 codegen 联合类型

- **做法**：把 V1.3.7 1.22 7 状态机改成 `REJECTED/DRAFT/SENT/CLOSED/ACCEPTED/IN_PRODUCTION/INSPECTED/COMPLETED/REWORK`（9 状态）以对齐 codegen
- **风险**：🟡 中高
  - 破坏 V1.3.7 已部署业务（已上线 PENDING_SHIP→SHIPPING→… 流程）
  - 与 9.1 触发逻辑冲突（V1.3.7 1.22 已绑定的 SQL 触发器 / Redis 缓存 key 前缀）
  - 与 10.5 composable `useOutsourceStateMachine.ts` 内部 `OutsourceState` 类型不一致（10.5 第 8 行 `type OutsourceState = 'DRAFT' | 'SUBMITTED' | ...`）
- **结论**：❌ **不可接受** · 与合同冲突 + 部署不可逆

### 选项 C：保留双轨 + 显式转换

- **做法**：
  1. 10.5 dev 用 `as OutsourceStateHistory['state']` 显式转换
  2. 4 typecheck 错误用 `// @ts-expect-error` 注释
- **风险**：🟡 中
  - 与 V1.3.8 技术债 1 项叠加（`// @ts-expect-error` + cast 是不严谨类型）
  - 不享受 codegen 强类型红利（10.1 codegen Story 价值部分丧失）
  - enum drift 长期保留 → Sprint 11 / 12 维护成本递增
- **结论**：🟡 **次选** · 仅在选项 A 不可行时使用

---

## 3. 决策

**选项 A · openapi.yaml 改 V1.3.7 1.22 定义**

理由链：合同定义（V1.3.7 1.22）→ codegen 派生 → 前端消费 · 一致性归一化从源头（合同）单向同步，避免下游多向漂移。

---

## 4. 依据

### 4.1 V1.3.7 1.22 是合同定义

- V1.3.7 PRD Story 1.22（10.5 引用基线）= `DRAFT/SUBMITTED/ACCEPTED/IN_PROGRESS/COMPLETED/REWORK/CLOSED` · 7 主状态
- V1.3.7 合同附录 B-2 业务定义是另一套（`PENDING_SHIP/...`）· 与 10.5 引用的 1.22 简化版不冲突（两版在不同模块）
- **原则**：合同定义优先 · codegen 是 IMPL 阶段派生 · 派生跟随合同

### 4.2 codegen 是 IMPL 阶段派生

- openapi.yaml enum 是 backend IMPL 早期（V1.3.7 1.22 IMPL 启动前）落地的扩展版
- Sprint 10.1 codegen Story 触发 enum drift 暴露 · 体现 codegen 的契约护栏价值
- **原则**：派生层（codegen）跟随源层（合同）· 不允许反向修改源层

### 4.3 一致性归一化

- 当前三方不一致 → 单一决策点（选项 A）归一到 V1.3.7 1.22
- 改动范围最小化（3 处 enum 段 + 10.5 .vue `as const` + 10.1 regen）
- 不破坏 V1.3.7 已部署业务（enum 改动 ≠ 业务流转改动）

---

## 5. 7 状态机精确映射表

### 5.1 V1.3.7 1.22 vs codegen vs openapi.yaml 当前

| # | V1.3.7 1.22 合同 | codegen 联合类型 | openapi.yaml 当前 | 业务语义 | 决策 |
|---|----------------|-----------------|------------------|----------|------|
| 1 | `DRAFT` | `DRAFT` | `DRAFT` | 草稿（生管下单后） | ✅ **保留** |
| 2 | `SUBMITTED` | ❌ 缺 | ❌ 缺 | 已提交（厂商待接单） | ➕ **加** |
| 3 | `ACCEPTED` | `ACCEPTED` | `ACCEPTED` | 已接单 | ✅ **保留** |
| 4 | `IN_PROGRESS` | ❌ 缺（`IN_PRODUCTION` 不一致） | ❌ 缺（`IN_PRODUCTION` 不一致） | 进行中（厂商加工） | ➕ **加 · IN_PRODUCTION → IN_PROGRESS** |
| 5 | `COMPLETED` | `COMPLETED` | `COMPLETED` | 已完成（厂商加工完成） | ✅ **保留** |
| 6 | `REWORK` | `REWORK` | `REWORK` | 返修中 | ✅ **保留** |
| 7 | `CLOSED` | `CLOSED` | `CLOSED` | 已关闭（终态） | ✅ **保留** |
| 8 | — | `SENT`（多余） | `SENT`（多余） | codegen 早期 IMPL 残留 | ❌ **删** |
| 9 | — | `INSPECTED`（多余） | `INSPECTED`（多余） | codegen 早期 IMPL 残留 | ❌ **删** |
| 10 | — | `REJECTED`（多余） | `REJECTED`（多余） | codegen 早期 IMPL 残留 | ❌ **删** |

### 5.2 openapi.yaml 改动范围

3 处 enum 段需要改（`backend/spec/openapi.yaml`）：

| 行号 | Schema | 字段 | 当前 enum | 改为 |
|------|--------|------|----------|------|
| 473-475 | `OutsourceStateHistory` | `fromState` | `[DRAFT, SENT, ACCEPTED, IN_PRODUCTION, INSPECTED, COMPLETED, CLOSED, REWORK, REJECTED]` | `[DRAFT, SUBMITTED, ACCEPTED, IN_PROGRESS, COMPLETED, REWORK, CLOSED]` |
| 476-478 | `OutsourceStateHistory` | `toState` | 同上 | 同上 |
| 494-495 | `OutsourceStateAdvanceRequest` | `targetState` | 同上 | 同上 |

> **注**：`OutsourceStateRollbackRequest`（line 504-510）无 enum 字段 · 无需改动。

### 5.3 codegen 联合类型预期（regen 后）

```typescript
// web-impl/src/api/generated/models/OutsourceStateHistory.ts
fromState?: 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REWORK' | 'CLOSED';
toState?: 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REWORK' | 'CLOSED';

// web-impl/src/api/generated/models/OutsourceStateAdvanceRequest.ts
targetState: 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REWORK' | 'CLOSED';
```

### 5.4 10.5 OutsourceStateMachine.vue 改动指引

| 错误位置 | 错误类型 | 修复指引 |
|----------|---------|---------|
| L12, L13 | `Property 'supplierName' / 'workorderNo' does not exist on type 'OutsourceStateHistory'` | `const currentOrder = ref<OutsourceOrder \| null>(null)` 改类型为 `OutsourceOrder`（已 generated `web-impl/src/api/generated/models/OutsourceOrder.ts`） |
| L22 | `Argument of type 'OutsourceState' is not assignable to parameter of type '"REJECTED" \| "DRAFT" \| ...'` | `function doTransition(s: OutsourceState)` · composable `transition` 函数签名对齐 codegen 联合类型（regen 后已对齐）· `OutsourceState` 类型定义同步改为 codegen 联合类型或 `as const` 字面量 |
| L44 | `Argument of type '联合类型' is not assignable to parameter of type 'OutsourceState'` | `composable transition` 函数签名 `to: OutsourceState` 改为 `to: OutsourceStateAdvanceRequest['targetState']`（直接用 codegen 联合类型） |
| 综合 | `as const` 字面量类型 | `useOutsourceStateMachine.ts` L8 `export type OutsourceState = ...` 替换为 `export const OutsourceState = ['DRAFT', ...] as const` + `export type OutsourceState = typeof OutsourceState[number]` · 与 codegen 联合类型结构同构 |

---

## 6. 委派

### 6.1 10.5 dev（dev agent Opus 4.8）

**截止**：2026-06-15 EOD（24h 内）

任务清单：

1. **修 `OutsourceStateMachine.vue` 4 typecheck 错误**（L12/L13/L22/L44）：
   - L12/L13：改 `currentOrder` 类型为 `OutsourceOrder`
   - L22：`doTransition` 函数参数用 `OutsourceStateAdvanceRequest['targetState']`
   - L44：`useOutsourceStateMachine` composable `transition` 函数签名与 codegen 一致
2. **`useOutsourceStateMachine.ts` 用 `as const` 字面量类型**：
   - 替换 `export type OutsourceState = ...` 为 `export const OutsourceState = [...] as const` + `export type OutsourceState = typeof OutsourceState[number]`
   - 保证与 codegen 联合类型结构同构
3. **本地验证**：`npm run typecheck` 退出 0（在 web-impl 仓内执行）
4. **回写**：在 10.5 dev log 标注 `§9 修复完成` + 4 错误逐条修复证据 + `typecheck` 退出码截图

### 6.2 10.1 dev（dev agent Opus 4.8）

**截止**：2026-06-15 EOD（24h 内 · 0.5h 增量 IMPL）

任务清单：

1. **修 `backend/spec/openapi.yaml` 3 处 enum**（L473-475 / L476-478 / L494-495）：
   - 改为 `[DRAFT, SUBMITTED, ACCEPTED, IN_PROGRESS, COMPLETED, REWORK, CLOSED]`
2. **二次 regen**：`cd web-impl && rm -rf src/api/generated && npm run gen:api` · 验证 codegen 输出联合类型
3. **全量 typecheck**：`npm run typecheck` 退出 0
4. **typecheck:ci 门禁**：`npm run typecheck:ci` 退出 0 + `git diff --exit-code src/api/generated` 退出 0
5. **回写**：在 10.1 dev log 标注 `§9 二次 regen 完成` + 改动文件 diff + codegen 输出验证

### 6.3 QA 商鞅（执行验证）

**截止**：2026-06-15 EOD（按集成 E 验证报告 §3.1 委派 2 时序）

任务清单：

1. typecheck:ci 门禁执行（10.1 dev 修完后）：`cd web-impl && npm run typecheck:ci` → 期望 退出 0
2. 10.5 9.2 4 测例回归 + 14 E2E test-execute（按集成 E 验证报告 §3.1 委派 1）

### 6.4 DevOps 张良（CI 接入）

**截止**：2026-06-15 EOD

任务清单（按集成 E 验证报告 §3.2 委派 4）：

1. `web-impl/.gitlab-ci.yml` 接入 GitLab repo
2. typecheck stage 加 `rm -rf node_modules/.cache`（10.1 architect review §6 IMPL 注意事项 2）
3. android-impl distributionUrl 镜像确认（10.4 architect review §6 IMPL 注意事项 2）

---

## 7. 风险与缓解

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | codegen 输出影响 10.5 / 10.2 / 9.2 既有代码 | 🟢 低 | 10.5 用 `as const` 字面量隔离 · 10.2 / 9.2 不直接消费 OutsourceState 联合类型（10.1 dev log §3 验证） |
| 2 | openapi.yaml 改动影响 backend 契约 | 🟢 低 | 改动仅 enum 字段 · 业务流转不变 · V1.3.7 已部署业务路径不受影响 |
| 3 | 10.5 dev / 10.1 dev 双仓同步时序 | 🟡 中 | 10.1 先改 openapi.yaml + regen → 10.5 后改 .vue 用 codegen 联合类型（避免 10.5 引用未 regen 的旧 codegen） |
| 4 | 9.1 触发逻辑冲突 | 🟢 低 | 9.1 用 `OutsourceStateHistory` schema 字段（`id/outsourceId/fromState/toState`）· enum 改动不影响 SQL 触发器逻辑（触发器按业务事件触发 · 不按 enum） |
| 5 | V1.3.7 已部署业务路径与新 enum 不一致 | 🟢 低 | V1.3.7 实际 7 状态路径（PENDING_SHIP/...）是另一模块 · 与 10.5 引用的 1.22 简化版不冲突 |
| 6 | Sprint 11 backlog 衔接 | 🟢 低 | 已在 `sprint-10-summary.md` §Sprint 11 backlog #5 标注 · enum drift 修复后该 backlog 项关闭 |

---

## 8. 验收标准

### 8.1 IMPL 完成标准

- ✅ `backend/spec/openapi.yaml` 3 处 enum 改为 `[DRAFT, SUBMITTED, ACCEPTED, IN_PROGRESS, COMPLETED, REWORK, CLOSED]`
- ✅ `web-impl/src/api/generated/` regen 后联合类型 = 7 状态
- ✅ `web-impl/src/views/production/OutsourceStateMachine.vue` 4 typecheck 错误修复
- ✅ `web-impl/src/composables/useOutsourceStateMachine.ts` 用 `as const` 字面量
- ✅ `npm run typecheck` 退出 0
- ✅ `npm run typecheck:ci` 退出 0 + `git diff --exit-code src/api/generated` 退出 0

### 8.2 QA 验证标准（按集成 E 验证报告 §6.3 时序）

- ✅ QA 商鞅 typecheck:ci 门禁 PASS
- ✅ 9.2 4 测例回归 PASS
- ✅ 14 E2E test-execute PASS

### 8.3 FAT 准入衔接

- 决策 #3 通过 → 集成 E 验证 CONDITIONAL GO → GO（按集成 E 验证报告 §5.2 判定）
- 与决策 #1（web-impl git remote）+ 决策 #2（InspectionDTO schema）共同构成 V1.3.8 FAT 准入前置

---

## 9. 签字

- **PO 范蠡** · 2026-06-13 · Sprint 10 PM 决策 #3 落定 · 选项 A · openapi.yaml 改 V1.3.7 1.22 定义
- **PM 范蠡** · 2026-06-13 · 委派 10.5 dev + 10.1 dev + QA 商鞅 + DevOps 张良
- **architect 鲁班** · 待审核（10.1 dev log §9 标注需 architect review 同步）
- **10.1 dev agent Opus 4.8** · 待执行（截止 2026-06-15）
- **10.5 dev agent Opus 4.8** · 待执行（截止 2026-06-15）
- **QA 商鞅** · 待执行 typecheck:ci 门禁 + 9.2 回归 + 14 E2E（截止 2026-06-15）
- **DevOps 张良** · 待执行 CI 接入 + Nexus 镜像（截止 2026-06-15）

**Sprint 10 PM 决策 #3 · 7 状态机 enum drift 对齐 · 选项 A · openapi.yaml 改 V1.3.7 1.22 定义 · 截止 2026-06-15**
