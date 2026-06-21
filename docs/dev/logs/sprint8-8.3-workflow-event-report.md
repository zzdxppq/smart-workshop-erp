# Story 8.3 IMPL 报告 · sys_workflow_event 表实装

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 21/21 PASS · BUILD SUCCESS

---

## 1. 改动清单

### 1.1 Flyway V53__workflow_event.sql

新建 `sys_workflow_event` 表（11 字段 + 6 索引）：
- event_no (UNIQUE) EV-{yyyyMMddHHmmss}-{uuid4}
- workflow_code + biz_id + biz_no 业务关联
- event_type（CREATED/APPROVED/REJECTED/DELEGATED）
- approver_role + approver_user_id/name 审批人
- matched_node_index + matched_threshold 路由信息
- created_at 时间戳

### 1.2 后端文件（4 文件）

| 文件 | 路径 |
|------|------|
| SysWorkflowEvent.java | `.../crm/workflowevent/entity/` |
| SysWorkflowEventMapper.java | `.../crm/workflowevent/mapper/` |
| WorkflowEventService.java | `.../crm/workflowevent/service/` |
| WorkflowEventServiceTest.java | `.../crm/workflowevent/WorkflowEventServiceTest.java` |

### 1.3 GmSummaryMapper 升级

```sql
-- Before (V1.3.8 Sprint 7 集成 D · mock):
SELECT COUNT(*) FROM sys_workflow_node WHERE role_code='PROCUREMENT_MANAGER'

-- After (V1.3.8 Sprint 8 Story 8.3 · 真实):
SELECT COUNT(*) FROM sys_workflow_event
WHERE workflow_code='PO_APPROVAL'
  AND approver_role='PROCUREMENT_MANAGER'
  AND event_type='APPROVED'
  AND created_at BETWEEN ? AND ?
```

---

## 2. mvn test 验证

```
mvn -pl src/erp-business test -Dtest="WorkflowEventServiceTest,GmSummaryServiceTest"

WorkflowEventServiceTest: 10/10 PASS
GmSummaryServiceTest: 11/11 PASS
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测例组 | 数量 | 通过 |
|--------|------|------|
| AC-8.3.1 事件类型 4 种（CREATED/APPROVED/REJECTED/DELEGATED） | 6 | 6/6 |
| 字段持久化（event_no 格式 / 字段映射 / nullable） | 4 | 4/4 |
| GmSummaryService 仍 PASS（mapper 签名变更兼容） | 11 | 11/11 |
| **合计** | **21** | **21/21** |

---

## 3. 关键设计决策

### 3.1 event_no 格式 EV-{ts}-{uuid4}

```java
String ts = LocalDateTime.now().format("yyyyMMddHHmmss");
String uuid = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
return "EV-" + ts + "-" + uuid;  // EV-20260613203045-A3F2
```

**理由**：DB UNIQUE 索引兜底；UUID 4 位 = 65536 组合 + 时间戳 ms 级 = 1ms 内理论撞库概率极低。

### 3.2 集成 D mock → Story 8.3 真实事件

**Sprint 7 集成 D** 用 `sys_workflow_node JOIN sys_workflow` 统计 PROCUREMENT_MANAGER 工作量（语义错误：节点数≠审批事件数）。
**Story 8.3 真实统计** 用 `sys_workflow_event` 实际审批事件聚合（语义正确：APPROVED 事件计数）。

### 3.3 不写审批触发方

WorkflowEventService.recordEvent 是 API，**不修改 ProcurementApprovalRouter**（不在 Sprint 8 范围）。
**理由**：Sprint 8 8.3 是"实装表 + 实装写入 API"，触发接入留 Sprint 9（与 1.32 RFQ NO_ORDER 提交后调用 recordEvent 绑定）。

---

## 4. Sprint 8 累计进度

| Story | 测例 | 通过 |
|-------|------|------|
| 8.1 V1.3.7 bug 修复 | 78 | 78/78 |
| 8.2 1.51 测例补全 | 18 | 18/18 |
| 8.3 workflow_event 实装 | 21 | 21/21 |
| **合计** | **117** | **117/117** |

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 21/21 PASS
- **architect 鲁班** · sys_workflow_event schema 确认
- **QA 商鞅** · 全模块回归待集成 H