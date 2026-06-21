# 迁移脚本修订报告 · V49 / V51 / V52

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **触发**：集成 C 阶段发现 crm_purchase_order / sys_workflow_node 表不存在
> **状态**：🟢 3 个迁移脚本全部修订完成

---

## 1. 修订总览

| V 迁移 | 原问题 | 修订动作 |
|--------|--------|----------|
| V49 | §3-§4 引用 `crm_purchase_order` 但表不存在 | §1 加 CREATE TABLE crm_purchase_order |
| V51 | §2 `AFTER po_type` 引用不存在列 | 移除 AFTER 子句 |
| V52 | §3 引用 `sys_workflow_node` 但表不存在 + 字段名错（is_deleted vs create_time） | §1 加 CREATE TABLE sys_workflow_node + §4 字段名修正 |

---

## 2. 根因分析

### 2.1 V1.3.7 缺失物理表

| 表 | V1.3.7 现状 | 来源 |
|----|---------------|------|
| crm_purchase_order | ❌ 不存在（RFQ 流程只在 crm_rfq.purchase_order_no 维护字符串） | V1-V46 任何 migration 无 CREATE TABLE |
| sys_workflow_node | ❌ docker-compose 部署下不存在（classpath 路径下由 V2__workflow_split.sql 创建） | docker-compose 引用 V2__erp_platform.sql 不存在 |

### 2.2 docker-compose 路径问题

```yaml
# docker-compose.yml 第 19-26 行
- ./db/migrations/V1__erp_business.sql  → 已挂载
- ./db/migrations/V2__erp_platform.sql  → 文件不存在！（应为 V2__workflow_split.sql 在 classpath）
- ./db/migrations/V3__crm_customer.sql   → 已挂载
...
- ./db/migrations/V7-V48                → 目录挂载（通配）
```

**生产部署**走 docker-compose 路径，sys_workflow_node 不会被创建。这是历史遗留 bug，V1.3.7 实际从未跑过这条路径下的 sys_workflow_node。

---

## 3. 修订详情

### 3.1 V49 加 CREATE TABLE crm_purchase_order

```sql
CREATE TABLE IF NOT EXISTS crm_purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    po_no VARCHAR(32) NOT NULL COMMENT 'XS-{yyyyMMdd}-{seq:4}',
    rfq_id BIGINT DEFAULT NULL COMMENT 'V1.3.7 RFQ 关联（FROM_ORDER 来源）',
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(128) DEFAULT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_SHIP',
    source_type VARCHAR(20) NOT NULL DEFAULT 'FROM_ORDER',
    purchase_reason VARCHAR(30) DEFAULT NULL,
    approval_route VARCHAR(50) DEFAULT NULL,
    approval_status VARCHAR(20) DEFAULT 'PENDING',
    remark VARCHAR(500) DEFAULT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_po_no (po_no),
    -- 7 个索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V1.3.8 采购订单主表';
```

### 3.2 V51 移除 AFTER po_type

原 V51 §2：
```sql
ADD COLUMN source_type ENUM(...) NOT NULL DEFAULT 'FROM_ORDER' AFTER po_type,
```

修订后：
```sql
ADD COLUMN source_type ENUM(...) NOT NULL DEFAULT 'FROM_ORDER',
```

理由：V49 新建的 crm_purchase_order 没有 po_type 列，AFTER 会导致 DDL 失败。

### 3.3 V52 加 CREATE TABLE sys_workflow_node

字段命名与 classpath 路径 V2__workflow_split.sql 完全一致：
- create_time / update_time（不用 created_at / updated_at）
- create_by / update_by（不用 created_by / updated_by）
- or_sign_required TINYINT(1)（不用 Boolean）
- 无 is_deleted 字段（V2 没用逻辑删除）

V52 §4 INSERT 语句同步修正字段名（created_at → create_time）。

---

## 4. 集成 C 后续工作

crm_purchase_order 表结构现在完整（含 source_type / purchase_reason / approval_route / approval_status 字段），集成 C 可实装 4.1 NoOrderPurchaseService 真实 INSERT：

```java
CrmPurchaseOrder po = new CrmPurchaseOrder();
po.setPoNo(docNoGenerator.nextOrderNo());
po.setSourceType("NO_ORDER");
po.setPurchaseReason(req.getPurchaseReason());
po.setApprovalRoute(determineApprovalRoute(total));
po.setApprovalStatus("PENDING");
po.setSupplierId(req.getSupplierId());
po.setTotalAmount(total);
po.setCreatedBy(createdBy);
crmPurchaseOrderMapper.insert(po);
```

---

## 5. 部署验证（生产部署前必做）

| 步骤 | 命令 | 期望 |
|------|------|------|
| 1 | `docker compose up -d mysql-master` | 容器启动 |
| 2 | `mysql -h ... < /docker-entrypoint-initdb.d/00-init.sql` | init.sql 70 表创建 |
| 3 | `mysql -h ... < /docker-entrypoint-initdb.d/01-V1__erp_business.sql` | V1 应用 |
| 4 | ... V2-V6 ... | 5 步 |
| 5 | `mysql -h ... < /docker-entrypoint-initdb.d/49-V49__batch.sql` | ✅ crm_batch + crm_purchase_order 创建 |
| 6 | `mysql -h ... < /docker-entrypoint-initdb.d/51-V51__purchase_reason.sql` | ✅ source_type 字段扩展 |
| 7 | `mysql -h ... < /docker-entrypoint-initdb.d/52-V52__procurement_manager_role.sql` | ✅ sys_workflow_node 创建 + PROCUREMENT_MANAGER 节点 |

---

## 6. 签字

- **dev agent Opus 4.8** · 2026-06-13 · V49/V51/V52 修订完成
- **architect 鲁班** · 字段命名与 V2 一致性校正确认
- **QA 商鞅** · 部署验证待集成 H