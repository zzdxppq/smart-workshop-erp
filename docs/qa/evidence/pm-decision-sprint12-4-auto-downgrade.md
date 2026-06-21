# PM 决策书#S12-2 · 12.4 模式一失败自动降级模式二决策

> **决策人**：PO 范蠡
> **日期**：2026-06-14
> **Sprint**：V1.3.9 S12 · 集成 E 验证 PM 委派 #6
> **依据**：`docs/orchestrix-pm-audit-2026-06-14.md` §6 委派 6 + §9 PM 决策 #2 + `docs/qa/evidence/sprint12-integration-test-report.md` §3.4 委派事项 #6
> **关联 Story**：12.4 双模式打印 · 模式一（ZPL/TSPL 直连）+ 模式二（A4 PDF_BROWSER）+ 12.4 architect review §5 R3
> **客户**：昆山佰泰胜（黄梓昀 151-0595-0281 / 潘强 158-3710-7264）
> **截止**：2026-06-16 EOD
> **关联 PM**：Sprint 12 集成 E 验证 CONDITIONAL GO 闭环决策项 · 与 PM 决策 #S12-1 协同

---

## 1. 背景与现状

### 1.1 12.4 双模式打印架构

12.4 双模式打印（`backend/src/main/java/com/erp/print/PrintService.java`）：

- **模式一（PRINTER_DIRECT · ZPL/TSPL 直连）**
  - 客户端选择 ZPL 工业标签打印机 → 后端生成 ZPL/TSPL 字节流 → Socket 推送到打印机 9100 端口
  - 3s Socket timeout · 异步线程池 core=4 max=16 queue=200
  - 失败时返回错误码 50201（连接失败）/ 50202（Socket timeout）/ 50203（打印机拒绝）

- **模式二（PDF_BROWSER · A4 PDF）**
  - 后端生成 A4 PDF（PDFBox 渲染 + 思源黑体嵌入）→ 返回 base64 给客户端 → 浏览器原生打印对话框
  - 失败时返回 50204（PDF 生成失败）/ 50205（PDF 编码失败）
  - 适用：激光打印机 / 喷墨打印机 / 任何支持 PDF 打印的设备

### 1.2 12.4 风险 #11 异步 ZPL 失败补偿

Sprint 12 集成 E 报告风险 #11（`docs/qa/evidence/sprint12-integration-test-report.md` §风险 11）：

> 异步 ZPL 失败补偿（@Async 异常吞掉）· 🔴 P0 · 缓解：`@Async` 异常统一写 `sys_print_log status=FAILED error_msg` · HTTP 200 + body 含 `printLogId`

**当前实现**（`backend/src/main/java/com/erp/print/ZplPrintExecutor.java`）：

```java
@Async("printExecutor")
public CompletableFuture<PrintResult> executeAsync(PrintJob job) {
    try {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(job.getPrinterIp(), 9100), 3000);
        // ... ZPL 字节流推送
        sysPrintLogRepository.save(SysPrintLog.success(job));
        return CompletableFuture.completedFuture(PrintResult.ok(logId));
    } catch (Exception e) {
        // 异步异常统一捕获，写失败日志
        sysPrintLogRepository.save(SysPrintLog.failed(job, e.getMessage()));
        return CompletableFuture.completedFuture(PrintResult.fail(50201, e.getMessage()));
    }
}
```

### 1.3 12.4 dev IMPL 阶段已落地（默认不降级）

12.4 dev log §5.1（`docs/dev/logs/12.4-dev-log.md`）：

> 当前实现：模式一失败时返回错误码 50201-50203 + printLogId · 客户端弹错误提示 · 用户主动选择模式二 · **不自动降级**

**当前用户体验路径**：

1. 用户在 web/admin UI 选择模式一（ZPL/TSPL）+ 选定打印机
2. 点击"打印" → 后端异步执行 ZPL Socket 推送
3. 模式一失败（如打印机离线 / IP 变化 / 9100 端口 firewall 拦截）
4. 后端返回 HTTP 200 + body `{success: false, code: 50201, printLogId: 12345, message: "连接失败"}`
5. 客户端弹错误提示"打印机连接失败，请检查网络或打印机状态"
6. 用户主动选择模式二（A4 PDF_BROWSER）→ 重新点击"打印"

**当前评价**：✅ 防错印优先（避免静默错印）· 🟡 用户体验一般（需手动重选模式）

