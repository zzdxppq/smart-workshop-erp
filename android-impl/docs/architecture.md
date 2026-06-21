# Android 端详细架构 · CNC ERP V1.3.7

> **仓模式**：android-impl（multi-repo role: android）
> **产品仓**：`E:\claude\smart-workshop-erp`
> **文档版本**：V1.3.7 · 2026-06-10
> **生成人**：Orchestrix Architect agent（鲁班）
> **输入文档**：`docs/prd.md` V1.3.7 · `docs/architect-handoff.md` V1.1 · `docs/mobile-tech-stack.md` V1.0 · `docs/ux-handoff.md` V1.1
> **范围**：Android APP（V1.0 主战场 · 3 个月交付）；iOS 不在本文档范围

---

## 1. Introduction

### 1.1 项目背景

昆山佰泰胜专属 ERP 系统 · Android 移动端是面向 CNC 加工厂一线人员（操作工 / 仓管 / 品质）的扫码作业入口。设计目标是"让操作工奶奶 30 秒学会扫码报工"，核心场景：扫码报工 / 扫码出入库 / 到货扫码 / 工序分配确认 / 不良品上报。

### 1.2 范围

| 维度 | 内容 |
|------|------|
| 端 | Android APP（手机 + PDA · 霍尼韦尔 CT60 / 斑马 TC57） |
| Min SDK | 26（Android 8.0 · 覆盖 95% 设备） |
| Target SDK | 34（Android 14） |
| 业务域 | 生产（E5）· 物料（E4）· 品质（E7）· 委外（E6）· 人事（E10） |
| Stories | 14 个（1.4 / 4.1 / 4.2 / 5.2 / 5.3 / 5.4 / 7.1-7.4 / 12.1 / 12.2 / 10.1 / 10.2） |
| V1.3.7 增量 | 5 类码（GD/WL/SB/LZ/WW）· 到货扫码（V1.3.5）· 7 类通知 · 返修 ≥ 2 强提醒 |

### 1.3 引用

- 产品仓 PRD：`E:\claude\smart-workshop-erp\docs\prd.md` V1.3.7
- 整体架构：`E:\claude\smart-workshop-erp\docs\architect-handoff.md` V1.1
- 移动端技术栈：`E:\claude\smart-workshop-erp\docs\mobile-tech-stack.md` V1.0
- UX 交接：`E:\claude\smart-workshop-erp\docs\ux-handoff.md` V1.1
- API 契约：`E:\claude\smart-workshop-erp\backend\spec\openapi.yaml`

---

## 2. System Architecture Context

android-impl 是 multi-repo 拆分中的 4 仓之一。整体架构由 `architect-handoff.md` 定义：

```
+------------------------------------------------------------------+
|                  CNC ERP · 4 仓 Multi-Repo                        |
+------------------------------------------------------------------+
|                                                                  |
|  +-----------------+   +-----------------+                       |
|  | product-repo    |   | backend-impl    |                       |
|  | (PRD / 合同)    |   | (Spring Cloud)  |                       |
|  | prd.md          |   | 16 容器         |                       |
|  | architect-      |   | OpenAPI 3.0     |                       |
|  |  handoff.md     |   | gateway × 2     |                       |
|  +--------+--------+   +--------+--------+                       |
|           |                     |                                |
|           |   +-----------------+-----------------+            |
|           |   |                                   |            |
|           v   v                                   v            |
|  +-----------------+                    +-----------------+     |
|  | web-impl        |                    | android-impl    |     |
|  | (Vue 3 + E.P.)  |                    | (Kotlin + JCP)  |     |
|  | V1.0 主         |  -- HTTPS + JWT --> | V1.0 主         |     |
|  | PC 端 + Web     |                    | 手机 + PDA      |     |
|  +-----------------+                    +-----------------+     |
|                                                                  |
|  Product Repo: E:\claude\smart-workshop-erp                      |
|  Android Repo: E:\claude\smart-workshop-erp\android-impl        |
+------------------------------------------------------------------+
```

**android-impl 边界**：
- IN：14 stories（见 core-config.yaml `assigned_stories`）
- OUT：iOS（V1.1 二期）· 后端 · Web · PDA 固件 · 第三方扫码枪 SDK
- DEPENDS：`../smart-workshop-erp` 产品仓（PRD / 合同 / 架构）

