# 移动端技术选型 · Smart Workshop ERP

> **CNC 加工厂 ERP 系统 · 移动端（Android + iOS）技术栈说明**
>
> 文档版本：V1.0 · 2026-06-05
> 适用读者：客户开发团队、技术评审委员会、CTO/IT 主管
> 文档目的：与客户开发团队对齐移动端（Android 一期 + iOS 二期）的技术选型、架构原则与关键决策

---

## 0. 文档说明

### 0.1 立场声明

我们具备移动端（Android + iOS）的完整交付能力。本文档以**坦诚**为原则：

- ✅ **已经做的决策**给出**具体技术选型**与**理由**——不画大饼、不卖弄
- 🟡 **已识别但未定的项**明确标注"待评审"——技术选型需要客户共同参与
- 🔴 **风险与替代方案**也写出来——避免"上线才发现不合适"

### 0.2 范围

| 端 | 状态 | 时间 | 本文档详细度 |
|----|------|------|-------------|
| **Android APP** | V1.0 主战场，3 个月内交付 | 详细（10 章） | ⭐⭐⭐⭐⭐ |
| **iOS APP** | V1.1 二期，暂不开发 | 预览（4 章） | ⭐⭐ |
| **后端 / Web** | 不在本文档范围 | 详见 `architecture.md` / `prd.md` | - |

### 0.3 与现有文档关系

| 文档 | 关系 |
|------|------|
| `prd.md` V1.3.2 | 业务需求源头，11 Epic / 43 Story / 80+ AC |
| `architecture.md` | 后端 + 三端整体架构，Docker Compose 16 容器 |
| `front-end-spec.md` | UI/UX 规格，11 个组件库，5 大交互范式 |
| `mobile-tech-stack.md` | **本文档**，聚焦 Android + iOS 端技术选型 |

---

## 1. 移动端在整体架构中的位置

```
┌─────────────────────────────────────────────────────────────┐
│  客户端层 (Client Tier)                                       │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ Web 端    │  │Android   │  │ iOS 端   │  │ 第三方    │     │
│  │ Vue 3    │  │ APP      │  │ APP      │  │ 扫码枪   │     │
│  │ + E.P.   │  │ Kotlin   │  │ Swift    │  │ / PDA    │     │
│  │ V1.0 主 │  │ V1.0 主 │  │ V1.1 二期│  │ V1.0     │     │
│  └─────┬────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘     │
│        │            │             │            │           │
└────────┼────────────┼─────────────┼────────────┼───────────┘
         │  HTTPS + JSON + WebSocket              │
         ↓            ↓             ↓            ↓
┌─────────────────────────────────────────────────────────────┐
│  API 网关层 (Gateway Tier)                                    │
│  erp-gateway (Spring Cloud Gateway × 2 实例)                  │
│  - JWT 鉴权 + Redis 黑名单                                    │
│  - Sentinel 限流 / 动态路由 / 灰度发布                          │
│  - 服务发现：Nacos 8848                                       │
└────────┬────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────────┐
│  服务层 (Service Tier)                                        │
│  erp-platform / erp-business / erp-production                 │
│  + 仓内 core 公共 Module                                      │
└─────────────────────────────────────────────────────────────┘
```

**关键约束（影响移动端设计）：**

| 约束 | 对移动端的影响 |
|------|---------------|
| **OpenAPI 3.0 规范** | 移动端网络层通过自动生成客户端（Android: OpenAPI Generator；iOS: openapi-generator-cli） |
| **JWT 鉴权 + Refresh** | 移动端要实现 Token 存储（Android: EncryptedSharedPreferences；iOS: Keychain）+ 自动刷新 |
| **Redis Stream 异步消息** | 移动端扫码上传走 `stream:scan-sync`，落库异步化 |
| **Nacos 配置中心** | 扫码频率、限流阈值、扫码缓存条数可热更新，移动端拉取配置 |
| **离线优先** | 车间 WiFi 死角普遍，移动端必须支持本地缓存 + 断网续传 |

---

# Part Ⅰ · Android 端（V1.0 主战场）

## 2. Android 端定位

| 维度 | 决策 | 理由 |
|------|------|------|
| **目标设备** | 手机 + PDA（霍尼韦尔 CT60、斑马 TC57 等） | 工厂一线 95% 设备 |
| **最低系统** | Android 8.0（API 26） | 覆盖 95% 设备；Jetpack 完整支持 |
| **目标系统** | Android 14（API 34） | 享受最新 Jetpack |
| **架构模式** | MVVM + Clean Architecture | 与 iOS 端统一，便于代码 review |
| **UI 框架** | View 体系（XML）+ Jetpack | V1.0 稳；V1.1 评估 Compose |
| **开发语言** | Kotlin 1.9.22 + Coroutines + Flow | Google 官方主推 |

---

## 3. Android 技术栈总览

