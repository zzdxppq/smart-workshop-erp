# PM 决策书#2 · InspectionDTO schema V1.3.9 backlog

> 决策人：PO 范蠡
> 日期：2026-06-13
> Sprint：V1.3.8 S10
> 关联：Sprint 10 集成 E 验证 CONDITIONAL GO · PM 决策#2
> 截止：V1.3.9 Sprint 1 启动时

---

## 1. 背景

### 1.1 问题定位

Sprint 10 Story 10.1 IMPL 阶段，dev agent 在执行 `npm run gen:api` 后对 `backend/spec/openapi.yaml` 全量 schema 检索发现：

- **InspectionDTO schema 在 `backend/spec/openapi.yaml` 中不存在**
- 10.1 dev log §5 已确认：`Inspection.ts` 模型文件未生成（openapi.yaml 无对应 schema 关键字）
- 10.1 dev log §3.1 IMPL 约束落实注释 #4 明确标注："InspectionDTO 在 openapi.yaml 中无对应 schema · 10.5 替换目标需重新评估"

### 1.2 10.5 临时方案（Option A · 已落地）

10.5 dev 在 `web-impl/src/views/quality/InspectionCreate.vue` 6 个 any 替换中，5 个采用 Option A：

```typescript
// 复用 codegen 已生成的 qualityStatus 枚举
import type { MaterialBarcodeParseResponse } from '@/api/generated/models/MaterialBarcodeParseResponse'
type QualityStatus = MaterialBarcodeParseResponse['qualityStatus']  // 'PENDING' | 'PASSED' | 'REJECTED'

// 本地声明表单入参接口（与 backend POST /api/v1/quality/inspections 实际入参一致）
interface InspectionFormPayload {
  type: 'IQC' | 'IPQC' | 'OQC'
  materialCode: string
  batchNo: string
  qty: number
  inspector: string
  aql: number
  qualityStatus?: QualityStatus
}
```

### 1.3 价值影响

- ✅ 当前 InspectionCreate.vue 0 `any` 命中 · 业务可工作
- ❌ InspectionFormPayload 本地接口**不享受 codegen 强类型保护**
- ❌ 一旦 backend 调整 `POST /api/v1/quality/inspections` 入参字段，frontend 无类型层感知
- ❌ 与 Story 10.1 codegen 集成的核心价值（"契约变更拦截"）存在脱节

### 1.4 Sprint 11 PRD 对齐检查发现

`sprint-11-prd-alignment-check.md` §PM 决策项标注：InspectionDTO schema 缺失是 1 项需 PM 决策事项 · 10.5 Option A 临时方案在 V1.3.9 可一次性替换为 `import type { InspectionDTO }`。

---

## 2. 选项分析

### 选项 A · 纳入 V1.3.9 backlog（推荐）

| 维度 | 评估 |
|------|------|
| 决策路径 | (A) backend dev 在 V1.3.9 Sprint 1 启动时补齐 Inspection schema + 端点定义 · (B) 10.5 dev 同步从 Option A 本地接口切换到 `import type { InspectionDTO }` |
| 阻塞 Sprint 10 FAT | ❌ 不阻塞（10.5 Option A 临时方案已可工作） |
| 阻塞 Sprint 11 立项 | ❌ 不阻塞（backend dev 任务在 V1.3.9 Sprint 1 启动时启动，非 Sprint 11 立项前置） |
| 享受 codegen 强类型 | ✅ 一次性切换后与 openapi.yaml 契约强绑定 · backend 字段调整前端 typecheck 立刻感知 |
| 与 7 状态机 enum 对齐（PM 决策#3）协同 | ✅ 同一时间窗口可一并处理 openapi.yaml schema 补齐工作（避免多次返工） |
| Sprint 11 backlog 优先级 | 🟡 中 · 不影响 Sprint 10 FAT 准入 |
| 风险 | 🟢 低 · 10.5 Option A 临时方案已工作 · V1.3.9 切换有兜底 |

### 选项 B · Sprint 11 立即补