---

## 3. Tech Stack

基于 `mobile-tech-stack.md` §3 + core-config.yaml `v137_custom`，核心依赖 27 个：

| 类别 | 库 | 版本 | 用途 |
|------|---|------|------|
| **语言** | Kotlin | 1.9.22 | 开发语言 |
| | Coroutines | 1.8.0 | 异步并发 |
| | Flow | - | 响应式数据流 |
| **构建** | Gradle | 8.5 | 构建 |
| | AGP | 8.3 | Android Gradle Plugin |
| | JDK | 17 | JVM |
| **UI** | Jetpack Compose | BOM 2024.06 | UI 框架 |
| | Material 3 | 1.12.0 | 组件库 |
| | Navigation Compose | 2.7.7 | 导航 |
| **架构** | Lifecycle (ViewModel) | 2.8.4 | 生命周期 |
| | Hilt | 2.51.1 | 依赖注入 |
| | Room | 2.6.1 | 本地 DB（SQLite） |
| **网络** | Retrofit | 2.11.0 | REST |
| | OkHttp | 4.12.0 | HTTP |
| | Moshi | 1.15.1 | JSON |
| **扫码** | CameraX | 1.3.4 | 相机 |
| | ML Kit Barcode | 17.3.0 | 主扫码 |
| | zxing-android-embedded | 4.3.0 | 备用扫码 |
| **数据** | DataStore | 1.1.1 | Key-Value |
| | WorkManager | 2.9.1 | 后台任务 |
| **安全** | EncryptedSharedPreferences | 1.1.0-alpha06 | Token 加密 |
| | Security Crypto | 1.1.0-alpha06 | AES-256 |
| | BiometricPrompt | 1.1.0 | 生物识别 |
| **推送** | Firebase Cloud Messaging | 24.0.0 | 推送 |
| **监控** | Firebase Crashlytics | 19.0.0 | Crash |
| **测试** | JUnit 4 | 4.13.2 | 单元测试 |
| | MockK | 1.13.11 | Mock 框架 |
| | Espresso | 3.6.0 | UI 测试 |
| | Robolectric | 4.13 | 单元测试带 Context |

**Version Catalog**：`gradle/libs.versions.toml` 集中管理，IDE 自动补全。

---

## 4. App Architecture

### 4.1 整体分层（MVVM + Clean Architecture）

```
+-----------------------------------------------------------------+
|  app/                                                            |
|  - App.kt (Hilt @HiltAndroidApp)                                |
|  - MainActivity.kt (NavHost + BottomBar)                        |
+-----------------------------------------------------------------+
                          |
+-----------------------------------------------------------------+
|  ui/ (Presentation Layer)                                       |
|  - screen/ (HomeScreen / WarehouseScreen / MessageScreen /      |
|            MineScreen / ScanScreen / ... )                      |
|  - component/ (ScanButton / StatusTag / ReworkBadge)           |
|  - theme/ (Material3 主题 / 颜色 / 7 状态机色板)               |
|  - nav/ (NavGraph + 嵌套图：home/warehouse/message/mine)       |
+-----------------------------------------------------------------+
                          |  UI State
+-----------------------------------------------------------------+
|  viewmodel/                                                      |
|  - HomeViewModel / ScanViewModel / WorkorderViewModel / ...    |
|  - StateFlow<UiState> + Intent 模式                             |
+-----------------------------------------------------------------+
                          |  UseCase
+-----------------------------------------------------------------+
|  domain/ (纯 Kotlin · 无 Android 依赖)                          |
|  - usecase/ (StartWorkorderUseCase / ScanMaterialUseCase /      |
|             InspectReportUseCase / ...)                        |
|  - entity/ (Workorder / Material / ScanRecord / ...)           |
|  - repository/ (接口)                                           |
+-----------------------------------------------------------------+
                          |  Repository
+-----------------------------------------------------------------+
|  data/                                                           |
|  - repository/ (Impl · Room + Retrofit)                        |
|  - remote/ (ApiService · DTO · Retrofit Factory)               |
|  - local/ (Room DAO · Database · 7 张表)                       |
|  - mapper/ (DTO <-> Entity)                                    |
+-----------------------------------------------------------------+
                          |
+-----------------------------------------------------------------+
|  di/ (Hilt Modules)                                              |
|  - NetworkModule · DatabaseModule · RepositoryModule            |
|  - SecurityModule · WorkManagerModule · ScanModule             |
+-----------------------------------------------------------------+
                          |
+-----------------------------------------------------------------+
|  security/                                                       |
|  - TokenManager · BiometricManager · NetworkSecurityConfig     |
|  - EncryptedPrefs (AES-256-GCM)                                |
+-----------------------------------------------------------------+
                          |
+-----------------------------------------------------------------+
|  work/ (WorkManager Workers)                                    |
|  - ScanSyncWorker · MessageSyncWorker · AttachmentUploadWorker |
|  - PeriodicSyncWorker · ConflictResolveWorker                  |
+-----------------------------------------------------------------+
```