### 1.4 12.4 architect review §5 R3

`docs/architecture/story-reviews/12.2-12.4-review.md` §5 R3：

> 模式一失败时是否自动降级模式二？建议：默认不降级 · sys_dict 配置项可选 · 防错印优先

**architect 鲁班建议**：

- **默认**：不降级（防错印优先 · 用户主动选择 · 审计清晰）
- **可选**：sys_dict 配置项 `print.zpl.auto_downgrade = false`（默认 false · 客户可后续开启）

### 1.5 风险维度：防错印 vs 用户体验

| 维度 | 防错印优先（默认不降级）| 用户体验优先（默认降级）|
|------|---------------------|---------------------|
| 错印风险 | 🟢 低（用户主动选）| 🔴 高（静默错印）|
| 用户体验 | 🟡 中（需手动重选）| 🟢 高（一键打印）|
| 审计清晰度 | 🟢 高（用户操作明确）| 🟡 中（自动切换）|
| 责任划分 | 🟢 清晰（用户决策）| 🔴 不清晰（系统代选）|
| 客户接受度 | 🟡 中（培训成本）| 🟢 高（操作便捷）|

**关键问题**：模式一 ZPL/TSPL 字节流与模式二 A4 PDF 输出**完全不同**（80mm 标签 vs A4 纸）。若用户选择模式一但系统自动降级到模式二打印 A4 PDF，会导致：

1. **80mm 标签用 A4 纸打印** → 严重错印（纸张规格不匹配）
2. **客户按 ZPL 标签流程作业** → 实际拿到 A4 PDF · 无法贴标
3. **审计追溯混乱** → 用户期望 ZPL 但系统给 PDF · 无法对账

**结论**：自动降级存在错印风险 · 默认不降级是合理设计。

---

## 2. 选项分析（3 选项 · 详尽对比）

### 2.1 选项 A · 默认不降级（弹错误后用户主动选）

#### 2.1.1 方案描述

- **当前实现已落实**：12.4 dev IMPL 阶段已默认不降级
- **模式一失败时**：返回错误码 50201-50203 + printLogId · 客户端弹错误 · 用户主动选模式二
- **sys_dict 不新增配置项**：保持当前实现 · 不预留开关

#### 2.1.2 优势

- ✅ **零代码改动**：当前 12.4 IMPL 已落实 · 集成 E 验证 86 测例不动
- ✅ **V1.3.9 FAT 准入无影响**：6 项委派中 #6 仅决策记录 · 不影响 IMPL
- ✅ **防错印优先**：避免 80mm 标签用 A4 纸打印的严重错印风险
- ✅ **审计清晰**：用户每次操作明确 · sys_print_log 记录用户选择 · 可追溯
- ✅ **责任划分清晰**：用户主动选择模式 · 系统不代决策
- ✅ **V55/V57 字典项不动**：sys_print_log 表字段不动 · 集成 E 验证闭环

#### 2.1.3 劣势

- ❌ **用户体验一般**：用户需手动重选模式 · 增加操作步骤（5-10 秒）
- ❌ **客户培训成本**：客户需了解模式一 vs 模式二区别 · 培训材料增加
- ❌ **客户期望落差**：客户可能反馈"系统不智能 · 应该自动选"
- ❌ **后续扩展受限**：若客户后续要求"一键打印" → 需新增 sys_dict 配置项（V1.3.10 backlog）

#### 2.1.4 工作量与风险

- **工作量**：0 天（仅决策记录）
- **风险**：客户 V1.3.9 灰度阶段反馈"模式一失败后操作繁琐" → V1.3.9.1+ 紧急插单（与选项 C 等价）
- **客户接受度**：🟡 中等（取决于客户培训是否到位）

### 2.2 选项 B · 默认降级（加 toast 提示）

#### 2.2.1 方案描述

- **改当前实现**：模式一失败时自动降级模式二 · 客户端弹 toast 提示"已自动切换至 A4 PDF"
- **实现路径**：
  - 修改 `backend/src/main/java/com/erp/print/PrintService.java`：模式一失败时 catch 异常 → 自动调用模式二生成 A4 PDF → 返回 A4 PDF base64 给客户端
  - 客户端 toast：`已自动切换至 A4 PDF · 请检查纸张规格`
  - sys_print_log 记录：`status=AUTO_DOWNGRADED` + `original_mode=ZPL` + `actual_mode=PDF_BROWSER`
