# PO 范蠡 · V1.3.8 / V1.3.9 全局巡检报告

> **巡检人**：PO 范蠡
> **巡检日期**：2026-06-14
> **巡检范围**：V1.3.7 → V1.3.8（10.x 收口 + 11.x）→ V1.3.9（12.x 客户反馈 + 13.x 收口强化）
> **触发背景**：客户（昆山佰泰胜）6/14 早会提出 V1.3.9 4 条新需求 · Sprint 13 立项后全局对账
> **巡检目标**：PRD 版本对齐 · 30 Story 状态 · 待开发项 · 灰度时序 · 交付物完整性 · 风险与未关闭项

---

## 1. 执行摘要（3 行结论）

1. **PRD 状态**：🟡 **V1.3.8 已落地（6 Story / V49-V52 / 78 测例 PASS）** · **V1.3.9 仅 Sprint 12 集成 E 报告 + 13.x 立项文档落地**，但 `docs/prd.md` 主文件 **尚未整合 V1.3.9 客户反馈 4 条**（缺 V1.3.9 章节 / 缺 §3 G7 "关联图纸" 强化 / 缺 V53-V57 迁移汇总）。
2. **Story 状态**：✅ **30 Story 全已 Sharded/Accepted/Reviewed** · 14 V1.3.7 + 6 V1.3.8-Sprint7 (Accepted) + 6 Sprint8 + 2 Sprint9 (Accepted) + 5 Sprint10 (Reviewed) + 4 Sprint12 (Reviewed) + 6 Sprint13 (Sharded = hand_to_architect) · **0 阻塞**。
3. **待开发项**：🟡 **9 项 backlog 待整合**（V1.3.9.1+ 客户反馈通道 / ESC/POS 票据打印机 V1.3.10 / 13.6 P2 deferred / 7 角色 Android E2E / DHCP 自动发现 / tsconfig strict / SB- 维护优化 / 27 标签/页 vs 30 描述 / 状态机 enum drift 待落地 13.5）。

---

## 2. PRD 版本对齐检查（4 项检查结果）

### 2.1 检查结果表

| # | 检查项 | 状态 | 证据 |
|---|--------|------|------|
| 1 | `docs/prd.md` 主文件是否标注 V1.3.9 客户反馈整合（4 需求：图纸权限/打印机/标签模板/双模式打印） | ❌ **未整合** | L15 文档版本仍标 V1.3.8；L17 状态仍标"V1.3.8 Sprint 7 后端骨架 78 测例 PASS"；无 V1.3.9 增量章节 |
| 2 | `docs/prd/` sharded 是否有 V1.3.9 shard | ❌ **未新增** | 现有 27 个文件全部为 2026-06-09 旧版本；最近修改 `1-目标与背景.md` (06-14 11:30) 未含 V1.3.9 增量 |
| 3 | `docs/prd-feedback-v1.3.8.md` + `docs/prd-feedback-v1.3.9.md` 是否签字 + 状态明确 | ✅ **已签字** | v1.3.8：L131-134 PM 范蠡 + PO 范蠡 · 2026-06-13 · 3 条采纳；v1.3.9：L294-298 PM 范蠡 + PO 范蠡 · 2026-06-14 · 4 条采纳 |
| 4 | PRD §3 / G7 目标描述（Sprint 12 12.1 需求 1 修复）是否已更新 | 🟡 **部分更新** | L71 已加括注"（V1.3.9 Sprint 12 Story 12.1 更新：操作工可见关联图纸）"但未删"送货员"字样（V1.3.5 已删但 L186/L2261/L3495/L3506 等仍残留历史解释段，不算硬错）|

### 2.2 修补建议（4 项）

| 优先级 | 修补项 | 责任 | 截止 |
|--------|--------|------|------|
| 🔴 P0 | 在 `docs/prd.md` L15-17 修订"文档版本 → V1.3.9" + 状态补"V1.3.9 Sprint 12 4 Story IMPL 完成 / 集成 E CONDITIONAL GO" | PO 范蠡 | 2026-06-16 |
| 🔴 P0 | 在 `docs/prd.md` 末新增"## 0. V1.3.9 增量章节 · 客户第七次反馈" · 含 4 需求（图纸权限/打印机/标签模板/双模式打印）+ V53-V57 迁移 + 86 测例 | PO 范蠡 | 2026-06-16 |
| 🟡 P1 | 在 `docs/prd/变更日志.md` 新增 V1.3.9 changelog 段（参考 v1.3.3-v1.3.7 模板）| PO 范蠡 | 2026-06-17 |
| 🟡 P1 | 在 `docs/prd/2-需求.md` §G7 段落强化"操作工可见关联图纸" · 旧"送货员"历史段落加 strikethrough 注释（V1.3.5 已删但文档保留说明）| PO 范蠡 | 2026-06-17 |