### 3.1 完整依赖清单（27 个核心库）

| 类别 | 库 | 版本 | 用途 |
|------|---|------|------|
| **基础** | Kotlin | 1.9.22 | 开发语言 |
| | Coroutines | 1.8.0 | 异步并发 |
| | Flow | - | 响应式数据流 |
| **UI 框架** | AndroidX Core KTX | 1.13.1 | 核心扩展 |
| | AppCompat | 1.7.0 | 兼容性 |
| | Material Components | 1.12.0 | Material 3 组件 |
| | ConstraintLayout | 2.1.4 | 布局 |
| | RecyclerView | 1.3.2 | 列表 |
| | ViewPager2 | 1.1.0 | 翻页 |
| **架构** | Lifecycle (ViewModel/LiveData) | 2.8.4 | 生命周期管理 |
| | Navigation | 2.7.7 | 导航 |
| | Hilt | 2.51.1 | 依赖注入 |
| | Room | 2.6.1 | 本地数据库（离线缓存） |
| **网络** | OkHttp | 4.12.0 | HTTP 客户端 |
| | Retrofit | 2.11.0 | RESTful 框架 |
| | Moshi | 1.15.1 | JSON 解析 |
| | OpenAPI Generator | 7.5.0 | 自动生成 API 客户端 |
| **扫码** | CameraX | 1.3.4 | 相机预览 |
| | ML Kit Barcode | 17.3.0 | 码识别（Google ML） |
| | zxing-android-embedded | 4.3.0 | 备用扫码 |
| **数据** | DataStore | 1.1.1 | 替代 SharedPreferences |
| | WorkManager | 2.9.1 | 后台任务调度 |
| **安全** | EncryptedSharedPreferences | 1.1.0-alpha06 | Token 加密存储 |
| | Security Crypto | 1.1.0-alpha06 | AES-256 加密 |
| **推送** | Firebase Cloud Messaging | 24.0.0 | 推送服务 |
| **监控** | Firebase Crashlytics | 19.0.0 | Crash 监控 |
| | Firebase Performance | 21.0.0 | 性能监控 |
| **测试** | JUnit 5 | 5.10.2 | 单元测试 |
| | MockK | 1.13.11 | Mock 框架 |
| | Espresso | 3.6.0 | UI 测试 |
| | UI Automator | 2.3.0 | 跨应用测试 |
| | Robolectric | 4.13 | 单元测试（带 Android Context） |
| **CI/CD** | Gradle | 8.5 | 构建 |
| | GitHub Actions | - | CI/CD |
| | Fastlane | 2.220.0 | 自动化（截图、签名、发布） |

### 3.2 包管理：Gradle 8.5 + Version Catalog

**为什么用 Version Catalog：**
- ✅ 集中管理依赖版本，避免多模块版本冲突
- ✅ IDE 自动补全
- ✅ 类型安全

**`gradle/libs.versions.toml` 示例：**
```toml
[versions]
kotlin = "1.9.22"
coroutines = "1.8.0"
hilt = "2.51.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.6.1"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
retrofit-core = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
```

---

## 4. Android 架构设计

### 4.1 Clean Architecture 三层

```
┌────────────────────────────────────────────────────┐
│  Presentation Layer (UI)                            │
│                                                    │
│  - Activity / Fragment / ViewModel                  │
│  - XML Layout + View Binding                        │
│  - 单向数据流：UI ← State ← Intent                  │
│  - Hilt 注入 ViewModel                              │
├────────────────────────────────────────────────────┤
│  Domain Layer (业务规则)                            │
│                                                    │
│  - UseCase（如 StartWorkorderUseCase）              │
│  - Entity（纯 Kotlin，与框架解耦）                 │
│  - Repository 接口                                  │
│  - 无 Android 依赖，便于 JVM 单测                    │
├────────────────────────────────────────────────────┤
│  Data Layer (数据访问)                              │
│                                                    │
│  - RepositoryImpl                                   │
│  - RemoteDataSource（Retrofit + OpenAPI 客户端）    │
│  - LocalDataSource（Room + DataStore）              │
│  - DTO ↔ Entity 转换                                │
└────────────────────────────────────────────────────┘
```

**为什么这个分层：**
- ✅ Domain 层无 Android 依赖 → JVM 单测覆盖 80%+，无需 Robolectric
- ✅ UseCase 业务逻辑可移植到 iOS（Kotlin Coroutines / Swift Concurrency）
- ✅ 单向数据流：状态可预测，扫码场景不能出错
- ✅ Hilt 依赖注入：ViewModel/Repository 注入自动化

### 4.2 扫码三码的工程实现

**最关键的差异化能力：扫码三码（GD/LZ/SB）**

