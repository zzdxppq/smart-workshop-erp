# Sprint 14 立项 · V1.3.9 Sprint 14 · 收口阶段

> **周期**：2026-06-25 ~ 2026-07-05（10 工作日）
> **前置**：V1.3.8 FAT 准入（2916 测例 PASS）+ 客户服务器就位 2026-06-23 + Sprint 13 集成 E 收口 + V1.3.9 客户上线 2026-07-14
> **PO 范蠡** · 2026-06-14 SHARD · Sprint 14 = 4 Story（2 P0 必做 + 2 P1 协同）

---

## 1. Sprint 14 定位

V1.3.9 收口阶段 = Sprint 13 IMPL + 集成 E + 灰度启动后到 V1.3.9 客户上线前的**最后收口窗口**：

- **P0 必做**：1 项 deferred E2E（13.6 · Story 1.4 pending_deploy 第 1/2 项收口）+ 1 项 baseline 错误收口（13.7 · 5 个 pre-existing typecheck 错误 · 阻塞 typecheck:ci gate）
- **P1 协同**：1 项 V1.3.10 backlog 评估启动（13.8 · ESC/POS 票据打印机）+ 1 项 PRD 文档补齐（13.9 · §5 13 Epic table 化）

**Sprint 14 = V1.3.9 客户上线（昆山佰泰胜精密机械有限公司）准入前置**：
- V1.3.9 灰度阶段 1（admin + ENGINEER）启动前 · baseline typecheck 必须 clean
- V1.3.9 灰度阶段 4（OPERATOR）后 · 7 角色 E2E 必须 ship（13.6）
- V1.3.10 backlog（客户验收阶段可能提出）· ESC/POS 评估必须就位（13.8）

---

## 2. 选定 Story（4 项 · 2 P0 必做 + 2 P1 协同）

| Story | Title | 优先级 | 复杂度 | 端点 | 测例 | 工时 | 协同 |
|-------|-------|--------|--------|------|------|------|------|
| 13.6 | 7 角色 connectedAndroidTest E2E 补齐（Story 1.4 pending_deploy 收口）| 🔴 **P0** | **M** | 0 | 22（E2E · Android Studio + 真机/模拟器）| 2-3 天 | parallel_group C（独立）|
| 13.7 | baseline-typecheck-fix 5 错误收口（vite-env.d.ts + Printers.vue + GmSummary.vue）| 🔴 **P0** | **S** | 0（补 openapi.yaml Printers 端点为 codegen 触发）| 0（typecheck:ci 验证）| 0.5-1 天 | parallel_group A（typecheck:ci gate 前置）|
| 13.8 | ESC/POS 票据打印机 V1.3.10 评估启动（12.2 + 12.4 + 12.2 扩协议）| 🟡 P1 | **S** | 0 | 0（评估输出 + 决策文档）| 1-2 天 | parallel_group B（独立 · V1.3.10 backlog）|
| 13.9 | PRD §5 13 Epic table 补齐（PM 巡检建议 #8）| 🟡 P1 | **S** | 0 | 0 | 0.5 天 | parallel_group D（独立 · 文档项）|
| **Sprint 14 累计** | **4 Story** | — | — | **0 端点** | **22 测例** | **4.5-7 天** | — |

---

## 3. Sprint 14 Story 立项详情

### 3.1 Story 13.6 · 7 角色 connectedAndroidTest E2E 补齐

- **来源**：Story 1.4 pending_deploy 第 1/2 项（`core-config.yaml` `assigned_stories` 第 44-47 行）
- **范围**：
  - android-impl `src/androidTest/kotlin/com/btsheng/erp/e2e/RoleBasedE2ETest.kt`（新增）
  - 7 角色（ENGINEER / PROD_PLANNER / SALES / PURCHASER / WAREHOUSE / QC / OPERATOR）+ FINANCE 验证 = 8 测试账号
  - DrawPermissionInterceptor 集成 E2E（Sprint 12.1 ship 拦截器）
  - `gradle connectedAndroidTest` exit 0
