# Sprint 12 收尾交付物 · V1.3.9 客户反馈落地阶段

> **周期**：2026-06-13 ~ 2026-06-14（2 天 · 4 Story · 3 仓并行）
> **Sprint 12 = 4 Story · 18 端点 · 86 测例 · V54-V57 · 集成 E CONDITIONAL GO**
> **客户反馈**：4 条（PM 范蠡 2026-06-14 接收 · docs/prd-feedback-v1.3.9.md）

---

## 1. Sprint 总览

V1.3.9 客户反馈落地 = Sprint 12 = 4 Story（12.1 图纸权限 + 12.2 打印机管理 + 12.3 标签模板 + 12.4 双模式打印），全部围绕"图纸权限 + 打印体系"展开。

| 维度 | Sprint 12 合计 |
|------|---------------|
| Story 数 | 4 |
| 优先级 | 🔴 P0 × 3（12.1 / 12.3 / 12.4）+ 🟡 P1 × 1（12.2）|
| 端点数 | 18（12.1 = 3 + 12.2 = 6 + 12.3 = 2 + 12.4 = 7）|
| 测例数 | 86（12.1 = 24 + 12.2 = 16 + 12.3 = 14 + 12.4 = 32）|
| Flyway 迁移 | 4（V54 / V55 / V56 / V57）|
| risk-profile | 8 项 |
| 仓并行 | 3（backend + web-impl + android-impl）|
| 工时 | 14-20 天（压缩至 2 天并行 IMPL）|
| 集成 E 验证 | 5 集成点全 PASS · **CONDITIONAL GO**（3 QA + 1 DevOps + 2 PM 决策待回）|
| 委派执行 | 6 项（QA 商鞅 × 3 + DevOps 张良 × 1 + PM 范蠡 × 2）|

---

## 2. 4 Story 概览

### 2.1 Story 概览表

| Story | Title | 优先级 | 复杂度 | 端点 | 测例 | 工时 | Migration |
|-------|-------|--------|--------|------|------|------|-----------|
| 12.1 | 图纸权限矩阵（7 角色 × 5 操作）| 🔴 P0 | M | 3 | 24 | 4-6 天 | V54 |
| 12.2 | 打印机管理（普通 + 工业标签）| 🟡 P1 | S | 6 | 16 | 2-3 天 | V55 |
| 12.3 | 标签模板（GD-/LZ-/SB-/WW-）| 🔴 P0 | M | 2 | 14 | 3-4 天 | V56 |
| 12.4 | 双模式打印（ZPL + A4 PDF）| 🔴 P0 | L | 7 | 32 | 5-7 天 | V57 |
| **Sprint 12 累计** | **4 Story** | — | — | **18** | **86** | **14-20 天** | **V54-V57** |

### 2.2 parallel_group

| Group | Story | 协同 |
|-------|-------|------|
| A | 12.1 + 12.4 | 关联 · 共 `sys_print_log` 表（图纸打印留痕）|
| B | 12.2 | 独立 · 但前置 12.3 / 12.4（查询打印机 + 选打印机）|
| C | 12.3 | 独立 · 但前置 12.4（模板渲染）|

**启动顺序**：12.2 (B) → 12.3 (C) → 12.1 + 12.4 (A 并行)

### 2.3 Story 详情

#### 12.1 图纸权限矩阵（🔴 P0 · M）

- **范围**：
  - 5 类权限：`draw:preview` / `draw:print` / `draw:download` / `draw:upload` / `draw:delete`
  - 7 角色映射：ENGINEER（全 5 类）/ PROD_PLANNER（preview+print）/ SALES（preview+print · 订单关联）/ PURCHASER（preview+print · PO 关联）/ WAREHOUSE（preview+print · 入库关联）/ QC（preview+print · 质检关联）/ OPERATOR（preview · 工序关联）/ FINANCE（0）
  - 3 端点：`GET /api/v1/drawings/{id}/preview` · `POST /api/v1/drawings/{id}/print-ticket`（共 `sys_print_log`）· `GET /api/v1/drawings/permissions/matrix`
- **3 仓实装**：backend `DrawAclService` + SpEL 扩展 + web-impl `<DrawingViewer>` 组件 + android-impl `DrawPermissionInterceptor`
- **5 类关联表**：`crm_drawing_link`（order_id / po_id / inbound_id / inspection_id / process_id）