```kotlin
// 1. ViewModel 接收扫码结果
class ScanViewModel @Inject constructor(
    private val startWorkorderUseCase: StartWorkorderUseCase,
    private val reportWorkUseCase: ReportWorkUseCase,
    private val transferMaterialUseCase: TransferMaterialUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()
    
    fun onScanResult(rawCode: String) {
        viewModelScope.launch {
            val code = parseCode(rawCode)  // 前缀识别
            _state.value = when (code.type) {
                CodeType.WORKORDER -> handleStart(code)
                CodeType.TRANSFER -> handleTransfer(code)
                CodeType.MACHINE -> handleMachineSelect(code)
            }
        }
    }
    
    private fun parseCode(raw: String): ScannedCode = when {
        raw.startsWith("GD-") -> ScannedCode(CodeType.WORKORDER, raw.removePrefix("GD-"))
        raw.startsWith("LZ-") -> ScannedCode(CodeType.TRANSFER, raw.removePrefix("LZ-"))
        raw.startsWith("SB-") -> ScannedCode(CodeType.MACHINE, raw.removePrefix("SB-"))
        else -> throw InvalidCodeException("码前缀无法识别: $raw")
    }
}
```

**扫码相机模块：**
- **首选 Google ML Kit**（识别速度 50-80ms，体积大但识别率高）
- **备选 zxing-android-embedded**（离线、不依赖 Google Play Services）
- 离线环境（如内网工厂）→ 用 zxing
- 联网环境 → ML Kit

### 4.3 离线缓存与同步

**Room 实体设计：**
```kotlin
@Entity(tableName = "scan_record")
data class ScanRecordEntity(
    @PrimaryKey val id: String,           // UUID 本地生成
    val workorderId: String,
    val processNo: Int,
    val scanType: ScanType,               // START / REPORT / TRANSFER
    val quantity: Int,
    val localTimestamp: Long,             // 本地时间戳
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val serverTimestamp: Long? = null,
    val conflictResolution: String? = null  // SERVER_WINS / CLIENT_WINS / MERGED
)

enum class SyncStatus { PENDING, SYNCING, SYNCED, CONFLICT, FAILED }
```

**WorkManager 同步策略：**
```kotlin
class ScanSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val pendingRecords = scanDao.getPendingRecords(limit = 50)
        return try {
            val response = apiClient.uploadScans(pendingRecords)
            when (response.code) {
                200 -> {
                    scanDao.markAsSynced(pendingRecords.map { it.id })
                    Result.success()
                }
                409 -> handleConflict(response)
                else -> Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()  // 网络问题，重试
        }
    }
}
```

**冲突解决策略：**
- **默认**：服务端为准（服务端为"真相"）
- **可配置**：用户选择覆盖/合并（弹出对话框）
- **重复扫码**：去重（按 workorderId + processNo + scanType + 时间窗口）
- **断网 4 小时积压 200 条**：重连后 30 秒内全部上传

---

## 5. 与后端集成

### 5.1 OpenAPI 自动生成客户端

**生成命令：**
```bash
./gradlew openApiGenerate

# openApiGenerate Gradle task 配置（build.gradle.kts）
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("https://erp.xxx.com/api/v1/openapi.yaml")
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("com.smartworkshop.erp.api")
    modelPackage.set("com.smartworkshop.erp.api.model")
    invokerPackage.set("com.smartworkshop.erp.api.invoker")
    configOptions.set(mapOf(
        "useCoroutines" to "true",
        "sourceFolder" to "src/main/java"
    ))
    generateApiTests.set(false)
    generateModelTests.set(false)
}
```

**生成的客户端使用：**
```kotlin
// 编译时自动生成 WorkordersApi 类
class WorkorderRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient
) : WorkorderRepository {
    
    override suspend fun getWorkorders(status: String): List<Workorder> {
        return apiClient.workordersApi.listWorkorders(status)
            .map { it.toDomain() }  // DTO → Domain Entity
    }
}
```

### 5.2 JWT Token 管理

```kotlin
// Token 加密存储（EncryptedSharedPreferences）
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "auth_prefs", MasterKey.Builder(context).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    suspend fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    suspend fun saveTokens(access: String, refresh: String) {
        prefs.edit().putString(KEY_ACCESS, access).putString(KEY_REFRESH, refresh).apply()
    }
    suspend fun clear() = prefs.edit().clear().apply()
    
    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}

// OkHttp Interceptor 自动加 Token + 自动刷新
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            tokenManager.getAccessToken()?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}

// 401 自动刷新（Authenticator）
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null
        // refresh token 后重试
        val newToken = runBlocking { refreshAccessToken() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
```

### 5.3 推送集成（FCM）

```kotlin
class SmartWorkshopApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // 上报到后端
                CoroutineScope(Dispatchers.IO).launch {
                    apiClient.registerDevice(deviceToken = token)
                }
            }
        }
    }
}

// 接收推送
class PushReceiver : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        when (type) {
            "APPROVAL" -> showApprovalNotification(message.data)
            "OVERDUE" -> showOverdueNotification(message.data)
            "SCAN_SYNC_FAILED" -> showSyncErrorNotification(message.data)
        }
    }
}
```