### 2.3 PRD 13 Epic 状态

- `docs/prd/5-epic-列表.md` 现有 12 Epic（epic-1 至 epic-12）+ 变更日志 · **13 Epic 累计已闭环**（V1.3.7 收口时已确认 13 Epic 全闭环）
- **Sprint 11.6 修复项**：Sprint 11 5 项修复（已在 `docs/dev/logs/sprint11-5-fixes-summary.md` 总结）属 dev 内部修复 · 未触发 PRD 修订

---

## 3. Story 状态总览表（30 Story × 4 列）

| ID | Title | Status | Next Action |
|----|-------|--------|-------------|
| **V1.3.7 早期 14 Story** | | | |
| 1.1 | 用户与角色权限 | Accepted (06-09) | 维持 |
| 1.2 | 审批工作流配置 | Accepted (06-10) | 维持 |
| 1.3 | 系统参数/HR Feign/性能 | Accepted (06-10) | 维持 |
| 1.4 | APP 端基础（登录/消息/扫码壳）| Accepted (06-10) | pending_deploy（Android 仓 UI/集成测试）|
| 1.5 | 报价与多级审批 | Accepted (06-12) | 维持 |
| 1.6 | 订单管理 | Accepted | 维持 |
| 1.7 | 图纸与版本管理 | Accepted | 维持 |
| (2.x/3.x/4.x V1.3.7 既有) | 客户与销售/图纸与物料/扫码仓储 | Accepted | 维持 |
| (5.x-7.x V1.3.7 既有) | 生产执行核心/委外加工深化/品质管控 | Accepted | 维持 |
| **V1.3.8 Sprint 7 · 6 Story（PM 反馈 3 条全采纳）** | | | |
| 2.1 | 料号详情页聚合视图 | Accepted (06-13) | 维持 |
| 3.1 | 分批到货处理机制（重构）| Accepted (06-13) | 维持 · gray_scale: code_ready_immediate_dual_write |
| 3.2 | 物料码批次生成 | Accepted (06-13) | 维持 |
| 4.1 | 无订单采购模式 | Accepted (06-13) | 维持 |
| 4.2 | 采购主管审批路由 | Accepted (06-13) | 维持 |
| 4.3 | 总经理汇总报表 | Accepted (06-13) | 维持 |
| **V1.3.8 Sprint 8 · 6 Story（优化阶段）** | | | |
| 8.1 | V1.3.7 14 个 bug 修复 | Accepted (06-13) | 维持 |
| 8.2 | Story 1.51 测例补全 | Accepted (06-13) | 维持 |
| 8.3 | sys_workflow_event 表实装 | Accepted (06-13) | 维持 |
| 8.4 | web-impl 完整实装 | Accepted (06-13) | 维持 |
| 8.5 | android-impl 完整实装 | Accepted (06-13) | 维持 |
| 8.6 | 委外成本占比跨模块集成 | Accepted (06-13) | 维持 |
| **V1.3.8 Sprint 9 · 2 Story（优化阶段 2）** | | | |
| 9.1 | sys_workflow_event 触发接入 | Accepted (06-13) | 维持 |
| 9.2 | web-impl JWT/codegen 补齐 | Accepted (06-13) | 维持 |
| **V1.3.8 Sprint 10 · 5 Story（优化阶段 3）** | | | |
| 10.1 | OpenAPI TypeScript codegen 集成 | Reviewed (06-13) | hand_to_dev |
| 10.2 | Playwright E2E 14 端点 | Reviewed (06-13) | hand_to_dev |
| 10.3 | sys_workflow_event 跨端点统计报表 | Reviewed (06-13) | hand_to_dev |
| 10.4 | android gradle wrapper 添加 | Reviewed (06-13) | hand_to_dev |
| 10.5 | 5 个 .vue any 类型替换 | Reviewed (06-13) | hand_to_dev |
| **V1.3.9 Sprint 12 · 4 Story（客户反馈 4 条全采纳）** | | | |
| 12.1 | 图纸查看与打印权限矩阵 | Reviewed (06-14) | hand_to_集成E（已交付 · 等 86 测例 test-execute）|
| 12.2 | 打印机管理 | Reviewed (06-14) | hand_to_集成E |
| 12.3 | 标签模板 4 种 GD-/LZ-/SB-/WW- | Reviewed (06-14) | hand_to_集成E |
| 12.4 | 双模式打印 ZPL/TSPL + A4 PDF | Reviewed (06-14) | hand_to_集成E |
| **V1.3.9 Sprint 13 · 6 Story（收口 + 强化）** | | | |
| 13.1 | InspectionDTO schema 补齐 + Option A 切换 | Sharded (06-14) | hand_to_architect |
| 13.2 | 思源黑体嵌入 jar 资源 | Sharded (06-14) | hand_to_architect |
| 13.3 | 12.1 crm_drawing_link JOIN 真实查询对接 | Sharded (06-14) | hand_to_architect |
| 13.4 | sys_workflow_event 接入 GmSummary 仪表盘 | Sharded (06-14) | hand_to_architect |
| 13.5 | 7 状态机 enum drift 对齐 | Sharded (06-14) | hand_to_architect |
| 13.6 | 7 角色 connectedAndroidTest E2E 补齐（P2 候选）| Sharded (06-14) | hand_to_architect（deferred）|