- **依赖**：Story 1.4 ✅（pending_deploy）+ Story 8.5 ✅（android-impl 完整实装）+ Story 12.1 ✅（DrawPermissionInterceptor 已 ship）
- **优先级**：🔴 **P0**（Story 1.4 pending_deploy 收口 · V1.3.9 灰度阶段 4 OPERATOR 启动前置）
- **复杂度**：**M**（7 角色 E2E + connectedAndroidTest 配置 + Android Studio + 真机/模拟器）
- **端点数**：0（不动契约）
- **测例**：22（E2E · 7 角色 × 2 + connectedAndroidTest 配置 2 + DrawPermissionInterceptor 集成 6 = 14+2+6）
- **执行约束**：需 Android Studio + 真机/模拟器 · 沙箱受限委托 DevOps 张良执行

### 3.2 Story 13.7 · baseline-typecheck-fix 5 错误收口

- **来源**：PM 巡检 `docs/orchestrix-pm-audit-2026-06-14.md` §7.1 + pre-existing baseline 5 错误
- **5 个错误明细**：

| # | 文件 | 行号 | 错误 | 修复 |
|---|------|------|------|------|
| 1 | `web-impl/src/api/http.ts` | L13 | 缺 `vite-env.d.ts`（`import.meta.env` 类型未声明）| 新增 `web-impl/src/vite-env.d.ts` 声明 `VITE_API_BASE_URL` + `VITE_USE_MOCK` 等 |
| 2 | `web-impl/src/views/admin/Printers.vue` | L163-164 | codegen 失败（openapi.yaml 缺 Printer 端点）| `backend/spec/openapi.yaml` 补 `POST/GET/PUT/DELETE /api/v1/admin/printers`（Sprint 12.2 已 ship · 仅 codegen 链路补齐）|
| 3 | `web-impl/src/views/admin/Printers.vue` | L163-164 | 缺 Printer 类型导入 | codegen 后 `import type { Printer } from '@/api/generated/models/Printer'` |
| 4 | `web-impl/src/views/v138/GmSummary.vue` | L33 | `data.amountThresholdPassedRate * 100` · 数字/字符串类型 | `as Dayjs` 类型断言或 generated DTO 字段类型修正 |
| 5 | `web-impl/src/views/v138/GmSummary.vue` | L47 | `data.outsourceCostRatio * 100` · 同 #4 | 同 #4 |

- **依赖**：Sprint 10.1 ✅（OpenAPI TypeScript codegen 已 ship）+ Sprint 12.2 ✅（Printer 端点已 ship）
- **优先级**：🔴 **P0**（阻塞 typecheck:ci gate · 所有 V1.3.9 后端/web 集成 E 验证前置）
- **复杂度**：**S**（1 文件新增 vite-env.d.ts + 1 yaml 文件补端点 + regen + 1 文件 2 处类型断言）
- **端点数**：0（补 Sprint 12.2 已 ship 端点的 codegen 链路 · 不新增）
- **测例**：0（typecheck:ci + `npm run gen:api` + `git diff --exit-code src/api/generated` 验证）

### 3.3 Story 13.8 · ESC/POS 票据打印机 V1.3.10 评估启动

- **来源**：PM 巡检 `docs/orchestrix-pm-audit-2026-06-14.md` §9 PM 决策 1 + `docs/prd-feedback-v1.3.9.md`
- **范围**：
  - 客户已购 ESC/POS 票据打印机（销售小票 / 发货单）
  - Sprint 12.2 + 12.4 PM 决策 A（当前 Sprint 12 不支持 ESC/POS）
  - V1.3.10 backlog 评估启动 · 评估输出含：
    - 协议扩展（V55 `sys_printer.protocol` 枚举扩 `ESC/POS`）
    - code_type 扩展（V57 `sys_print_log.code_type` 枚举扩 `RECEIPT`/`DELIVERY_NOTE`）
    - 协议 adapter 实现（`EscPosAdapter` 与 `ZplAdapter`/`TsplAdapter` 平级）
    - 工时估算 + 风险评估 + 客户验收阶段提出的预案