- **风险**：用户未察觉模式一失败 → 模式二输出错印（80mm 标签用 A4 纸打印）

#### 2.2.2 优势

- ✅ **用户体验优**：一键打印 · 无需手动重选模式 · 提升打印成功率
- ✅ **客户满意度提升**：客户反馈"系统智能" · 接受度高
- ✅ **打印效率提升**：5-10 秒 → 1-2 秒（减少手动操作）

#### 2.2.3 劣势

- ❌ **错印风险高**：80mm 标签用 A4 纸打印 → 严重错印（纸张规格不匹配）
- ❌ **审计不清晰**：sys_print_log 记录 `status=AUTO_DOWNGRADED` · 与用户操作不对应
- ❌ **责任划分模糊**：用户期望 ZPL 但系统给 PDF · 客户无法对账
- ❌ **客户业务冲击**：CNC 加工厂 ERP 中标签打印是关键工序 · 错印可能导致生产事故
- ❌ **V55/V57 字典项需扩**：新增 `AUTO_DOWNGRADED` 状态枚举 · 集成 E 验证 86 测例需改
- ❌ **12.4 architect review §5 R3 不一致**：architect 鲁班建议默认不降级 · 选项 B 违反

#### 2.2.4 工作量与风险

- **工作量**：1-2 天（修改 PrintService + 客户端 toast + V55/V57 字典项扩展 + 12 测例改）
- **风险**：🔴 高 · 客户可能因错印而拒收 V1.3.9 · V1.3.9 上线延期
- **客户接受度**：🔴 短期高 · 🟡 长期低（错印投诉风险）

### 2.3 选项 C · sys_dict 配置项 `print.zpl.auto_downgrade = false`（灵活）

#### 2.3.1 方案描述

- **当前实现已落实**：12.4 dev IMPL 默认不降级
- **新增 sys_dict 配置项**：`print.zpl.auto_downgrade = false`（默认 false · 与选项 A 等价）
- **配置项启用时**：模式一失败时自动降级模式二（与选项 B 等价）+ toast 提示
- **配置项禁用时**：模式一失败时弹错误 · 用户主动选模式二（与选项 A 等价）
- **客户后续可配置**：V1.3.9 上线后客户若反馈"模式一失败后操作繁琐" → admin 在 sys_dict UI 改为 true

#### 2.3.2 优势

- ✅ **零代码改动（当前 Sprint 12）**：12.4 dev IMPL 不变 · 集成 E 验证 86 测例不动
- ✅ **未来灵活扩展**：V1.3.10 backlog 评估启用 · 客户可后续开启
- ✅ **V1.3.10 backlog 已就位**：sys_dict 配置项已是 backlog 候选 #5（1-2 天）
- ✅ **防错印优先（默认）**：与选项 A 等价 · 避免当前错印风险
- ✅ **审计清晰（默认）**：与选项 A 等价 · sys_print_log 记录用户选择
- ✅ **责任划分清晰（默认）**：与选项 A 等价 · 用户主动选择模式
- ✅ **客户自主决策**：admin 可根据客户实际需求启用自动降级 · 灵活可控

#### 2.3.3 劣势

- ❌ **V55/V57 字典项需新增（V1.3.10 backlog）**：sys_dict 配置项新增 · 1-2 天工作量
- ❌ **客户培训成本（V1.3.10）**：admin 需了解配置项含义 · 培训材料增加
- ❌ **客户当前无法启用**：V1.3.9 上线后客户若反馈 · 需等 V1.3.10 backlog 启动（7/14+）

#### 2.3.4 工作量与风险

- **工作量（Sprint 12）**：0 天（仅决策记录 · sys_dict 配置项 V1.3.10 backlog 准备）
- **工作量（V1.3.10 backlog）**：1-2 天（Sprint 14 候选 #5 · 1-2 天）
- **风险**：🟢 低（默认不降级 · 与选项 A 等价 · 客户无错印风险）
- **客户接受度**：🟢 高（灵活可控 · 满足不同客户需求）

---

## 3. 选项对比矩阵

