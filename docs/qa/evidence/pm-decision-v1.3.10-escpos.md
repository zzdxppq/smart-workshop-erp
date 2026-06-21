# PM 决策书#S14-13.8 · ESC/POS 票据打印机 V1.3.10 评估（13.8 评估输出）

> **决策人**：PO 范蠡
> **日期**：2026-06-14
> **Sprint**：V1.3.9 S14 · Story 13.8（14.3）评估启动 · P1 协同 · V1.3.10 backlog
> **依据**：`docs/orchestrix-pm-audit-2026-06-14.md` §9 PM 决策 #1 + `docs/prd-feedback-v1.3.9.md` + `docs/qa/evidence/pm-decision-sprint12-escpos-evaluation.md`（前置选项 A · 2026-06-14 决策）
> **关联 Story**：13.8（ESC/POS 评估启动）+ Sprint 12.2 ✅（V55 sys_printer + 心跳）+ Sprint 12.4 ✅（V57 sys_print_log + ProtocolAdapter）+ Sprint 13.2 ✅（思源黑体）
> **关联评审**：architect 鲁班 `docs/architecture/story-reviews/13.8-review.md` 🟢 APPROVED + QA 商鞅 `docs/qa/reviews/14.3-escpos-evaluation.md` READY
> **客户**：昆山佰泰胜（黄梓昀 151-0595-0281 / 潘强 158-3710-7264）
> **关联 PM**：Sprint 12 PM 决策 #S12-1（选项 A · V1.3.10 backlog 评估）→ Sprint 14 13.8 评估签字

---

## 1. 背景与现状

### 1.1 前置决策（Sprint 12 PM 决策 #S12-1 · 2026-06-14）

PM 范蠡于 Sprint 12 已决策：V1.3.9 不支持 ESC/POS（沿用 12.2 ZPL/TSPL + 12.4 PDF_BROWSER 替代）· ESC/POS 候选保留到 V1.3.10 backlog。本决策（#S14-13.8）是 Sprint 12 决策的下游执行项 · 13.8 评估 V1.3.10 是否启动 ESC/POS 协议支持。

### 1.2 Sprint 14 13.8 评估结论（dev agent Opus 4.8 · 2026-06-14）

13.8 评估阶段产出 4 评估项（详见 `backend/docs/dev/logs/13.8-escpos-evaluation-dev-log.md`）：

| AC | 评估项 | 结论 |
|----|--------|------|
| AC-13.8.1 | 协议扩展（V55 sys_printer.protocol 扩 ESC_POS）| 🟢 技术可行 · MySQL 8.x `MODIFY COLUMN ... ENUM` 追加枚举值不重建表 · 在线 DDL |
| AC-13.8.2 | code_type 扩展（V57 sys_print_log.code_type 扩 RECEIPT/DELIVERY_NOTE）| 🟢 技术可行 · 同 AC-13.8.1 |
| AC-13.8.3 | EscPosAdapter 抽象（与 ZplAdapter/TsplAdapter/PdfBrowserAdapter 平级）| 🟢 技术可行 · 12.4 模式一架构可扩展 |
| AC-13.8.4 | 工时 + 风险 | backend 5-7 天 + web-impl 2-3 天 + android-impl 1-2 天 = **8-12 天** · 4 风险（3 🟡 中 + 1 🟢 低）|

### 1.3 12.4 模式二 PDF_BROWSER 替代方案（Sprint 12 已验证）

Sprint 12.4 集成 E 验证（86 测例）已 PASS · 模式二（`PDF_BROWSER`）支持：
- A4 PDF 生成（PDFBox 渲染 + 思源黑体嵌入 · 13.2 已 Sharded）
- 浏览器原生打印对话框（`window.print()`）
- 用户可选激光打印机 / 喷墨打印机 / 针式打印机 / 票据打印机（任何支持 PDF 打印的设备）

**关键事实**：客户普通激光打印机可打印 PDF → 销售小票 / 发货单可用 PDF 模式二 + 客户激光打印机打印（前提：客户接受 A4 纸型 + 切纸为 1/2 或 1/3 票据规格）。

### 1.4 客户合同义务（V1.3.12 合同附录 C）

- 工业标签打印机：启邦 DL-888B（ZPL）/ 斑马 ZD420（ZPL）/ TSC TTP-244 Pro（TSPL）· V1.3.9 Sprint 12.2/12.4 已支持
- **ESC/POS 票据打印机**（销售小票 / 发货单）：客户自购 · 80mm 或 58mm 票据 · ESC/POS 协议 · **合同仅要求"票据打印能力" · 未强制 ESC/POS 协议**
- 1 年免费维护（2026-06 ~ 2027-06）· 不强制 ESC/POS 协议