- **依赖**：Sprint 12.2 ✅（V55 sys_printer 表 + ProtocolAdapter 抽象）+ Sprint 12.4 ✅（V57 sys_print_log 表 + Socket 抽象）
- **优先级**：🟡 **P1**（V1.3.10 backlog · 客户可能在 V1.3.9 验收阶段提出 · 评估启动准备）
- **复杂度**：**S**（评估输出 · 0 端点 · 0 测例 · 仅决策文档 + backlog 准备）
- **端点数**：0（评估阶段不动契约）
- **测例**：0（评估文档 + 决策签字）

### 3.4 Story 13.9 · PRD §5 13 Epic table 补齐

- **来源**：PM 巡检建议 #8 + `docs/orchestrix-pm-audit-2026-06-14.md` §7.1
- **范围**：
  - `docs/prd.md` §5 当前 12 Epic 表格（V1.0 = Epic 1-5 · V1.1 = Epic 6-11 · Epic 12 V1.3.5/3.6 改版）
  - 13 Epic table 补齐（V1.3.7 收口时新增 Epic 13 = V1.3.7 委外深化 + 料号成本面板 · 当前 PRD 缺）
  - 表格列：编号 + 标题 + 版本 + Story 数 + 端点数 + 状态（13 Epic 累计状态）
- **依赖**：无（文档项 · 独立）
- **优先级**：🟡 **P1**（PM 巡检修补建议 #8 · 文档完整性 · 不阻塞 IMPL）
- **复杂度**：**S**（1 章节修订 · 1 新增表格 · 13 Epic 行数据整理）
- **端点数**：0（文档项）
- **测例**：0（文档 review）

---

## 4. 与 V1.3.8 FAT 准入 / V1.3.9 客户上线关系

### 4.1 V1.3.8 FAT 准入路径（2026-06-23 已就位）

| 阶段 | 验收项 | 责任 | 截止 | 状态 |
|------|--------|------|------|------|
| Sprint 12 IMPL 阶段 | 4 Story 自验证 | dev agent Opus 4.8 | 2026-06-14 | ✅ 完成 |
| Sprint 12 集成 E 验证 | 5 集成点 + 0 阻塞 + 2 PM 决策通过 | SM 萧何 | 2026-06-23 | 🟡 待收口 |
| Sprint 12 QA 委托执行 | 86 测例 test-execute + typecheck:ci + 灰度 4 阶段 | QA 商鞅 | 2026-06-16（+2 day）| 🟡 待启动 |
| 客户服务器就位 | Redis 7 + 9100 端口白名单 + DHCP 预案 | DevOps 张良 | 2026-06-23 | 🟡 待执行 |
| **V1.3.8 FAT 验收最终关** | 全量 2830 + 86 = **2916 测例** 准入 | PO 范蠡 + 客户 | 2026-06-23 | 🟡 待客户服务器就位 |

### 4.2 Sprint 14 启动时序（不阻塞 V1.3.8 FAT）

- **Sprint 14 启动时间**：2026-06-25（V1.3.8 FAT 准入后第 2 个工作日）
- **Sprint 14 工期**：10 工作日（2026-06-25 ~ 2026-07-05）· 与 V1.3.9 灰度阶段 1-2 重叠
- **Sprint 14 完成后**：V1.3.9 灰度阶段 3-4 + 客户验收 + 正式上线（2026-07-14+）

### 4.3 Sprint 14 与 V1.3.9 灰度 4 阶段协同

