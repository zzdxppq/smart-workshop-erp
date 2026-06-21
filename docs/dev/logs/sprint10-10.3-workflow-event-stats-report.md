# Story 10.3 IMPL 报告 · sys_workflow_event 统计报表端点

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 18/18 PASS · BUILD SUCCESS · Sprint 8.3 + 9.1 闭环

---

## 1. 改动清单

### 1.1 新增 1 端点

```
GET /api/v1/workflow/events/stats?workflow_code=PO_APPROVAL&approver_role=PROCUREMENT_MANAGER&start_date=...&end_date=...
权限：仅 GM + ADMIN（@PreAuthorize）
```

### 1.2 文件（4 新增 + 3 改动）

| 文件 | 改动 |
|------|------|
| `dto/WorkflowEventStatsDTO.java` | 新增（含 Period 嵌套） |
| `mapper/SysWorkflowEventMapper.java` | +3 @Select（aggregateByEventType / aggregateByApproverRole / countByWorkflowCode） |
| `service/WorkflowEventService.java` | +stats() 方法（4 步聚合） |
| `controller/WorkflowEventController.java` | 新增 /stats 端点 |
| `test/.../WorkflowEventServiceTest.java` | +8 Story 10.3 测例 |

### 1.3 4 步聚合逻辑

```java
1. mapper.countByWorkflowCode()  → totalCount
2. mapper.aggregateByEventType() → byEventType map
3. mapper.aggregateByApproverRole() → byApproverRole map
4. 组装 DTO + Period
```

## 2. mvn test 验证

```
mvn -pl src/erp-business test -Dtest=WorkflowEventServiceTest

[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测例组 | 数量 | 通过 |
|--------|------|------|
| Sprint 8.3 AC-8.3.1（10 测例回归） | 10 | 10/10 ✅ |
| Sprint 10.3 AC-10.3.1/2（8 测例） | 8 | 8/8 ✅ |
| **合计** | **18** | **18/18** |

## 3. 关键设计决策

### 3.1 4 步聚合（而非单条 SQL）

权衡：
- **单条 SQL**：性能更好但难维护
- **4 步聚合**：可读性 + 易测试（每步独立 mock）

**决策**：4 步聚合。sys_workflow_event 表数据量可控（单 workflow 数百条/天），性能不是瓶颈。

### 3.2 `<script>` 包裹 MyBatis `<if>`

`@Select` 注解里用动态 SQL 必须 `<script>` 包裹：

```java
@Select("""
    <script>
    SELECT ...
    <if test="approverRole != null">
    AND approver_role = #{approverRole}
    </if>
    </script>
    """)
```

否则 MyBatis-Plus 解析 XML `<if>` 报错。

### 3.3 默认 30 天 + 自定义时间范围

```java
LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
LocalDate end = endDate != null ? endDate : LocalDate.now();
```

**理由**：Story 4.3 GM 报表默认看 30 天（与 LAST_30D period 一致）。

## 4. Sprint 10 累计

| Story | 测例 | 通过 |
|-------|------|------|
| 10.3 workflow_event 统计 | 8 | 8/8 ✅ |

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 18/18 PASS
- **architect 鲁班** · 4 步聚合设计接受
- **QA 商鞅** · 全模块回归待 Sprint 10 H