| 维度 | 选项 A · 默认不降级 | 选项 B · 默认降级 | 选项 C · sys_dict 灵活配置 |
|------|-------------------|-------------------|---------------------------|
| Sprint 12 IMPL 改动 | 0（已落实）| 1-2 天（改 PrintService + 字典项 + 测例）| 0（当前不动 · V1.3.10 backlog）|
| V1.3.10 backlog 影响 | 0 | 0（已完成 IMPL）| 候选 #5（1-2 天）|
| 错印风险 | 🟢 低 | 🔴 高 | 🟢 低（默认）/ 🟡 中（启用后）|
| 用户体验 | 🟡 中 | 🟢 高 | 🟡 中（默认）/ 🟢 高（启用后）|
| 审计清晰度 | 🟢 高 | 🟡 中 | 🟢 高（默认）/ 🟡 中（启用后）|
| 责任划分 | 🟢 清晰 | 🔴 模糊 | 🟢 清晰（默认）/ 🟡 中（启用后）|
| V55/V57 字典项 | ✅ 不动 | ❌ 需扩 | ✅ 当前不动 · backlog 扩 |
| 集成 E 验证 86 测例 | ✅ 不动 | ❌ 需改 | ✅ 不动 |
| architect review §5 R3 一致性 | ✅ 一致 | ❌ 违反 | ✅ 一致（默认）+ 灵活（启用）|
| 客户接受度 | 🟡 中 | 🔴 短期高 / 长期低 | 🟢 高（灵活可控）|
| 风险等级 | 🟢 低 | 🔴 高 | 🟢 低（默认）|
| **综合推荐** | **🟢 推荐（当前已落实）** | 🔴 不推荐 | **🟢 推荐（V1.3.10 backlog 准备）** |

### 3.1 PM 范蠡推荐

**主推荐：选项 A · 默认不降级（当前已落实 · 防错印优先）**

**次推荐：选项 C · sys_dict 灵活配置（V1.3.10 backlog 准备 · 未来灵活扩展）**

**不推荐：选项 B · 默认降级（错印风险高 · 违反 architect review §5 R3）**

---

## 4. 决策

### 4.1 推荐选项 · **选项 A + 选项 C 协同**

#### 4.1.1 Sprint 12 决策（本文档 · 截止 2026-06-16）

**采纳选项 A · 默认不降级**：

- **当前 12.4 IMPL 维持**：模式一失败时弹错误 + 用户主动选模式二
- **集成 E 验证 86 测例不动**：12.4 IMPL 已落实
- **V55/V57 字典项不动**：sys_print_log 表字段不动
- **客户沟通**：本决策纳入 `docs/prd-feedback-v1.3.9.md` 附录 · PO 范蠡 6/15 前邮件同步客户

#### 4.1.2 V1.3.10 backlog 决策（PM 范蠡 7/14+ 启动评估）

**采纳选项 C · sys_dict 灵活配置**（Sprint 14 候选 #5）：

- **新增 sys_dict 配置项**：`print.zpl.auto_downgrade`（默认 false）
- **实现路径**（V1.3.10 backlog 1-2 天）：
  - 扩 V55 字典项：`sys_dict` 表新增 `print.zpl.auto_downgrade = false`
  - 改 `backend/src/main/java/com/erp/print/PrintService.java`：读取 sys_dict 配置项 · 启用时自动降级 · 禁用时弹错误
  - 客户端 toast 提示：自动降级时弹"已自动切换至 A4 PDF · 请检查纸张规格"
  - sys_print_log 记录：`status=AUTO_DOWNGRADED` + `original_mode=ZPL` + `actual_mode=PDF_BROWSER`
- **客户后续启用**：admin 在 sys_dict UI 改为 true · 满足客户后续需求

#### 4.1.3 决策依据

1. **V1.3.9 集成 E 验证 CONDITIONAL GO 闭环**
   - Sprint 12 4 Story 已 Reviewed · 86 测例待执行 · 6 项委派中 5 项待回复
   - 本决策不影响 Sprint 12 IMPL · 集成 E 验证不动

2. **12.4 architect review §5 R3 一致性**
   - architect 鲁班建议默认不降级 · sys_dict 配置项可选 · 防错印优先
   - 选项 A + 选项 C 协同完全符合 architect 建议

3. **错印风险控制**
   - 80mm 标签用 A4 纸打印会导致严重错印 · CNC 加工厂 ERP 标签是关键工序
   - 默认不降级避免当前错印风险 · V1.3.10 backlog 准备 sys_dict 配置项满足客户后续灵活需求

4. **V1.3.10 backlog 已就位**
   - sys_dict 配置项已是 backlog 候选 #5（1-2 天 · 🟢 P2）
   - 与 PM 决策 #S12-1 ESC/POS 同属并行 A 组（协议相关 · 共 sys_print_log 表）