| V1.3.9 灰度阶段 | 角色 | 观察时间 | Sprint 14 关联 |
|-----------------|------|---------|---------------|
| 阶段 1 | admin + ENGINEER | 1 天（6/30）| 13.7 baseline typecheck 必须 clean（typecheck:ci gate）|
| 阶段 2 | SALES | 1 天（7/1）| 13.7 必须 ship（web-impl typecheck 通过）|
| 阶段 3 | PURCHASER + WAREHOUSE + QC | 1 天（7/2）| 13.7 已 ship |
| 阶段 4 | OPERATOR | 2 天（7/4-7/5）| **13.6 必须 ship**（7 角色 E2E · OPERATOR 工序关联验证）|

**协同结论**：
- **13.7 baseline-typecheck-fix** 必须**早于或同步于**灰度阶段 1 启动（typecheck:ci gate）
- **13.6 7 角色 E2E** 必须**早于或同步于**灰度阶段 4 启动（OPERATOR E2E 覆盖）
- **13.8 / 13.9** 与灰度完全独立 · 可任意时序启动

---

## 5. 4 Story 协同关系（4 parallel_group）

### 5.1 parallel_group A · 13.7 baseline-typecheck-fix（P0 · 前置）

- **13.7** 5 个 pre-existing typecheck 错误收口（vite-env.d.ts + Printers.vue + GmSummary.vue）
- **协同点**：typecheck:ci gate 是所有 web-impl IMPL 阶段的前置条件 · 13.7 必须最先 ship
- **顺序约束**：13.7 是 Sprint 14 IMPL 起点 · 所有其他 Story dev IMPL 必须在 13.7 ship 后启动

### 5.2 parallel_group B · 13.8 ESC/POS 评估（独立）

- **13.8** V1.3.10 backlog 评估启动 · 0 端点 · 0 测例
- **协同点**：与 Sprint 12 集成 E 完全独立 · 仅依赖 Sprint 12.2 + 12.4 已 ship
- **顺序约束**：可最早启动（评估文档 + 决策签字）· 不阻塞其他 Story

### 5.3 parallel_group C · 13.6 7 角色 E2E（独立）

- **13.6** android-impl 7 角色 E2E + FINANCE 验证（8 测试账号 · 22 测例）
- **协同点**：与 13.7/13.8/13.9 完全独立 · 仅依赖 Story 1.4/8.5/12.1
- **顺序约束**：可与 13.8 并行启动 · 但必须在 12.1 灰度阶段 4 启动前 ship

### 5.4 parallel_group D · 13.9 PRD §5 Epic table（独立）

- **13.9** PRD 文档补齐 · 0 端点 · 0 测例
- **协同点**：纯文档项 · 与其他 3 Story 完全独立
- **顺序约束**：可任意时序启动 · 0.5 天可 ship

---

## 6. Sprint 14 端点 / Flyway / 工时汇总

| 维度 | Sprint 14 合计 |
|------|---------------|
| Story 数 | 4（13.6/13.7/13.8/13.9）|
| 端点数 | 0（13.7 补 Sprint 12.2 已 ship 端点的 codegen 链路 · 不新增）|
| Flyway 迁移 | 0 |
| 测例 | 22（13.6 22 E2E + 13.7/13.8/13.9 0）|
| 工时 | 4.5-7 天 · 13.6 2-3 + 13.7 0.5-1 + 13.8 1-2 + 13.9 0.5 |
| 复杂度 | M/S/S/S |
| 优先级 | 🔴 P0 × 2 + 🟡 P1 × 2 |

---

## 7. orchestrator 后续行动建议（arch REVIEW → QA test-design → dev IMPL 顺序约束）

### 7.1 顺序约束（PO 范蠡指令）

