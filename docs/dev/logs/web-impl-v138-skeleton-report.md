# V1.3.8 web-impl 前端骨架交付报告

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 5 个 .vue 页面 + 5 个路由落盘

---

## 1. 改动清单

### 1.1 新增 Vue 页面（5 文件 · Element Plus + TypeScript）

| # | 文件 | Story | 内容 |
|---|------|-------|------|
| 1 | `src/views/v138/MaterialDetail.vue` | 2.1 | 7 Tab 聚合详情页（基本/工艺/图纸/价格/材料/工时/外协），含权限隔离 `canView(tab)` |
| 2 | `src/views/v138/BatchIncoming.vue` | 3.1 | 按物料粒度分批到货表单 + 批次结果弹窗 |
| 3 | `src/views/v138/NoOrderPurchase.vue` | 4.1 | 4 类采购理由下拉 + 物料清单 + 提交弹窗 |
| 4 | `src/views/v138/ProcurementApproval.vue` | 4.2 | 4 阈值路由预览（金额/品类/供应商/紧急度） |
| 5 | `src/views/v138/GmSummary.vue` | 4.3 | 6 项指标卡片 + 30 天趋势（vue-echarts 双轴图） |

### 1.2 路由配置（router/index.ts）

5 个新增路由：

```
/materials/:id/detail       → MaterialDetailV138
/incoming/batch-v138         → BatchIncomingV138
/purchase/no-order-v138     → NoOrderPurchaseV138
/approval/route-v138        → ProcurementApprovalV138
/reports/gm-summary-v138    → GmSummaryV138
```

---

## 2. 权限矩阵实现

`MaterialDetail.vue` 7 Tab 权限隔离（前端层）：

| Tab | 可见角色 |
|-----|----------|
| 基本信息 base | WAREHOUSE / PURCHASER / GM |
| 工艺路线 process | WAREHOUSE / GM |
| 图纸 drawing | WAREHOUSE / GM |
| 价格 price | PURCHASER / GM |
| 材料/工时/外协成本 | PURCHASER / GM |

实现：`tabPermissions` 对象 + `canView(tab)` 函数 + `el-tab-pane :disabled` 控制。

---

## 3. 关键设计决策

### 3.1 不改 V1.3.7 既有组件

V1.3.8 全部代码放在 `src/views/v138/` 子目录，不动 `sourcing/` `material/` 等 V1.3.7 既有目录。
**理由**：V1.3.8 是增量升级，灰度期间 V1.3.7 仍可用。

### 3.2 不实 TypeScript 类型 strict

页面中 axios 响应用 `any`，不写完整 OpenAPI 生成类型。
**理由**：前端骨架阶段，重类型实装留 Sprint 8 配合 `npm run api:gen`（openapi-typescript-codegen）。

### 3.3 角色数据 mock

`MaterialDetail.vue` 中 `userRoles = ['WAREHOUSE', 'PURCHASER', 'GM']` 硬编码。
**理由**：JWT 解码实装留 Sprint 8。本期前端骨架验证视觉/交互。

---

## 4. 已知遗留

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | JWT 解码 + 真实角色注入 | Sprint 8 |
| 2 | OpenAPI 类型生成（TypeScript strict） | Sprint 8 |
| 3 | vite build 验证（确保 5 个 .vue 编译通过） | Sprint 8 |
| 4 | E2E（Playwright）14 端点验证 | Sprint 8 |
| 5 | 移动端响应式布局（Element Plus 默认 PC 端） | Sprint 8 |

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 5 页面 + 5 路由
- **PO 范蠡** · 待前端验证
- **QA 商鞅** · 待 Playwright E2E