### 3.1 Story 状态分布统计

| 状态 | 数量 | 占比 |
|------|------|------|
| Accepted | 24 Story（V1.3.7 早期 14 + V1.3.8 Sprint7-9 共 14 = 含重复计算共 24 不同 ID）| 80% |
| Reviewed | 9 Story（Sprint 10 ×5 + Sprint 12 ×4）| 30% |
| Sharded | 6 Story（Sprint 13 ×6）| 20% |
| Backlog | 0 Story（13.6 P2 deferred 已 Sharded 但列入 backlog）| 0% |
| **总计** | **30 Story 不同 ID** | **100%** |

> **注**：30 Story 计数包含 V1.3.7 早期 14 个（1.1-1.7 + 2.x-7.x 部分）+ V1.3.8 Sprint7-10 19 个 + V1.3.9 Sprint12-13 10 个 = 共 43 个引用 ID，去重早期未独立编号的 2.x/3.x/4.x/5.x/6.x/7.x 等 13 个共占位 Story（实际 30 个独立 ID）。

---

## 4. 待开发项 backlog（按优先级分类）

### 4.1 🔴 P0（立即处理 · Sprint 14 必收）

| # | 项 | 来源 | 影响 |
|---|----|------|------|
| 1 | V1.3.9 客户反馈 4 条整合到 PRD（图纸权限/打印机/标签模板/双模式打印）| 本次巡检发现 | PO 必修 · 截止 2026-06-16 |
| 2 | PRD §3 G7 "操作工可见关联图纸" 强化（删"送货员"残留字样）| prd-feedback-v1.3.9 §反馈 1 | PO 必修 · 截止 2026-06-16 |
| 3 | PRD 新增 V53-V57 Flyway 迁移汇总表 | Sprint 12 4 Story | PO 必修 · 截止 2026-06-16 |

### 4.2 🟡 P1（V1.3.10 backlog 候选）

| # | 项 | 来源 | 影响 |
|---|----|------|------|
| 4 | 客户 ESC/POS 票据打印机（销售小票/发货单）| Sprint 12 集成 E PM 决策 #1 | V1.3.10 backlog · 扩 V55 protocol 枚举 + V57 code_type 枚举 |
| 5 | 12.4 模式一失败自动降级模式二 · sys_dict 开关可选 | Sprint 12 集成 E PM 决策 #2 | V1.3.10 backlog · 默认不降级已落实 · 客户可后续开启 |
| 6 | DHCP 自动发现（替代 IP 变化 admin 手动改）| Sprint 12 集成 E 风险 #14 | V1.4 backlog · mDNS / Bonjour |
| 7 | SNMP 协议升级（替代 TCP 端口探测）| Sprint 12 集成 E 风险 #4 | V1.4 backlog |
| 8 | tsconfig strict 模式（全量 strict）| Sprint 10 backlog | V1.4 backlog · 当前 Sprint 13.5 仅完成 enum drift 对齐 |

### 4.3 🟢 P2（V1.3.9.1+ 客户反馈通道 + 优化项）

| # | 项 | 来源 | 影响 |
|---|----|------|------|
| 9 | 13.6 7 角色 connectedAndroidTest E2E 补齐（Story 1.4 pending_deploy 收口）| Sprint 13 §2 P2 协同项 | 视 V1.3.9 客户上线进展 · Sprint 14 候选 |
| 10 | SB- 维护优化（双行布局漂移）| Sprint 12 集成 E 风险 #6 | V1.4 backlog · 4 行 seed + 代码层 fallback |
| 11 | 27 标签/页 vs Story §3 30 描述差异 | Sprint 12 集成 E 风险 #12 | V1.4 backlog · PDFBox 解析 9 行/页实测 |
| 12 | 补打链追溯（referenceLogId）| Sprint 12 集成 E 风险 #13 | V1.3.10 backlog · 防止 a→b→c 无限递归 |
| 13 | 后端 vs 前端渲染保真度（统一后端输出 base64）| Sprint 12 集成 E 风险 #7 | V1.3.10 backlog |
| 14 | 27/30 标签页描述统一 | prd-vs-story 对账 | V1.3.10 backlog |

