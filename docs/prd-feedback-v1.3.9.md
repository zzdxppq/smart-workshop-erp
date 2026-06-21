# PM 范蠡 · V1.3.9 反馈评估

> **反馈人**：PM 范蠡
> **日期**：2026-06-14
> **评估人**：PO 范蠡
> **V1.3.8 状态**：Sprint 10 收口 · 客户服务器 FAT 待启动 · 4 Story 在 hand_to_dev
> **触发场景**：客户（昆山佰泰胜）6/14 早会提出 4 条新需求 · 围绕"图纸权限 + 打印体系"

---

## 总览

| # | 需求 | 优先级 | 复杂度 | 关联 Story | 独立? |
|---|------|--------|--------|-----------|------|
| 1 | 图纸查看与打印权限（7 角色 × 5 操作矩阵） | 🔴 P0 | M | 12.1 | 独立 |
| 2 | 打印机管理（普通 + 工业标签） | 🟡 P1 | S | 12.2 | 独立 |
| 3 | 标签模板（4 种 GD-/LZ-/SB-/WW-） | 🔴 P0 | M | 12.3 | 依赖 12.2 |
| 4 | 双模式打印（ZPL 直连 + A4 PDF 浏览器） | 🔴 P0 | L | 12.4 | 依赖 12.2 + 12.3 |

**4 需求全部采纳 · 列入 V1.3.9 Sprint 12**

---

## 反馈 1：图纸查看与打印权限矩阵

### 评估

- **可行性**：🟢 高（角色枚举与权限体系 V1.3.7 Story 1.1 已 ship · 只需新增 5 类权限 + 7 角色映射）
- **优先级**：🔴 **P0**（业务合规红线 · 财务无权见图纸 = 强隔离 · 当前全员可见属"安全债"）
- **实施复杂度**：M（backend ACL 拦截器 + web-impl/web viewer 适配 + android-impl MaterialBarcodeScan/NoOrderPurchase 两个 Fragment 适配）
- **依赖**：Story 1.1 ✅ · Story 1.7 ✅（crm_drawing 表 + AES-256-GCM）· Story 2.1 ✅（料号详情聚合 · 图纸 Tab 需权限过滤）

### 建议

- V1.3.9 新增 Story **12.1 图纸权限矩阵**
- 权限定义（5 类）：`draw:preview` / `draw:print` / `draw:download` / `draw:upload` / `draw:delete`
- 角色映射（7 角色 × 5 操作 = 35 个权限单元）：
  - ENGINEER：全 5 类 ✅
  - PROD_PLANNER：preview+print ✅
  - SALES：preview+print（**仅关联订单图纸** · order_id 关联过滤）
  - PURCHASER：preview+print（**仅关联 PO 图纸** · po_id 关联过滤）
  - WAREHOUSE：preview+print（**仅关联入库单图纸** · inbound_id 关联过滤）
  - QC：preview+print（**仅关联质检单图纸** · inspection_id 关联过滤）
  - OPERATOR：preview（**APP 端仅当前工序关联图纸** · process_id 关联过滤 · 不含 print）
  - FINANCE：无图纸权限（与现行 4 阈值路由独立 · 两套 ACL 互不干扰）
- **3 端点**：`GET /api/v1/drawings/{id}/preview`（带 ACL） + `POST /api/v1/drawings/{id}/print-ticket`（打印留痕 · 12.4 共表） + `GET /api/v1/drawings/permissions/matrix`（admin 查矩阵）
- **多仓实装**：
  - backend：`DrawAclService` + `@PreAuthorize("hasAuthority('draw:preview') and #id.matchesOwnScope()")` SpEL 扩展
  - web-impl：图纸查看器 `<DrawingViewer>` 组件 + Element Plus `<el-image-preview>` 包权限拦截
  - android-impl：`MaterialBarcodeScan` + `NoOrderPurchase` 两个 Fragment + 共用 `DrawPermissionInterceptor`
- **G7 目标描述更新**：原"一线只见数量/工序/状态" → 改为"一线只见数量/工序/状态/关联图纸"
- **文档清理**：删原文"送货员"字样（V1.3.8 已无此角色）
- 实施工时：4-6 天

