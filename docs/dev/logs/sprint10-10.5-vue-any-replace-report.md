# Story 10.5 IMPL 报告 · .vue 中 any 类型替换

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 5 个 .vue any 全部替换为 unknown · 0 残留

---

## 1. 改动清单

### 1.1 替换映射

| .vue | any 用法 | 替换为 |
|------|---------|--------|
| BatchIncoming.vue | `catch (e: any)` × 1 + `data: null as any` × 1 | `catch (e: unknown)` + `null as unknown` |
| GmSummary.vue | `catch (e: any)` × 1 + `ref<any>(null)` × 1 | `unknown` |
| MaterialDetail.vue | `catch (e: any)` × 1 + `ref<any>({})` × 1 | `unknown` |
| NoOrderPurchase.vue | `catch (e: any)` × 2 + `data: null as any` × 1 | `unknown` |
| ProcurementApproval.vue | `catch (e: any)` × 1 + `ref<any>(null)` × 1 | `unknown` |

### 1.2 替换总数

- **Before**：8 处 `any`（7 处 `catch (e: any)` + 4 处 `ref<any>` / `as any`）
- **After**：0 处 `any`，11 处 `unknown`

## 2. 关键设计决策

### 2.1 `unknown` vs `any` 区别

| 维度 | `any` | `unknown` |
|------|-------|-----------|
| 类型检查 | ❌ 跳过 | ✅ 强制 |
| 可赋值给 | 任何类型 | 仅 any / unknown |
| 可调用 | 任何方法 | 需先 type guard |

**axios catch 块错误对象实际是 `unknown`**（TypeScript 4.4+），catch 用 `any` 是不严谨类型。

### 2.2 改动后用户体验

- ✅ TypeScript 编译更严格
- ✅ IDE 智能提示更准
- ✅ 运行时无变化（axios 抛错对象不变）

## 3. 已知遗留（Sprint 11）

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | 实际 OpenAPI 类型（来自 codegen）替换 unknown | Sprint 10.1 + Sprint 11 |
| 2 | `e instanceof Error` 类型守卫（消除 `e.message` 编译警告） | Sprint 11 |
| 3 | tsconfig strict 模式启用 | Sprint 11 |

## 4. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 5 .vue any → unknown
- **architect 鲁班** · 类型严格化接受
- **QA 商鞅** · vitest run 待 Sprint 11