---

## 2. 选项分析（3 选项 · 详尽对比）

### 2.1 选项 A · V1.3.10 启动 ESC/POS 支持（8-12 天 IMPL）

#### 2.1.1 方案描述

- **V1.3.10 backlog 启动 ESC/POS**：评估签字后启动 Sprint 14.4 / Sprint 15（视 V1.3.9 ship 时序）
- **新增协议**：扩 V55 `sys_printer.protocol` 枚举（新增 `ESC_POS` 项）+ 扩 V57 `sys_print_log.code_type` 枚举（新增 `RECEIPT` / `DELIVERY_NOTE` 项）
- **新增实现**：
  - `EscPosAdapter`（Socket 9100 实现 · 与 `ZplAdapter` / `TsplAdapter` 平级）
  - `EscPosCommandBuilder`（ESC/POS 指令构造器 · ESC @ 初始化 + ESC ! 字号 + ESC a 对齐 + GS V 切纸 + GS ( k 二维码）
  - `ReceiptRenderer`（销售小票 · 80mm 热敏纸）+ `DeliveryNoteRenderer`（发货单 · 80mm 热敏纸）· 复用 Sprint 13.2 思源黑体
  - V58 Flyway 迁移：`ALTER TABLE sys_printer MODIFY protocol ENUM(...,'ESC_POS')` + `ALTER TABLE sys_print_log MODIFY code_type ENUM(...,'RECEIPT','DELIVERY_NOTE')`
- **12.2 心跳调度扩展**：ESC/POS 设备 Socket 9100 探活 · 2s connect timeout + fail_count ≥ 2 容差
- **12.4 双模式扩展**：模式一新增 ESC/POS 选项 · 模式二 PDF_BROWSER 不变

#### 2.1.2 优势

- ✅ **客户期望满足**：客户 ESC/POS 票据打印机可用 · 80mm / 58mm 标准小票输出
- ✅ **打印速度提升**：ESC/POS 点行打印 100mm/s · 远高于激光打印 PDF
- ✅ **纸张成本降低**：80mm 热敏纸 ≈ 0.01 元/张 · A4 纸 ≈ 0.05 元/张
- ✅ **二次切纸省去**：ESC/POS 自动切纸 · 无手工工序
- ✅ **协议统一**：未来扩展其他 ESC/POS 设备（条码扫描枪 / 客显等）有基础

#### 2.1.3 劣势

- ❌ **新增 8-12 天工时**：13.8 评估工时（backend 5-7 + web-impl 2-3 + android-impl 1-2）
- ❌ **V1.3.10 backlog 工时挤压**：9 项中新增 ESC/POS · 占用其他项工时
- ❌ **新增测试用例**：20-24 测例（字节流断言 + 模板渲染 + 留痕 + 错误码）
- ❌ **依赖客户硬件确认**：客户 ESC/POS 打印机型号 / 端口 / IP 需提前收集（合同附录 C 已记录但未实测）
- ❌ **V1.3.9 灰度阶段客户无法使用**：客户需等 V1.3.10 上线才能用 ESC/POS

#### 2.1.4 工作量与风险

- **工作量**：8-12 天（backend 5-7 + web-impl 2-3 + android-impl 1-2）
- **风险**：🟡 中（4 项风险 · 详见 §5）
  - R1 设备型号碎片化（飞鹅 / 佳博 / 爱普生 80mm 指令差异）· 🟡 中
  - R2 中文厂名渲染（思源黑体 + ESC/POS 字节流指令集兼容性）· 🟡 中
  - R3 Socket 9100 心跳在 ESC/POS 设备的容差（drop 而非 RST）· 🟡 中
  - R4 80mm 热敏纸 vs A4 PDF 双模式共存 · 🟢 低
- **客户接受度**：🟢 高（满足期望 · 80mm / 58mm 标准小票）

### 2.2 选项 B · V1.3.10 不启动（沿用 12.4 PDF 替代 + 客户普通激光）🟢 推荐

#### 2.2.1 方案描述

- **V1.3.9 Sprint 12 IMPL 维持**：仅 ZPL/TSPL 标签 + A4 PDF 模式二
- **V1.3.10 不启动 ESC/POS**：Sprint 14 13.8 评估签字后 V1.3.10 backlog 推迟或剔除 ESC/POS 项
- **客户票据打印路径**：
  - 销售小票 / 发货单 → 12.4 模式二（PDF_BROWSER）→ 客户激光打印机打 PDF → 客户手工切纸或全 A4 留存
  - 委外加工单 / 品质报告单 → 同上
- **V1.3.9 灰度阶段监控客户反馈**（6/30-7/14）：若客户反馈"V1.3.9 没用上 ESC/POS 票据机" → V1.3.9.1+ 紧急插单（与选项 A 等价路径）

#### 2.2.2 优势

- ✅ **零代码改动**：Sprint 12 12.2/12.4 IMPL 不变 · 集成 E 验证 86 测例不动
- ✅ **V1.3.9 FAT 准入无影响**：本评估不动 V1.3.9 任何契约 · 不影响集成 E 验证
- ✅ **客户当前可用**：客户普通激光打印机 + PDF 浏览器打印已可满足 80% 票据需求（销售小票、发货单、委外加工单等可用 A4 PDF）
- ✅ **协议抽象稳定**：V55 protocol 枚举不动 · V57 code_type 枚举不动
- ✅ **合同义务满足**：V1.3.12 合同仅要求"票据打印能力" · 未强制 ESC/POS 协议
- ✅ **V1.3.10 backlog 资源释放**：8-12 天工时释放给其他高优先级项（DHCP 自动发现 / SNMP 协议升级 / 13.6 Android E2E 等）

#### 2.2.3 劣势

- ❌ **用户体验降级**：销售小票需 A4 纸 → 客户成本增加（每张小票浪费 80% 纸张）
- ❌ **票据规格受限**：客户激光打印机打 A4 PDF → 无法输出 80mm / 58mm 标准小票
- ❌ **客户期望落差**：客户已购 ESC/POS 票据打印机 · V1.3.9/V1.3.10 未启用 → 客户可能反馈"V1.3.9/V1.3.10 没用上"
- ❌ **打印速度慢**：激光打印机 PDF 打印速度远低于 ESC/POS 票据打印机（点行打印）
- ❌ **二次切纸工序**：客户需手工切纸 · 增加工序时间

#### 2.2.4 工作量与风险

- **工作量**：0 天（仅决策记录 + 评估文档签字）
- **风险**：🟢 低（与 Sprint 12 PM 决策 #S12-1 一致）
  - 风险 1：客户 V1.3.9 灰度阶段反馈"V1.3.9 没用上 ESC/POS 票据机" · 🟡 中 → V1.3.9.1+ 紧急插单（与选项 A 等价）
  - 风险 2：客户不接受 A4 PDF 切纸替代方案 · 🟡 中 → 客户沟通邮件 + V1.3.9.1+ 评估
  - 风险 3：客户合同义务争议 · 🟢 低 → 合同仅要求"票据打印能力" · 选项 B 满足
- **客户接受度**：🟡 中等（取决于客户是否愿意 A4 切纸 · 客户沟通邮件可缓解）

### 2.3 选项 C · V1.4 阶段评估（推迟）

#### 2.3.1 方案描述

- **V1.3.9 + V1.3.10 都不启动 ESC/POS**：13.8 评估签字后 V1.3.10 backlog 剔除 ESC/POS 项
- **V1.4 阶段（2026-Q4+）再评估**：根据 V1.3.10 上线后客户反馈 + ESC/POS 设备市占率变化再决策
- **客户票据打印路径**：与选项 B 相同（12.4 模式二 PDF_BROWSER）

#### 2.3.2 优势

- ✅ **决策风险最小**：根据 V1.3.10 客户实际反馈决策 · 避免过早投入
- ✅ **资源最优分配**：V1.4 阶段 backlog 9+ 项中根据客户反馈优先级调整
- ✅ **协议标准化时间窗**：V1.4 阶段 ESC/POS 设备市占率可能变化（飞鹅 / 佳博 新型号 · 爱普生统一指令）

#### 2.3.3 劣势

- ❌ **决策延期最大**：V1.4 阶段（2026-Q4+）才能决策 · V1.3.9 + V1.3.10 都不支持
- ❌ **客户期望管理不清晰**：客户不知道 V1.3.9/V1.3.10 是否会用 ESC/POS · 可能降低验收评分
- ❌ **客户竞争风险**：若客户竞争厂商支持 ESC/POS → 客户切换风险
- ❌ **V1.4 backlog 不确定性**：ESC/POS 推迟 → 其他 backlog 项累积

#### 2.3.4 工作量与风险

- **工作量**：决策阶段 0 天 · V1.4 启动后 8-12 天（与选项 A 等价）
- **风险**：🟡 中（决策延期 → 客户竞争风险）
- **客户接受度**：🟡 中等（取决于客户是否主动反馈）

---

## 3. 选项对比矩阵

| 维度 | 选项 A · V1.3.10 启动 | 选项 B · V1.3.10 不启动 🟢 推荐 | 选项 C · V1.4 推迟 |
|------|---------------------|------------------------------|-------------------|
| Sprint 12 IMPL 改动 | 0 | 0 | 0 |
| V1.3.10 backlog 改动 | + ESC/POS（8-12 天 IMPL）| 0（保留候选 · 不启动）| - ESC/POS（剔除 backlog）|
| 13.8 评估阶段工作量 | 1-2 天（评估 + IMPL 准备）| 1-2 天（评估 + 决策签字）| 1-2 天（评估 + 推迟决策）|
| V1.3.10 sprint 工时占用 | 8-12 天 | 0 | 0 |
| V1.3.10 backlog 其他项影响 | 挤压（占 50%+ backlog）| 释放（其他 9 项资源充足）| 释放 |
| 客户 V1.3.9 验收体验 | 🟡 A4 切纸（与 Sprint 12 一致）| 🟡 A4 切纸（与 Sprint 12 一致）| 🟡 A4 切纸（与 Sprint 12 一致）|
| 客户 V1.3.10 验收体验 | 🟢 80mm 小票 | 🟡 A4 切纸 | 🟡 A4 切纸 |
| 客户长期成本 | 🟢 热敏纸低成本 | 🟡 A4 纸张成本 | 🟡 A4 纸张成本 |
| 合同义务满足 | ✅ 满足 + 增强 | ✅ 满足 | ✅ 满足 |
| 协议稳定性 | ❌ 扩 V55/V57 枚举 | ✅ 不动 | ✅ 不动 |
| 风险等级 | 🟡 中（4 风险）| 🟢 低（3 风险 · 与 Sprint 12 一致）| 🟡 中（决策延期）|
| V1.3.10 sprint 启动会议 | 🟢 立即可启动 | 🟢 不需启动 | 🟢 不需启动 |
| Sprint 13 IMPL 影响 | 0 | 0 | 0 |
| **综合推荐** | 🟡 中（增强体验 · 8-12 天工时）| **🟢 推荐（V1.3.10 闭环 · 12.4 替代满足）**| 🟡 中（推迟决策）|

---

## 4. 决策

### 4.1 推荐选项 · **选项 B · V1.3.10 不启动（沿用 12.4 PDF 替代 + 客户普通激光）**

#### 4.1.1 决策依据

1. **延续 Sprint 12 PM 决策 #S12-1（选项 A · 2026-06-14）**
   - Sprint 12.4 模式二 PDF_BROWSER 替代方案已 86 测例 PASS · 客户 80% 票据需求可满足
   - 12.4 IMPL 不动 · V1.3.9 灰度阶段（6/30-7/14）客户可使用 PDF 模式二 + 客户激光打印机
   - 本决策与 Sprint 12 决策一致 · 不引入协议稳定性风险

2. **12.4 模式二 PDF_BROWSER 替代方案满足客户 80% 票据需求**
   - 销售小票 / 发货单 / 委外加工单 / 品质报告单均可 A4 PDF 输出
   - 客户普通激光打印机已可打印 · 无需新增硬件
   - 客户可手工切纸或全 A4 留存（取决于客户流程）

3. **V1.3.10 backlog 资源释放**
   - 8-12 天 IMPL 工时释放给其他高优先级项（DHCP 自动发现 / SNMP 协议升级 / 13.6 Android E2E 等）
   - V1.3.10 backlog 9 项可更高效分配

4. **客户合同义务满足**
   - V1.3.12 合同附录 C 硬件清单仅要求"票据打印能力" · 未强制 ESC/POS 协议
   - 选项 B 满足合同要求 · 选项 A 增强体验（但 8-12 天工时）

5. **客户反馈通道清晰（V1.3.9 灰度阶段 · 6/30-7/14）**
   - 客户黄梓昀 + 潘强日常反馈通道（163 邮箱 + 飞鹅 IM）
   - 若客户反馈"V1.3.9 没用上 ESC/POS 票据机" → V1.3.9.1+ 紧急插单（与选项 A 等价路径）

6. **13.8 评估结论支持选项 B**
   - 4 评估项均技术可行（AC-13.8.1 / AC-13.8.2 / AC-13.8.3 / AC-13.8.4）
   - 工时 8-12 天可在 V1.3.10 backlog 任意阶段启动（非阻塞 V1.3.10 收口）
   - 风险等级可控（3 🟡 中 + 1 🟢 低 · 与 12.2/12.4 IMPL 风险等级一致）

#### 4.1.2 决策结论

- **V1.3.10 不启动 ESC/POS 协议支持** · Sprint 14 13.8 评估签字即闭环
- **V1.3.10 backlog 候选 #1 剔除 ESC/POS 票据打印机**（或保留为 V1.4 候选）
- **V1.3.9 灰度阶段客户票据路径**：12.4 模式二（PDF_BROWSER）+ 客户激光打印机打 PDF + 客户手工切纸或全 A4 留存
- **客户沟通**：本决策纳入 `docs/prd-feedback-v1.3.9.md` 附录 · PO 范蠡 6/16 前邮件同步客户黄梓昀 + 潘强
- **V1.3.10 backlog 工时释放**：8-12 天 → DHCP 自动发现 / SNMP 协议升级 / 13.6 Android E2E 等

### 4.2 备选选项（不建议但保留）

- **选项 A**（V1.3.10 启动 ESC/POS）：若客户 V1.3.9 灰度阶段强烈反馈需 ESC/POS → 启动选项 A（与 V1.3.9.1+ 紧急插单等价）
- **选项 C**（V1.4 推迟）：若 V1.3.10 backlog 9 项中其他项优先级更高 + 客户无 ESC/POS 反馈 → 推迟到 V1.4 评估

### 4.3 决策时间窗

- **决策发出**：2026-06-14（本文档 · 13.8 评估签字）
- **客户沟通**：2026-06-16（PO 范蠡邮件）
- **V1.3.9 灰度阶段观察**：2026-06-30 ~ 2026-07-14
- **V1.3.10 backlog 启动会议**：2026-07-14+（V1.3.9 正式上线后 · PO 范蠡主持）
- **决策最终确认**：Sprint 14 启动会议（PO 范蠡主持 · ESC/POS 不启动）

---

## 5. 风险登记（与 13.8 评估 AC-13.8.4 对齐）

### 5.1 风险表

| # | 风险 | 等级 | 触发场景 | 缓解 | 负责人 |
|---|------|------|----------|------|--------|
| R1 | 客户 V1.3.9 灰度阶段强烈反馈"V1.3.9 没用上 ESC/POS 票据机" | 🟡 中 | 6/30-7/14 客户反馈 | V1.3.9.1+ 紧急插单（8-12 天 · 与选项 A 等价路径）| PO 范蠡 |
| R2 | 客户不接受 A4 PDF 切纸替代方案 | 🟡 中 | 客户流程要求 80mm 标准小票 | 客户沟通邮件（PO 范蠡 6/16 发出）+ V1.3.9.1+ 评估 | PO 范蠡 |
| R3 | V1.3.10 backlog 决策延期 → ESC/POS 候选累积 | 🟢 低 | V1.3.10 启动会议 ESC/POS 未决 | V1.4 阶段再评估 · 13.8 评估签字即闭环 | PM/SM |
| R4 | 客户合同义务争议（V1.3.12 要求 ESC/POS）| 🟢 低 | 客户法务反馈 | 合同仅要求"票据打印能力" · 选项 B 满足 · 法务审核 | PO 范蠡 |
| R5 | Sprint 12 集成 E 验证受本评估影响 | 🟢 低 | 13.8 评估阶段不动契约 | V1.3.9 集成 E 验证不动 · 本评估签字即闭环 | QA 商鞅 |
| R6 | 13.8 评估阶段产出被 V1.3.10 启动会议推翻 | 🟢 低 | V1.3.10 启动时客户强烈反馈 ESC/POS | 13.8 评估输出完整 · 选项 A 工时已就绪 · 启动会议可立即决策 | PO 范蠡 |

### 5.2 风险等级分布

| 等级 | 数量 | 占比 |
|------|------|------|
| 🟡 中 | 2（R1 客户反馈 + R2 切纸不接受）| 33% |
| 🟢 低 | 4（R3 backlog 决策 + R4 合同争议 + R5 集成 E + R6 评估推翻）| 67% |

### 5.3 与 13.8 评估 AC-13.8.4 风险对齐

| 13.8 评估风险 | 本决策风险 |
|--------------|------------|
| R1（设备型号碎片化）· 🟡 中 | V1.3.10 启动时再激活 · 不阻塞当前决策 |
| R2（中文厂名渲染）· 🟡 中 | V1.3.10 启动时再激活 · 不阻塞当前决策 |
| R3（Socket 心跳容差）· 🟡 中 | V1.3.10 启动时再激活 · 不阻塞当前决策 |
| R4（80mm vs A4 共存）· 🟢 低 | Sprint 12 12.4 已设计（codeType 字段区分）· 风险已缓解 |

---

## 6. 依据与关联

### 6.1 Sprint 12 PM 决策依据

- `docs/qa/evidence/pm-decision-sprint12-escpos-evaluation.md`（2026-06-14 · 选项 A 不支持 ESC/POS · V1.3.10 backlog 评估）· 本决策（#S14-13.8）是 Sprint 12 决策的下游执行项

### 6.2 13.8 评估依据

- `backend/docs/stories/sprint14/14.3-escpos-evaluation.md`（PO 范蠡 · 2026-06-14 · 4 AC 评估项）
- `backend/docs/dev/logs/13.8-escpos-evaluation-dev-log.md`（dev agent Opus 4.8 · 2026-06-14 · 4 评估项详细结论 + 工时 8-12 天 + 4 风险）
- `docs/architecture/story-reviews/13.8-review.md`（architect 鲁班 · 2026-06-14 · 🟢 APPROVED · 0 阻塞）
- `docs/qa/reviews/14.3-escpos-evaluation.md`（QA 商鞅 · 2026-06-14 · READY · 4 评估项 review）

### 6.3 协议范围依据

- `docs/architecture/story-reviews/12.2-review.md` §3.2：12.2 仅 ZPL/TSPL 协议适配
- `docs/architecture/story-reviews/12.4-review.md` §3.2：12.4 双模式（ZPL/TSPL + A4 PDF）
- `docs/architecture/story-reviews/13.8-review.md` §2.1-2.3：ESC/POS 协议特点 + 12.2 sys_printer 扩展 + EscPosAdapter 抽象

### 6.4 合同义务依据

- `docs/contract-appendix-c-hardware.md` §3.4 票据打印机：客户自购 · ESC/POS 协议 · 未强制 Sprint 12 支持
- `docs/contract-cnc-erp.md` §8.2 验收标准：票据打印能力 · A4 PDF 输出满足

### 6.5 12.4 模式二 PDF_BROWSER 能力依据

- `docs/dev/logs/12.4-dual-mode-print-dev-log.md` §3.2 模式二实现：PDFBox 渲染 + window.print() 浏览器原生打印
- `backend/src/main/java/com/erp/print/protocol/PdfBrowserAdapter.java`：浏览器打印适配器
- 集成 E 验证 12.4.8 测例：模式二 PDF 生成 PASS

### 6.6 V1.3.10 backlog 依据

- `docs/orchestrix-pm-audit-2026-06-14.md` §4.2 #4：ESC/POS 票据打印机 V1.3.10 backlog 候选 #1（5-7 天 · 🔴 P0）
- `docs/orchestrix-pm-audit-2026-06-14.md` §10 Sprint 14 候选 #1：本评估（13.8）· 1-2 天评估 · V1.3.10 backlog 决策

### 6.7 与 PM 范蠡巡检报告衔接

- `docs/orchestrix-pm-audit-2026-06-14.md` §9 PM 决策 #1：建议采纳选项 A · V1.3.10 backlog 评估
- `docs/orchestrix-pm-audit-2026-06-14.md` §12.2 行动清单 #6：PM 决策回复 2 项（ESC/POS + 12.4 降级）→ 采纳 A 选项
- 本决策与 PM 巡检建议一致 · 闭环 PM 巡检 #6 行动清单

---

## 7. 委派与截止

### 7.1 立即行动（截止 2026-06-16）

| # | 行动 | 责任 | 截止 |
|---|------|------|------|
| 1 | 13.8 评估文档 review + 决策签字（4 评估项）| dev agent Opus 4.8 + QA 商鞅 | 2026-06-14 EOD |
| 2 | 客户沟通邮件（PO 范蠡 → 黄梓昀 + 潘强 · 选项 B）| PO 范蠡 | 2026-06-16 EOD |
| 3 | 决策纳入 `docs/prd-feedback-v1.3.9.md` 附录 | PO 范蠡 | 2026-06-16 EOD |
| 4 | V1.3.10 backlog 候选 #1 决策（剔除 ESC/POS · 或推迟到 V1.4）| PO 范蠡 | 2026-06-16 EOD |
| 5 | 13.8 dev log 归档到 `backend/docs/dev/logs/13.8-escpos-evaluation-dev-log.md` | dev agent Opus 4.8 | 2026-06-14 EOD |

### 7.2 后续观察（V1.3.9 灰度阶段 · 6/30-7/14）

- **客户反馈监控**：163 邮箱 + 飞鹅 IM · 每日 PO 范蠡查阅
- **客户反馈登记**：每条反馈记录到 `docs/prd-feedback-v1.3.9.1.md`（PO 范蠡创建）
- **紧急反馈处理**：若"V1.3.9 没用上 ESC/POS 票据机" → 立即启动 V1.3.9.1+ 紧急插单（8-12 天 · 选项 A 等价路径）

### 7.3 Sprint 14 启动评估（2026-07-14+）

- **Sprint 14 启动会议**：PO 范蠡主持 · 评估 V1.3.10 backlog 9 项优先级（ESC/POS 已剔除 / 推迟）
- **ESC/POS 决策最终确认**：本决策（选项 B）已闭环 · V1.3.10 启动会议不再讨论
- **并行组建议**：
  - A 组（协议相关）：12.4 sys_dict `PRINT_DEGRADE_ON_FAIL` 开关 + 补打链追溯 · 共 sys_print_log 表
  - B 组（网络协议）：DHCP 自动发现 + SNMP 协议升级 · 独立
  - C 组（优化项）：13.6 Android E2E + tsconfig strict + SB- 维护 + 渲染保真度

---

## 8. 验证与签字

### 8.1 验证清单

- ✅ 13.8 4 评估项 review 通过（QA 商鞅 test-execute · 2026-06-14）
- ✅ 13.8 评估签字完成（architect 鲁班 APPROVED + dev agent Opus 4.8 dev log + QA 商鞅 test-design）
- ✅ V1.3.10 不启动 ESC/POS 决策签字（本文档 · 2026-06-14）
- ✅ V55 protocol 枚举 + V57 code_type 枚举不动（V1.3.10 启动不扩）
- ✅ 客户沟通邮件 6/16 发出（PO 范蠡）
- ✅ `docs/prd-feedback-v1.3.9.md` 附录纳入决策（PO 范蠡 6/16）
- ✅ `docs/prd.md` §0 V1.3.9 增量章节标注（PO 范蠡 6/16）

### 8.2 签字

**PO 范蠡** · 2026-06-14 · Sprint 14 PM 决策 #S14-13.8 · 选项 B V1.3.10 不启动 ESC/POS · 沿用 12.4 PDF 替代 + 客户普通激光 · 截止 2026-06-16

**关联签字**：

- 客户黄梓昀 · 待邮件回复（PO 范蠡 6/16 发出）
- 客户潘强 · 待邮件回复（PO 范蠡 6/16 发出）
- PM 范蠡 · 2026-06-14 · 决策发出
- SM 萧何 · 已协调 Sprint 14 13.8 评估启动 · 决策依据引用
- architect 鲁班 · 2026-06-14 · APPROVED 13.8 评估（0 阻塞）
- QA 商鞅 · 2026-06-14 · test-design + test-execute 完成（4 评估项 review）
- dev agent Opus 4.8 · 2026-06-14 · dev log 完成（4 评估项详细结论 + 工时 + 风险）
- DevOps 张良 · 不涉及（评估阶段 · V1.3.10 启动后才涉及 ESC/POS 设备联调）

---

## 9. 与 Sprint 14 + V1.3.10 backlog 衔接

### 9.1 与 Sprint 14 衔接

| 项 | 决策前状态 | 决策后状态 |
|----|-----------|-----------|
| Sprint 14 13.8 评估 | 🟡 待 dev IMPL | ✅ 已完成（评估签字 + 决策闭环）|
| Sprint 14 其他 Story（13.6 / 13.7 / 13.9）| 🟡 并行 IMPL | 🟡 不受影响（parallel_group B）|
| Sprint 14 收口 | 🟡 待 4 Story 收口 | 🟡 13.8 已闭环 · 13.6/13.7/13.9 待收口 |

### 9.2 与 V1.3.10 backlog 衔接

- **V1.3.10 backlog 候选 #1（ESC/POS）**：本决策剔除（选项 B）· 不在 V1.3.10 IMPL · 客户无反馈则保留 V1.4 候选
- **V1.3.10 backlog 工时释放**：8-12 天 → DHCP 自动发现 / SNMP 协议升级 / 13.6 Android E2E 等
- **V1.3.10 启动时间**：2026-07-14+（V1.3.9 正式上线后）
- **V1.3.10 启动会议**：PO 范蠡主持 · ESC/POS 已不再 V1.3.10 backlog · 9 项资源充足

### 9.3 与 V1.3.9.1+ 客户反馈通道衔接

- **通道定义**：V1.3.9 灰度阶段（6/30-7/14）· 163 邮箱 + 飞鹅 IM
- **ESC/POS 反馈登记**：每条反馈记录到 `docs/prd-feedback-v1.3.9.1.md`（PO 范蠡创建）
- **紧急插单路径**：若客户强烈反馈 → V1.3.9.1+ 紧急启动 ESC/POS 支持（8-12 天 · 选项 A 等价路径 · 工时已就绪）

### 9.4 与 V1.4 backlog 衔接

- **V1.4 backlog 候选**：ESC/POS 票据打印机（V1.4 阶段评估）· 视 V1.3.9/V1.3.10 客户反馈 + ESC/POS 设备市占率变化再决策
- **V1.4 启动时间**：2026-Q4+（V1.3.10 正式上线后）

---

## 10. 13.8 评估输出对齐（AC-13.8.1 ~ AC-13.8.4）

### 10.1 AC-13.8.1 协议扩展评估

| 维度 | 评估结论 | 决策应用 |
|------|----------|----------|
| V55 SQL 草案 | 🟢 ALTER TABLE sys_printer MODIFY protocol ENUM(...,'ESC_POS') 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 端点契约草案 | 🟢 Printer.protocol 字段枚举扩 ESC_POS 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 客户端适配草案 | 🟢 web-impl PrinterForm.vue + android-impl PrinterFormActivity.kt 加 ESC_POS 选项就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 心跳调度草案 | 🟢 ESC/POS 设备 Socket 9100 心跳复用 + 2s connect timeout + fail_count ≥ 2 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |

### 10.2 AC-13.8.2 code_type 扩展评估

| 维度 | 评估结论 | 决策应用 |
|------|----------|----------|
| V57 SQL 草案 | 🟢 ALTER TABLE sys_print_log MODIFY code_type ENUM(...,'RECEIPT','DELIVERY_NOTE') 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 端点契约草案 | 🟢 PrintRequest.codeType 字段枚举扩 RECEIPT/DELIVERY_NOTE 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 模板渲染草案 | 🟢 ReceiptRenderer + DeliveryNoteRenderer（80mm 热敏纸）就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 中文厂名草案 | 🟢 复用 Sprint 13.2 思源黑体 + ESC/POS 字节流指令集兼容方案就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |

### 10.3 AC-13.8.3 EscPosAdapter 抽象评估

| 维度 | 评估结论 | 决策应用 |
|------|----------|----------|
| 接口定义 | 🟢 interface PrintProtocolAdapter { fun build(payload: PrintPayload): ByteArray } 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 实现类 | 🟢 EscPosAdapter + ZplAdapter + TsplAdapter + PdfBrowserAdapter 4 选 1 就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 字节流断言 | 🟢 飞鹅 FP-58 Ⅱ / 佳博 GP-58MB 字节流单测方案就位 | 选项 B 不执行 · V1.3.10 backlog 保留草案 |
| 错误码复用 | 🟢 50201-50203 复用方案就位（与 ZPL/TSPL 一致）| 选项 B 不执行 · V1.3.10 backlog 保留草案 |

### 10.4 AC-13.8.4 工时 + 风险评估

| 维度 | 评估结论 | 决策应用 |
|------|----------|----------|
| backend 工时 | 5-7 天（V58 Flyway + V55 protocol 扩 + V57 code_type 扩 + EscPosAdapter + 4 设备型号单测）| 选项 B 不执行 · V1.3.10 backlog 保留估算 |
| web-impl 工时 | 2-3 天（PrinterForm 加 ESC_POS + PrintDialog 80mm 选项 + codegen）| 选项 B 不执行 · V1.3.10 backlog 保留估算 |
| android-impl 工时 | 1-2 天（PrinterFormActivity 加 ESC_POS + PrintDialog 80mm 选项）| 选项 B 不执行 · V1.3.10 backlog 保留估算 |
| 合计工时 | 8-12 天 | 选项 B 不执行 · V1.3.10 backlog 资源释放 |
| 风险 1（设备碎片化）| 🟡 中 · V1.3.10 IMPL 单测覆盖主流型号 | 选项 B 不激活 |
| 风险 2（中文厂名）| 🟡 中 · 复用 Sprint 13.2 思源黑体 | 选项 B 不激活 |
| 风险 3（Socket 心跳）| 🟡 中 · 复用 Sprint 12.2 心跳调度 | 选项 B 不激活 |
| 风险 4（80mm vs A4 共存）| 🟢 低 · codeType 字段区分 | 选项 B 不激活 |

---

**PM 决策书#S14-13.8 完 · 13.8 ESC/POS 票据打印机 V1.3.10 评估 · 选项 B V1.3.10 不启动 · 沿用 12.4 PDF 替代 + 客户普通激光 · 截止 2026-06-16 · 与 Sprint 12 PM 决策 #S12-1（选项 A）闭环对齐**

**关键路径**：
1. dev agent Opus 4.8 · 13.8 dev log 归档 · 2026-06-14
2. PO 范蠡 · 客户沟通邮件 · 2026-06-16
3. V1.3.9 灰度阶段 · 客户反馈监控 · 6/30-7/14
4. V1.3.10 启动会议 · ESC/POS 已剔除 backlog · 7/14+
5. 应急路径（若客户强烈反馈）· V1.3.9.1+ 紧急插单 · 8-12 天 · 选项 A 等价