### 与 V1.3.7/V1.3.8 资源对照

| PM 建议 | V1.3.7 现状 | V1.3.8 现状 | 缺口 |
|--------|-------------|-------------|------|
| 7 角色 × 5 操作矩阵 | 1.1 已 ship 10 角色枚举 | 无新增 | 需新增 5 类 `draw:*` 权限 + 角色映射表 |
| 角色关联过滤（订单/PO/入库/质检/工序） | 1.6/1.18/1.34/1.51/1.16 已 ship 关联表 | 无变化 | 需新增 5 类 `crm_drawing_link` 关联表 |
| 财务无图纸权限 | 1.1 角色 + 1.3 `sys_global_threshold` 金额 4 阈值 | 无变化 | 需新增 FINANCE 角色 → 0 权限映射 |
| 与金额 4 阈值路由独立 | 1.3 sys_global_threshold 双轨 | 无变化 | 需 ACL 设计明确"两套互不干扰"|
| G7 一线目标描述更新 | PRD G7 原文"只见数量/工序/状态" | 未变 | 文档修订 |

### PO 决策

✅ **采纳** · 列入 V1.3.9 Sprint 12 · 与 12.4 关联（打印留痕共用 `sys_print_log` 表）· **必须**更新 PRD G7 章节 + 删"送货员"字样

### 风险

- ⚠️ 当前全员可见图纸 → 上线后业务员/采购/仓管/品检/操作工全"看不到部分图纸" = 业务冲击面大 · 需灰度
- ⚠️ V1.3.7 Story 1.7 `crm_drawing` 表无关联字段（order_id/po_id/inbound_id/inspection_id/process_id），需新增 5 类关联
- ⚠️ 与金额 4 阈值（sys_global_threshold）两套 ACL 并存 · 新人易混 · 需明确术语"图纸 ACL vs 金额 ACL"

---

## 反馈 2：打印机管理

### 评估

- **可行性**：🟢 高（基础字典表 · 心跳用 ScheduledExecutorService + 端口 TCP 探测）
- **优先级**：🟡 **P1**（标签打印前置依赖 · 但系统参数 admin 页同款风格易实装）
- **实施复杂度**：S（backend 字典表 + 心跳调度 + web-impl admin 页）
- **依赖**：Story 1.3 ✅（系统参数/Dict 字典 · 同款 admin 页风格）

### 建议

- V1.3.9 新增 Story **12.2 打印机管理**
- 新增表 `sys_printer`（id / name / type[NORMAL/LABEL] / ip / port[9100] / status[ONLINE/OFFLINE] / last_heartbeat_at）
- **3 端点**：
  - `GET /api/v1/printers` · `POST /api/v1/printers` · `PUT /api/v1/printers/{id}` · `DELETE /api/v1/printers/{id}`（admin only）
  - `POST /api/v1/printers/{id}/test`（测试连接 · TCP Socket 探活）
  - `GET /api/v1/printers/available?type=LABEL`（查询当前可用同类型）
- **心跳**：`@Scheduled(fixedRate = 60000)` + TCP Socket 端口探测 · 不在线则 `status=OFFLINE` 标红
- **用户点击打印逻辑**（在 12.4 共用）：
  - 只配 1 台同类型 → 直接打印
  - 多台 → 弹选择框（Vue `<el-dialog>` / Android `<AlertDialog>`）
  - 未配 → 提示"管理员先配置"（前端拦截 · 不抛错）
- **协议区分**：
  - NORMAL（普通激光/喷墨）：OS 打印队列 · 走 PDF 浏览器打印（模式二）
  - LABEL（标签打印机）：ZPL（Zebra）/ TSPL（TSC）· Socket 9100 直连（模式一）
- **支持型号**：得力 DL-888B / 斑马 ZD420 / TSC TTP-244 Pro（V1.3.9 默认预置 `model_suggestion` 字段）
- **不支持**：ESC/POS 票据打印机（明确文档说明）
- 实施工时：2-3 天

### 与 V1.3.7/V1.3.8 资源对照