```
Sprint 14 立项（2026-06-14 已完成）
  ↓
Step 1: SM 萧何委托 architect 鲁班 *review（4 Story 并行）
  ↓
Step 2: SM 萧何委托 QA 商鞅 *test-design（4 Story 并行）
  ↓
Step 3: SM 萧何委托 dev agent Opus 4.8 *develop-story
  顺序约束：13.7 先行（typecheck:ci gate）→ 13.6/13.8/13.9 并行
  ↓
Step 4: Sprint 14 集成 E 验证（SM 萧何协调 · 预计 2026-07-08）
  ↓
Step 5: Sprint 14 QA test-execute（22 E2E 测例 · 委托 DevOps 张良 Android Studio）
  ↓
Step 6: V1.3.9 客户验收（2026-07-07+） + V1.3.9 正式上线（2026-07-14+）
```

### 7.2 关键顺序约束（不可逆）

- **13.7 必须最先 ship**（typecheck:ci gate 是所有 web-impl dev IMPL 的前置）→ 不可逆
- **13.6 必须早于或同步于 12.1 灰度阶段 4**（OPERATOR E2E 验证）→ 不可逆
- **13.7 必须早于或同步于 12.1 灰度阶段 1**（typecheck:ci gate）→ 不可逆
- **13.8 / 13.9 完全独立** → 任意时序启动 · 可最早启动

### 7.3 arch REVIEW 委派清单（给 SM 萧何）

| 优先级 | Story | arch 委派项 | 截止 |
|--------|-------|------------|------|
| 1 | 13.7 | vite-env.d.ts 缺失影响面 + openapi.yaml 补 Printers 端点的 codegen 链路验证 + GmSummary.vue Dayjs 类型断言安全性 | 2026-06-25 |
| 2 | 13.6 | 7 角色 E2E 测试金字塔分层（10% E2E + 30% 集成 + 60% 单元）+ DrawPermissionInterceptor 集成策略 + 22 测例覆盖度 | 2026-06-26 |
| 3 | 13.8 | ESC/POS 协议扩展影响面（V55/V57 枚举扩展 + EscPosAdapter 抽象位置 + 客户端适配层）| 2026-06-27 |
| 4 | 13.9 | PRD §5 13 Epic table 修订影响面（与 V1.3.7/V1.3.8/V1.3.9 changelog 一致性）| 2026-06-27 |

### 7.4 QA test-design 委派清单（给 SM 萧何）

| 优先级 | Story | QA 委派项 | 截止 |
|--------|-------|----------|------|
| 1 | 13.7 | typecheck:ci 退出 0 + `npm run gen:api` regen + `git diff --exit-code src/api/generated` 验证 + vite-env.d.ts 字段覆盖度 | 2026-06-26 |
| 2 | 13.6 | 22 E2E 测例 design（7 角色 × 2 + connectedAndroidTest 配置 2 + DrawPermissionInterceptor 集成 6）· 沙箱受限委托 DevOps 张良 | 2026-06-27 |
| 3 | 13.8 | 评估文档 review + 协议扩展 schema 完整性 + 风险评估合理性 | 2026-06-28 |
| 4 | 13.9 | 文档 review + 13 Epic 数据准确性（与 V1.3.7/V1.3.8/V1.3.9 changelog 对账）| 2026-06-28 |

---

## 8. 风险与 PM 决策需求

### 8.1 风险（合并去重 · 5 项）

| # | 风险 | 来源 Story | 等级 | 缓解 |
|---|------|-----------|------|------|
| 1 | 13.6 E2E 需 Android Studio + 真机/模拟器（沙箱受限）| 13.6 | 🟡 中 | 委托 DevOps 张良执行 connectedAndroidTest · AVD 模拟器已就位 |
| 2 | 13.7 typecheck:ci gate 是所有 web-impl IMPL 前置 | 13.7 | 🟡 中 | 13.7 必须在 Sprint 14 IMPL 起点 ship · 不可逆 |
| 3 | 13.6 与 V1.3.7 既有 E2E 冲突（Story 8.5 仅 11 测例）| 13.6 | 🟢 低 | 13.6 新增 22 测例不与 8.5 冲突 |
| 4 | 13.8 ESC/POS 协议扩展可能影响 V1.3.9 已 ship 端点契约 | 13.8 | 🟢 低 | 仅评估阶段不动契约 · V1.3.10 backlog 评估输出 |
| 5 | Sprint 13 集成 E CONDITIONAL GO 仍未转 GO（3 QA + 1 DevOps）| 跨 Sprint | 🟡 中 | Sprint 13 集成 E 必须在 2026-06-23 前 GO · 否则 Sprint 14 顺延 |