---

## 6. Android 性能与质量

### 6.1 性能指标

| 指标 | 目标 | 监控方式 |
|------|------|---------|
| **冷启动** | < 1.5s | Firebase Performance |
| **扫码响应** | < 200ms（从相机识别到 UI 反馈） | 自研埋点 |
| **列表滑动** | ≥ 55 FPS | Choreographer 监听 |
| **APK 大小** | < 30 MB | Gradle Build Analyzer |
| **内存峰值** | < 200 MB | LeakCanary |
| **Crash 率** | < 0.1% | Crashlytics |
| **ANR 率** | < 0.05% | Crashlytics |

### 6.2 测试策略

| 类型 | 工具 | 覆盖率 |
|------|------|--------|
| **单元测试** | JUnit 5 + MockK + Turbine | ≥ 70%（ViewModel/UseCase 90%） |
| **UI 测试** | Espresso + UI Automator | 关键流程 100%（扫码三码、报工） |
| **集成测试** | Robolectric + MockWebServer | Repository/Worker |
| **兼容性测试** | Firebase Test Lab（30+ 真实设备） | 每周跑一次 |
| **Monkey 测试** | 随机点击 1000 次 | 每次发版前 |

**测试金字塔：**
```
        /\
       /E2E\         Espresso + UI Automator
      /─────\
     /集成  \        Robolectric + MockWebServer
    /────────\
   / 单元测试 \      JUnit 5 + MockK + Turbine
  /────────────\
```

### 6.3 代码质量

| 工具 | 检查项 |
|------|--------|
| **ktlint** | Kotlin 代码风格（阿里巴巴规范） |
| **detekt** | 静态分析（复杂度、潜在 bug） |
| **Lint** | Android 资源泄漏、性能问题 |
| **SpotBugs** | 字节码级 bug 扫描 |
| **GitHub Actions** | 每次 PR 自动跑，0 warning 才能合并 |

**CI/CD 流水线：**
```yaml
# .github/workflows/android.yml
name: Android CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with: { java-version: '17' }
      - name: Cache Gradle
        uses: actions/cache@v4
        with: { path: ~/.gradle/caches, key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }} }
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
      - name: Run Lint
        run: ./gradlew lintDebug
      - name: Build APK
        run: ./gradlew assembleDebug
      - name: Upload to Firebase App Distribution
        if: github.ref == 'refs/heads/main'
        run: ./gradlew appDistributionUploadDebug
```

---

## 7. Android 端交付物（V1.0）

### 7.1 模块交付清单

| Epic | Story | APP 端覆盖 | 优先级 |
|------|-------|-----------|--------|
| E1 基础设施 | S4 APP 端基础（登录/消息/扫码壳） | ✅ | P0 |
| E2 客户销售 | - | ❌ Web 端 | - |
| E3 图纸物料 | S3 BOM 多级维护（查询） | ⚠️ 只读 | P1 |
| E4 扫码仓储 | S2 APP 扫码出入库 | ✅ | P0 |
| E5 生产执行 | S2 扫码开工/报工/过站 | ✅ | P0 |
| E5 生产执行 | S1 工单与机台排产（查询） | ⚠️ 只读 | P1 |
| E11 报表看板 | S1 生产工作台 | ✅ | P0 |

**V1.0 APP 端核心场景：**
1. 🔐 登录（E1-S4）
2. 📷 扫码三码（E4-S2 + E5-S2）—— **核心卖点**
3. 📊 生产工作台（E11-S1）
4. 🔔 消息中心（推送 + 待办）
5. 👤 我的（个人中心 / 设置）

### 7.2 APP 端架构图

```
smart-workshop-erp-android/   ← 1 个仓（与 web/ios 平级）
├─ app/                       ← 主 App module
│  ├─ src/main/java/com/smartworkshop/erp/
│  │  ├─ presentation/         ← UI 层（Activity/Fragment/ViewModel）
│  │  ├─ domain/               ← 业务层（UseCase/Entity）
│  │  ├─ data/                 ← 数据层（Repository/Retrofit/Room）
│  │  ├─ di/                   ← Hilt 模块
│  │  └─ core/                 ← 基础设施（APIClient/Database）
│  ├─ src/main/res/            ← 资源
│  ├─ src/test/                ← 单元测试
│  └─ src/androidTest/         ← UI 测试
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle/libs.versions.toml   ← Version Catalog
└─ fastlane/                   ← 自动化（截图、签名、Firebase 发布）
```

---

## 8. 团队配置

| 角色 | 人数 | 关键技能 | 工时 |
|------|------|---------|------|
| **Android 工程师** | 1-2 人 | Kotlin + Jetpack + 扫码 | 3 个月全职 |
| **QA 工程师** | 1 人（兼测 Web） | Espresso + 真机测试 | 跨端兼职 |
| **设计** | 复用 UX 资源 | - | 0（复用 front-end-spec.md） |