| PM 建议 | V1.3.7/V1.3.8 现状 | 缺口 |
|--------|--------------------|------|
| 打印机配置 admin 页 | 1.3 sys_dict 字典 admin 页 | 需新增 `sys_printer` 表 + admin 页 |
| 心跳 60 秒 | 1.1 `server-status` 健康检查 | 需新增 `@Scheduled` 调度 + TCP 探测 |
| 多台支持 | 无 | 需新增 `available?type=` 端点 |
| 协议区分 ZPL/TSPL/PDF | 无 | 需明确 `protocol` 字段（`ZPL`/`TSPL`/`PDF_BROWSER`）|
| 不支持 ESC/POS | 无 | 文档明确 + 校验拦截 |

### PO 决策

✅ **采纳** · 列入 V1.3.9 Sprint 12 · 作为 12.3/12.4 前置依赖（parallel_group B 优先跑）

### 风险

- 🟢 低：心跳调度若 host 网络不稳（如客户机房断网 1 分钟）会误标 OFFLINE · 阈值容差 2 次失败再标
- 🟢 低：web-impl 跨域调用后端 9100 不可能（CORS + 浏览器不允许）· 必须在后端 Socket 代理（架构已明确）

---

## 反馈 3：标签模板（4 种）

### 评估

- **可行性**：🟢 高（4 个独立模板 + SB- 复用 GD- 模板仅色条色不同 · 工程复用度高）
- **优先级**：🔴 **P0**（核心交付物 · 无标签则工单/流转/委外/设备无法物理落地）
- **实施复杂度**：M（4 模板渲染 + ZPL 指令生成 + 前端打印预览）
- **依赖**：12.2 ✅（打印机配置）· Story 1.4 ✅（APP 扫码壳 5 类码已支持 WL-）· Story 1.11/3.2 ✅（WL- 物料码前缀一致）

### 建议

- V1.3.9 新增 Story **12.3 标签模板**
- **4 标签规格统一** 50mm × 30mm · 三区布局（顶行：色条 + 厂名 / 中央：QR / 下方：明文 6 行）
- **4 模板**：
  | 类型 | 前缀 | 色条色 | 复用 |
  |------|------|--------|------|
  | 工单码 | `GD-` | 蓝色 #1E40AF | 独立 |
  | 流转码 | `LZ-` | 绿色 #16A34A | 独立 |
  | 设备码 | `SB-` | 灰色 #6B7280 | 复用 GD- 模板（仅色条色不同）|
  | 委外单码 | `WW-` | 橙色 #EA580C | 独立 |
- **2 端点**：
  - `GET /api/v1/labels/templates`（列 4 模板元数据）
  - `POST /api/v1/labels/preview`（生成 PNG/PDF 预览 · 二维码内容 + 明文 6 行）
- **二维码内容**（纯文本 · APP 扫码壳按前缀自动路由 · 1.4 已 ship）：
  - `LZ-260613-001-P03` → 流转码
  - `GD-260614-001` → 工单码
  - `WW-260614-001` → 委外单码
  - `SB-260614-001` → 设备码
  - `WL-...` → 物料码（V1.3.8 已有 · 保持）
- **厂名可配置**：admin 在系统设置（复用 1.3 `sys_dict`）配置 · 默认"昆山佰泰胜精密加工"
- **前端预览**：web-impl `<LabelPreview>` Vue 组件 + Element Plus `<el-image>` 显示 PNG · Android `LabelPreviewFragment` + ZXing 渲染
- 实施工时：3-4 天

### 与 V1.3.7/V1.3.8 资源对照

| PM 建议 | V1.3.7/V1.3.8 现状 | 缺口 |
|--------|--------------------|------|
| 50mm×30mm 统一规格 | 1.11/3.2 物料码已是 50×30 | 无 |
| 4 模板三区布局 | 无 | 需新增 4 模板资源 |
| 二维码内容路由前缀 | 1.4 APP 扫码壳已支持 5 类码 | 无（路由已就绪 · 仅新增 SB-/WW- 两类）|
| 厂名可配置 | 1.3 sys_dict 字典 | 需新增 `sys_company_name` 配置项 |

