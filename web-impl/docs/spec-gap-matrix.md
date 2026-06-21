# Front-end Spec Gap Matrix（联调版）

> 最后联调：2026-06-15 · 命令：`npm run gap:smoke` + `npm run typecheck:ci`

## 图例

| 符号 | 含义 |
|------|------|
| ✅ | 路由 + 页面 + API 薄壳 + 组件已接 |
| 🟡 | 列表/流程有，高保真/字段未全 |
| ☐ | 未实现 |
| APP | Android 端 |

---

## Epic 1 基础设施

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E1-S1 | 用户角色 | /admin/users | ✅ | ✅ | — | ✅ |
| E1-S2 | 工作流 | /admin/workflows | ✅ | ✅ | ApprovalChain | ✅ |
| E1-S3 | 字典 | /admin/dict | ✅ | ✅ | — | ✅ |
| E1-S5 | 邮件模板 | /admin/email-templates | ✅ | ✅ | — | ✅ |
| E1-S6 | 字段加密 | /admin/field-encryption | ✅ | ✅ | — | ✅ |
| E1-S4 | APP扫码 | ScanScreen (Android) | ✅ | ✅ | — | ✅ APP |

## Epic 2 销售

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E2-S1 | 客户列表 | /sales/customers | ✅ | ✅ | — | ✅ |
| E2-S1 | 客户详情 | /sales/customers/:id | ✅ | ✅ | MoneyAmount | ✅ |
| E2-S1 | 客户保护 | /sales/customer/protection | ✅ | ✅ | — | ✅ |
| E2-S2 | 报价列表/录入/审批 | /sales/quotes* | ✅ | ✅ | Figure/BOM/Money/Approval | ✅ |
| E2-S3 | 订单 | /sales/orders | ✅ | ✅ | MoneyAmount | ✅ |
| E2-S3 | 订单详情 | /sales/orders/:id | ✅ | ✅ | MoneyAmount | ✅ |
| E2-S3 | 变更/时间线 | /sales/orders/:id/* | ✅ | ✅ | MoneyAmount | ✅ |
| E2-S4 | 合同回款 | /sales/contracts/* | ✅ | ✅ | MoneyAmount | ✅ |

## Epic 3 物料

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E3-S1 | 图纸库 | /material/drawings | ✅ | ✅ | FigureNumberSearch | ✅ |
| E3-S2 | 工程转化 | /material/drawings（转化 Tab） | ✅ | ✅ | DrawingConversionWizard | ✅ |
| E3-S3 | BOM | /material/boms | ✅ | ✅ | Figure/BomTree | ✅ |
| E3-S4 | 工艺 | /material/process | ✅ | 🟡 | — | ✅ |
| E3-S3 | 料号成本 | /material/cost-aggregator（5 Tab+趋势+PDF） | ✅ | ✅ | echarts | ✅ |
| E3-IA | 仓储归物料菜单 | /material → /warehouse/* | ✅ | ✅ | menuPath | ✅ |

## Epic 4 仓储

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E4-S1 | 多仓库 | /warehouse/index | ✅ | ✅ | — | ✅ |
| E4-S2 | 扫码入/出库 | APP ScanScreen | ✅ | APP | — | ✅ APP |
| E4-S3 | 库位/库存 | /warehouse/locations,inventory | ✅ | ✅ | — | ✅ |
| E4-S4 | 库存预警 | /warehouse/inventory-alert | ✅ | ✅ | — | ✅ |

## Epic 5 生产

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E5-S1 | 排产甘特 | /production/schedule-gantt | ✅ | ✅ | MachineLoadBar/drag | ✅ |
| E5-S2 | 扫码三码 | APP WorkorderProcessScan | ✅ | APP | — | ✅ APP |
| E5-S3 | MRP | /production/mrp* | ✅ | ✅ | — | ✅ |
| E5-S4 | 工序分配 | /production/allocation | ✅ | ✅ | — | ✅ |
| E5-S4 | 委外下单（采购） | /sourcing/outsub-order | ✅ | ✅ | — | ✅ |
| E5-S5 | 委外创建 | /production/outsource-create | ✅ | ✅ | — | ✅ |
| E5-S6 | 过站 | APP 过站扫码 | ✅ | APP | — | ✅ APP |

## Epic 6 委外

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E6-S1 | 月度对账 | /sourcing/reconcile* | ✅ | ✅ | ApprovalChain | ✅ |
| E6-S5 | 7状态机面板 | /production/outsub-panel | ✅ | ✅ | — | ✅ |
| E6-S6 | 返修单 | /sourcing/rework | ✅ | ✅ | — | ✅ |
| E6-S8 | 厂商资料 | /sourcing/vendors | ✅ | ✅ | — | ✅ |

## Epic 7 品质

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E7-S1 | 检验 | /quality/inspection* | ✅ | ✅ | FigureNumberSearch | ✅ |
| E7-S2 | FA | /quality/fa* | ✅ | ✅ | — | ✅ |
| E7-S3 | CMM | /quality/cmm* | ✅ | ✅ | — | ✅ |
| E7-S4 | 不良品 | /quality/defect* | ✅ | ✅ | — | ✅ |

## Epic 8 采购

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E8-S1 | 询比价 | /sourcing/rfq* | ✅ | ✅ | — | ✅ |
| E8-S2 | 采购订单 | /sourcing/po*（含明细） | ✅ | ✅ | MaterialSelect | ✅ |
| E8-S3 | 到货提醒 | /sourcing/incoming（只读） | ✅ | ✅ | — | ✅ |
| E8-S4 | 来料检 | /quality/inspection-create | ✅ | ✅ | FigureNumberSearch | ✅ |

## Epic 9 财务

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E9-S1 | 应收应付 | /finance/receivables,payables | ✅ | ✅ | MoneyAmount | ✅ |
| E9-S2 | 成本 | /finance/cost* | ✅ | 🟡 | — | ✅ |
| E9-S3 | 付款 | /finance/payments* | ✅ | ✅ | Money/Approval | ✅ |
| E9-S4 | 利润 | /finance/profit | ✅ | ✅ | 阈值色 | ✅ |
| E9-S5 | 签字扫描件档案 | /finance/signed-scans | ✅ | ✅ | — | ✅ |

## Epic 10 人事

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E10-S1 | 人事 | /admin/hr | ✅ | 🟡 | — | ✅ |

## Epic 11 报表/工作台

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E11-S1 | 生产工作台 | /dashboard/production | ✅ | ✅ | WS/Kanban | ✅ |
| E11-S2 | 多维度看板 | /dashboard/multi | ✅ | ✅ | — | ✅ |
| E11-S2 | 委外看板 | /production/outsub-panel | ✅ | ✅ | — | ✅ |
| E11-S3 | 销售报表 | /reports/sales-* | ✅ | ✅ | — | ✅ |
| E11-S4 | 审批统计 | /reports/workflow-stats | ✅ | ✅ | charts | ✅ |

## Epic 12 供应商协同

| Story | 页面 | 路由 | API | UI | 组件 | 联调 |
|-------|------|------|-----|-----|------|------|
| E12-S2 | 到货扫码 | APP BatchIncomingScan | ✅ | APP | — | ✅ APP |

## 横切 DoD

| 项 | 状态 | 验证 |
|----|------|------|
| design-tokens.json | ✅ | src/styles/tokens/ |
| Storybook ≥30 | ✅ | npm run build-storybook |
| 6 自定义组件 | ✅ | src/components/erp/ |
| gap:smoke | ✅ | npm run gap:smoke |
| keyboard-shortcuts.md | ✅ | Ctrl+K 全局搜索 · docs/keyboard-shortcuts.md |
| dod-checklist.md | ✅ | docs/dod-checklist.md |

## 联调命令

```bash
cd web-impl
npm run gap:smoke          # 路由/文件存在性
npm run typecheck:ci       # TS + OpenAPI 对账
npm run build              # 生产构建
cd ../android-impl && ./gradlew :app:test
```

## 后端 CONDITIONAL（需联调环境）

| 频道/端点 | 状态 | 说明 |
|-----------|------|------|
| WS dashboard:* | 轮询兜底 | GET `/dashboard/kanban` `/events` `/production` 已落地 |
| WS schedule:machine | 轮询兜底 | GET/POST `/production/schedule` 已落地 |
| `/boms/save-tree` | ✅ | BomController POST |
| `/customers/protection` | ✅ | CustomerProtectionController |
| `/admin/workflows` | ✅ | AdminWorkflowController 别名 |
| `/files/init` 分片 | ✅ | FileChunkController |
| WebSocket 9 频道 | ✅ | `/api/v1/ws?channel=` + 5s 广播 |
| SSE inventory/payment | ✅ | `/api/v1/sse/inventory/alert` |
| 图纸预览 APP | ✅ | ScanScreen → DrawingPreviewActivity |
| 奶奶 E2E | ✅ | GrandmaScanE2ETest（Compose） |

## Android 联调（V1.3.9 修补）

| 项 | 状态 |
|----|------|
| 码前缀 GD/LZ/SB | ✅ 全仓统一，见 barcode-prefix.md |
| ZXing 摄像头 | ✅ ScanScreen ScanContract |
| OpenAPI 扫码路径 | ✅ `/app/workorders/{barcode}/start\|report` |
| Hilt + Retrofit + Room | ✅ NetworkModule / ScanRepository |
| Tab 扫码/待办/消息/我的 | ✅ MainActivity |
| 离线 Worker | ✅ PendingSyncWorker @HiltWorker |
