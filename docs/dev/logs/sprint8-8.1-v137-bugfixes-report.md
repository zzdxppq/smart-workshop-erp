# Story 8.1 IMPL 报告 · V1.3.7 14 个既有 bug 修复

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 78/78 PASS · BUILD SUCCESS · 14 bug 全部修复

---

## 1. 修复明细

### 1.1 类型 A：BigDecimal 精度（2 测例）

| 文件 | 行 | 修复 |
|------|----|----|
| ProcessServiceTest.java | 328 | `assertEquals(BigDecimal("1500.00"), x)` → `assertEquals(0, BigDecimal("1500.00").compareTo(x))` |
| ProcessIntegrationTest.java | 352 | 同上 |

**根因**：BigDecimal.equals 对 scale 敏感（1500 ≠ 1500.00）。改用 compareTo（scale-insensitive）。

### 1.2 类型 B：ConversionService HashMap→List 强转（12 测例 · 服务侧 bug）

| 文件 | 行 | 修复 |
|------|----|----|
| ConversionService.java | 105-114 | 删除 `(List<Map<String, Object>>) costData` 强转；改用 `new ArrayList<>()` + 仅 instanceof Map 取 totalCost |

**根因**：代码先强转 List 再 instanceof Map 兜底（逻辑顺序反了），HashMap 转 List 直接抛 ClassCastException。

**影响 12 测例自动恢复**：
- ConversionServiceTest: 6 测例 PASS（version_lock + convert_released + pdf_aggregate）
- ConversionIntegrationTest: 5 测例 PASS（crossmodule_*）
- ConversionServiceTest.convert_handles_null_request: 1 测例 PASS

---

## 2. mvn test 验证

```
mvn -pl src/erp-business test \
  -Dtest="ProcessServiceTest,ProcessIntegrationTest,ConversionServiceTest,ConversionIntegrationTest"

[INFO] Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测例类 | 通过 |
|--------|------|
| ProcessServiceTest | 25/25 ✅ |
| ProcessIntegrationTest | 20/20 ✅ |
| ConversionServiceTest | 21/21 ✅ |
| ConversionIntegrationTest | 12/12 ✅ |
| **合计** | **78/78** ✅ |

---

## 3. 关键发现

### 3.1 类型 B 是真实 bug（不是测试错）

`ConversionService.java:108` 的 `(List<Map<String, Object>>) costData` 是 **V1.3.7 真实代码 bug**（先强转失败后兜底，逻辑顺序错误）。这个 bug 在生产环境：
- 如果 pdfExportService 正常返回 Map → 强转异常 → 整个 convertDrawing 端点 500 错误
- 老代码兜底实际上永远执行不到（强转先抛 ClassCastException）

**V1.3.7 这 12 个测例在生产部署时这个端点是 100% 失败的**。Sprint 8 8.1 Story 修复了**真实生产 bug**（不只是测试）。

### 3.2 类型 A 是测试错（业务合理）

业务代码 `BigDecimal.valueOf(double)` 返回 scale=0（1500）是合理的（用户输入浮点累加）。测试不应强制 scale。

---

## 4. 全模块回归预期

修复后跑 erp-business 全模块 mvn test 预期：
- V1.3.7 1190 测例 · 14 失败 → 全部修复
- 预期 1190/1190 PASS

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 14 bug 修复 + 78/78 PASS
- **architect 鲁班** · 修复策略接受（类型 B 是服务侧真实 bug）
- **QA 商鞅** · 全模块回归待集成 H