| 维度 | 评估 |
|------|------|
| 决策路径 | backend dev 立即在 Sprint 11 立项前加 InspectionDTO schema 到 openapi.yaml · 10.5 dev 同步从 Option A 切换 |
| 阻塞 Sprint 10 FAT | ❌ 不阻塞（与 A 相同 · 10.5 已可工作） |
| 阻塞 Sprint 11 立项 | 🟡 **是** · 需等待 backend dev 完成 schema 补齐 + 10.5 dev 切换后再启动 Sprint 11 |
| 享受 codegen 强类型 | ✅ 提前 1-2 周享受强类型保护 |
| 推迟影响 | 🔴 推迟其他 Story · Sprint 11 5 项 backlog 中至少 1 项需延后 |
| 紧急度评估 | 🟡 中 · InspectionCreate.vue 当前可用，无紧急业务需求 |

### 选项 C · 永久保留 Option A（不推荐）

| 维度 | 评估 |
|------|------|
| 决策路径 | 10.5 Option A 永久保留 · 后续 InspectionCreate.vue 变更需手动同步本地 interface |
| 阻塞 Sprint 10 FAT | ❌ 不阻塞 |
| 享受 codegen 强类型 | ❌ 永久脱节 · InspectionFormPayload 与 openapi.yaml 契约无强绑定 |
| 与 10.1 价值冲突 | 🔴 **是** · 10.1 核心价值是"openapi.yaml 契约变更前端 typecheck 立刻感知" · InspectionCreate.vue 与该价值脱节 |
| 后续清理成本 | 🔴 高 · 每次 InspectionCreate.vue 字段变更需手动同步 InspectionFormPayload |
| 不推荐理由 | 违背 10.1 codegen 集成的初衷 |

---

## 3. 决策

**选项 A · 纳入 V1.3.9 backlog**

- InspectionDTO schema 缺失是 type-level gap，不阻塞 Sprint 10 FAT 准入
- 10.5 Option A 临时方案已工作，业务层面无紧急需求
- V1.3.9 启动时一次性切换到 codegen `import type { InspectionDTO }` 享受强类型保护
- 与 PM 决策#3（7 状态机 enum drift 对齐）协同处理 openapi.yaml schema 补齐工作

---

## 4. 依据

### 4.1 V1.3.8 FAT 截止压力

- Sprint 10 集成 E 验证为 CONDITIONAL GO · 5/5 Story IMPL 完成 · 0 硬阻塞
- 3 项 PM 决策（#1 web-impl git remote / #2 InspectionDTO / #3 enum drift）为 V1.3.8 FAT 准入前置
- InspectionDTO 不属于"必须 Sprint 10 FAT 解决"的硬阻塞 · 纳入 V1.3.9 不影响准入

### 4.2 10.5 Option A 临时方案可工作

- 5 文件 6 处 any → 0 处 · 优于 Story AC-10.5.1"≤ 1"基线
- InspectionCreate.vue 复用 `MaterialBarcodeParseResponse.qualityStatus` 枚举 · 业务字段约束类型层有保障
- 本地 `InspectionFormPayload` 与 backend `POST /api/v1/quality/inspections` 实际入参一致 · 6.4 dev log 已验证

### 4.3 V1.3.9 享受 codegen 强类型

- 一次性切换 `import type { InspectionDTO }` 后：
  - backend 调整 inspection 入参字段 → frontend typecheck 立刻感知
  - 与 10.1 codegen 集成核心价值对齐
  - 7 状态机 enum drift（PM 决策#3）协同处理 openapi.yaml schema 补齐
- 切换成本 🟢 低 · Option A 临时方案已声明 6 字段 · codegen schema 落地后一行 import type 替换即可

### 4.4 风险评估

| 风险 | 等级 | 缓解 |
|------|------|------|
| V1.3.9 Sprint 1 启动时 InspectionDTO 落地延误 | 🟢 低 | 10.5 Option A 临时方案可工作 · 不影响 V1.3.8 FAT |
| backend dev 与 10.5 dev 同步切换脱节 | 🟢 低 | 决策书 §5 明确委派清单 · V1.3.9 Sprint 1 启动会同步 |
| InspectionDTO schema 设计变更影响现有 InspectionFormPayload | 🟢 低 | 10.5 dev log §6.3 已标注 InspectionFormPayload 与 backend 实际入参一致 · 字段集已稳定 |

---

## 5. 委派

### 5.1 backend dev（V1.3.9 Sprint 1 启动时）