### 8.2 PM 决策需求（0 项硬阻塞）

✅ **Sprint 14 无硬阻塞**：
- 所有 4 Story IMPL 范围均已明确 · 无 architect 决策待回复 · 无 DevOps 委派待回复
- 13.8 ESC/POS 评估启动是 PM 决策 1（采纳 A · V1.3.10 backlog）的具体落地

---

## 9. 与 V1.3.9 客户上线的关系

### 9.1 客户（昆山佰泰胜精密机械有限公司）上线路径

| 阶段 | 时间 | 范围 | 责任 |
|------|------|------|------|
| V1.3.8 FAT 准入 | 2026-06-23 | 2916 测例全 PASS | PO 范蠡 + 客户 |
| Sprint 13 IMPL | 2026-06-23+（3-5 天）| 5 Story · 32 测例 | dev agent Opus 4.8 |
| Sprint 14 IMPL | 2026-06-25 ~ 2026-07-05 | 4 Story · 22 E2E 测例 | dev agent Opus 4.8 |
| Sprint 14 集成 E 验证 | 2026-07-08+ | 4 Story 跨 Story 集成点 | SM 萧何 |
| V1.3.9 客户灰度（与 Sprint 14 重叠）| 2026-06-30+ | 12.1 灰度 4 阶段 + Sprint 13 + Sprint 14 P0 已 ship | PO 范蠡 + 客户 |
| V1.3.9 客户验收 | 2026-07-07+ | 全量功能验收 + 签字 | 客户（黄梓昀 151-0595-0281）|
| V1.3.9 正式上线 | 2026-07-14+ | 生产环境切换 | PO 范蠡 + 客户 |

### 9.2 Sprint 14 风险预警

- **Sprint 12 集成 E 仍未 GO**：3 QA + 1 DevOps 委派必须在 2026-06-23 前完成 · 否则 Sprint 13 + Sprint 14 顺延
- **客户服务器就位延期**：DevOps 张良需在 2026-06-23 前完成（Redis 7 + 9100 端口白名单 + DHCP 预案）
- **Sprint 14 与 V1.3.9 灰度重叠**：13.7 typecheck:ci gate 必须先于灰度阶段 1 启动（6/30）

---

## 10. 签字

- **PO 范蠡** · 2026-06-14 · Sprint 14 SHARDED · 4 Story 立项（2 P0 必做 + 2 P1 协同）
- **SM 萧何** · 2026-06-14 · 待启动 Sprint 14 · 待委托 architect 鲁班 *review（4 Story 并行）
- **architect 鲁班** · 待 4 Story *review（优先级 13.7 > 13.6 > 13.8 > 13.9）
- **QA 商鞅** · 待 4 Story *test-design（重点 13.6 22 E2E + 13.7 typecheck:ci gate）
- **dev agent Opus 4.8** · 待启动 4 parallel_group IMPL（A 13.7 优先 · B 13.6/13.8/13.9 并行）
- **DevOps 张良** · 待 Android Studio + 真机/模拟器就位 + connectedAndroidTest 执行 + 客户机房就位
- **客户（黄梓昀）** · 待 V1.3.9 灰度 + 验收 + 正式上线（2026-07-14+）

**Sprint 14 SHARDED · 4 Story · 0 端点 · 22 E2E 测例 · 0 Flyway · V1.3.9 客户上线前置 · 2026-06-25 ~ 2026-07-05 启动**