### PO 决策

✅ **采纳** · 列入 V1.3.9 Sprint 12 · 作为 12.4 前置（parallel_group C 优先跑）

### 风险

- 🟡 中：QR 二维码内容长度限制（QR v3-M 最多 200 字符）· 明文 6 行若超长需切换 v4
- 🟢 低：色条复用 · SB- 复用 GD- 模板仅色条色不同 · 代码层面抽 `BaseLabelTemplate.render(BarColor)`
- 🟢 低：APP 扫码壳新增 SB-/WW- 两类需配合发布 · 但 1.4 已设计为可扩展（按前缀分发）

---

## 反馈 4：双模式打印

### 评估

- **可行性**：🟡 中（ZPL Socket 直连需后端做 Socket 代理 · 浏览器无法跨域）
- **优先级**：🔴 **P0**（业务交付物 · 没打印则 12.3 标签无法物化）
- **实施复杂度**：L（backend ZPL 生成 + Socket 客户端 + A4 PDF 排版 + 前端打印入口 + 留痕）
- **依赖**：12.2 ✅ + 12.3 ✅ + Story 1.5/1.6 ✅（OpenPDF 1.3.34）

### 建议

- V1.3.9 新增 Story **12.4 双模式打印**
- **模式一：ZPL/TSPL 直连**
  1. 后端 `LabelRenderService.renderZpl(BarColor, qrText, lines)` → 输出 ZPL II 字符串
  2. 后端 `SocketClient.sendZpl(ip, port, zpl)` → 端口 9100 · `OutputStream.write(zpl.getBytes(UTF_8))`
  3. 失败 → 抛 `PRINT_CONNECTION_FAILED` (40950) · 前端弹"请检查打印机连接"
- **模式二：A4 PDF 浏览器打印**
  1. 后端 `LabelRenderService.renderPdfA4(templates, count)` → OpenPDF · 一页 30 标签（3 列 × 10 行）
  2. 前端 `window.print()` · 用户选激光/喷墨 + A4 纸 · 打印后人工裁剪
- **新增表 `sys_print_log`**（与 8.3 `sys_workflow_event` 同 sys_ 命名空间）：
  - `id` · `operator_user_id` · `printed_at` · `code_type[GD/LZ/SB/WW/WL]` · `code_value` · `count` · `printer_ip` · `printer_name` · `mode[ZPL_DIRECT/PDF_BROWSER]` · `status[SUCCESS/FAILED]` · `error_msg`
- **5 端点**：
  - `POST /api/v1/print/labels/zpl`（模式一 · 直接送 ZPL）
  - `POST /api/v1/print/labels/pdf-a4`（模式二 · 返回 PDF 流）
  - `GET /api/v1/print/logs`（打印历史查询 · 分页 · 多维过滤）
  - `POST /api/v1/print/logs/{id}/replay`（一键补打 · 同模式或换模式）
  - `GET /api/v1/print/statistics`（按月/人/类型聚合）
- **前端入口**：
  - web-impl：`PrintButton` Vue 组件（V1.3.8 10.1/10.5 已 ship · 集成）
  - android-impl：`PrintDialogFragment`（V1.3.8 已 ship 8 角色 · 集成）
  - 用户点击 → 12.2 逻辑：1 台直打 / 多台选 / 未配提示
- 实施工时：5-7 天

### 与 V1.3.7/V1.3.8 资源对照

| PM 建议 | V1.3.7/V1.3.8 现状 | 缺口 |
|--------|--------------------|------|
| ZPL Socket 9100 直连 | 无 | 需新增 `ZplSocketClient` · 后端代理 |
| A4 PDF 3×10 = 30 标签/页 | 1.5/1.6 OpenPDF 已 ship 报价/订单 | 需新增 `LabelPdfLayout` 组件 |
| sys_print_log 表 | 8.3 sys_workflow_event 同命名空间 | 需新增迁移 V57 |
| 一键补打（同/换模式） | 无 | 需新增 `replay` 端点 · 复用渲染逻辑 |
| 跨模式 | 无 | 模式一失败 → 自动降级模式二（前端可选）|