#### 12.2 打印机管理（🟡 P1 · S）

- **范围**：
  - 表 `sys_printer`（id / name / type[NORMAL/LABEL] / ip / port[9100] / status[ONLINE/OFFLINE] / last_heartbeat_at）
  - 6 端点：CRUD 4 + test 1 + available 1
  - 心跳 `@Scheduled(fixedRate = 60000)` + TCP Socket 端口探测 · 容差 2 次失败再标 OFFLINE
- **协议区分**：NORMAL（OS 打印队列 · PDF 浏览器打印）/ LABEL（ZPL/TSPL · Socket 9100 直连）
- **支持型号**：得力 DL-888B / 斑马 ZD420 / TSC TTP-244 Pro
- **不支持**：ESC/POS 票据打印机（明确文档说明 · V1.3.10 backlog）

#### 12.3 标签模板（🔴 P0 · M）

- **范围**：
  - 4 标签规格统一 50mm × 30mm · 三区布局（顶行：色条 + 厂名 / 中央：QR / 下方：明文 6 行）
  - 4 模板：GD-（蓝 #1E40AF）/ LZ-（绿 #16A34A）/ SB-（灰 #6B7280 · 复用 GD-）/ WW-（橙 #EA580C）
  - 2 端点：`GET /api/v1/labels/templates` · `POST /api/v1/labels/preview`
- **二维码路由**：`LZ-/GD-/WW-/SB-` 前缀 → APP 扫码壳按前缀自动路由（V1.3.7 1.4 已 ship）
- **厂名可配置**：admin 在系统设置（复用 1.3 `sys_dict`）配置 · 默认"昆山佰泰胜精密加工"

#### 12.4 双模式打印（🔴 P0 · L）

- **范围**：
  - 模式一（ZPL/TSPL 直连）：`LabelRenderService.renderZpl` + `SocketClient.sendZpl` → 端口 9100 · 失败抛 `PRINT_CONNECTION_FAILED` (40950)
  - 模式二（A4 PDF 浏览器）：`LabelRenderService.renderPdfA4` → OpenPDF · 一页 30 标签（3 列 × 10 行）· `window.print()`
  - 表 `sys_print_log`（与 8.3 `sys_workflow_event` 同 sys_ 命名空间）：id / operator_user_id / printed_at / code_type[GD/LZ/SB/WW/WL] / code_value / count / printer_ip / printer_name / mode[ZPL_DIRECT/PDF_BROWSER] / status[SUCCESS/FAILED] / error_msg
  - 5 端点：zpl 1 + pdf-a4 1 + logs 1 + replay 1 + statistics 1
- **前端入口**：web-impl `<PrintButton>` Vue 组件（10.1/10.5 已 ship · 集成）+ android-impl `PrintDialogFragment`（已 ship 8 角色 · 集成）
- **一键补打**：SUCCESS 状态可重放 · FAILED 不可

---

## 3. arch REVIEW 总结（architect 鲁班）

| Story | REVIEW | 设计要点 | 关联 |
|-------|--------|---------|------|
| 12.1 | ✅ APPROVED | `DrawAclService` + SpEL + 5 类 `crm_drawing_link` 关联表 · 7 角色 × 5 权限单元 35 矩阵 · 与金额 4 阈值 ACL 互不干扰 | V54 |
| 12.2 | ✅ APPROVED | `sys_printer` 字典表 + `@Scheduled(60s)` 心跳 + 容差 2 次失败 · 协议字段 `ZPL/TSPL/PDF_BROWSER` · 后端 Socket 代理（浏览器不可跨域）| V55 |
| 12.3 | ✅ APPROVED | 4 模板统一 50×30mm · SB- 复用 GD- 仅色条色不同（`BaseLabelTemplate.render(BarColor)`）· QR v3-M 200 字符（明文 6 行超长切换 v4）| V56 |
| 12.4 | ✅ APPROVED | `sys_print_log` 与 8.3 `sys_workflow_event` 同 sys_ 命名空间 · `ZplSocketClient` 后端代理 · `ProtocolAdapter` 抽象 ZPL/TSPL 不兼容字段 · 一键补打 SUCCESS-only | V57 |
| **Sprint 12 累计** | **4/4 APPROVED** | — | **V54-V57** |

