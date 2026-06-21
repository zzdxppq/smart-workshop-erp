# 集成 C 报告 · 4.1 crm_purchase_order NO_ORDER 真实插入

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 18 测例 PASS（含 3 新增集成 C 测例）· BUILD SUCCESS

---

## 1. 改动清单

### 1.1 新增文件

| 文件 | 路径 |
|------|------|
| CrmPurchaseOrder.java | `.../crm/purchaseorder/entity/CrmPurchaseOrder.java` |
| CrmPurchaseOrderMapper.java | `.../crm/purchaseorder/mapper/CrmPurchaseOrderMapper.java` |

### 1.2 NoOrderPurchaseService 改动

| Before | After |
|--------|-------|
| `public NoOrderPurchaseService()` 无参 | `@Autowired CrmPurchaseOrderMapper + DocNoGenerator + GmSummaryService` |
| `setPoId(System.currentTimeMillis())` 模拟 | `crmPurchaseOrderMapper.insert(po)` 真实 MyBatis-Plus insert |
| 静态返回 | `po.getId()` 从 insert 后 entity 取 |
| 无缓存失效 | `TransactionSynchronizationManager.registerSynchronization` AFTER_COMMIT → `gmSummaryService.evictCache()` |

### 1.3 V49 修订（已落）

V49 加 `CREATE TABLE crm_purchase_order`（含 14 字段 + 7 索引），详见 `migration-revision-v49-v51-v52-report.md`。

---

## 2. mvn test 验证

```
mvn -pl src/erp-business test -Dtest=NoOrderPurchaseServiceTest

[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测例组 | 数量 | 通过 |
|--------|------|------|
| IMPL 阶段（既有 15 测例） | 15 | 15/15 ✅ |
| 集成 C.a 真实 INSERT | 1 | 1/1 ✅ |
| 集成 C.b gm:summary 缓存失效 | 1 | 1/1 ✅ |
| 集成 C.c 30k 路由 PM | 1 | 1/1 ✅ |
| **合计** | **18** | **18/18** ✅ |

---

## 3. 关键设计决策

### 3.1 AFTER_COMMIT 注册模式

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        gmSummaryService.evictCache();
    }
});
```

**理由**：避免事务回滚后缓存被清（DB 数据未生效但缓存被清空），保证数据一致性。

### 3.2 mock 测试策略

测试用 Mockito mock 三个依赖（CrmPurchaseOrderMapper / DocNoGenerator / GmSummaryService）。
- mock mapper insert 时用 doAnswer 设置 entity.id=8008L（模拟 MyBatis-Plus 自动回填 ID）
- mock DocNoGenerator 返回固定 PO 编号
- 用 ArgumentCaptor 捕获 insert 参数验证字段映射

### 3.3 NO_ORDER 模式 rfq_id 必为 null

crm_purchase_order.rfq_id 字段在 V49 新建时允许 null，集成 C 显式 setRfqId(null) 强调无订单采购的特征（V1.3.7 1.32 RFQ 流程走 FROM_ORDER 才有 rfq_id）。

---

## 4. 已知遗留（与集成 D 衔接）

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | supplier_name 字段未补全（TODO 查 supplier 表） | 集成 D 同步 |
| 2 | PO 状态机 PARTIAL_ARRIVED/ALL_ARRIVED 由 crm_batch 累积触发（3.1 集成 B 已实装，但与 4.1 PO 状态同步未联动） | 集成 H 全模块 |
| 3 | sys_workflow_event 写（4.2 审批触发） | 集成 D |

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 真实 INSERT + 缓存失效 + 18/18 PASS
- **architect 鲁班** · crm_purchase_order 表结构与 V49 一致
- **QA 商鞅** · AFTER_COMMIT 模式符合预期