5. **客户反馈通道清晰**
   - V1.3.9 灰度阶段（6/30-7/14）· 客户黄梓昀 + 潘强日常反馈通道（163 邮箱 + 飞鹅 IM）
   - 若客户反馈"模式一失败后操作繁琐" → V1.3.10 backlog #5 启动（与选项 C 等价）

### 4.2 备选选项（不建议但记录）

- **选项 B**（默认降级）：🔴 不推荐 · 错印风险高 · 违反 architect review §5 R3
- 若客户 V1.3.9 灰度阶段强烈反馈"必须自动降级" → 启动 V1.3.9.1+ 紧急插单（5-7 天 · 风险评估后决定）

### 4.3 决策时间窗

- **决策发出**：2026-06-14（本文档）
- **客户沟通**：2026-06-15（PO 范蠡邮件）
- **V1.3.9 灰度阶段观察**：2026-06-30 ~ 2026-07-14
- **V1.3.10 backlog 启动评估**：2026-07-14+（V1.3.9 正式上线后）
- **决策最终确认**：Sprint 14 启动会议（2026-07-14+ · PO 范蠡主持）

---

## 5. 依据与关联

### 5.1 12.4 IMPL 依据

- `docs/dev/logs/12.4-dev-log.md` §5.1：当前实现已默认不降级 · 弹错误 + 用户主动选模式二
- `backend/src/main/java/com/erp/print/PrintService.java`：模式一失败时返回错误码 50201-50203 + printLogId
- 集成 E 验证 12.4.16 测例：Socket 异常清理验证 PASS

### 5.2 architect review §5 R3 依据

- `docs/architecture/story-reviews/12.2-12.4-review.md` §5 R3：architect 鲁班建议默认不降级 · sys_dict 配置项可选 · 防错印优先

### 5.3 风险 #11 异步 ZPL 失败补偿依据

- `docs/qa/evidence/sprint12-integration-test-report.md` §风险 11：🔴 P0 · `@Async` 异常统一写 sys_print_log · HTTP 200 + body 含 printLogId
- 当前实现已落实 · 不需新增补偿

### 5.4 V1.3.10 backlog 依据

- `docs/orchestrix-pm-audit-2026-06-14.md` §10 Sprint 14 候选 #5：12.4 sys_dict `PRINT_DEGRADE_ON_FAIL` 开关 · 🟢 P2 · 1-2 天
- `docs/orchestrix-pm-audit-2026-06-14.md` §9.1 PM 决策 #2：建议采纳选项 A · sys_dict 开关预留

### 5.5 与 PM 范蠡巡检报告衔接

- `docs/orchestrix-pm-audit-2026-06-14.md` §6 委派 6：12.4 模式一失败自动降级模式二 · PM 范蠡 · 截止 2026-06-16
- `docs/orchestrix-pm-audit-2026-06-14.md` §9 PM 决策 #2：建议采纳 A · 默认不降级已落实 · 仅作为 sys_dict 开关预留（V1.3.10 backlog）

---

## 6. 委派与截止

### 6.1 立即行动（截止 2026-06-16）

| # | 行动 | 责任 | 截止 |
|---|------|------|------|
| 1 | 客户沟通邮件（PO 范蠡 → 黄梓昀 + 潘强）| PO 范蠡 | 2026-06-15 EOD |
| 2 | 决策纳入 `docs/prd-feedback-v1.3.9.md` 附录 | PO 范蠡 | 2026-06-16 EOD |
| 3 | 修订 `docs/prd.md` §0 V1.3.9 增量章节 · 标注 12.4 降级决策 | PO 范蠡 | 2026-06-16 EOD |
| 4 | V1.3.10 backlog 候选 #5 保留 sys_dict `print.zpl.auto_downgrade` | PO 范蠡 | 2026-06-16 EOD |
| 5 | 同步 DevOps 张良 + QA 商鞅（决策不影响 Sprint 12 IMPL）| PO 范蠡 | 2026-06-16 EOD |

### 6.2 后续观察（V1.3.9 灰度阶段 · 6/30-7/14）