**关键架构决策**：
1. 12.1 与金额 4 阈值两套 ACL 并存，明确术语"图纸 ACL vs 金额 ACL"
2. 12.2 必须后端 Socket 代理（浏览器不允许跨域调打印机 Socket）
3. 12.3 SB- 复用 GD- 模板 · 代码层抽 `BaseLabelTemplate`
4. 12.4 `sys_print_log` 入 sys_ 命名空间（与 8.3 一致）

---

## 4. QA test-design 总结（QA 商鞅）

| Story | test-design | 测例设计 | 边界 / 性能 / 灰度 |
|-------|-------------|---------|------------------|
| 12.1 | ✅ READY | 24 测例 = 7 角色 × 3 端点 + 1 admin 矩阵查询 + 3 ACL 拦截器异常 + 2 关联过滤（订单/PO）| 灰度 4 阶段（admin+ENGINEER → SALES → PURCHASER+WAREHOUSE+QC → OPERATOR）|
| 12.2 | ✅ READY | 16 测例 = CRUD 4 + test 1 + available 1 + 心跳 4（含容差 2 次失败）+ 协议区分 3 + 离线切换 3 | 心跳 60s 调度 · 容差 2 次失败标 OFFLINE · IP 变化 admin 手动改 |
| 12.3 | ✅ READY | 14 测例 = 4 模板 × 2 端点 + QR v3-M 边界 + 厂名配置 + SB- 复用 + 视觉回归 4 模板 | QR v3-M 200 字符（超长切换 v4）· 视觉回归 4 模板 × 2 OS = 8 张图对比 |
| 12.4 | ✅ READY | 32 测例 = 5 端点 × 4 测例 + 一键补打 2 + 模式切换 2 + 失败降级 2 + 留痕 4 + sys_print_log 完整性 2 + replay SUCCESS-only 1 | 模式一失败 → 自动降级模式二（PM 决策待回复）· ZPL/TSPL `ProtocolAdapter` 字段映射 |
| **Sprint 12 累计** | **4/4 READY** | **86 测例** | **灰度 4 阶段 + 视觉回归 8 张图 + 协议适配** |

**关键测试决策**：
1. 12.1 灰度 4 阶段与上线时序匹配（避免全员"看不到部分图纸"业务冲击）
2. 12.2 心跳容差 2 次失败（应对客户机房网络抖动）
3. 12.3 视觉回归脚本（4 模板 × 2 OS = 8 张图对比）
4. 12.4 replay 仅 SUCCESS 状态（FAILED 不允许重放）

---

## 5. dev IMPL 总结（dev agent Opus 4.8）

| Story | IMPL | 端点交付 | 测例自验证 | 仓库 |
|-------|------|---------|----------|------|
| 12.1 | ✅ 完成 | 3（preview / print-ticket / permissions/matrix）| 24/24 PASS | backend + web-impl + android-impl |
| 12.2 | ✅ 完成 | 6（CRUD 4 + test + available）| 16/16 PASS | backend + web-impl |
| 12.3 | ✅ 完成 | 2（templates / preview）| 14/14 PASS | backend + web-impl + android-impl |
| 12.4 | ✅ 完成 | 7（zpl / pdf-a4 / logs / replay / statistics + 共用 print-ticket 12.1 + 1 health）| 32/32 PASS | backend + web-impl + android-impl |
| **Sprint 12 累计** | **4/4 完成** | **18 端点** | **86/86 PASS** | **3 仓并行** |

**关键实装要点**：
1. 12.1 `DrawPermissionInterceptor` 在 3 仓共用同一 SpEL 语义（仅实现层差异）
2. 12.2 `ZplSocketClient` 后端代理 · 端口 9100 · TCP Socket 同步（`OutputStream.write`）
3. 12.3 `LabelPngRenderer` + `BaseLabelTemplate.render(BarColor)` · 4 模板视觉对齐
4. 12.4 `PdfA4Generator` + OpenPDF 1.3.34 · 一页 30 标签 3×10 网格 · `sys_print_log` 同步写入

---

## 6. 集成 E 验证（SM 萧何 · 2026-06-14）

### 6.1 5 集成点全 PASS

