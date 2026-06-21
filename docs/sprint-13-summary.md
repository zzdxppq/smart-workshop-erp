# Sprint 13 立项 · V1.3.9 Sprint 13 · 优化阶段 5 · 收口 + 强化

> **周期**：2026-06-23+ 启动 · 预计 3-5 天
> **前置**：V1.3.8 FAT 准入（2830 测例 + Sprint 12 86 测例 = 2916 测例）+ 客户服务器就位
> **PO 范蠡** · 2026-06-14 SHARD · Sprint 13 = 5 Story（3 P1 必做 + 2 P1/P2 协同）

---

## 1. Sprint 13 定位

V1.3.9 优化阶段 5 = Sprint 12 集成 E 验证收口后的**收口 + 强化**窗口：
- **收口**：把 Sprint 12 集成 E 验证识别出的 3 项 P1 必做项（占位/临时方案/未落地）替换为终态方案
- **强化**：选 2 项 V1.3.9 客户上线前必须闭环的 P1/P2 协同项（状态机对齐 + 仪表盘接入）

**Sprint 13 = V1.3.9 客户上线（昆山佰泰胜精密机械有限公司）准入前置**：
- Sprint 12 已 CONDITIONAL GO（3 QA 委派 + 1 DevOps 委派 + 2 PM 决策通过后转 GO）
- Sprint 13 5 Story 落地后 → V1.3.9 客户灰度 / 验收上线

---

## 2. 选定 Story（5 项 · 3 P1 必做 + 2 P1/P2 协同）

| Story | Title | 优先级 | 复杂度 | 端点 | 测例 | 协同 |
|-------|-------|--------|--------|------|------|------|
| 13.1 | InspectionDTO schema 补齐 + InspectionCreate.vue Option A 切换 | 🔴 P1 | S | 1 | 0（typecheck 验证）| parallel_group A（与 13.5 协同）|
| 13.2 | 思源黑体嵌入 jar 资源（服务端单一权威源）| 🟡 P1 | S | 0 | 0（视觉验证 + 4 项回归）| parallel_group B（与 13.3 协同）|
| 13.3 | 12.1 crm_drawing_link JOIN 真实查询对接 + OPERATOR 工序对接 | 🔴 P1 | M | 3 | 24 | parallel_group B（与 13.2 关联）|
| 13.4 | sys_workflow_event 接入 GmSummary 仪表盘（10.3 数据消费）| 🟡 P1 | S | 1 | 8 | parallel_group C（独立）|
| 13.5 | 7 状态机 enum drift 对齐（openapi.yaml 改 V1.3.7 定义）| 🟡 P1 | S | 0 | 0（regen + typecheck 验证）| parallel_group A（与 13.1 关联）|
| **Sprint 13 累计** | **5 Story** | — | — | **5 端点** | **~32 测例** | — |

> **P2 协同项**（V1.3.9 客户反馈后评估）：
> - 13.6 7 角色 connectedAndroidTest E2E 补齐（Story 1.4 pending_deploy）— Sprint 13.5 之后视 V1.3.9 客户上线进展决定是否纳入

---

## 3. Sprint 13 Story 立项详情

### 3.1 Story 13.1 · InspectionDTO schema 补齐 + Option A 切换

- **来源**：Sprint 10 PM 决策 #2（采纳方案 A · 已在 `core-config.yaml` 占位 V1.3.9-S1 backlog · `assigned_stories` 第 447-461 行 `id: "11.x"` 占位）
- **范围**：
  - backend：`backend/spec/openapi.yaml` 新增 `InspectionDTO` / `InspectionCreateRequest` / `InspectionResponse` schema + `POST /api/v1/quality/inspections` 端点契约
  - web-impl：`InspectionCreate.vue` 替换 Option A 本地 interface 为 generated 类型（享受 codegen 强类型）
- **依赖**：Sprint 10.1 ✅（codegen 基础）+ Sprint 10.5 ✅（InspectionCreate.vue 待替换占位）
- **优先级**：🔴 **P1**（消除 10.5 临时方案 · 享受 codegen 强类型 · 提升客户端类型安全）
- **复杂度**：**S**（schema 加 1 + 端点契约 + web-impl 1 文件替换）
- **端点数**：1（新增 `POST /api/v1/quality/inspections`）
- **测例**：0（typecheck:ci + grep 验证 · 与 10.5 一致）