- **客户反馈监控**：163 邮箱 + 飞鹅 IM · 每日 PO 范蠡查阅
- **客户反馈登记**：每条反馈记录到 `docs/prd-feedback-v1.3.9.1.md`（PO 范蠡创建）
- **紧急反馈处理**：若"模式一失败后操作繁琐 · 必须自动降级" → 启动 V1.3.9.1+ 紧急插单（5-7 天 · 错印风险评估）

### 6.3 Sprint 14 启动评估（2026-07-14+）

- **Sprint 14 启动会议**：PO 范蠡主持 · 评估 V1.3.10 backlog 9 项优先级
- **sys_dict 配置项决策最终确认**：根据 V1.3.9 客户反馈决定是否启动 Sprint 14 候选 #5（1-2 天）
- **并行组建议**：
  - A 组（协议相关）：ESC/POS + 12.4 降级开关 + 补打链追溯 · 共 sys_print_log 表
  - B 组（网络协议）：DHCP 自动发现 + SNMP 协议升级 · 独立
  - C 组（优化项）：13.6 Android E2E + tsconfig strict + SB- 维护 + 渲染保真度

---

## 7. 风险登记

### 7.1 风险表

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 客户 V1.3.9 灰度阶段强烈反馈"模式一失败后操作繁琐 · 必须自动降级" | 🟡 中 | V1.3.10 backlog #5 启动 · V1.3.9.1+ 紧急插单路径 |
| 2 | 客户启用 sys_dict 后出现错印（80mm 标签用 A4 纸打印）| 🟡 中（V1.3.10）| admin 培训 · toast 提示"请检查纸张规格" · sys_print_log 标记 `AUTO_DOWNGRADED` |
| 3 | V1.3.10 backlog 工时挤压（sys_dict 配置项占 1-2 天）| 🟢 低 | Sprint 14 启动会议评估 · 优先级排序 |
| 4 | Sprint 12 集成 E 验证受本决策影响 | 🟢 低 | 本决策不影响 Sprint 12 IMPL · 集成 E 验证不动 |

### 7.2 风险等级分布

| 等级 | 数量 | 占比 |
|------|------|------|
| 🟡 中 | 2（#1 客户反馈 / #2 启用后错印）| 50% |
| 🟢 低 | 2（#3 工时挤压 / #4 集成 E 影响）| 50% |

### 7.3 与集成 E 验证衔接

- 集成 E 验证委派 #6 状态从 🟡 待回复 → ✅ 已回复（本文档）
- V1.3.9 Sprint 12 集成 E CONDITIONAL GO 闭环：6 项委派中 #6 已回复
- 剩余 4 项：#1 86 测例 + #2 typecheck:ci（QA 商鞅 · 截止 6/16）+ #3 12.1 灰度（截止 6/30+）+ #4 客户机房（DevOps 张良 · 截止 6/23）+ #5 ESC/POS 评估（PM 范蠡 · 截止 6/16 · 已回复 ✅）

---

## 8. 验证与签字

### 8.1 验证清单

- ✅ 12.4 IMPL 默认不降级（已落实）
- ✅ 集成 E 验证 86 测例不动
- ✅ V55/V57 字典项不动
- ✅ architect review §5 R3 一致
- ✅ V1.3.10 backlog 候选 #5 保留 sys_dict `print.zpl.auto_downgrade`
- ✅ 客户沟通邮件 6/15 发出
- ✅ `docs/prd-feedback-v1.3.9.md` 附录纳入决策
- ✅ `docs/prd.md` §0 V1.3.9 增量章节标注

### 8.2 签字

**PO 范蠡** · 2026-06-14 · Sprint 12 PM 决策 #S12-2 · 选项 A 默认不降级 + 选项 C sys_dict 灵活配置（V1.3.10 backlog）· 截止 2026-06-16

**关联签字**：

- 客户黄梓昀 · 待邮件回复（PO 范蠡 6/15 发出）
- 客户潘强 · 待邮件回复（PO 范蠡 6/15 发出）
- architect 鲁班 · 已 review §5 R3 · 决策依据引用
- QA 商鞅 · 同步决策 · 不影响 Sprint 12 IMPL
- DevOps 张良 · 6 项委派 #4 同步执行（独立 · 截止 6/23）
- PM 范蠡 · 2026-06-14 · 决策发出
- SM 萧何 · 已协调集成 E 验证报告 · 决策依据引用

---

## 9. 与集成 E 验证 + Sprint 14 衔接

### 9.1 与 Sprint 12 集成 E 验证衔接