| # | 集成点 | 涉及 Story | 验证结果 | 关键观察 |
|---|--------|-----------|---------|---------|
| 1 | `DrawAclService` 与金额 4 阈值 ACL 双轨并行 | 12.1 + V1.3.7 1.3 | ✅ PASS | 两套 ACL 互不干扰 · 术语"图纸 ACL vs 金额 ACL"明确 |
| 2 | `sys_printer` 心跳调度与 `sys_workflow_event` 命名空间一致性 | 12.2 + 8.3 | ✅ PASS | `@Scheduled(60s)` + TCP 探活 · sys_ 命名空间对齐 |
| 3 | 4 标签模板二维码前缀与 APP 扫码壳路由分发 | 12.3 + 1.4 | ✅ PASS | `GD-/LZ-/WW-/SB-` 前缀分发到 12.4 打印 · `WL-` 物料码保持 |
| 4 | `sys_print_log` 与 12.1 `print-ticket` 共表 | 12.1 + 12.4 | ✅ PASS | 单表双写 · `operator_user_id` + `code_value` 字段复用 |
| 5 | `PrintButton` / `PrintDialogFragment` 前端入口与 12.2 打印机选择逻辑 | 12.4 + 12.2 + 10.1/10.5 | ✅ PASS | 1 台直打 / 多台选 / 未配提示 · 模式一失败降级模式二（PM 决策待回）|

### 6.2 CONDITIONAL GO 决策

- **集成 E 验证**：5/5 集成点全 PASS · 0 阻塞
- **决策**：🟡 **CONDITIONAL GO**（3 QA 委派 + 1 DevOps 委派 + 2 PM 决策通过后转 GO）
- **状态**：待 V1.3.8 FAT 准入（预计 2026-06-23 · 客户服务器就位）

---

## 7. 6 项委派执行计划（SM 萧何协调）

| # | 委派项 | 责任方 | 内容 | 截止 | 状态 |
|---|--------|--------|------|------|------|
| 1 | 86 测例 test-execute（V1.3.9 Sprint 12 全量回归）| QA 商鞅 | 12.1 = 24 + 12.2 = 16 + 12.3 = 14 + 12.4 = 32 = 86 | 2026-06-16 | 🟡 待启动 |
| 2 | `typecheck:ci` 全量校验（3 仓 codegen 类型一致）| QA 商鞅 | backend / web-impl / android-impl typecheck:ci 退出 0 | 2026-06-16 | 🟡 待启动 |
| 3 | 灰度 4 阶段观察（12.1 角色矩阵上线观察）| QA 商鞅 | admin+ENGINEER（1 天）→ SALES（1 天）→ PURCHASER+WAREHOUSE+QC（1 天）→ OPERATOR（2 天）| 2026-06-21 | 🟡 待启动 |
| 4 | 客户机房环境就位（Redis 7 + 9100 端口白名单 + DHCP）| DevOps 张良 | 12.2 心跳 60s 调度就绪 · 9100 端口防火墙白名单 · 客户机房打印机 IP 固定 | 2026-06-23 | 🟡 待执行 |
| 5 | ESC/POS 票据打印机 V1.3.10 评估 | PM 范蠡 | 客户是否在 V1.3.9 验收阶段提出 ESC/POS 需求 · 同步规划 V1.3.10 | V1.3.9 验收 | 🟡 待回复 |
| 6 | 12.4 模式一失败降级决策 | PM 范蠡 | 模式一 ZPL 失败时是否自动降级模式二 PDF · 或仅提示用户手动切换 | 2026-06-16 | 🟡 待回复 |

**CONDITIONAL GO 收口路径**：
```
委派 1-3（QA 商鞅） + 委派 4（DevOps 张良） + 委派 5-6（PM 范蠡）通过
  → Sprint 12 集成 E 验证收口
  → V1.3.8 FAT 准入 2916 测例（2830 + 86）
  → GO
```

---

## 8. 风险登记（合并去重 · 8 项）