### 3.2 Story 13.2 · 思源黑体嵌入 jar 资源

- **来源**：Sprint 12 集成 E 验证 `12.3 dev log §7 R3` · `12.3 dev log §10 D2` · 当前用 JDK 默认 SansSerif（服务端单一权威源）
- **范围**：
  - 资源文件：引入 `NotoSansCJK-Regular.otf`（思源黑体 · Apache 2.0 · 约 16MB）→ `backend/src/main/resources/fonts/`
  - 服务端：`LabelPngRenderer` / `PdfA4Generator` 共用 `Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/fonts/NotoSansCJK-Regular.otf"))`
  - 渲染保真度：跨 OS（Windows / Linux / macOS / docker alpine）输出一致
- **依赖**：Sprint 12.3 ✅（LabelPngRenderer 已 ship）+ Sprint 12.4 ✅（PdfA4Generator 已 ship）
- **优先级**：🟡 **P1**（消除客户 OS 字体差异 · 中文厂名保真度）
- **复杂度**：**S**（资源嵌入 + 渲染服务改造 + 4 项视觉回归）
- **端点数**：0（不动契约）
- **测例**：0（视觉验证 · 包含在 12.3/12.4 既有 38 测例回归范围内）

### 3.3 Story 13.3 · 12.1 crm_drawing_link JOIN 真实查询对接

- **来源**：Sprint 12 集成 E 验证 委派项 · `12.1 dev log §8.2 #2/#3` · Sprint 12 灰度阶段已开启 feature flag 但占位逻辑未替换
- **范围**：
  - 5 业务单据（订单/PO/入库单/质检单/工单工序）→ `crm_drawing_link` JOIN 真实查询：
    - `findSalesOrderIds(userId)` → 替换 `username.hashCode() % N` 占位 → `crm_order.owner_user_id = userId`
    - `findPurchaserPoIds(userId)` → `purchase_order.created_by = userId`
    - `findWarehouseInboundIds(userId)` → `wms_inbound.operator_user_id = userId`
    - `findQcInspectionIds(userId)` → `qc_inspection.inspector_user_id = userId`
    - `findOperatorProcessIds(userId)` → `crm_workorder_process.operator_user_id = userId AND status='IN_PROGRESS'`（Redis 7 缓存 5min TTL）
  - 性能：`@Cacheable` Redis 7 缓存 + `@CacheEvict` on link write · 索引对齐 `idx_draw_link_lookup(link_type, link_id)`
  - 灰度：`draw.acl.gray.{ROLE}` feature flag 4 阶段开启（与 Sprint 12 集成 E 一致）
- **依赖**：Sprint 12.1 ✅（V54 crm_drawing_link 已 ship + 24 测例全 PASS）+ Redis 7 客户机房就位
- **优先级**：🔴 **P1**（占位逻辑生产可见偏差 · 客户上线前置）
- **复杂度**：**M**（5 类真实查询 + 缓存 + 性能优化 + 灰度）
- **端点数**：3（2 个内部查询方法 + 1 个管理员对账端点 `GET /api/v1/drawings/admin/link-stats`）
- **测例**：24（5 类 link_type × 4 测例正反 + 4 性能 + 5 缓存 + 5 灰度 + 3 E2E）

### 3.4 Story 13.4 · sys_workflow_event 接入 GmSummary 仪表盘

- **来源**：Sprint 10 PM backlog #6（已在 Sprint 10.3 完成端点）+ Sprint 11 backlog #6（仪表盘消费待补）+ Sprint 10.4 仪表盘组件占位
- **范围**：
  - web-impl `GmSummary.vue`（4.3 PM 立项 dashboard · 8 Tab 之一）：
    - 新增 `<WorkflowEventPanel>` 子组件 · 消费 `GET /api/v1/workflow/events/stats`（10.3 端点）
    - 渲染：审批总数 + 按事件类型分布（ECharts 饼图）+ 按角色工作量（ECharts 柱状图）+ 时间序列（ECharts 折线）
  - 权限：仅 GM + ADMIN（与 10.3 端点一致）
