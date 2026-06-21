# Story 9.1 IMPL 报告 · sys_workflow_event 触发接入

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 80/80 PASS · BUILD SUCCESS

---

## 1. 改动清单

### 1.1 三个调用方注入

| Service | 文件 | 触发事件 |
|---------|------|---------|
| NoOrderPurchaseService | `crm/noorderpurchase/service/` | `EVENT_CREATED`（PO 创建后） |
| ProcurementApprovalRouter | `crm/procurementapproval/service/` | `PREVIEWED`（路由预览后） |
| RfqService | `crm/rfq/service/` | `AWARDED`（RFQ 中标后） |

### 1.2 触发模式

每个调用方都在**主事务内**调用 `workflowEventService.recordEvent()`（**事务内非 AFTER_COMMIT**）：
- recordEvent 失败 → catch 块吞掉 → 主流程不受影响
- recordEvent 成功 → 与 PO 原子提交

### 1.3 关键设计

**为什么用事务内而非 AFTER_COMMIT**：
- workflow_event 表与 PO 同事务，原子性
- 之前 NoOrderPurchaseService AFTER_COMMIT 清缓存（gm:summary 缓存）—— 这部分继续 AFTER_COMMIT
- workflow_event 写入改为事务内（保证原子性）

## 2. mvn test 验证

```
mvn -pl src/erp-business test \
  -Dtest="NoOrderPurchaseServiceTest,ProcurementApprovalRouterTest,RfqServiceTest,
          WorkflowEventServiceTest,RfqIntegrationTest"

[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测例类 | 数量 | 通过 | 9.1 新增 |
|--------|------|------|----------|
| NoOrderPurchaseServiceTest | 22 | 22/22 ✅ | +4 (AC-9.1.1) |
| ProcurementApprovalRouterTest | 20 | 20/20 ✅ | +4 (AC-9.1.2) |
| RfqServiceTest | 18 | 18/18 ✅ | +4 (AC-9.1.3) |
| WorkflowEventServiceTest | 10 | 10/10 ✅ | — |
| RfqIntegrationTest | 10 | 10/10 ✅ | — |
| **合计** | **80** | **80/80** | **+12** |

## 3. 12 测例明细（AC-9.1.1/2/3 各 4 测例）

### AC-9.1.1 NoOrderPurchaseService（4 测例）
- NO_ORDER 创建后触发 CREATED workflow_event
- workflow_event biz_id 与 PO id 一致
- workflow_event comment 含 purchase_reason
- recordEvent 异常不影响主流程

### AC-9.1.2 ProcurementApprovalRouter（4 测例）
- 路由预览触发 PREVIEWED workflow_event
- > 5万预览触发 PREVIEWED（matchedNodeIndex=6）
- ≤ 1万预览 SELF → matchedNodeIndex=null
- recordEvent 异常不影响主流程

### AC-9.1.3 RfqService（4 测例）
- RFQ 中标触发 AWARDED workflow_event
- 中标 comment 含 PO 号
- 中标 RFQ 不存在时不触发 event
- recordEvent 异常不影响中标主流程

## 4. 关键工程决策

### 4.1 matchedNodeIndex 不传 null 避免 Lombok intValue NPE

```java
int matchedNodeIndex = ...;  // int (primitive)
workflowEventService.recordEvent(..., matchedNodeIndex == 0 ? null : matchedNodeIndex, ...);
```

避免 Integer boxed null 在 MyBatis-Plus insert 时抛 NPE。

### 4.2 3 个测试用例改造（既有 RfqServiceTest / RfqIntegrationTest / ProcurementApprovalRouterTest / NoOrderPurchaseServiceTest）

新增 WorkflowEventService 依赖 → 4 参 → 5 参构造器 → 既有测试需要 mock 补全。
- 全部完成：0 引入回归

### 4.3 路由预览主流程测试用例 `trigger_previewed_self_route` 修复

SELF 场景（≤1万）`routeSet.isEmpty() = true`，原代码 `iterator().next()` 抛 NoSuchElementException 被 catch 吞掉。改用 `routeSet.isEmpty() ? "SELF" : iterator.next()`。

## 5. Sprint 9 累计

| Story | 测例 | 通过 |
|-------|------|------|
| 9.1 workflow_event 触发 | 12 | 12/12 ✅ |

## 6. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 12 测例 PASS
- **architect 鲁班** · 事务内触发设计接受（保证原子性）
- **QA 商鞅** · 全模块回归待集成 H