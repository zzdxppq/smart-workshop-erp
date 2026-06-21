# PM 范蠡 · V1.4.0 反馈评估

> **反馈人**：客户（昆山佰泰胜）第八次反馈  
> **日期**：2026-06-19  
> **评估人**：PO 范蠡  
> **V1.3.8 状态**：✅ Shipped（78 测例 PASS）  
> **V1.3.9 状态**：📋 规划中（图纸权限 + 打印体系 · 未实现）  
> **触发场景**：客户现场走访 + 业务员报价录入 + 生管考核公示 + 客户到厂看进度演示

---

## 总览

| # | 需求 | 优先级 | 复杂度 | 关联 Story | 独立? |
|---|------|--------|--------|-----------|------|
| 1 | 员工考核 · 操作工/机台生产数据 · 生管工作台「绩效看板」 | 🔴 P0 | L | E11-S6 | 依赖 E5-S2 报工 |
| 2 | 客户现场演示账号（搜索进度 · 屏蔽金额/委外） | 🔴 P0 | M | E11-S7 | 独立 |
| 3 | 新建报价选图号带出金额 + 呈现工艺路线 | 🔴 P0 | S | E2-S2 增强 | 依赖 2.1 料号详情 |
| 4 | E11-S2 AC-11.2.3 交付期检索收口（生管入口 + 工单进度） | 🟡 P1 | S | E11-S2 补丁 | 部分已有 |
| 5 | 绩效看板技术方案（聚合表 + ECharts + 大屏轮播） | 🟡 P1 | M | E11-S6 子项 | 依赖 #1 |

**5 项全部采纳 · 列入 V1.4.0 Sprint 15（不另收费 + 不延期）**

---

## 反馈 1：员工考核 · 生产绩效看板

### 评估

- **可行性**：🟢 高（`crm_production_report` + `crm_production_scan` 已 ship · E10-S3 绩效方案可挂接）
- **优先级**：🔴 **P0**（客户原话："ERP 一定要有员工考核"）
- **实施复杂度**：L（聚合 API + 看板页 + 权限 + 可选大屏）
- **依赖**：E5-S2 扫码报工 ✅ · E11-S1 生产工作台 ✅ · E10-S3 绩效方案 ✅

### 与 PRD 差距

| PRD 现状 | 客户要求 | 缺口 |
|---------|---------|------|
| E10-S3 HR 月度绩效（产量/质量/态度权重） | 车间**日/周/月**操作工+机台产量公示 | 缺 E11 生产绩效看板 |
| E11-S1 设备稼动率 + 人员出勤 | 每人/每机**完工、合格、报废、利用率**排行 | 缺个人/机台维度 |
| 无大屏 | 车间电视轮播 | 可选 fullscreen 模式 |

### 建议实现（客户确认方案）

**数据采集**：复用 `crm_production_report`（`reported_by`≈操作工）+ `crm_production_scan`（`equipment_id`≈机台），按日聚合：

- 完工总数（`reported_qty` 合计）
- 合格数（`is_abnormal=0` 的 `reported_qty`）
- 报废数（`is_abnormal=1` 的 `reported_qty`）
- 合格率 = 合格 / 完工
- 工时利用率 = `actual_minutes` / 标准工时（来自 `crm_workorder_step.estimated_minutes`）

**看板内容**（路由 `/dashboard/performance-board`）：

1. **个人排行**：日/周/月 Tab · 产量 · 合格率 · 超产率
2. **机台排行**：产出量 · 利用率 · 故障率（结合 `prod_machine.status`）
3. **趋势图**：近 30 天产量/合格率（ECharts）
4. **考核分**：读取 E10 绩效方案权重自动算分（产量 + 质量 + 出勤）

**权限**：

- 生管 `PROD_MGR`、车间主管 `PRODUCTION_MANAGER`、总经理 `GM`：全员
- 操作工 `OPERATOR`：`@DataScope` 仅本人（`reported_by = currentUserId`）

**技术**：

- 新增 `crm_employee_performance_daily` 日聚合表（XXL-JOB 每晚刷新，日间 API 实时查报工表）
- `GET /api/v1/dashboard/performance?period=day|week|month&groupBy=operator|machine`
- 生管工作台 `RoleWorkflowPanel` 快捷入口新增「绩效看板」
- 可选：`?mode=tv` 全屏轮播（个人 Top10 → 机台 Top10 → 趋势）

### PO 决策

✅ **采纳** · 新增 Story **E11-S6 生产绩效看板**

---

## 反馈 2：客户现场演示账号

### 评估

- **可行性**：🟢 高（工单/订单数据已有 · 需后端脱敏视图）
- **优先级**：🔴 **P0**（生管带客户到车间演示 · 不能暴露金额/委外）
- **实施复杂度**：M（新角色 + 单页搜索 + API 强制过滤）
- **依赖**：E5-S1 工单 ✅ · E2-S3 订单 ✅

### 场景

```
生管提前登录「演示账号」→ 仅搜索页
  按料号 / 工单号 / 客户名 检索
  展示：当前工序、进度%、预计交期
  屏蔽：单价、金额、委外厂商、WW-单号、外协成本
  委外工序展示为普通厂内工序名（客户感觉全在厂里加工）
```

### 建议