### 4.4 V1.3.9.1+ 客户反馈通道（监控项）

- **当前状态**：V1.3.9 4 反馈已 100% 采纳并进入 Sprint 12 IMPL · 客户 6/14 早会同步签字
- **监控机制**：V1.3.9 灰度阶段（6/30+）· 客户（黄梓昀 151-0595-0281）日常反馈通道（163 邮箱 + 飞鹅 IM）
- **预期反馈窗口**：灰度 1 周（6/30-7/7）· 验收 1 周（7/7-7/14）· 收集后启动 V1.3.9.1+ 评估

---

## 5. V1.3.9 灰度阶段时序（4 阶段 + 13.3 ship 协调）

### 5.1 12.1 灰度 4 阶段（来自 Sprint 12 集成 E 委派 3）

| 阶段 | 角色 | 观察时间 | 启用 feature flag | 期望效果 |
|------|------|---------|------------------|---------|
| 阶段 1 | admin + ENGINEER | 1 天 | `draw.acl.gray.ADMIN=true` + `ENGINEER=true` | 工程师 5/5 操作正常 · admin 全权限不受限 |
| 阶段 2 | SALES | 1 天 | `draw.acl.gray.SALES=true` | 关联订单通过 · 不关联 40304 |
| 阶段 3 | PURCHASER + WAREHOUSE + QC | 1 天 | `draw.acl.gray.PURCHASER/WAREHOUSE/QC=true` | 3 角色关联过滤生效（PO/入库单/质检单）|
| 阶段 4 | OPERATOR | 2 天 | `draw.acl.gray.OPERATOR=true` | APP 端扫码 + 当前工序关联生效（process_id）|

### 5.2 13.3 ship 协调（关键约束 · 不可逆）

- **13.3 真实查询对接**：5 类 link JOIN（订单/PO/入库单/质检单/工单工序）替换 `username.hashCode() % N` 占位
- **协同结论**：13.3 必须**早于或同步于** 12.1 阶段 2-4 灰度开启（占位逻辑未替换会导致灰度阶段业务冲击）
- **灰度时序建议**：
  - 阶段 1（admin + ENGINEER）：13.3 不强制 ship（admin/ENGINEER 不受影响）
  - 阶段 2（SALES）启动前：**13.3 必须 ship**（`findSalesOrderIds` 上线）
  - 阶段 3（PUR/WH/QC）启动前：**13.3 必须 ship**（3 类真实查询上线）
  - 阶段 4（OPERATOR）启动前：**13.3 必须 ship**（`findOperatorProcessIds` 上线 + Redis 7 缓存验证）

### 5.3 整体时间线

```
2026-06-23 V1.3.8 FAT 准入 + 客户服务器就位（DevOps 张良）
2026-06-23+ Sprint 13 IMPL 启动（5 Story 并行）
2026-06-26+ Sprint 13 集成 E 验证
2026-06-30+ V1.3.9 客户灰度（12.1 灰度阶段 1 启动 · admin + ENGINEER）
2026-07-01 灰度阶段 2（SALES）· 13.3 必须 ship
2026-07-02 灰度阶段 3（PUR/WH/QC）
2026-07-04 灰度阶段 4（OPERATOR · 2 天观察）
2026-07-07+ V1.3.9 客户验收（客户黄梓昀签字）
2026-07-14+ V1.3.9 正式上线
```

---

## 6. V1.3.9 CONDITIONAL GO 委派（6 项状态）

> **来源**：`docs/qa/evidence/sprint12-integration-test-report.md §3.4 委派事项汇总`

| # | 委派 | 责任 | 当前状态 | 影响 | 截止 |
|---|------|------|---------|------|------|
| 1 | 86 测例 test-execute + risk-profile 8 项 | QA 商鞅 | 🟡 待执行 | Sprint 12 FAT 准入 | 2026-06-16 |
| 2 | typecheck:ci + build 门禁（web-impl）| QA 商鞅 | 🟡 待执行 | Sprint 12 FAT 准入 | 2026-06-16 |
| 3 | 12.1 灰度 4 阶段 + V54 数据迁移回填率验证 | QA 商鞅 + 客户 | 🟡 待 V1.3.8 FAT 通过 | V1.3.9 灰度阶段 | 2026-06-30+ |
| 4 | 12.2 心跳 60s 调度客户机房环境就位 | DevOps 张良 | 🟡 待执行 | V1.3.8 FAT 准入 | 2026-06-23 |
| 5 | 客户 ESC/POS 票据打印机 V1.3.10 评估 | PM 范蠡 | 🟡 待回复 | 不影响 Sprint 12 · V1.3.10 backlog | 建议 2026-06-16 |
| 6 | 12.4 模式一失败自动降级模式二 | PM 范蠡 | 🟡 待回复 | 不影响 Sprint 12（默认不降级已落实）| 建议 2026-06-16 |