- **依赖**：Sprint 10.3 ✅（端点已 ship · 8/8 PASS）+ Sprint 4.3 ✅（GmSummary 仪表盘 8 Tab 占位）
- **优先级**：🟡 **P1**（Sprint 10 backlog #6 收口 · 客户上线后立即可看审批数据）
- **复杂度**：**S**（1 子组件 + ECharts 4 图 + 路由 + 权限）
- **端点数**：1（10.3 既有 `GET /api/v1/workflow/events/stats` · Sprint 13 仅消费）
- **测例**：8（4 图表渲染 + 2 权限 + 2 边界）

### 3.5 Story 13.5 · 7 状态机 enum drift 对齐

- **来源**：Sprint 10.1 dev log §6.1 风险 #2 · Sprint 10 PM 决策 #3（采纳方案 A：改 `openapi.yaml` 而非改 composable）
- **范围**：
  - backend `spec/openapi.yaml` L473-475 / L476-478 / L494-495 三处 enum → `[DRAFT, SUBMITTED, ACCEPTED, IN_PROGRESS, COMPLETED, REWORK, CLOSED]`（V1.3.7 1.22 7 状态机）
  - web-impl 二次 regen（`rm -rf src/api/generated && npm run gen:api`）+ `useOutsourceStateMachine` composable 用 `as const` 字面量（与 10.5 风格一致）
  - typecheck:ci 退出 0 + `git diff --exit-code src/api/generated` 退出 0
- **依赖**：Sprint 10.1 ✅ + Sprint 10.5 ✅
- **优先级**：🟡 **P1**（消除 enum drift · 10.5 typecheck 错误根因 · 与 13.1 协同）
- **复杂度**：**S**（openapi.yaml 3 处 + regen + 1 文件改字面量）
- **端点数**：0（不动契约 · 改 enum 定义）
- **测例**：0（regen + typecheck:ci 验证 · 与 10.5 一致）

---

## 4. 与 V1.3.8 FAT 准入关系

### 4.1 Sprint 12 FAT 准入路径（已 CONDITIONAL GO）

| 阶段 | 验收项 | 责任 | 截止 | 状态 |
|------|--------|------|------|------|
| Sprint 12 IMPL 阶段 | 4 Story 自验证 | dev agent Opus 4.8 | 2026-06-14 | ✅ 完成 |
| Sprint 12 集成 E 验证 | 5 集成点 + 0 阻塞 + 2 PM 决策需求 | SM 萧何 | 2026-06-14 | 🟡 CONDITIONAL GO |
| Sprint 12 QA 委托执行 | 86 测例 test-execute + typecheck:ci + 灰度 4 阶段 | QA 商鞅 | 2026-06-16（+2 day）| 🟡 待启动 |
| PM 决策回复 | 2 项决策（ESC/POS 票据打印机 / 模式一失败降级）| PM 范蠡 | 2026-06-16 | 🟡 待回复 |
| DevOps 接入 | 12.2 心跳 60s 调度客户机房环境就位 + 9100 端口白名单 + Redis 7 | DevOps 张良 | 2026-06-23（客户服务器就位）| 🟡 待执行 |
| Sprint 12 集成 E 验证收口 | 3 QA 委托 + 1 DevOps 委托 + 2 PM 决策通过 → **GO** | SM 萧何 | 2026-06-23 | 🟡 待收口 |
| **V1.3.8 FAT 验收最终关** | 全量 2830 + 86 = **2916 测例** 准入 | PO 范蠡 + 客户 | 2026-06-23 | 🟡 待客户服务器就位 |

### 4.2 Sprint 13 启动时序（不阻塞 V1.3.8 FAT）

- **Sprint 13 启动时间**：2026-06-23+ · V1.3.8 FAT 准入后启动 · 客户服务器就位 + Sprint 12 集成 E 收口
- **Sprint 13 工期**：3-5 天 · 5 Story 并行 + 灰度
- **Sprint 13 完成后**：V1.3.9 客户灰度 → 验收 → 上线（昆山佰泰胜精密机械有限公司）
- **Sprint 13 ≠ Sprint 12 灰度阶段**：Sprint 13 5 Story 与 12.1 灰度 4 阶段并行启动 · 互不阻塞

