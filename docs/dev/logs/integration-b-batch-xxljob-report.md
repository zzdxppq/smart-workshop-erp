# 集成 B 报告 · 3.1 crm_batch 真实持久化 + xxl-job 切换

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 编译通过

---

## 1. 改动清单

### 1.1 BatchService（IMPL 阶段已实装）

| 方法 | 持久化 | 状态 |
|------|--------|------|
| createBatch | MyBatis-Plus insert crm_batch + crm_batch_shadow（双写） | ✅ 真实 |
| getPoStatus | MyBatis-Plus @Select aggregatePoProgress | ✅ 真实 |
| compareShadow | MyBatis-Plus @Select compareShadow（聚合 SQL） | ✅ 真实 |

**注**：IMPL 阶段就用了真实 MyBatis-Plus（非 mock），集成 B 主要是 cron 切换。

### 1.2 BatchShadowCompareCron（集成 B 改动）

**Before**：
```java
@Component
public class BatchShadowCompareCron {
    @Scheduled(cron = "0 0 * * * *")
    public void compareShadowHourly() { ... }
}
```

**After**：
```java
@Component
public class BatchShadowCompareCron extends XxlJobBase {
    @XxlJob("batchShadowCompareHourly")
    @Override
    public void execute() {
        safeRun(() -> { ... });
    }
}
```

**改动点**：
- 移除 Spring `@Scheduled`
- 继承 V1.3.7 既有 `XxlJobBase`（提供 safeRun 包装）
- 加 `@XxlJob("batchShadowCompareHourly")` 注解
- 实现 `execute()` 接口（xxl-job 入口）

**参考范本**：`erp-platform/.../ApprovalTimeoutService.java`（V1.3.7 1.2 既有 xxl-job 用法）

---

## 2. mvn compile 验证

```
mvn -pl src/erp-business -am compile -q -DskipTests
BUILD SUCCESS（无输出 = 成功）
```

---

## 3. xxl-job 部署侧动作（生产环境）

本期集成 B 仅代码改造，**xxl-job-admin 端需手动注册 JobHandler**：

| 字段 | 值 |
|------|-----|
| JobHandler | batchShadowCompareHourly |
| Cron | `0 0 * * * ?` |
| 路由策略 | FIRST |
| 阻塞处理 | SERIAL_EXECUTION |
| 任务超时 | 60s |

---

## 4. 已知遗留

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | stream:notify 告警实装（占位 TODO） | 部署后 |
| 2 | xxl-job-admin 控制台注册 JobHandler | 运维侧 |
| 3 | 第一次跑对比 cron 前需 seed 测试数据 | 集成 H（跑全模块 mvn test） |

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · xxl-job 切换完成
- **architect 鲁班** · 与 V1.3.7 既有架构一致
- **QA 商鞅** · 待集成 H 全模块验证