### PO 决策

✅ **采纳** · 列入 V1.3.9 Sprint 12 · 作为 12.1 关联（图纸打印留痕共表）· **必须**新增 V57 迁移

### 风险

- 🔴 **高**：浏览器无法跨域调打印机 Socket → 必须在后端做 Socket 代理 · 架构明确
- 🟡 中：客户机房打印机若 IP 变化 · admin 需手动改配置（暂不支持 DHCP 自动发现 · V1.4 backlog）
- 🟡 中：ZPL 与 TSPL 指令集部分字段不兼容 · 需 `ProtocolAdapter` 抽象
- 🟢 低：一键补打若历史记录被归档 → 仅 SUCCESS 状态可重放 · FAILED 不可

---

## 跨 Story 依赖矩阵

```
                 12.1 (图纸权限)   12.2 (打印机管理)   12.3 (标签模板)   12.4 (双模式打印)
12.1                  -               无               无              共用 sys_print_log
12.2                  -                -           前置（查询打印机）  前置（选打印机）
12.3                  -                -                -          前置（模板渲染）
12.4              共用表               -                -                -
```

**V1.3.7/V1.3.8 既有依赖**：
- 12.1 ← 1.1（角色）· 1.7（crm_drawing + AES-256-GCM）· 2.1（料号详情聚合）
- 12.2 ← 1.3（系统参数 admin 页风格）
- 12.3 ← 1.4（APP 扫码壳 5 类码）· 1.11/3.2（WL- 物料码）· 1.7（图纸）
- 12.4 ← 1.5/1.6（OpenPDF 1.3.34 / POI 5.2.5）· 8.3（sys_workflow_event sys_ 命名空间）

---

## Sprint 12 建议范围

| Story | Title | 优先级 | 工时 | 复杂度 | 端点 | 测例 | Migration |
|-------|-------|--------|------|--------|------|------|-----------|
| 12.1 | 图纸权限矩阵 | 🔴 P0 | 4-6 天 | M | 3 | 24 | V54 |
| 12.2 | 打印机管理 | 🟡 P1 | 2-3 天 | S | 6 | 16 | V55 |
| 12.3 | 标签模板 | 🔴 P0 | 3-4 天 | M | 2 | 14 | V56 |
| 12.4 | 双模式打印 | 🔴 P0 | 5-7 天 | L | 7 | 32 | V57 |
| **Sprint 12 预估** | **4 Story** | — | **14-20 天** | — | **18** | **86** | **V54-V57** |

**parallel_group**：
- A：12.1 + 12.4（关联 · 共 sys_print_log 表）
- B：12.2（独立 · 但前置 12.3/12.4）
- C：12.3（独立 · 但前置 12.4）

**启动顺序建议**：12.2 (B) → 12.3 (C) → 12.1 + 12.4 (A 并行)

---

## 优先级分布

- **🔴 P0** × 3（12.1 / 12.3 / 12.4 · 业务红线 + 交付物）
- **🟡 P1** × 1（12.2 · 前置依赖但复杂度低）

---

## V1.3.9 Sprint 12 启动条件

1. ✅ V1.3.8 Sprint 10 FAT 准入（预计 6/15）
2. ✅ V1.3.8 Sprint 10 dev 4 Story 完结（10.1/10.2/10.3/10.5 · 6/14 EOD）
3. ✅ 客户（昆山佰泰胜）图纸权限矩阵签字（与 PM 范蠡 6/14 同步）
4. ✅ V1.3.8 11.x（PM 决策#2 InspectionDTO schema 补齐）随 Sprint 12 启动同步切换

**预计启动时间**：2026-06-15

---

## 签字

- **PM 范蠡** · 2026-06-14
- **PO 范蠡** · 2026-06-14
- **4 条反馈全部采纳 · 列入 V1.3.9 Sprint 12**
- **优先级 P0 × 3 + P1 × 1 · 预计 14-20 天完成（~3 周）**
- **截止 V1.3.9 Sprint 12 启动时间 2026-06-15**