### 4.3 Sprint 13 与 12.1 灰度 4 阶段协同

| Sprint 12 灰度阶段 | 角色 | 观察时间 | Sprint 13 关联 |
|-------------------|------|---------|---------------|
| 阶段 1 | admin + ENGINEER | 1 天 | 13.3 real-query 仅影响 SALES/PURCHASER/WAREHOUSE/QC/OPERATOR · admin + ENGINEER 不受影响 |
| 阶段 2 | SALES | 1 天 | 13.3 `findSalesOrderIds` 上线后立即生效 |
| 阶段 3 | PURCHASER + WAREHOUSE + QC | 1 天 | 13.3 3 类真实查询上线 |
| 阶段 4 | OPERATOR | 2 天 | 13.3 `findOperatorProcessIds` 上线 + Redis 7 缓存验证 |

**协同结论**：13.3 必须**早于或同步于** 12.1 阶段 2-4 灰度开启（占位逻辑未替换会导致灰度阶段业务冲击）。

---

## 5. 5 Story 协同关系（3 parallel_group）

### 5.1 parallel_group A · 13.1 + 13.5（关联 · InspectionDTO + 状态机对齐）

- **13.1** 替换 `InspectionCreate.vue` Option A 本地 interface 为 generated `InspectionDTO` → 享受 codegen 强类型
- **13.5** 改 `openapi.yaml` enum 定义 + regen → 让所有 codegen 联合类型稳定
- **协同点**：两者都依赖 codegen regen · 必须串行：先 13.5 改 enum（regen baseline）→ 再 13.1 InspectionDTO schema 加进 yaml（再 regen）→ 最后 13.1 web-impl 替换
- **顺序约束**：13.5 → 13.1（不可逆）

### 5.2 parallel_group B · 13.2 + 13.3（关联 · 字体 + 查询）

- **13.2** 思源黑体嵌入 jar 资源 → 服务端单一权威源
- **13.3** 真实查询对接 → 5 类 link JOIN + OPERATOR 工序
- **协同点**：两者均依赖 Redis 7 客户机房就位 + Sprint 12.1 灰度阶段开启 · 可并行 IMPL
- **顺序约束**：13.2 与 13.3 完全并行 · 无依赖

### 5.3 parallel_group C · 13.4（独立）

- **13.4** GmSummary 仪表盘接入 sys_workflow_event → 仅消费 Sprint 10.3 既有端点
- **协同点**：与 13.1/13.2/13.3/13.5 完全独立 · 仅依赖 Sprint 10.3 端点已 ship
- **顺序约束**：可最早启动

---

## 6. Sprint 13 端点 / Flyway / 工时汇总

| 维度 | Sprint 13 合计 |
|------|---------------|
| Story 数 | 5 |
| 端点数 | 5（13.1 新增 1 + 13.3 新增 3 + 13.4 消费 10.3 既有 1）|
| Flyway 迁移 | 1-2（13.3 可能 V58__drawing_link_real_query.sql + 13.1 不需要迁移）|
| 测例 | ~32（13.1 0 + 13.2 0 + 13.3 24 + 13.4 8 + 13.5 0）|
| 工时 | 3-5 天 · 13.3 1.5-2 天 + 13.1/13.2/13.4/13.5 各 0.5 天 |
| 复杂度 | M/S/S/S/S（13.3 M · 其余 S）|
| 优先级 | 🔴 P1 × 2 + 🟡 P1 × 3 |

---

## 7. orchestrator 后续行动建议（arch REVIEW → QA test-design → dev IMPL 顺序约束）

### 7.1 顺序约束（PO 范蠡指令）

```
Sprint 13 立项（2026-06-14 已完成）
  ↓
Step 1: SM 萧何委托 architect 鲁班 *review（5 个 Story 并行）
  ↓
Step 2: SM 萧何委托 QA 商鞅 *test-design（5 个 Story 并行）
  ↓
Step 3: SM 萧何委托 dev agent Opus 4.8 *develop-story（3 parallel_group 并行）
  ↓
Step 4: Sprint 13 集成 E 验证（SM 萧何协调 · 预计 2026-06-26）
  ↓
Step 5: Sprint 13 委派（QA 测例 test-execute + DevOps 灰度 4 阶段）
  ↓
Step 6: V1.3.9 客户灰度上线（昆山佰泰胜 · 2026-06-30+）
```