**为什么这个分层**（来自 mobile-tech-stack §4.1）：
- Domain 层无 Android 依赖 → JVM 单测覆盖 80%+
- UseCase 可移植到 iOS（V1.1 二期）
- 单向数据流：扫码场景零出错

---

## 5. Screen Structure

### 5.1 4 Tab 主导航（V1.3.5 新增"仓储"）

```
NavGraph (root)
|
+-- home/           (Tab 1 · 首页)
|   +-- HomeScreen          (80% 扫码框 + 待办/消息/离线)
|   +-- ScanResultScreen    (扫码后业务路由)
|
+-- warehouse/      (Tab 2 · 仓储 · V1.3.5 新增)
|   +-- WarehouseScreen     (入口列表)
|   +-- InboundScanScreen   (扫 WL- 入库)
|   +-- OutboundScanScreen  (扫 WL- 出库)
|   +-- ArrivalScanScreen   (V1.3.5 新增 · 扫 WW- 委外到货)
|       +-- ArrivalDetailScreen
|
+-- message/        (Tab 3 · 消息)
|   +-- MessageListScreen   (7 类通知)
|   +-- MessageDetailScreen
|
+-- mine/           (Tab 4 · 我的)
    +-- MineScreen          (个人资料 / 设置)
    +-- OfflineCacheScreen  (离线缓存管理 · 500 条)
    +-- SettingsScreen
    +-- AboutScreen
```

### 5.2 关键页面 UX 引用

| 页面 | 角色 | 关键 UX（ux-handoff.md） |
|------|------|--------------------------|
| 工作台首页 | 操作工/仓管/品质 | §4.1 · 80% 扫码框 + 离线条数明牌 |
| 到货扫码 | 仓管 | §4.7 · V1.3.5 新增 · 录实收/重量/照片 |
| 消息中心 | 全员 | §6.3 · 7 类通知 · 返修 ≥ 2 强提醒 |

---

## 6. State Management

### 6.1 模式

- **ViewModel + StateFlow + Intent** 单向数据流
- UI 发送 Intent → ViewModel 处理 → emit UiState → UI 渲染
- Coroutines 处理异步；Flow 链式处理数据流

### 6.2 5 类 UiState

```kotlin
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val code: String, val msg: String) : UiState<Nothing>
    object Empty : UiState<Nothing>            // 列表无数据
    data class Offline(val cached: Int) : UiState<Nothing>  // 离线模式（V1.3.7 新增）
}
```

### 6.3 典型 ViewModel 结构

```kotlin
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanUseCase: ScanUseCase,
    private val workManager: WorkManagerManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<ScanResult>>(UiState.Loading)
    val uiState: StateFlow<UiState<ScanResult>> = _uiState.asStateFlow()

    fun onIntent(intent: ScanIntent) { ... }
}
```

---

## 7. API Integration

### 7.1 Retrofit + OkHttp + Moshi

- Base URL：`https://erp-gateway.xxx.com/api/v1/`（Nacos 热更新）
- OpenAPI Generator 自动生成 `ApiService` 接口 + DTO
- 拦截器链：Logging → JWT Auth → RefreshToken → ErrorMapping

### 7.2 5 类码 Prefix 路由表（V1.3.7 关键）

