# V1.3.8 android-impl 前端骨架交付报告

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 4 个 Kotlin 文件落盘（3 Fragment + 1 API Client）

---

## 1. 改动清单

### 1.1 新增 Kotlin 文件（4 文件 · Jetpack + Retrofit）

| # | 文件 | Story | 内容 |
|---|------|-------|------|
| 1 | `feature/v138/MaterialBarcodeScanFragment.kt` | 3.2 | 物料码扫码（继承 V1.3.7 QrScanFragment，复用相机扫码壳） |
| 2 | `feature/v138/NoOrderPurchaseFragment.kt` | 4.1 | 无订单采购 APP 端入口（4 类采购理由下拉 + 物料清单提交） |
| 3 | `feature/v138/BatchIncomingScanFragment.kt` | 3.1 | 分批到货扫码（扫 PO 二维码 → 按物料填写到货数量） |
| 4 | `feature/v138/ApiClient.kt` | 全部 | Retrofit 接口（11 端点）+ 12 DTO 类 |

### 1.2 复用 V1.3.7 既有组件

- `QrScanFragment`：扫码相机封装（V1.3.7 1.50）
- `apiClient`：Retrofit 单例（V1.3.7 既有）

---

## 2. 端点对接（11 个）

| 端点 | Story | 调用方 |
|------|-------|--------|
| POST `/api/v1/incoming/batch-create` | 3.1 | BatchIncomingScanFragment |
| GET `/api/v1/incoming/po-status/{poId}` | 3.1 | （待实现） |
| POST `/api/v1/material-barcode/generate` | 3.2 | （待实现） |
| GET `/api/v1/material-barcode/parse` | 3.2 | MaterialBarcodeScanFragment |
| POST `/api/v1/purchase/no-order` | 4.1 | NoOrderPurchaseFragment |
| GET `/api/v1/purchase/reasons` | 4.1 | NoOrderPurchaseFragment |
| POST `/api/v1/approval/route-preview` | 4.2 | （待实现） |
| GET `/api/v1/roles/procurement-manager-perms` | 4.2 | （待实现） |
| GET `/api/v1/reports/gm-summary` | 4.3 | （待实现） |
| GET `/api/v1/materials/{id}/detail` | 2.1 | （待实现） |
| GET `/api/v1/materials/{id}/price-history` | 2.1 | （待实现） |

---

## 3. 关键设计决策

### 3.1 继承 V1.3.7 QrScanFragment

3 个扫码 Fragment 都继承 `QrScanFragment`（V1.3.7 1.50 既有），不复写相机扫码逻辑。
**理由**：扫码壳是成熟组件，V1.3.8 只需添加 onScanSuccess 回调逻辑。

### 3.2 Retrofit + suspend 协程

API 调用用 Retrofit `@POST/@GET` + `suspend` 函数，配合 `viewLifecycleOwner.lifecycleScope.launch`。
**理由**：V1.3.7 1.50 已使用，V1.3.8 沿用保持一致。

### 3.3 不实装 binding 类生成

3 个 Fragment 引用了 `FragmentV138MaterialBarcodeScanBinding` 等 ViewBinding 类（由 layout XML 生成）。
**理由**：layout XML 文件未生成（Kotlin 编译需要 binding 类存在才能通过），需要 Sprint 8 补 layout XML + 在 build.gradle.kts 启用 viewBinding。

---

## 4. 已知遗留

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | 3 个 layout XML 文件（fragment_v138_*） | Sprint 8 补 |
| 2 | build.gradle.kts 启用 viewBinding | Sprint 8 补 |
| 3 | 11 端点全 Fragment 调用补全（目前 3 个） | Sprint 8 补 |
| 4 | DTO 字段与后端严格对齐（OpenAPI codegen） | Sprint 8 |
| 5 | AndroidManifest.xml 注册 Fragment | Sprint 8 |
| 6 | connectedAndroidTest E2E | Sprint 8 |

---

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 4 Kotlin 文件
- **PO 范蠡** · 待前端验证
- **QA 商鞅** · 待 connectedAndroidTest