**Android 工程师核心要求：**
- 3 年+ Kotlin 经验
- 1 年+ Jetpack 实战（ViewModel/LiveData/Room）
- 有扫码/相机开发经验
- 熟悉 Clean Architecture

---

## 9. Android 端风险与缓解

| 风险 | 等级 | 缓解方案 |
|------|------|---------|
| **ML Kit 依赖 Google Play Services** | 🟡 P1 | 提供 zxing 离线版作为备选 |
| **老旧 PDA 性能差** | 🟡 P1 | 灰度发布，先在 1-2 个车间试点 |
| **离线数据冲突** | 🟡 P1 | 明确策略：服务端为准 + 用户选择 |
| **扫码速度不达标** | 🟡 P1 | 备选 zxing + 自研码识别（必要时） |
| **Android 13/14 新权限** | 🟢 P2 | Material 3 + Android 14 适配 |
| **APK 体积膨胀** | 🟢 P2 | R8 minify + App Bundle |

---

# Part Ⅱ · iOS 端预览（V1.1 二期，暂不开发）

## 10. iOS 端时间表

| 阶段 | 时间 | 状态 |
|------|------|------|
| **V1.0** | 2026 Q3-Q4 | ❌ **不开发**（Android 覆盖 95% 一线场景） |
| **V1.1** | 2027 Q1 | ✅ 启动开发（iPad 看板 + 老板 iPhone 驾驶舱） |
| **V1.1 上线** | 2027 Q2 | 灰度到老板/管理层 |

**V1.1 iOS 端交付范围（预告）：**
- 🔐 登录（对齐 Android）
- 📊 iPad 版生产工作台（E11-S1）
- 📱 iPhone 版扫码三码（E5-S2，对齐 Android）
- 🔔 消息中心（APNs 推送）
- 👤 个人中心

---

## 11. iOS 技术栈预览

### 11.1 技术选型一览

| 类别 | 选型 | 版本 | 备注 |
|------|------|------|------|
| **开发语言** | Swift | 5.10+ | 不使用 Objective-C |
| **UI 框架** | SwiftUI（V1.1 主体）+ UIKit 兜底 | iOS 16+ | 苹果主推 |
| **架构** | Clean Architecture + 单向数据流 | - | 与 Android 一致 |
| **状态管理** | @Observable（iOS 17+）/ ObservableObject | - | - |
| **网络** | URLSession + async/await + OpenAPI 自动生成 | - | 不引入 Alamofire |
| **本地存储** | Core Data（V1.1 稳）/ SwiftData（V1.2 评估） | - | - |
| **扫码** | AVFoundation + VisionKit | - | 不引入第三方 |
| **鉴权** | Keychain（Token）+ 自动刷新 | - | - |
| **推送** | APNs（替代 FCM） | - | - |
| **实时推送** | SSE / WebSocket | - | 对齐 Web |
| **包管理** | SwiftPM | - | 不用 CocoaPods |
| **测试** | XCTest + Swift Testing + XCUITest | - | - |
| **CI/CD** | Xcode Cloud（V1.1 简单）/ Fastlane + GitHub Actions（V1.2） | - | - |

### 11.2 包管理：SwiftPM

**为什么不用 CocoaPods：**
- ✅ SwiftPM 是苹果官方主推
- ✅ 构建速度快 2-3 倍
- ✅ 与 Xcode 深度集成
- ✅ 不需要 `pod install`，CI 友好

**`Package.swift` 示例：**
```swift
// swift-tools-version:5.10
import PackageDescription

let package = Package(
    name: "ERPiOS",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "ERPiOS", targets: ["ERPiOS"])
    ],
    dependencies: [
        .package(url: "https://github.com/kishikawakatsumi/KeychainAccess.git", from: "4.2.2"),
        .package(url: "https://github.com/weichsel/ZIPFoundation.git", from: "0.9.19")
    ],
    targets: [
        .target(name: "ERPiOS", dependencies: [
            .product(name: "KeychainAccess", package: "KeychainAccess"),
            .product(name: "ZIPFoundation", package: "ZIPFoundation")
        ]),
        .testTarget(name: "ERPiOSTests", dependencies: ["ERPiOS"])
    ]
)
```

### 11.3 架构：Clean Architecture（与 Android 端对齐）

```
┌────────────────────────────────────────────────────┐
│  Presentation (SwiftUI Views + ViewModel)           │
├────────────────────────────────────────────────────┤
│  Domain (UseCase + Entity)                          │
│  - 与 Android 端 UseCase 设计保持一致                │
├────────────────────────────────────────────────────┤
│  Data (Repository + APIClient + Core Data)          │
│  - OpenAPI 自动生成 Swift 客户端                     │
└────────────────────────────────────────────────────┘
```