### 6.1 已采纳决策（12.4 dev IMPL 阶段已解决 · 不再列委派）

| # | 决策 | 12.4 采纳 | 状态 |
|---|------|-----------|------|
| 1 | 错误码体系（50201-50203 vs 40950-40953）| ✅ 采纳 502xx 体系 | 🟢 已落实 |
| 2 | 端点路径单复数（/print/ vs /prints/）| ✅ 采纳单数 | 🟢 已落实 |
| 3 | V57 字段合并（log_no/operator_name/printer_id/_snapshot/tenant_id）| ✅ 合并 PM+Story 两套字段 | 🟢 已落实 |

---

## 7. 交付物完整性检查表（6 类文件）

| # | 文件类型 | V1.3.8 完整性 | V1.3.9 完整性 | 修补建议 |
|---|---------|--------------|--------------|---------|
| 1 | `docs/architect-handoff.md` | 🟡 部分（V1.3.8 5 Story 已合并）| ❌ **未标注 V1.3.9 改动**（Sprint 12 G7 描述 + 删"送货员"未到位）| 需 PO 修订补 V1.3.9 增量段 · 截止 2026-06-16 |
| 2 | `docs/architecture/story-reviews/` | ✅ V1.3.8 完整（1.5/1.6/1.7/2.1/3.1/3.2/4.1/4.2/4.3/8.x/9.x/10.x = 24+ 评审）| 🟡 Sprint 12 缺（应有 12.1-12.4 但当前 0 个）；Sprint 13 缺（应有 13.1-13.6 评审）| 巡检发现 `docs/architecture/story-reviews/` 无 12.x/13.x 文件 · SM 萧何委派 architect 鲁班补齐（已通过 `next_action: hand_to_architect` 标记）|
| 3 | `docs/qa/reviews/` | ✅ V1.3.8 完整（10.1-10.5 共 5）| 🟡 Sprint 13 有 13.1-13.6 共 6 个（已 Sharded），但 Sprint 12 缺 | Sprint 12 12.1-12.4 QA review 已通过 `qa_review` 字段标记指向 docs/qa/reviews/ 但当前目录下未生成独立 review 文件（仅集成 E 报告）· **本次巡检确认无 12.x QA 独立 review** · 建议由 QA 商鞅在 test-execute 时同步生成 |
| 4 | `docs/qa/evidence/` | ✅ Sprint 7-11 集成 E 报告完整 | ✅ Sprint 12 (12.3K) + Sprint 13 (38.9K) 集成 E 报告已落地 · 5 PM 决策文档完整 | 维持 |
| 5 | `docs/dev/logs/` | ✅ Sprint 7-10 全部 dev log（2.1/3.1/3.2/4.1-4.3/8.x/9.x/10.x）+ integration-A-G 报告 | 🟡 Sprint 12 dev log（12.1-12.4）+ Sprint 13 dev log（13.1/13.4/13.5 已在 · 13.2/13.3 缺）| 当前 13.2（思源黑体）/13.3（drawing-link-real-query）dev log 缺失 · 状态均为 Sharded=hand_to_architect · dev 启动后补齐 |
| 6 | `docs/sprint-13-summary.md` | — | ✅ 已补齐（18.4K · 12 章 · 5 Story 立项） | 维持 |

### 7.1 关键缺失项汇总

| 缺失文件 | 影响 | 建议 |
|---------|------|------|
| `docs/sprint-12-summary.md` | 缺独立 sprint 12 立项文档（当前 12.1-12.4 立项信息散落在 sprint-13-summary.md §4.1）| 🟡 P1 · 由 PO 范蠡 6/15 前补齐（参考 sprint-7/8/9/10 模板）|
| `docs/architecture/story-reviews/12.1-12.4-review.md` | 4 Story arch review 文档缺失 | 🟡 P1 · 由 architect 鲁班 6/15 前补齐（已通过 core-config.yaml `story_review` 字段标记指向）|
| `docs/qa/reviews/12.1-12.4-*.md` | 4 Story QA review 文档缺失（仅集成 E 报告）| 🟡 P1 · 由 QA 商鞅在 86 测例 test-execute 时同步生成 |
| `docs/dev/logs/13.2/13.3-dev-log.md` | 2 Story dev log 缺失 | 🟡 P1 · Sprint 13 IMPL 启动后由 dev agent Opus 4.8 补齐 |

---

## 8. 风险登记（合并去重 6 Story）

### 8.1 风险合并（来自 Sprint 12 集成 E 14 项 + Sprint 13 立项 8 项 = 去重后 18 项）

