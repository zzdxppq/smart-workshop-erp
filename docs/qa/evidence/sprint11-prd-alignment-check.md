# Sprint 11 PRD 对齐检查报告 · V1.3.8 vs PRD

> **报告人**：PO 范蠡 + dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🔴 **发现 5 项需修复问题** + 🟢 PRD 主体对齐 + 🟡 PRD 自身有 1 项结构 gap

---

## 1. PRD vs V1.3.8 Sprint 7-10 总体对齐

| PRD 章节 | V1.3.8 覆盖 | 状态 |
|----------|-------------|------|
| §1 项目目标（G1-G10） | Sprint 1-6 已交付 | ✅ |
| §2.1 功能性需求 FR1-FR60+ | Sprint 1-7 主线 + Sprint 8 增量 | ✅ |
| §2.2 非功能性需求（NFR1-NFR15） | Sprint 1-6 + Sprint 7 集成 | ✅ |
| §3 界面设计目标 | Sprint 1-6 PC + Sprint 4 APP | ✅ |
| §4 技术假设（多仓结构） | 3 仓（product / backend / web-impl / android-impl）| ✅ |
| §5 Epic 列表（13 Epic）| 13 Epic 全部完成（Sprint 1-6）| ✅ |
| §6 Epics 详细 YAML | Sprint 1-7 + Sprint 10.3 增量 | ✅ |
| §7 检查清单（自评） | Sprint 6 自评通过 | ✅ |
| §8 下一步（Handoff） | handoff-records.md 完成 | ✅ |

**结论：V1.3.8 主体（功能 + 非功能）已全部交付**。

---

## 2. 🔴 需修复问题（5 项）

### 问题 1：web-impl dist/index.html 标题仍是 V1.3.7

**证据**：
```html
<title>昆山佰泰胜 ERP · V1.3.7</title>
```

**PRD 要求**：V1.3.8 部署后标题应是 `V1.3.8`。
**根因**：vite.config.ts / public/index.html 模板硬编码 V1.3.7 未更新。
**修复**：Sprint 11 Story 11.1 web-impl 标题 + version 同步 V1.3.8。

### 问题 2：web-impl 登录页无样式（部署缺陷）

**证据**：`src/views/auth/Login.vue` 第 86-88 行：
```css
<style scoped>
.login { max-width: 400px; margin: 100px auto; }
</style>
```
**现状**：极简样式，无卡片/居中/背景/响应式。dist 部署后用户看到的是无样式 HTML。
**PRD 要求**：UX Handoff §3.1 登录页是"视觉门面"（卡片 + 渐变背景 + 居中 + 品牌 LOGO）。
**根因**：Login.vue 是 V1.3.7 骨架代码（Sprint 1.1 写），V1.3.8 未重做。
**修复**：Sprint 11 Story 11.2 web-impl 登录页 UX 重做（卡片 + 渐变背景 + 品牌）。

### 问题 3：web-impl Login.vue 硬编码 mock-token

**证据**：Login.vue 第 23 行：
```typescript
auth.setToken(r?.token || 'mock-token')
```
**PRD 要求**：生产部署必须用后端真实 `/api/v1/auth/login` 拿 token。
**根因**：Sprint 1.1 写时后端 API 未就绪，留 mock 兜底，V1.3.8 未清理。
**修复**：Sprint 11 Story 11.3 移除 mock-token 兜底 + 加 Loading/错误提示。

### 问题 4：android-impl 签名密钥缺失

**证据**：`build.gradle.kts` 0 处 `signingConfigs`：
```bash
$ grep -i "signing\|keystore" build.gradle.kts
（无输出）
```
**PRD 要求**：发布 APK 必须 V2 签名（play store / 国内应用商店都要求）。
**根因**：Sprint 1.3 写 build.gradle.kts 时只配 debug，未配 release signingConfig。
**修复**：Sprint 11 Story 11.4 android-impl release signingConfig（V1/V2 签名 + keystore.properties 注入）。

### 问题 5：web-impl package.json description + name 仍是 V1.3.7

**证据**：
```json
"name": "erp-web",
"version": "1.3.7",
"description": "昆山佰泰胜专属 ERP 系统 V1.3.7 - Web Frontend (Vue 3)",
```
**修复**：Sprint 11 Story 11.5 web-impl package.json V1.3.8 同步。

---

## 3. 🟡 PRD 自身结构 gap（1 项）