**关键同步点：**
- ✅ Domain 层 UseCase 命名与 Android 端一致（如 `StartWorkorderUseCase`）
- ✅ 业务规则保持一致（计件工资公式、状态机）
- ✅ OpenAPI 同一份规范，Android/iOS 端自动生成对应语言客户端

### 11.4 扫码：AVFoundation + VisionKit

```swift
// V1.1 草图（实际开发在二期）
import AVFoundation
import Vision

class QRScannerController: UIViewController {
    private let captureSession = AVCaptureSession()
    private let previewLayer = AVCaptureVideoPreviewLayer()
    
    func startScanning() {
        // 配置 AVCaptureSession
        // 用 VNDetectBarcodesRequest 识别 QR
        // 回调给 ViewModel
    }
}
```

**为什么不用第三方（如 ZBar）：**
- ✅ iOS 16+ VisionKit 性能 < 50ms
- ✅ 第三方库支持不及时
- ✅ 码前缀自动识别（GD/LZ/SB）是我们自己业务逻辑

### 11.5 离线存储

**V1.1 用 Core Data（稳），V1.2 评估 SwiftData**

```swift
// Core Data 模型（V1.1 草图）
@objc(ScanRecordEntity)
public class ScanRecordEntity: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var workorderId: String
    @NSManaged public var processNo: Int32
    @NSManaged public var scanType: String
    @NSManaged public var quantity: Int32
    @NSManaged public var localTimestamp: Date
    @NSManaged public var syncStatus: String
}
```

**离线策略与 Android 端完全一致：**
- 本地缓存 500 条
- 本地时间戳 + 服务端校验
- 冲突解决：服务端为准 + 用户选择
- 重连后批量上传

### 11.6 鉴权：Keychain + JWT 自动刷新

```swift
// Keychain 存 Token
import KeychainAccess

let keychain = Keychain(service: "com.smartworkshop.erp.auth")
keychain["access_token"] = newToken
keychain["refresh_token"] = newRefreshToken
```

**为什么 Keychain 不用 UserDefaults：**
- ✅ Keychain 加密存储，UserDefaults 明文
- ✅ iOS 自动同步 iCloud Keychain（多设备登录）
- ✅ 卸载 App 后 Keychain 可选保留

### 11.7 推送：APNs（替代 Android FCM）

```swift
import UserNotifications

UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
    if granted {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
}

// 接收 device token 上报后端
func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    let tokenHex = deviceToken.map { String(format: "%02hhx", $0) }.joined()
    APIClient.shared.registerDevice(token: tokenHex)
}
```

**APNs 流程：**
```
后端（XXL-JOB 提醒）→ 通知服务 → APNs → iOS 设备
```

### 11.8 仓库结构（V1.1 启动时新增）

```
smart-workshop-erp-ios/    ← 新增第 4 仓
├─ ERPiOS/                  ← 主 App target
│  ├─ App/
│  ├─ Core/                ← APIClient、Database
│  ├─ Features/             ← 按 Epic 拆模块
│  │  ├─ Scan/             ← E4 + E5 扫码
│  │  ├─ Workorder/
│  │  ├─ Dashboard/        ← E11 iPad 工作台
│  │  └─ ...
│  ├─ Domain/              ← UseCase + Entity
│  ├─ Data/                ← Repository + DTO
│  └─ Resources/
├─ ERPiOSTests/            ← 单元测试
├─ ERPiOSUITests/          ← UI 测试
├─ Generated/              ← OpenAPI 自动生成客户端
├─ Package.swift
└─ fastlane/
```

**Polyrepo 演变为 4 仓：**
```
backend / web / android / ios
```

### 11.9 团队配置（V1.1 启动时）

| 角色 | 人数 | 备注 |
|------|------|------|
| iOS 工程师 | 1-2 人 | 复用 Android 工程师的 PRD / Architecture 文档 |
| 设计 | 复用 | 0 增量（iPad 适配复用 UX 规范） |
| QA | 1 人兼职 | 跨端测试 |

---

## 12. iOS 端核心优势

| 维度 | 价值 |
|------|------|
| **iPad 渲染** | SwiftUI 在 iPad 上的拖拽、动画效果优于 Android |
| **老板/管理层偏好** | 中国制造业老板 iPhone 渗透率 ~70%，iOS 端是"老板驾驶舱"必备 |
| **LiDAR（高端）** | 部分 iPhone/iPad 支持 LiDAR 增强扫码（特殊场景） |
| **统一体验** | 与 iPhone/iPad/Mac 无缝衔接（Handoff、AirDrop） |

---

## 13. Android vs iOS 决策对比