- 新增角色 `CUSTOMER_VISITOR`（客户现场演示）
- 登录后仅 `/visitor/progress`（无侧边栏 · 大字号搜索框）
- `GET /api/v1/visitor/progress/search?keyword=` 后端 `viewMode=visitor`：
  - 金额字段一律 `null`
  - `is_outsource=1` → 展示名保留、不标委外、不返回厂商
  - 可按 `customer_id` 绑定账号限制可见范围
- 演示用户种子：`visitor_demo` / 密码见 `seed-demo-users.sql`

### 与 G7 关系

G7 管内部角色金额隔离；本需求为**专用脱敏视图**，后端强制过滤，不靠前端 `v-if` 隐藏。

### PO 决策

✅ **采纳** · 新增 Story **E11-S7 客户现场演示视图**

---

## 反馈 3：新建报价 · 选图号带出金额 + 工艺路线

### 评估

- **可行性**：🟢 高（图纸 `processRoute` JSON + 料号详情 `current_price` 已有）
- **优先级**：🔴 **P0**
- **实施复杂度**：S（`QuoteForm.vue` + 料号 lookup API）
- **依赖**：E2-S2 报价 ✅ · Story 2.1 料号详情 ✅

### 现状缺口

`QuoteForm.onDrawingSelect` 仅带出材质/尺寸/FA，**未设置 `unitPrice`**，右侧无工艺路线预览。

### 建议

- 选图号后：
  1. `GET /materials/lookup?code={materialCode}` → `GET /materials/{id}/detail` 取 `price.currentPrice` 填入单价（参考价，可改）
  2. 解析 `Drawing.processRoute` 或料号 `process.routes` 在右侧展示「工艺路线预览」步骤条
- PRD 增补 **AC-2.2.4**：选图号自动带出参考单价 + 工艺路线预览

### PO 决策

✅ **采纳** · E2-S2 增强 · 不新增独立 Story

---

## 反馈 4：E11-S2 交付期检索收口

### 评估

- **可行性**：🟢 高（`MultiDashboard.vue` 第四 Tab + `DashboardDeliveryService` 已存在）
- **优先级**：🟡 **P1**（客户/生管反馈"没看到"——入口与字段不完整）
- **实施复杂度**：S

### 代码现状 vs PRD AC-11.2.3

| AC 要求 | 现状 | 缺口 |
|--------|------|------|
| 交付期检索页 | `MultiDashboard` Tab「交付期检索」 | ✅ 页面存在 |
| 客户名 + 状态多选 | ✅ UI 已有 | — |
| 订单 + **工单进度** | 仅订单级 `estimateProgress` | ❌ 未关联工单 |
| 预计/实际交期 | 部分有 | 表格缺工单号/当前工序列 |
| 一键复制文案 | ✅ | — |
| 生管快捷入口 | 仅 GM 工作台有「多维度看板」 | ❌ 生管 `PROD_MGR` 快捷入口缺失 |
| 交期范围过滤 | API 支持 `deliveryFrom/To` | ❌ 前端未暴露 |

### 建议

1. `PROD_MGR` 快捷入口新增「交付期检索」→ `/dashboard/multi?tab=delivery`
2. `DashboardDeliveryService` 关联 `crm_workorder` 返回工单号、工序、真实进度
3. 前端补交期范围 DatePicker + 工单列

### PO 决策

✅ **采纳** · E11-S2 补丁 · 不新增 Story ID

---

## 反馈 5：绩效看板技术细化（客户补充）

### 物化视图 / 定时任务

```sql
-- crm_employee_performance_daily（V72 迁移）
-- 字段：stat_date, operator_id, machine_id, finished_qty, qualified_qty,
--       scrap_qty, actual_minutes, std_minutes, score, grade
-- 刷新：XXL-JOB cron 0 30 0 * * ?（每日 00:30）
-- 日间查询：直接聚合 crm_production_report（实时）+ 日表（历史趋势）
```

### 前端

- ECharts：柱状排行 + 折线趋势
- WebSocket 可选（V1.4.0 先 30s 轮询，与 E11-S1 一致）
- `?mode=tv`：隐藏导航 · 60s 自动切 Tab

### PO 决策

✅ **采纳** · 作为 E11-S6 技术附录写入 PRD 增量章节

---

## Sprint 15 实施顺序

| 顺序 | 项 | 估时 |
|------|-----|------|
| 1 | E2-S2 报价选图号带出（#3） | 0.5d |
| 2 | E11-S7 客户演示账号（#2） | 1.5d |
| 3 | E11-S2 交付期检索收口（#4） | 0.5d |
| 4 | E11-S6 绩效看板（#1+#5） | 2d |

**合计约 4.5 人天**

---

## Flyway 规划（V72）

| 文件 | 内容 |
|------|------|
| V72__v140_visitor_performance.sql | `CUSTOMER_VISITOR` 角色 · 演示用户 · 绩效日聚合表 · 菜单 `dash.performance` / `visitor.progress` |

---

## 风险

- ⚠️ `crm_production_report` 无独立 `qualified_qty`/`scrap_qty` 字段，V1.4.0 用 `is_abnormal` 区分，后续可扩展扫码报工字段
- ⚠️ 演示账号若未绑定 `customer_id`，需生管演示前确认搜索范围
- ⚠️ V1.3.9 打印/图纸权限与本章无冲突，可并行排期

---

## PO 终审

✅ **V1.4.0 全量采纳** · 更新 `docs/prd.md` 变更日志 + 文末增量章节 · Sprint 15 按序实施