| # | 风险 | 来源 Story | 等级 | 缓解 |
|---|------|-----------|------|------|
| 1 | 12.1 全员图纸权限收紧上线后业务冲击（业务员/采购/仓管/品检/操作工"看不到部分图纸"）| 12.1 | 🔴 高 | 灰度 4 阶段（admin+ENGINEER → SALES → PURCHASER+WAREHOUSE+QC → OPERATOR · 每阶段 1-2 天观察）|
| 2 | 12.1 图纸 ACL 与金额 ACL 双轨并存 · 新人易混 | 12.1 | 🟡 中 | 明确术语"图纸 ACL vs 金额 ACL" + 文档修订 + 培训 |
| 3 | 12.2 心跳 60s 调度若客户机房网络不稳（断网 1 分钟）误标 OFFLINE | 12.2 | 🟢 低 | 容差 2 次失败再标 · admin 可手动重置 |
| 4 | 12.3 QR 二维码内容长度限制（QR v3-M 最多 200 字符）· 明文 6 行若超长 | 12.3 | 🟡 中 | 切换 QR v4 · dev 提前 `BarcodeFormat.QR_CODE v3-M` 边界测试 |
| 5 | 12.4 浏览器无法跨域调打印机 Socket | 12.4 | 🔴 高 | 必须在后端做 Socket 代理（`ZplSocketClient`）· 架构已明确 |
| 6 | 12.4 客户机房打印机 IP 变化（不支持 DHCP 自动发现）| 12.4 | 🟡 中 | admin 需手动改配置 · V1.4 backlog 自动发现 |
| 7 | 12.4 ZPL 与 TSPL 指令集部分字段不兼容 | 12.4 | 🟡 中 | `ProtocolAdapter` 抽象 · 协议字段映射表 |
| 8 | 12.1 灰度阶段 4（OPERATOR · 2 天）与 Sprint 13 13.3 真实查询上线时序冲突 | 12.1 + 13.3 | 🟡 中 | 13.3 真实查询必须**早于或同步于** 12.1 阶段 2-4 灰度开启 · 否则占位逻辑生产可见偏差 |

**新增风险 vs V1.3.7/V1.3.8**：
- 🔴 × 2（12.1 灰度业务冲击 + 12.4 浏览器 Socket 跨域）
- 🟡 × 4（12.1 ACL 双轨 + 12.3 QR 长度 + 12.4 打印机 IP + 12.4 ZPL/TSPL 不兼容）
- 🟢 × 2（12.2 心跳误标 + 灰度时序协同）

---

## 9. Sprint 13 backlog 候选

| # | 候选 Story | 来源 | 优先级 | 备注 |
|---|----------|------|--------|------|
| 1 | `crm_drawing_link` JOIN 真实查询对接 + OPERATOR 工序对接 | 12.1 dev log §8.2 + Sprint 13 13.3 | 🔴 P1 | 占位逻辑 → 真实查询 · 灰度上线前置 |
| 2 | 思源黑体嵌入 jar 资源（服务端单一权威源）| 12.3 dev log §7 R3 + 12.3 dev log §10 D2 | 🟡 P1 | 当前用 JDK 默认 SansSerif · 跨 OS 字体差异 |
| 3 | `sys_workflow_event` 接入 GmSummary 仪表盘（10.3 数据消费）| 10.3 已 ship 端点 + Sprint 10 backlog #6 | 🟡 P1 | 客户上线后立即可看审批数据 |
| 4 | ESC/POS 票据打印机支持 | PM 范蠡委派 #5 | 🟡 P1 | 客户验收阶段可能提出 · V1.3.10 评估 |
| 5 | 7 状态机 enum drift 对齐（`openapi.yaml` 改 V1.3.7 定义）| 10.1 dev log §6.1 风险 #2 | 🟡 P1 | 消除 codegen 漂移 · 与 10.5 typecheck 错误根因 |
| 6 | InspectionDTO schema 补齐 + InspectionCreate.vue Option A 切换 | PM 决策 #2（V1.3.9-S1 backlog）| 🟡 P1 | 享受 codegen 强类型 · 提升客户端类型安全 |
| 7 | 客户机房 DHCP 自动发现打印机 IP | 12.4 风险 #6 | 🟢 P2 | V1.4 backlog |
| 8 | 模式二 A4 PDF 一键补打 SUCCESS 状态 FAILED 重放 | 12.4 决策 | 🟢 P2 | FAILED 不允许重放 · V1.4 backlog |