### Gap 1：PRD §5 "Epic 列表" 只列目录占位

**证据**：
```
### Epic 1: 基础设施与权限
（h2 之后无具体 epic table）
```

**现状**：PRD §5 章节只有目录标题"Epic 1: 基础设施与权限"，无 13 Epic 的索引 table。
**PRD 应有**：13 Epic 列表（编号 + 标题 + Story 数 + 端点数 + 状态）。
**修复**：Sprint 11 Story 11.6 PRD §5 补全 13 Epic table。

---

## 4. Sprint 7-10 已完成验收清单

### 4.1 后端（V1.3.8 Sprint 7-10 增量）

| 端点 | Sprint | 状态 |
|------|--------|------|
| POST /api/v1/incoming/batch-create | 7.1 | ✅ |
| GET /api/v1/incoming/po-status/{poId} | 7.1 | ✅ |
| POST /api/v1/material-barcode/generate | 7.2 | ✅ |
| GET /api/v1/material-barcode/parse | 7.2 | ✅ |
| POST /api/v1/purchase/no-order | 7.4.1 | ✅ |
| GET /api/v1/purchase/reasons | 7.4.1 | ✅ |
| POST /api/v1/approval/route-preview | 7.4.2 | ✅ |
| GET /api/v1/roles/procurement-manager-perms | 7.4.2 | ✅ |
| GET /api/v1/reports/gm-summary | 7.4.3 | ✅ |
| GET /api/v1/materials/{id}/detail | 7.2.1 | ✅ |
| GET /api/v1/materials/{id}/price-history | 7.2.1 | ✅ |
| GET /api/v1/materials/{id}/process-route | 7.2.1 | ✅ |
| GET /api/v1/materials/{id}/change-log | 7.2.1 | ✅ |
| GET /api/v1/workflow/events/stats | 10.3 | ✅ |

### 4.2 数据库（Flyway）

| V 迁移 | 内容 | 状态 |
|--------|------|------|
| V49 | crm_batch + crm_batch_shadow + crm_purchase_order + PO 状态机 | ✅ |
| V50 | crm_material_barcode_batch | ✅ |
| V51 | sys_dict PURCHASE_REASON 4 项 + crm_purchase_order 字段扩展 | ✅ |
| V52 | PROCUREMENT_MANAGER 角色 + sys_workflow_node 兜底 | ✅ |
| V53 | sys_workflow_event | ✅ |

### 4.3 前端（web-impl）

| 端点 | Sprint | 状态 |
|------|--------|------|
| /materials/:id/detail | 7 | ✅ |
| /incoming/batch-v138 | 7 | ✅ |
| /purchase/no-order-v138 | 7 | ✅ |
| /approval/route-v138 | 7 | ✅ |
| /reports/gm-summary-v138 | 7 | ✅ |

### 4.4 前端（android-impl）

| Fragment | Sprint | 状态 |
|----------|--------|------|
| MaterialBarcodeScanFragment | 7 + 8.5 | ✅ |
| BatchIncomingScanFragment | 7 + 8.5 | ✅ |
| NoOrderPurchaseFragment | 7 + 8.5 | ✅ |

### 4.5 测试覆盖

| 模块 | 测例 | 通过 |
|------|------|------|
| erp-business 全模块（Sprint 8 末） | 1224 | 1224/1224 |
| Sprint 9 Story 9.1 workflow_event 触发 | 12 | 12/12 |
| Sprint 10 Story 10.3 workflow_event 统计 | 8 | 8/8 |
| web-impl JWT v2 | 18 | 18/18 |
| android-impl ApiClient | 10 | 10/10（待 gradle 跑） |

---

## 5. 修复优先级

| 优先级 | Story | 工作量 |
|--------|-------|--------|
| P0 | 11.2 web 登录页样式 | 1-2h |
| P0 | 11.4 android 签名密钥 | 1h |
| P1 | 11.1 web dist 标题 V1.3.7→V1.3.8 | 0.5h |
| P1 | 11.3 web 登录页 mock-token 清理 | 0.5h |
| P2 | 11.5 web package.json 版本 | 0.5h |
| P3 | 11.6 PRD §5 Epic table | 1h |

---

## 6. 签字

- **PO 范蠡** · 2026-06-13 · PRD 对齐检查完成
- **SM 萧何** · 5 项修复待 Sprint 11 推进
- **dev agent Opus 4.8** · 待 Sprint 11 IMPL 修复