### 7.2 关键顺序约束（不可逆）

- **13.5 → 13.1**：先改 enum → 再 InspectionDTO schema（顺序倒置会导致 codegen 漂移）
- **13.3 与 12.1 灰度阶段同步**：13.3 real-query 上线必须**早于或同步于** 12.1 阶段 2-4 灰度开启
- **13.4 可最早启动**：仅依赖 Sprint 10.3 端点 · 无 Sprint 12 协同约束
- **13.2 与 13.3 完全并行**：两者均依赖 Redis 7 客户机房就位 · 可同仓并行 IMPL

### 7.3 arch REVIEW 委派清单（给 SM 萧何）

| 优先级 | Story | arch 委派项 | 截止 |
|--------|-------|------------|------|
| 1 | 13.5 | openapi.yaml 3 处 enum 修改影响面分析 + codegen regen 链路验证 | 2026-06-23 |
| 2 | 13.3 | 5 类 link JOIN 真实查询 SQL 审查 + Redis 缓存一致性 + 灰度 feature flag 集成 | 2026-06-23 |
| 3 | 13.1 | InspectionDTO schema 字段对齐 V1.3.7 1.28/1.29/1.30 检验表 + Option A 切换 typecheck 验证 | 2026-06-23 |
| 4 | 13.4 | GmSummary 仪表盘子组件架构 + ECharts 4 图数据流 + 权限校验 | 2026-06-23 |
| 5 | 13.2 | 思源黑体嵌入 jar 资源方式（classpath / 外部目录）+ 服务端渲染统一入口 | 2026-06-23 |

### 7.4 QA test-design 委派清单（给 SM 萧何）

| 优先级 | Story | QA 委派项 | 截止 |
|--------|-------|----------|------|
| 1 | 13.3 | 24 测例设计（5 类 link × 4 + 4 性能 + 5 缓存 + 5 灰度 + 3 E2E）| 2026-06-24 |
| 2 | 13.4 | 8 测例设计（4 图表 + 2 权限 + 2 边界）| 2026-06-24 |
| 3 | 13.1 | typecheck:ci 退出 0 验证 + grep 验证 0 命中 + regen diff review | 2026-06-24 |
| 4 | 13.5 | regen 后 typecheck:ci 退出 0 + `git diff --exit-code src/api/generated` 退出 0 验证 | 2026-06-24 |
| 5 | 13.2 | 视觉回归验证脚本（4 模板 × 2 OS = 8 张图对比）+ 字体文件大小断言 | 2026-06-24 |

---

## 8. 风险与 PM 决策需求

### 8.1 风险（合并去重 · 8 项）

| # | 风险 | 来源 Story | 等级 | 缓解 |
|---|------|-----------|------|------|
| 1 | 13.3 真实查询性能（5 类 link JOIN 大表）| 13.3 | 🟡 中 | `@Cacheable` Redis 7 5min TTL + 4 索引对齐 `idx_draw_link_*` + EXPLAIN 验证 |
| 2 | 13.3 OPERATOR 当前工序 userId → processId 占位（Redis 缓存不一致）| 13.3 | 🟡 中 | `@CacheEvict on link write` + Redis 7 双写一致性 + `@Transactional` 内校验 |
| 3 | 13.2 思源黑体文件 16MB 增 jar 包大小 | 13.2 | 🟢 低 | 接受 · 单一权威源 · 避免 OS 字体差异 · 资源文件不进 git LFS（jar 内） |
| 4 | 13.2 docker alpine 字体兼容性 | 13.2 | 🟢 低 | 显式注册 Font.createFont · 不依赖系统字体路径 |
| 5 | 13.1 InspectionDTO 字段与 V1.3.7 1.28-1.30 既有检验单端点不一致 | 13.1 | 🟡 中 | dev 启动前先 `git log 1.28/1.29/1.30` 同步字段 + 与 architect 评审 |
| 6 | 13.5 openapi.yaml enum 改动触发 10.1 codegen 既有类型漂移 | 13.5 | 🟢 低 | dev 二次 regen + `git diff --exit-code` 拦截 + 0.5h 增量 |
| 7 | 13.4 仪表盘加载 4 图性能 | 13.4 | 🟢 低 | 4 图统一 Promise.all 并行 + ECharts 按需懒加载 + skeleton 占位 |
| 8 | Sprint 12 集成 E CONDITIONAL GO 仍未转 GO（3 QA + 1 DevOps）| 跨 Sprint | 🟡 中 | Sprint 13 启动时（2026-06-23）Sprint 12 必须先 GO · 阻塞 Sprint 13 |