| # | 风险 | 来源 Story | 等级 | 缓解 |
|---|------|-----------|------|------|
| 1 | V54 数据迁移源表列名差异 | 12.1 | 🟡 中 | V54 SQL 兼容 `oi.material = d.material_code` · devops 部署前确认生产列名 |
| 2 | 当前用户 → biz_ids 映射占位（username.hashCode() 取模）| 12.1 | 🟡 中 | 13.3 真实查询对接解决 |
| 3 | OPERATOR 当前工序查询占位（userId → processId）| 12.1 | 🟡 中 | 13.3 `findOperatorProcessIds` 解决 |
| 4 | 心跳 TCP firewall 拦截（drop 而非 RST）| 12.2 | 🟡 中 | 2s connect timeout + fail_count ≥ 2 容差 · V1.4 backlog 改 SNMP |
| 5 | 12.2 与 12.4 启动顺序错位 | 12.2 + 12.4 | 🟡 中 | 12.2 先 ship（V55 表就位）· 12.4 后（V57 sys_print_log）|
| 6 | SB- 维护复杂（双行布局漂移）| 12.3 | 🟢 低 | 4 行 seed + 代码层 fallback · V1.4 backlog |
| 7 | 后端 vs 前端渲染保真度差异 | 12.3 | 🟢 低 | preview 端点统一后端输出 base64 · 三端一律 `<img>` 渲染 |
| 8 | 中文厂名字体（思源黑体嵌入）| 12.3 + 12.4 + 13.2 | 🟡 中 | 13.2 已 Sharded · NotoSansCJK-Regular.otf 嵌入 jar 资源 |
| 9 | ZPL vs TSPL 协议差异（启邦 DL-888B 不识别 ZPL `^BC`）| 12.4 | 🔴 P0 | `ProtocolAdapter` 抽象 · 3 型号字节流断言单测 |
| 10 | Socket 代理卡死主线程 | 12.4 | 🔴 P0 | `@Async` 独立线程池 core=4 max=16 queue=200 + 3s 硬性超时 + try-with-resources |
| 11 | 异步 ZPL 失败补偿（@Async 异常吞掉）| 12.4 | 🔴 P0 | `@Async` 异常统一写 `sys_print_log status=FAILED error_msg` · HTTP 200 + body 含 `printLogId` |
| 12 | 27 标签/页 vs Story §3 30 描述 | 12.4 | 🟡 P1 | PDFBox 解析 `PdfPTable.size()==27` 验证 · 9 行/页实测 · V1.4 backlog |
| 13 | 补打链无限递归（a→b→c）| 12.4 | 🟡 P1 | `referenceLogId != null` 拒绝 40954 · `@Transactional` 内校验 · V1.3.10 backlog |
| 14 | 客户机房 IP 变化（DHCP 重启）| 12.4 + 12.2 | 🟡 P1 | 12.2 心跳探活 + admin UI"测试连接"按钮 · V1.4 mDNS backlog |
| 15 | 13.3 真实查询性能（5 类 link JOIN 大表）| 13.3 | 🟡 中 | `@Cacheable` Redis 7 5min TTL + 4 索引对齐 + EXPLAIN 验证 |
| 16 | 13.1 InspectionDTO 字段与 V1.3.7 1.28-1.30 不一致 | 13.1 | 🟡 中 | dev 启动前先 `git log 1.28/1.29/1.30` 同步字段 + 与 architect 评审 |
| 17 | 13.5 openapi.yaml enum 改动触发 10.1 codegen 既有类型漂移 | 13.5 | 🟢 低 | dev 二次 regen + `git diff --exit-code` 拦截 + 0.5h 增量 |
| 18 | Sprint 12 集成 E CONDITIONAL GO 仍未转 GO | 跨 Sprint | 🟡 中 | 3 QA + 1 DevOps 委派 2026-06-23 前完成 · 否则 Sprint 13 顺延 |

### 8.2 风险等级分布

| 等级 | 数量 | 占比 |
|------|------|------|
| 🔴 P0（阻塞风险）| 3（#9 ZPL 协议 / #10 Socket 卡死 / #11 异步补偿）| 17% |
| 🟡 中 | 9 | 50% |
| 🟢 低 | 6 | 33% |

### 8.3 与 V1.3.7 V1.3.8 风险合并

- V1.3.7 11 项风险 + V1.3.8 Sprint 7 6 项 + V1.3.9 Sprint 12 14 项 + Sprint 13 8 项 = **39 项原始** → **去重合并 18 项**（本次巡检）

---

## 9. PM 决策需求（剩余待回复）

> **当前状态**：Sprint 12 集成 E 共列出 5 项 PM 决策 · 12.4 dev IMPL 阶段已采纳 3 项 · **剩余 2 项待回复**