| # | 任务 | 路径 | 验收 |
|---|------|------|------|
| 1 | 加 InspectionDTO schema 到 openapi.yaml | `backend/spec/openapi.yaml` `components.schemas` 区块 | schema 含 6 字段（type/materialCode/batchNo/qty/inspector/aql/qualityStatus） |
| 2 | 加 `POST /api/v1/quality/inspections` 端点定义 | `backend/spec/openapi.yaml` `paths` 区块（位于 `E7QualityService` tag 下） | requestBody 引用 InspectionDTO schema + 200 响应引用 InspectionResponse schema |
| 3 | 同步 InspectionResponse schema（如尚无） | `backend/spec/openapi.yaml` `components.schemas` 区块 | 响应字段含 inspectionNo/createdAt 等业务返回字段 |
| 4 | 重新跑 codegen 验证 | `cd web-impl && npm run gen:api` | generated/models/Inspection.ts + InspectionResponse.ts 存在 |

### 5.2 10.5 dev（V1.3.9 Sprint 1 启动时同步切换）

| # | 任务 | 路径 | 验收 |
|---|------|------|------|
| 1 | 替换 Option A 本地 interface 为 generated 类型 | `web-impl/src/views/quality/InspectionCreate.vue` | `import type { InspectionDTO } from '@/api/generated/models/Inspection'` + 删除本地 `InspectionFormPayload` 接口声明 |
| 2 | 删除 `MaterialBarcodeParseResponse.qualityStatus` 复用代码 | 同上 | qualityStatus 字段类型由 `InspectionDTO['qualityStatus']` 提供 |
| 3 | typecheck 验证 | `cd web-impl && npm run typecheck` | 退出码 0 · 无新增 TS 报错 |

### 5.3 截止时间

- **V1.3.9 Sprint 1 启动时**（预计 2026-06-23 ± 1 周 · 视 V1.3.8 FAT 客户服务器就绪时间）
- backend dev + 10.5 dev 同步切换 · 1 个 Sprint 内闭环

---

## 6. core-config 标注

### 6.1 backlog 占位

在 `.orchestrix-core/core-config.yaml` 的 `assigned_stories` 区块 V1.3.9 section 标注 backlog 占位（V1.3.9 section 将在 Sprint 11 启动时由 PO 范蠡 shard · 当前仅占位）：

```yaml
# V1.3.9 Sprint 1 backlog 占位（PM 决策#2 · 2026-06-13）
- id: "11.x"
  title: "InspectionDTO schema 补齐 + InspectionCreate.vue Option A 切换"
  status: "Backlog"
  sprint: "V1.3.9-S1"
  priority: "P2"
  complexity: "S"
  pm_decision_ref: "docs/qa/evidence/sprint10-pm-decision-2-inspection-dto.md"
  repository_type: "backend+web-impl"
  test_cases: 4
  next_action: "hand_to_architect"
  source_story: "backend/docs/stories/sprint10/10.5-vue-any-replace.md#option-a-migration"
  # 任务分解：
  #   backend dev: 加 InspectionDTO schema + POST /api/v1/quality/inspections + InspectionResponse
  #   10.5 dev: InspectionCreate.vue 替换 Option A 本地 interface 为 generated 类型
```

### 6.2 状态字段说明

- `status: "Backlog"` · 当前未 shard · 等待 V1.3.9 启动时由 PO 范蠡 shard 为 "Sharded"
- `pm_decision_ref` · 反向追溯到本决策书
- `priority: "P2"` · 不阻塞 V1.3.8 FAT · V1.3.9 Sprint 1 期间完成即可

---

## 7. 签字

PO 范蠡 · 2026-06-13 · 决策选项 A · InspectionDTO schema 纳入 V1.3.9 backlog

---

## 8. 关联文档

- 10.1 dev log：`docs/dev/logs/10.1-dev-log.md` §3.1 约束 #4 + §5 协同指引 + §6 PM 决策 #4
- 10.5 dev log：`docs/dev/logs/10.5-dev-log.md` §2.5 Option A IMPL + §6 决策项 #2 + §6.3 IMPL 关键点
- 集成 E 验证报告：`docs/qa/evidence/sprint10-integration-test-report.md` §4.3 PM 决策 #2 + §6.4 Sprint 11 衔接 #1
- Sprint 11 PRD 对齐检查：`docs/qa/evidence/sprint11-prd-alignment-check.md` §PM 决策项
- core-config 标注：`.orchestrix-core/core-config.yaml` `assigned_stories` 区块（V1.3.9 section 占位）