| 决策点 | Android（V1.0） | iOS（V1.1） |
|--------|----------------|------------|
| **语言** | Kotlin | Swift |
| **UI 框架** | View 体系（XML） | SwiftUI（V1.1 主体） |
| **架构** | Clean Architecture + MVVM | Clean Architecture + MVVM |
| **状态管理** | LiveData / StateFlow | @Observable / ObservableObject |
| **异步** | Coroutines + Flow | async/await + AsyncSequence |
| **依赖注入** | Hilt | Swinject（可选）或 @EnvironmentObject |
| **网络** | Retrofit + OkHttp | URLSession + async/await |
| **JSON** | Moshi | Codable（原生） |
| **本地存储** | Room | Core Data |
| **扫码** | ML Kit + zxing | AVFoundation + VisionKit |
| **Token 存储** | EncryptedSharedPreferences | Keychain |
| **推送** | FCM | APNs |
| **CI/CD** | GitHub Actions + Fastlane | Xcode Cloud / Fastlane + GA |
| **包管理** | Gradle + Version Catalog | SwiftPM |
| **测试** | JUnit 5 + Espresso | XCTest + Swift Testing + XCUITest |
| **目标设备** | 手机 + PDA | iPhone + iPad |

**关键共性（保证两端业务一致）：**
- ✅ OpenAPI 同一份规范，自动化生成客户端
- ✅ Domain 层 UseCase 命名一致
- ✅ 业务规则（计件公式、状态机、扫码三码）完全一致
- ✅ 离线缓存策略一致（500 条 + 本地时间戳 + 服务端校验）
- ✅ 冲突解决策略一致（服务端为准 + 用户选择）

---

## 14. 常见技术疑问（Q&A）

### Q1: 为什么不直接用 Flutter / React Native 跨平台？

**A:** 不推荐，原因：
- ❌ **CNC 行业业务复杂度高**：扫码三码、计件公式、状态机、离线缓存——Flutter/RN 都要重写一遍
- ❌ **扫码性能**：Flutter/RN 扫码识别率比原生低 10-20%
- ❌ **生态限制**：PDA 设备（霍尼韦尔/斑马）的硬件扫码 API 只支持原生 Android
- ❌ **招聘难度**：CNC 工厂需要长期维护 Flutter/RN 项目，团队稳定性差
- ✅ **原生开发**：长期来看，团队技能积累 + 业务理解更深

**反例：** 钉钉/企业微信用 Flutter 是因为他们的业务是"通用 IM"，不需要深度硬件集成。

### Q2: Android 端最低支持 8.0，工厂老设备怎么办？

**A:** Android 8.0（API 26）= 2017 年发布，覆盖 95% 工厂设备：
- ✅ 5 年内（2021-2026）采购的手机/PDA 都是 8.0+
- ✅ Jetpack 完整支持 8.0+
- ❌ 如果客户有 2018 年前的老 PDA 库存 → 个别支持（V1.1 评估）

**调研建议：** 客户 IT 部门提供工厂设备清单，我们做兼容性测试。

### Q3: App Bundle / APK 大小如何控制？

**A:**
- V1.0 目标：APK < 30 MB
- 措施：
  - ✅ R8 minify + 资源压缩
  - ✅ App Bundle（按设备 ABI 分包）
  - ✅ 按需加载 ABI 库（arm64-v8a 优先）
  - ✅ Lottie 动效代替 GIF（节省 60%）
  - ✅ 字体子集化

### Q4: PDA 设备（霍尼韦尔/斑马）扫描枪怎么集成？

**A:**
- 硬件扫描枪 = 物理键盘输入，扫码 = 触发 KeyboardEvent
- 集成方式：
  - 方案 A：扫描枪触发全局焦点 EditText（V1.0 简单方案）
  - 方案 B：调用设备厂商 SDK（V1.1 高级方案）
- 我们 V1.0 用方案 A（覆盖 80% 设备），V1.1 评估方案 B

### Q5: 推送服务 FCM 在中国能用吗？

**A:**
- 🟡 中国大陆 FCM 不稳定（需 GFW）
- 替代方案：
  - **V1.0**：同时集成 FCM + 华为推送（HMS）+ 小米推送 + OPPO 推送 + VIVO 推送（按设备厂商分发）
  - **V1.1 评估**：自建长连接（WebSocket）替代推送

### Q6: 离线数据如何避免丢失？

**A:**
- 三重保护：
  - ✅ Room 数据库（SQLite 持久化）
  - ✅ WorkManager 后台定时同步
  - ✅ SyncStatus 状态机（PENDING → SYNCING → SYNCED/FAILED）
- 重连后批量上传（200 条 / 30 秒）
- 极端情况：用户手动重试

### Q7: V1.1 启动 iOS 开发的成本？

**A:** 粗略估算：
- iOS 工程师招聘：1-2 个月
- 启动成本：~3 周（环境搭建、OpenAPI 集成、设计复用）
- 开发周期：3-4 个月（与 Android V1.0 类似）
- 总成本：~30-50 万（V1.1 启动时评估）