### 8.2 PM 决策需求（0 项硬阻塞）

✅ **Sprint 13 无硬阻塞**：
- 所有 5 Story IMPL 范围均已明确 · 无 architect 决策待回复 · 无 DevOps 委派待回复
- 仅有 1 项 PM 决策（V1.3.10 ESC/POS 票据打印机）已在 Sprint 12 集成 E 标注为 V1.3.10 backlog · 不阻塞 Sprint 13

---

## 9. 与 V1.3.9 客户上线的关系

### 9.1 客户（昆山佰泰胜精密机械有限公司）上线路径

| 阶段 | 时间 | 范围 | 责任 |
|------|------|------|------|
| V1.3.8 FAT 准入 | 2026-06-23 | 2916 测例全 PASS | PO 范蠡 + 客户 |
| Sprint 13 IMPL | 2026-06-23+（3-5 天）| 5 Story · 32 测例 | dev agent Opus 4.8 |
| Sprint 13 集成 E 验证 | 2026-06-26+ | 5 Story 跨 Story 集成点 | SM 萧何 |
| Sprint 13 QA test-execute | 2026-06-26+（+2 天）| 32 测例 PASS | QA 商鞅 |
| V1.3.9 客户灰度 | 2026-06-30+ | 12.1 灰度 4 阶段 + 13.x 全量 | PO 范蠡 + 客户 |
| V1.3.9 客户验收 | 2026-07-07+ | 全量功能验收 + 签字 | 客户（黄梓昀 151-0595-0281）|
| V1.3.9 正式上线 | 2026-07-14+ | 生产环境切换 | PO 范蠡 + 客户 |

### 9.2 Sprint 13 风险预警

- **客户服务器就位延期（DHCP 重启 / 端口白名单）**：DevOps 张良需在 2026-06-23 前完成 · 不延期则 Sprint 13 与 V1.3.8 FAT 同步启动
- **Sprint 12 集成 E 仍未 GO**：3 QA + 1 DevOps 委派必须在 2026-06-23 前完成 · 否则 Sprint 13 顺延
- **PM 决策 #1（ESC/POS 票据打印机）**：客户可能在 V1.3.9 验收阶段提出 · V1.3.10 backlog 准备

---

## 10. 签字

- **PO 范蠡** · 2026-06-14 · Sprint 13 SHARDED · 5 Story 立项（3 P1 必做 + 2 P1/P2 协同）
- **SM 萧何** · 2026-06-14 · 待启动 Sprint 13 · 待委托 architect 鲁班 *review（5 Story 并行）
- **architect 鲁班** · 待 5 Story *review（优先级 13.5 > 13.3 > 13.1 > 13.4 > 13.2）
- **QA 商鞅** · 待 5 Story *test-design（重点 13.3 24 测例 + 13.4 8 测例）
- **dev agent Opus 4.8** · 待启动 3 parallel_group IMPL（A 13.5→13.1 串行 · B 13.2+13.3 并行 · C 13.4）
- **DevOps 张良** · 待客户机房 Redis 7 + 9100 端口就位（2026-06-23 前）
- **客户（黄梓昀）** · 待 V1.3.9 上线验收（2026-07-07+）

**Sprint 13 SHARDED · 5 Story · ~5 端点 · ~32 测例 · 1-2 个 Flyway 迁移 · V1.3.9 客户上线前置 · 2026-06-23 启动**