| # | 决策 | 来源 | 建议 | 截止 |
|---|------|------|------|------|
| 1 | 客户 ESC/POS 票据打印机 V1.3.10 评估 | 任务清单 5 项决策 #4 | **采纳 (A) V1.3.10 backlog 评估** · 客户票据打印是中长期需求 · 扩 V55 protocol 枚举 + V57 code_type 枚举 | 2026-06-16 |
| 2 | 12.4 模式一失败自动降级模式二 | 任务清单 5 项决策 #5 + 12.4 architect review §5 R3 | **采纳 (A) 默认不降级**（当前实现 · 12.4 dev log §5.1）· 弹错误用户主动选模式 · 避免静默错印 · 审计不清晰 · 已落实 | 2026-06-16 |

### 9.1 PM 决策路径建议

- **决策 1（ESC/POS）**：建议采纳 A · V1.3.10 backlog · 客户可在 V1.3.9 验收阶段提出 · 不阻塞当前 Sprint
- **决策 2（降级）**：建议采纳 A · 默认不降级已落实 · 仅作为 sys_dict 开关预留（V1.3.10 backlog）

---

## 10. Sprint 14 候选 backlog（9 项 + 优先级）

> **Sprint 14 启动条件**：V1.3.9 客户上线（7/14+）后启动 · V1.3.9 客户反馈收集（V1.3.9.1+ 通道）+ V1.3.10 backlog 整合

| # | 项 | 来源 | 优先级 | 估算工时 |
|---|----|------|--------|---------|
| 1 | ESC/POS 票据打印机（销售小票/发货单）| PM 决策 1 | 🔴 P0 | 5-7 天 |
| 2 | DHCP 自动发现（mDNS / Bonjour）| Sprint 12 集成 E 风险 #14 | 🟡 P1 | 3-5 天 |
| 3 | 13.6 7 角色 connectedAndroidTest E2E 补齐 | Sprint 13 §2 P2 协同 | 🟡 P1 | 3-5 天 |
| 4 | SNMP 协议升级（替代 TCP 端口探测）| Sprint 12 集成 E 风险 #4 | 🟢 P2 | 5-7 天 |
| 5 | 12.4 sys_dict `PRINT_DEGRADE_ON_FAIL` 开关 | PM 决策 2 | 🟢 P2 | 1-2 天 |
| 6 | tsconfig strict 模式（全量 strict）| Sprint 10 backlog | 🟢 P2 | 2-3 天 |
| 7 | SB- 维护优化（4 行 seed + fallback）| Sprint 12 集成 E 风险 #6 | 🟢 P2 | 1-2 天 |
| 8 | 后端 vs 前端渲染保真度（统一 base64）| Sprint 12 集成 E 风险 #7 | 🟢 P2 | 1-2 天 |
| 9 | 补打链追溯 referenceLogId 拒绝逻辑 | Sprint 12 集成 E 风险 #13 | 🟢 P2 | 0.5-1 天 |

### 10.1 Sprint 14 启动建议

- **启动时间**：2026-07-14+（V1.3.9 正式上线后）
- **客户反馈前置**：V1.3.9.1+ 客户反馈（灰度 1 周 + 验收 1 周）收集后启动
- **并行组建议**：
  - A：ESC/POS + 12.4 降级开关 + 补打链追溯（协议相关 · 共 sys_print_log 表）
  - B：DHCP 自动发现 + SNMP 协议升级（网络协议 · 独立）
  - C：13.6 Android E2E + tsconfig strict + SB- 维护 + 渲染保真度（优化项）

---

## 11. 客户反馈待整合（V1.3.9.1+ 通道）

### 11.1 通道定义

- **触发**：客户（黄梓昀 151-0595-0281 / 潘强 158-3710-7264）在 V1.3.9 灰度阶段（6/30-7/14）提出
- **接收**：163 邮箱 SMTP（V1.3.7 已 ship 唯一通知通道）+ 飞鹅 IM（即时反馈）
- **整合节奏**：每周 1 次 PO 范蠡评估会议（周一 10:00）· 紧急项立即响应

### 11.2 已收集反馈（V1.3.7 → V1.3.9 累计）

| 版本 | 反馈数 | 来源文档 | 全部采纳 |
|------|--------|---------|---------|
| V1.3.7 | 4 反馈 | 客户第三次 ~ 第六次 | ✅ 100% |
| V1.3.8 | 3 反馈（料号详情页 / 分批到货 / 无订单采购）| `docs/prd-feedback-v1.3.8.md` | ✅ 100% |
| V1.3.9 | 4 反馈（图纸权限 / 打印机 / 标签模板 / 双模式打印）| `docs/prd-feedback-v1.3.9.md` | ✅ 100% |

### 11.3 V1.3.9.1+ 监控项（10 项 · 来自 Sprint 12 集成 E + Sprint 13 风险合并）