扫码后根据 prefix 路由到不同业务接口（ux-handoff §4.1 + §5.5）：

```
+------------------+-------------------+----------------------------+
| 码 Prefix        | 业务含义          | 后端 API 路径              |
+------------------+-------------------+----------------------------+
| GD-              | 工单 (开工/报工)  | /api/v1/workorder/{id}     |
|                  |                   | /api/v1/workorder/{id}/start|
|                  |                   | /api/v1/workorder/{id}/report|
+------------------+-------------------+----------------------------+
| WL-              | 物料码 (出入库)   | /api/v1/material/{id}      |
|                  |                   | /api/v1/inventory/inbound  |
|                  |                   | /api/v1/inventory/outbound |
+------------------+-------------------+----------------------------+
| SB-              | 机台码 (选机台)   | /api/v1/equipment/{id}     |
+------------------+-------------------+----------------------------+
| LZ-              | 流转码 (过站)     | /api/v1/process/{id}/move  |
+------------------+-------------------+----------------------------+
| WW-              | 委外单 (V1.3.5)   | /api/v1/outsub/{id}        |
|                  |                   | /api/v1/outsub/{id}/arrival|
|                  |                   | /api/v1/outsub/{id}/inspect|
+------------------+-------------------+----------------------------+
```

### 7.3 JWT 拦截器

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${tokenManager.accessToken()}")
            .addHeader("X-Device-Id", deviceId)
            .build()
        return chain.proceed(req).also { resp ->
            if (resp.code == 401) tokenManager.refresh()
        }
    }
}
```

---

## 8. Local Data Management

### 8.1 Room 7 张表（V1.3.7）

| # | 表 | 用途 | 同步策略 |
|---|----|------|---------|
| 1 | `scan_record` | 扫码记录（离线缓存 · 上限 500） | WorkManager 上传后删除 |
| 2 | `workorder_cache` | 工单缓存（开工用） | TTL 24h |
| 3 | `material_cache` | 物料码缓存 | TTL 24h |
| 4 | `outsub_cache` | 委外单缓存（WW-） | TTL 7d |
| 5 | `message_cache` | 消息缓存（离线可看） | 上线后增量同步 |
| 6 | `user_pref` | 用户偏好（非敏感） | 本地 |
| 7 | `sync_log` | 同步日志（排查用） | 30 天滚动 |

### 8.2 EncryptedSharedPreferences

敏感数据（Token · 用户凭证 · 加密 DEK 引用）：
- `access_token` · `refresh_token` · `user_id` · `device_id` · `last_login_at`
- 算法：AES-256-GCM（KeyStore 托管 Master Key）

### 8.3 DataStore

非敏感偏好（DataStore · 替代 SharedPreferences）：
- `theme_mode` · `language` · `scan_sound_enabled` · `last_sync_at`

---

## 9. Security

### 9.1 层级

```
+-------------------------------------+
| 1. 网络传输：HTTPS + 证书锁定         |
|    (network_security_config.xml)    |
+-------------------------------------+
| 2. Token 存储：AES-256-GCM           |
|    (EncryptedSharedPreferences)     |
+-------------------------------------+
| 3. 登录鉴权：JWT + Refresh           |
|    (Access 2h / Refresh 30d)        |
+-------------------------------------+
| 4. 关键操作：BiometricPrompt         |
|    (登录 / 重要数据提交)            |
+-------------------------------------+
| 5. APK 加固：ProGuard + R8           |
|    + 防 root / 防调试               |
+-------------------------------------+
```

### 9.2 Biometric 场景

- 登录（冷启动 + 30 分钟无操作）
- 委外到货扫码（金额敏感 · 财务追溯）
- 品质不良品上报（≥ 不良等级）

### 9.3 Network Security Config

- 禁止 cleartext（API 28+ 默认）
- 生产证书锁定（pin set）
- 域名白名单

---

## 10. Offline Support（V1.3.7 关键）

### 10.1 核心策略

车间 WiFi 死角普遍，扫码后断网不卡顿、不丢数。V1.3.7 关键升级：

| 维度 | 规格 |
|------|------|
| 缓存上限 | **500 条**（scan_record 表 · 超过 FIFO 清理） |
| 状态明牌 | 首页显示"离线缓存 N 条待上传 ↑" |
| 同步触发 | 联网后自动 / WorkManager 周期 / 用户手动 |
| 冲突解决 | Last-Write-Win（业务时间戳为准）+ 冲突日志 |

### 10.2 WorkManager Workers

| Worker | 触发 | 策略 |
|--------|------|------|
| `ScanSyncWorker` | 联网后 / 周期 15min | OneTime + NetworkType.CONNECTED |
| `MessageSyncWorker` | 周期 1h | Periodic + NetworkType.CONNECTED |
| `AttachmentUploadWorker` | 拍照后 | OneTime + 重试 3 次（指数退避） |
| `PeriodicSyncWorker` | 周期 6h | Periodic + 兜底 |
| `ConflictResolveWorker` | 检测冲突时 | OneTime + 写 sync_log |

### 10.3 冲突解决流程

```
1. 客户端上传 record (client_ts=T1)
2. 服务端检查服务端版本 (server_ts=T2)
3. 若 T1 < T2 - threshold: 冲突 → ConflictResolveWorker
4. 写 sync_log · 提示用户"X 条记录已合并到最新版本"
5. UI 重新拉取
```

---

## 11. Push Notifications（V1.3.7 7 类）

### 11.1 通知矩阵

| # | 通知类型 | 触发场景 | 优先级 | 渠道 |
|---|---------|---------|-------|------|
| 1 | 审批通知 | 待我审批 | DEFAULT | FCM |
| 2 | 逾期提醒 | 工单 / WW- 单逾期 | HIGH | FCM |
| 3 | 异常上报 | 设备故障 / 来料异常 | HIGH | FCM |
| 4 | 扫码回执 | 报工 / 入库成功 | LOW | FCM |
| 5 | 库存预警 | 库存低于安全水位 | DEFAULT | FCM |
| 6 | **返修预警** | **返修次数 ≥ 2** | **URGENT** | FCM + 顶部 banner + 震动 |
| 7 | 163 邮件失败兜底 | 邮件发送失败 | HIGH | FCM |

### 11.2 强提醒（V1.3.7 红线）

**返修 ≥ 2**（V1.3.4 升级 + V1.3.7 强化）：
- 阈值 Nacos 可配（默认 2）
- 推送对象：高层 + 采购主管 + 生管主管
- 通道：APP 推送 + PC 红点 + 企业微信 webhook
- UI：消息中心顶部 banner（深红 #82071e）+ 设备震动 500ms

### 11.3 离线消息

- 本地 Room 缓存（`message_cache`）
- 上线后增量同步（按 `last_sync_at` 增量拉取）
- 强提醒消息即使离线也要震动（本地闹钟触发）

---

## 12. Testing Strategy

### 12.1 测试金字塔

```
+----------------------+   E2E
|   Espresso + UI      |   - 关键流程：扫码 → 报工 → 入库
|   Automator          |   - 跨应用：扫码 → 系统相机
|   (10% 用例)         |
+----------------------+
|   Robolectric        |   Integration
|   (ViewModel + Room) |   - ViewModel + UseCase + Room
|   (30% 用例)         |   - 带 Android Context
+----------------------+
|   JUnit 4 + MockK    |   Unit
|   (UseCase + Domain) |   - Domain 层 80% 覆盖
|   (60% 用例)         |   - UseCase + Repository 接口
+----------------------+
```

### 12.2 工具分工

| 工具 | 范围 | 用例类型 |
|------|------|---------|
| JUnit 4 | Domain · UseCase | 纯 Kotlin 单测 |
| MockK | Mock 协程 + Final class | 配合 JUnit |
| Robolectric | ViewModel + Room | 带 Context |
| Espresso | UI 流程 | 关键 5 流程 |
| UI Automator | 跨应用 | 扫码 → 系统相机 |

### 12.3 关键测试场景

- 5 类码路由（GD/WL/SB/LZ/WW）
- 离线扫码 → 重连 → 自动同步
- 返修 ≥ 2 强提醒触发
- Token 失效 → 自动刷新
- Biometric 失败降级

---

## 13. Deployment & Coding Standards

### 13.1 APK 签名

- Debug：`~/.android/debug.keystore`
- Release：Jenkins 注入 · `keystore.properties` 不入 Git
- V2 + V3 签名方案

### 13.2 ProGuard / R8

- 启用 R8（minify + shrink + obfuscate）
- 保留：Hilt / Room / Moshi / Retrofit 反射类
- 映射文件：`mapping.txt` 上传 Crashlytics
- 关键 keep 规则：`@HiltViewModel` / `@Entity` / DTO

### 13.3 Kotlin 编码规范

- 官方 Kotlin 风格 + ktlint 强制
- 命名：Class `PascalCase` · 函数 `camelCase` · 常量 `UPPER_SNAKE`
- 函数 ≤ 50 行 · 文件 ≤ 500 行
- 协程：ViewModelScope / LifecycleScope，禁止 GlobalScope
- 不可变 `val` 优先，禁止 `!!` 非空断言

### 13.4 Git & CI/CD

- 分支：main / develop / feature/*
- PR 必须过：ktlint + detekt + unit test + build
- Fastlane：自动化截图 + 签名 + 发布到蒲公英/企业内部分发

---

## 14. V1.3.7 关键升级 + Risks

### 14.1 6 大升级

| # | 升级 | 来源 | 影响 |
|---|------|------|------|
| 1 | 5 类码 prefix 路由 | ux-handoff §4.1 | ScanViewModel + 路由分发器 |
| 2 | 到货扫码（V1.3.5）| ux-handoff §4.7 | Warehouse Tab 新增 + WW- 单接口 |
| 3 | 离线 500 条缓存 | ux-handoff §3.1 原则 4 | Room + WorkManager |
| 4 | 7 类通知 | ux-handoff §6.3 | FCM 渠道分类 + 强提醒 |
| 5 | 返修 ≥ 2 强提醒 | ux-handoff §5.5 | 顶部 banner + 震动 + 阈值配置 |
| 6 | 163 邮件失败兜底 | ux-handoff §4.8 | FCM #7 通知 + 重试 3 次 |

### 14.2 8 项风险 + 缓解

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | PDA 扫码延迟 > 500ms | 中 | ML Kit 主 + ZXing 备用 + 手动输入兜底 |
| 2 | 离线 500 条上限溢出 | 中 | FIFO + 满载告警 + WorkManager 强制上传 |
| 3 | 冲突合并数据丢失 | 高 | Last-Write-Win + 写 sync_log + 提示用户 |
| 4 | EncryptedSharedPreferences Android 12 兼容 | 中 | 1.1.0-alpha06 验证 · 备选 DataStore + Tink |
| 5 | Biometric 设备不支持 | 低 | 降级 PIN 码 · 不阻断流程 |
| 6 | FCM 国内推送不通 | 高 | 接入 极光/小米/华为/OPPO/VIVO 多通道（V1.1 评估）|
| 7 | WorkManager 厂商杀进程 | 中 | 引导用户加白名单 + 前台服务保活 |
| 8 | ProGuard 误删 Hilt 类 | 中 | 标准 keep 规则 + CI 全量回归 |

### 14.3 后续 Sprint 计划

- Sprint 0：项目脚手架 + DI + 4 Tab 骨架 + 扫码 demo
- Sprint 1-2：1.4（APP 端基础） + 4.1（物料码） + 4.2（扫码出入库）
- Sprint 3-4：5.2（扫码报工） + 5.3/5.4（委外辅助） + 7.1-7.4（品质）
- Sprint 5：12.1/12.2（V1.3.5 仓管到货） + 10.1/10.2（人事）
- Sprint 6：V1.3.7 7 类通知 + 返修强提醒 + 离线缓存压测

---

**参考文档**：
- `E:\claude\smart-workshop-erp\docs\prd.md` V1.3.7
- `E:\claude\smart-workshop-erp\docs\architect-handoff.md` V1.1
- `E:\claude\smart-workshop-erp\docs\mobile-tech-stack.md` V1.0
- `E:\claude\smart-workshop-erp\docs\ux-handoff.md` V1.1
- `E:\claude\smart-workshop-erp\backend\spec\openapi.yaml`

🎯 HANDOFF TO dev: *develop-story 1.4