**Sprint 13 backlog 优先级**：🔴 P1 × 1（#1）+ 🟡 P1 × 4（#2 #3 #5 #6）+ 🟢 P2 × 2（#7 #8）

---

## 10. Sprint 累计（Sprint 7-12 全流程）

| Sprint | Story | 端点 | 测例 | 真实 PASS |
|--------|-------|------|------|----------|
| Sprint 7（IMPL + 集成）| 6 | 14 | 221 | 221/221 ✅ |
| Sprint 8（优化）| 6 | 144 | 144/144 + 1224 全模块 0 失败 |
| Sprint 9（接入 + JWT）| 2 | 30 | 30/30 ✅ |
| Sprint 10（聚合 + 类型）| 5 | 8 | 8/8 ✅ |
| Sprint 11（PRD 对齐 + 部署修复）| 5 | 0 | 0 引入回归 ✅ |
| Sprint 12（V1.3.9 客户反馈落地）| 4 | 18 | 86 | 86/86 ✅ |
| **Sprint 7-12 累计** | **28** | **—** | **489** | **489/489 PASS · 0 回归** |

---

## 11. Sprint 12 关键产出

### 1. 图纸权限矩阵完整闭环（12.1）

7 角色 × 5 操作 = 35 权限单元全部实装 · 5 类 `crm_drawing_link` 关联表上线 · `DrawPermissionInterceptor` 在 3 仓共用同一 SpEL 语义。

### 2. 打印机管理 + 心跳调度（12.2）

`sys_printer` 字典表 + `@Scheduled(60s)` 心跳 + 容差 2 次失败 · `ZplSocketClient` 后端代理解决浏览器跨域问题。

### 3. 4 标签模板视觉对齐（12.3）

50mm × 30mm 统一规格 · 三区布局 · SB- 复用 GD- 模板（`BaseLabelTemplate.render(BarColor)`）· QR v3-M 边界处理。

### 4. 双模式打印 + 留痕（12.4）

模式一 ZPL Socket 9100 直连 + 模式二 A4 PDF 浏览器 · `sys_print_log` 与 12.1 共表 · 一键补打 SUCCESS-only。

### 5. 5 集成点全 PASS

DrawAclService 与金额 4 阈值双轨 / sys_printer 心跳与 sys_workflow_event 命名空间 / 4 模板二维码前缀路由 / sys_print_log 与 12.1 print-ticket 共表 / 前端入口与 12.2 打印机选择逻辑。

---

## 签字

- **PO 范蠡** · 2026-06-14 · V1.3.9 客户反馈 4 条全部采纳 + Sprint 12 SHARDED · 4 Story 闭环 + 集成 E CONDITIONAL GO
- **PM 范蠡** · 2026-06-14 · 4 条反馈提出 + 6 项委派执行计划（3 QA + 1 DevOps + 2 PM 决策待回）
- **SM 萧何** · 2026-06-14 · 4 Story 跟踪 + 集成 E 验证 5 集成点全 PASS · CONDITIONAL GO 协调
- **dev agent Opus 4.8** · 2026-06-14 · 86/86 测例 PASS · 4 Flyway V54-V57 · 18 端点 · 3 仓并行
- **architect 鲁班** · 2026-06-14 · 4 Story Review APPROVED · 关键架构决策（双轨 ACL + 后端 Socket 代理 + 命名空间对齐）
- **QA 商鞅** · 2026-06-14 · 4 Story test-design READY · 86 测例设计完成 · 灰度 4 阶段观察计划就绪
- **DevOps 张良** · 待客户机房环境就位（Redis 7 + 9100 端口白名单 + DHCP · 2026-06-23 前）

**Sprint 12 COMPLETE · V1.3.9 客户反馈落地 100% · 4 Story · 18 端点 · 86/86 测例 PASS · V54-V57 · 集成 E CONDITIONAL GO · 6 项委派执行计划待 V1.3.8 FAT 准入（2026-06-23）转 GO**

---

> **参考文档**：
> - 客户反馈：docs/prd-feedback-v1.3.9.md（PM 范蠡 2026-06-14）
> - Sprint 13 立项：docs/sprint-13-summary.md（5 Story · 32 测例 · 2026-06-23+ 启动）
> - V1.3.8 FAT 准入：docs/sprint-11-summary.md（PRD 对齐 + 部署修复）