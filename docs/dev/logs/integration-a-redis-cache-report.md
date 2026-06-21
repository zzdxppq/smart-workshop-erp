# 集成 A 报告 · Redis @Cacheable/@CacheEvict 实装

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 编译通过 · 78/78 Sprint 7 测例 PASS

---

## 1. 改动清单

### 1.1 pom.xml（erp-business）

新增 2 个 Spring Boot 启动器：

```xml
<!-- V1.3.8 Sprint 7 集成 A -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 1.2 application.yml

新增 Spring Cache 抽象配置：

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000   # 默认 5min TTL
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "erp:cache:"
```

### 1.3 ErpBusinessApplication

新增 `@EnableCaching` 注解。

### 1.4 MaterialDetailService（Story 2.1）

| 方法 | 注解 | 说明 |
|------|------|------|
| getMaterialDetail | @Cacheable("mat:detail", key="#materialId", unless=...) | 5min 缓存 |
| getPriceHistory | @Cacheable("mat:price-history", key="#materialId", unless=...) | 5min 缓存 |
| evictCache | @CacheEvict({"mat:detail", "mat:price-history"}, key="#materialId") | AFTER_COMMIT 触发 |

### 1.5 GmSummaryService（Story 4.3）

| 方法 | 注解 | 说明 |
|------|------|------|
| getSummary | @Cacheable("gm:summary", key="#period", unless=...) | 5min 缓存（CUSTOM 模式跳过） |
| evictCache | @CacheEvict("gm:summary", allEntries=true) | 清空所有 period 缓存 |

---

## 2. mvn test 验证

```
mvn -pl src/erp-business test \
  -Dtest="BatchServiceTest,MaterialBarcodeBatchServiceTest,NoOrderPurchaseServiceTest,
          ProcurementApprovalRouterTest,MaterialDetailServiceTest,GmSummaryServiceTest"

[INFO] Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测例类 | 通过 |
|--------|------|
| BatchServiceTest | 12/12 ✅ |
| MaterialBarcodeBatchServiceTest | 12/12 ✅ |
| NoOrderPurchaseServiceTest | 15/15 ✅ |
| ProcurementApprovalRouterTest | 16/16 ✅ |
| MaterialDetailServiceTest | 12/12 ✅ |
| GmSummaryServiceTest | 11/11 ✅ |
| **合计** | **78/78** ✅ |

---

## 3. 关键设计决策

### 3.1 unless 条件

```java
unless = "#result == null || !#result.isSuccess()"
```

只缓存成功响应，失败响应（参数缺失、404、500）不缓存，避免 Redis 缓存抛错响应。

### 3.2 GmSummaryService CUSTOM 模式不走缓存

```java
unless = "... || #period == 'CUSTOM'"
```

CUSTOM 模式 startDate/endDate 任意组合，缓存键爆炸（不缓存是正确选择）。

### 3.3 key-prefix 隔离

```yaml
key-prefix: "erp:cache:"
```

与其他可能共享 Redis 实例的系统隔离，避免 key 冲突。

---

## 4. 已知遗留（与集成 B/C/D 衔接）

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | @CacheEvict 触发方未接（1.7/1.10/1.33/1.40 Service AFTER_COMMIT 调用 evictCache） | 集成 E（2.1 接真实 Service） |
| 2 | NoOrderPurchaseService.createNoOrderPurchase AFTER_COMMIT 调用 GmSummaryService.evictCache | 集成 C（4.1 真实持久化） |
| 3 | ProcurementApprovalRouter 审批完成 AFTER_COMMIT 调用 GmSummaryService.evictCache | 集成 D（4.2 真实 SQL） |
| 4 | K6 性能脚本 backend/k6/material-detail.js + gm-summary.js | 集成 H |

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · Redis 缓存实装完成
- **architect 鲁班** · Redis 5min 策略与 PM 决策一致
- **QA 商鞅** · 78/78 PASS