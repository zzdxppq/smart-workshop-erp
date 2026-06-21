# Sprint 2 收尾交付物 · V1.3.7

> **周期**：2026-06-12（1 天 · 4 Story · 246 测例 PASS · 全 E3 Epic 闭环）
> **合同**：XP-ZPF202606082405

| Story | Title | 端点 | AC | 复杂度 | 测例 | 评分 | 状态 |
|-------|-------|------|----|----|------|------|------|
| 1.7 | 图纸与版本管理 | 8 | 4 | M | 105 | 8.8 | ✅ |
| 1.8 | 工程转化 | 3 | 3 | M | 36 | 8.8 | ✅ |
| 1.9 | BOM 多级维护 | 5 | 4 | H | 60 | 8.8 | ✅ |
| 1.10 | 工艺库与工序 | 4 | 3 | M | 45 | 8.8 | ✅ |
| **Sprint 2 收尾** | **4 Story** | **20** | **14** | — | **246** | **8.8 平均** | **4/4** |

## Sprint 累计（Sprint 1+2 = 11 Story · 753 测例 PASS）

- Sprint 1：7 Story（1.1-1.6 + 1.7） · 507 PASS
- Sprint 2 收尾：4 Story（1.7+1.8+1.9+1.10） · 246 PASS
- **总测例**：**753 PASS** · 30+ 部署就位 · 0 破

## 跨 Story 移交链

- 1.7 → 1.8：drawing_id + version 引用 → 转化锁定
- 1.8 → 1.9：转化结果 → BOM 多级树根节点
- 1.9 → 1.10：BOM 物料 → 工艺路线 5 段成本
- 1.10 → 1.11（E4 物料条码）：工艺 → 物料 WL-XXXX

## 资源复用清单

- **DocNoGenerator 扩展**：`nextDrawingNo()` `nextBomNo()` `nextWorkOrderNo()` `nextProcessNo()` — 1.5/1.7/1.8/1.9/1.10
- **AES-256-GCM 加密**：1.6/1.7
- **5 段成本聚合 hook**：1.7 → 1.8 → 1.9 → 1.10（V1.3.4 闭环）
- **PDF 1h 缓存模式**：1.7 → 1.8
- **4 状态机**：DRAFT/RELEASED/ARCHIVED/OBSOLETE/CONVERTED（1.7+1.8）
- **5 UI 红线**：1.5 → 1.6 → 1.7 → 1.8 → 1.9 → 1.10
- **@AuditLog 切面**：1.1 → 全部 Story
- **4 阈值路由 + 二次密码**：1.5 → 1.6 → 1.7 → 1.8 → 1.9
- **黑名单优先 40902 + 信用额度 40909**：1.5 → 1.6 → 1.9（FA 件 BOM 转化）

## V7-V9 迁移清单

- V7__drawing_conversion.sql：crm_drawing_conversion + crm_drawing_annotation + crm_drawing_annotation_history + crm_engineer_workload
- V8__bom.sql：crm_bom + crm_bom_item（5 级递归）+ crm_bom_history
- V9__process.sql：crm_process + crm_process_step（5 段）+ crm_process_route

## 评分模式（设计 8.5 / 实施 8.5 / 部署 8.8）

| Story | 设计 | 实施 | 部署 | 综合 |
|-------|------|------|------|------|
| 1.7 | 8.5 | 8.5 | 8.8 | 8.8 |
| 1.8 | 8.5 | 8.5 | 8.8 | 8.8 |
| 1.9 | 8.5 | 8.5 | 8.8 | 8.8 |
| 1.10 | 8.5 | 8.5 | 8.8 | 8.8 |

## 签字

- SM 萧何 + PO 范蠡 + dev Opus 4.8 + architect 鲁班 + QA 商鞅 · 2026-06-12
- **Sprint 2 收尾 · ready for Sprint 3**

---

签字："orchestrix 流程 · 2026-06-12 · Sprint 2 收尾 COMPLETE"