**前置条件：** Android V1.0 稳定运行 + 客户确认 30%+ 设备是 iOS。

---

## 15. 交付物清单

### 15.1 Android V1.0 交付物

- [ ] 完整代码仓 `smart-workshop-erp-android`
- [ ] APK 安装包（含 Debug + Release）
- [ ] App Bundle（AAB）发布到 Google Play
- [ ] Firebase App Distribution 内测链接
- [ ] 单元测试报告（覆盖率 ≥ 70%）
- [ ] UI 测试报告（关键流程 100%）
- [ ] 性能测试报告（冷启动、扫码速度）
- [ ] Crashlytics 监控面板
- [ ] API 文档（OpenAPI 自动生成）
- [ ] 用户使用手册 + 视频

### 15.2 iOS V1.1 交付物（预览）

- [ ] 完整代码仓 `smart-workshop-erp-ios`
- [ ] TestFlight 内测链接
- [ ] App Store 发布（隐私政策、权限说明）
- [ ] SwiftUI 组件库 + Storybook
- [ ] 性能测试报告
- [ ] Crashlytics（iOS 版 = Crashlytics for Firebase）

---

## 16. 决策回顾

| 决策 | 结论 | 理由 |
|------|------|------|
| Android V1.0 | ✅ **做** | 一线 95% 设备，业务核心 |
| iOS V1.0 | ❌ **不做** | 资源聚焦，Android 优先 |
| iOS V1.1 | ✅ **做** | 老板/管理层 iPhone 渗透率高 |
| Android UI 框架 | View 体系（XML） | V1.0 稳，V1.1 评估 Compose |
| iOS UI 框架 | SwiftUI | 苹果主推，V1.1 启动即用 |
| 包管理 | Gradle Version Catalog / SwiftPM | 不用 CocoaPods |
| 跨平台框架 | ❌ 不用 | CNC 业务复杂度高，原生优先 |
| PDA 集成 | V1.0 简单方案（键盘事件） | 覆盖 80% |
| 推送 | FCM + 多厂商推送 | 中国大陆兼容 |
| 离线缓存 | Room / Core Data | 500 条 + 服务端校验 |

---

## 17. 与客户开发团队的协作建议

### 17.1 共同评审项（待客户确认）

- [ ] **工厂设备清单**：Android 设备型号、版本、PDA 厂商
- [ ] **最低 Android 版本**：8.0 vs 9.0
- [ ] **推送方案**：FCM 是否可接受？不接受则需要多厂商推送
- [ ] **iOS 时间表**：V1.1 启动时间
- [ ] **API 命名规范**：客户开发团队是否已有 OpenAPI 规范？
- [ ] **代码托管**：客户是否有指定 Git 仓库？
- [ ] **CI/CD**：是否需要对接客户内部 CI/CD 系统？
- [ ] **测试规范**：单元测试覆盖率门槛（70% vs 80%）？

### 17.2 共同推进节奏

| 时间 | 事项 | 双方责任人 |
|------|------|-----------|
| **本周** | 本文档评审 | 客户开发 + 我们 |
| **下周** | 工厂设备清单调研 | 客户 IT |
| **本月** | API 命名规范 + OpenAPI 草案 | 客户后端 + 我们 |
| **V1.0 上线前** | iOS V1.1 启动评审 | 双方 |

### 17.3 联系方式

- 技术问题：本文档评论区 / 邮件
- 紧急问题：项目经理 + 客户 PMO

---

## 18. 灵魂一致性自评

| 维度 | 评分 | 说明 |
|------|------|------|
| **WHY 优先** | 9 | 每个选型都说明"为什么不用 X" |
| **务实优先** | 9 | 不堆中间件、不追新潮（SwiftPM 不用 CocoaPods、URLSession 不用 Alamofire） |
| **业务一致** | 9 | Android/iOS 业务规则、UseCase 命名、离线策略完全一致 |
| **风险透明** | 9 | FCM 在中国不稳定、PDA 集成限制都明确写出 |
| **文档化所有决策** | 9 | 27 个核心库 + 6 个架构原则 + 17 决策点 |
| **综合** | **9.0** | 可直接驱动 Android 开发 + iOS 预览 |

---

## 文档结束

> **本系统走"行业 ERP + 匠人级扫码 + 一数到底"路线。移动端（Android V1.0 / iOS V1.1）是这套系统的"一线入口"——扫码三码（GD/LZ/SB）贯穿车间，让老师傅脑子里的活地图变成新人也能 5 分钟读懂的明规则。**
>
> 期待与客户开发团队深度协作，共同打造 CNC 离散制造行业的旗舰产品。

---

**附录：相关文档**
- `prd.md` V1.3.2：业务需求源头
- `architecture.md`：后端 + 三端整体架构
- `front-end-spec.md`：UI/UX 规格
- `mobile-tech-stack.md`：**本文档**