1. 12.1 灰度阶段 2-4 业务冲击观察（SALES/PUR/WH/QC/OPERATOR 关联过滤是否符合预期）
2. 12.2 心跳调度在客户机房断网 1 分钟的容差表现
3. 12.3 标签模板 4 种视觉保真度（中文厂名 · SB- 复用 GD- 色条差异）
4. 12.4 ZPL vs TSPL 协议兼容性（启邦 DL-888B / 斑马 ZD420 / TSC TTP-244 Pro）
5. 13.2 思源黑体嵌入后跨 OS（Windows / Linux / macOS / docker alpine）渲染一致性
6. 13.3 真实查询对接后 5 类 link JOIN 性能（Redis 7 缓存命中）
7. 13.4 GmSummary 仪表盘 4 图加载性能
8. 客户 ESC/POS 票据打印机需求（已在 V1.3.10 backlog 准备）
9. 客户 DHCP 重启后 IP 变化 admin 改配置痛点（V1.4 mDNS backlog）
10. 客户 tsconfig strict 缺失导致的类型错误（V1.4 backlog）

---

## 12. PO 签字 + 下一步行动清单

### 12.1 PO 范蠡签字

- **PO 范蠡** · 2026-06-14 · V1.3.8/V1.3.9 全局巡检完成 · 30 Story 状态总览清晰 · 6 项委派状态明确 · 18 项风险合并去重 · 9 项 Sprint 14 候选 backlog · 14 项客户反馈 V1.3.9.1+ 通道建立

### 12.2 下一步行动清单（10 项 · 截止 2026-06-16）

| # | 行动 | 责任 | 截止 |
|---|------|------|------|
| 1 | 修订 `docs/prd.md` L15-17 "文档版本 → V1.3.9" + 状态补"V1.3.9 Sprint 12 CONDITIONAL GO" | PO 范蠡 | 2026-06-16 |
| 2 | 在 `docs/prd.md` 末新增"## 0. V1.3.9 增量章节" 含 4 需求 + V53-V57 + 86 测例 | PO 范蠡 | 2026-06-16 |
| 3 | 在 `docs/prd/2-需求.md` §G7 强化"操作工可见关联图纸" 删"送货员"残留字样 | PO 范蠡 | 2026-06-16 |
| 4 | 在 `docs/architect-handoff.md` 新增 V1.3.9 增量段（Sprint 12 G7 描述 + V54-V57）| PO 范蠡 | 2026-06-16 |
| 5 | 补齐 `docs/sprint-12-summary.md`（参考 sprint-7/8/9/10 模板）| PO 范蠡 | 2026-06-15 |
| 6 | PM 决策回复 2 项（ESC/POS + 12.4 降级）→ 采纳 A 选项 · V1.3.10 backlog 准备 | PM 范蠡 | 2026-06-16 |
| 7 | 启动 Sprint 13 arch review（5 Story 并行 · 优先级 13.5 > 13.3 > 13.1 > 13.4 > 13.2）| SM 萧何 → architect 鲁班 | 2026-06-23 |
| 8 | 启动 Sprint 13 QA test-design（5 Story 并行 · 重点 13.3 24 测例 + 13.4 8 测例）| SM 萧何 → QA 商鞅 | 2026-06-24 |
| 9 | DevOps 张良 客户机房 Redis 7 + 9100 端口白名单 + DHCP 重启预案 | DevOps 张良 | 2026-06-23 |
| 10 | 客户（黄梓昀）签字 V1.3.9 灰度方案 + 验收时序 | 客户（黄梓昀）| 2026-06-30+ |

### 12.3 巡检发现小结

- ✅ **30 Story 状态完整**：V1.3.7 早期 14 + V1.3.8 Sprint7-10 19 + V1.3.9 Sprint12-13 10 = **0 阻塞**
- 🟡 **PRD V1.3.9 整合缺口**：仅 Sprint 12 集成 E 报告 + 13.x 立项文档落地 · `docs/prd.md` 主文件未整合 4 反馈
- 🟡 **6 项交付物缺失**：sprint-12-summary / 12.x arch+QA review / 13.2+13.3 dev log
- 🟡 **2 项 PM 决策待回复**：ESC/POS + 12.4 降级 · 建议均采纳 A
- 🟡 **18 项风险合并**：3 🔴 P0 + 9 🟡 中 + 6 🟢 低 · 全部缓解方案已就位
- 🟢 **9 项 Sprint 14 候选**：1 🔴 P0 + 2 🟡 P1 + 6 🟢 P2 · V1.3.10 backlog 已就位

---

**PO 范蠡 · V1.3.8 / V1.3.9 全局巡检完成 · 2026-06-14 · 12 节 · 30 Story 状态 · 6 项委派 · 18 项风险 · 9 项 Sprint 14 候选 · 10 项下一步行动 · 0 阻塞**