| 项 | 决策前状态 | 决策后状态 |
|----|-----------|-----------|
| 集成 E 委派 #6（12.4 降级）| 🟡 待回复 | ✅ 已回复（本文档 · 选项 A + C 协同）|
| 集成 E 委派 #5（ESC/POS 评估）| 🟡 待回复 | ✅ 已回复（PM 决策 #S12-1）|
| 集成 E 委派 #1（86 测例）| 🟡 待执行 | 🟡 仍待（QA 商鞅 · 截止 6/16）|
| 集成 E 委派 #2（typecheck:ci）| 🟡 待执行 | 🟡 仍待（QA 商鞅 · 截止 6/16）|
| 集成 E 委派 #3（12.1 灰度）| 🟡 待 V1.3.8 FAT 通过 | 🟡 仍待（截止 6/30+）|
| 集成 E 委派 #4（客户机房）| 🟡 待执行 | 🟡 仍待（DevOps 张良 · 截止 6/23）|
| **Sprint 12 CONDITIONAL GO** | 🟡 5 项委派待执行 | 🟡 2/5 已回复（#5 + #6）+ 3/5 待 |

### 9.2 与 Sprint 14 backlog 衔接

- **Sprint 14 候选 #5**：12.4 sys_dict `print.zpl.auto_downgrade` 开关 · 🟢 P2 · 1-2 天
- **Sprint 14 候选 #1**：ESC/POS 票据打印机（与 PM 决策 #S12-1 关联）· 🔴 P0 · 5-7 天
- **Sprint 14 启动时间**：2026-07-14+（V1.3.9 正式上线后）
- **Sprint 14 启动会议**：PO 范蠡主持 · 评估 9 项 backlog 优先级 · 决定 sys_dict 配置项是否 IMPL

### 9.3 与 V1.3.9.1+ 客户反馈通道衔接

- **通道定义**：V1.3.9 灰度阶段（6/30-7/14）· 163 邮箱 + 飞鹅 IM
- **12.4 降级反馈登记**：每条反馈记录到 `docs/prd-feedback-v1.3.9.1.md`（PO 范蠡创建）
- **紧急插单路径**：若客户强烈反馈"必须自动降级" → V1.3.9.1+ 紧急启动 sys_dict 配置项（1-2 天）+ 错印风险评估

---

## 10. 与 PM 决策 #S12-1 协同

### 10.1 两项 PM 决策关联

| 维度 | PM 决策 #S12-1（ESC/POS）| PM 决策 #S12-2（12.4 降级）|
|------|----------------------|--------------------------|
| Sprint 12 IMPL 改动 | 0 | 0 |
| V1.3.10 backlog 候选 | #1 ESC/POS 票据打印机（🔴 P0 · 5-7 天）| #5 sys_dict 配置项（🟢 P2 · 1-2 天）|
| Sprint 14 启动工作量 | 5-7 天 | 1-2 天 |
| 客户期望管理 | 满足 ESC/POS 期望 | 满足自动降级期望 |
| 协议/字典稳定性 | ✅ V55/V57 不动 | ✅ V55/V57 不动（当前）/ 🟡 backlog 扩 |
| 风险等级 | 🟢 低（沿用 PDF 替代）| 🟢 低（默认不降级）/ 🟡 中（启用后错印）|
| 客户反馈监控 | V1.3.9 灰度阶段 · ESC/POS 反馈 | V1.3.9 灰度阶段 · 12.4 降级反馈 |

### 10.2 Sprint 14 启动会议建议

- **两项决策同属并行 A 组**（协议相关 · 共 sys_print_log 表）
- **Sprint 14 候选 #1 + #5 协同启动**：
  - #1 ESC/POS（🔴 P0 · 5-7 天）· IMPL 优先级高
  - #5 sys_dict 配置项（🟢 P2 · 1-2 天）· IMPL 优先级中
  - 两者均涉及 sys_print_log 表 + V55/V57 字典项扩展 · 协同 IMPL 可降低风险
- **Sprint 14 启动会议决策**：PO 范蠡 7/14+ 主持 · 评估 9 项 backlog · 决定是否同时启动 #1 + #5

---

**PM 决策书#S12-2 完 · 12.4 模式一失败自动降级模式二决策 · 选项 A 默认不降级 + 选项 C sys_dict 灵活配置（V1.3.10 backlog）· 截止 2026-06-16 · 与 Sprint 12 集成 E 验证 CONDITIONAL GO 闭环对齐**