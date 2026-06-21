# erp-android · 昆山佰泰胜专属 ERP V1.3.9

> **Android Mobile Implementation Repository** (multi-repo)
> **角色**: android
> **产品仓**: `../smart-workshop-erp`
> **架构依据**: 产品仓 `docs/architect-handoff.md` V1.1 + `docs/mobile-tech-stack.md`
> **配套 backend**: `../backend-impl`
> **项目结构**: **标准 Android Studio 多模块布局**（V1.3.9 重构）

## 快速开始

```bash
# 1. 打开 Android Studio Hedgehog (2023.1.1)+ 加载此目录（File > Open > android-impl/）
# 2. 等待 Gradle Sync 完成（首次需下载 gradle-8.7 + AGP 8.2.2 + Kotlin 1.9.22）
# 3. 配置签名
cp app/keystore.properties.template app/keystore.properties
# 编辑 app/keystore.properties 填入 4 项密码（或运行 ./build-apk.sh 自动生成 keystore）
# 4. 同步后端：启动 ../backend 仓 mvn spring-boot:run
# 5. ADB 安装
./gradlew :app:installDebug

# 单元测试
./gradlew :app:test

# UI 测试
./gradlew :app:connectedAndroidTest
```

## 项目结构（V1.3.9 标准 Android Studio 多模块）

```
android-impl/                              ← 项目根（Android Studio 识别目录）
├── build.gradle.kts                       ← 根：仅声明插件版本（不应用）
├── settings.gradle.kts                    ← 根：include(":app")
├── gradle.properties                      ← 根：JVM/缓存/AndroidX 配置
├── gradle/wrapper/                        ← 根：Gradle 8.7 wrapper
├── gradlew / gradlew.bat                  ← 根：CLI 入口
├── local.properties                       ← 根：sdk.dir（git 忽略）
├── keystore.properties.template           ← 根：密钥库模板（备份）
├── .gitignore                             ← 根：忽略 .gradle/build/keystore
├── .gitlab-ci.yml                         ← 根：CI/CD
├── .orchestrix-core/core-config.yaml      ← Orchestrix 工具配置
├── docs/                                  ← 架构 + story + QA
└── app/                                   ← 应用 module（唯一 module）
    ├── build.gradle.kts                   ← app 模块构建配置
    ├── proguard-rules.pro                 ← app 模块 ProGuard/R8 规则
    ├── keystore.properties                 ← 签名配置（git 忽略）
    ├── keystore.properties.template       ← 签名配置模板
    ├── release.keystore                   ← 签名密钥（git 忽略）
    ├── build-apk.sh                       ← 打包脚本
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml        ← 应用清单（4 角色 + 4 大 TAB）
        │   ├── kotlin/com/btsheng/erp/    ← Kotlin 源码（按 feature 分层）
        │   │   ├── ErpApplication.kt      ← Application（Hilt 入口）
        │   │   ├── core/                  ← 核心层：data/scan/security/sync
        │   │   └── feature/               ← 业务层：auth/message/scan/v138/v139
        │   └── res/                       ← 资源
        │       ├── layout/                ← 3 个 v138 Fragment layout XML
        │       ├── mipmap-anydpi-v26/     ← Adaptive Icon
        │       ├── values/                ← strings/colors/themes
        │       └── xml/                   ← FileProvider 路径
        ├── test/                          ← 本地单元测试
        │   └── kotlin/com/btsheng/erp/...
        └── androidTest/                   ← connectedAndroidTest（E2E）
            ├── kotlin/com/btsheng/erp/...
            └── res/values/                ← 测试资源
```

## 技术栈

| 维度 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9.22 |
| JDK | Java | 17 |
| UI | Jetpack Compose + Material 3 | BOM 2024.02.02 |
| 导航 | Navigation Compose | 2.7.7 |
| DI | Hilt | 2.51 |
| 异步 | Coroutines + Flow | 1.7.3 |
| 网络 | Retrofit + OkHttp + Moshi | 2.11 / 4.12 / 1.15 |
| 本地 DB | Room | 2.6.1（V1.3.7 离线扫码缓存 500 条） |
| 安全存储 | EncryptedSharedPreferences | androidx.security:1.1.0（V1.3.7 JWT 存此） |
| 扫码 | ZXing Android Embedded | 4.3.0 |
| 指纹 | BiometricPrompt | 1.1.0 |
| 同步 | WorkManager | 2.9.0（V1.3.7 离线→上线自动同步） |
| 测试 | JUnit + Espresso + Mockk + Robolectric | — |
| minSdk | Android 8.0 (API 26) | |
| targetSdk | Android 14 (API 34) | |
| build | Gradle 8.7 + AGP 8.2.2 | |

## 4 大 TAB 导航（V1.3.5+ 升级）

按 `docs/ux-handoff.md` V1.1 §6.1，由 `feature/MainActivity.kt` 实现：

| Tab | 模块 | 关键功能 | 典型角色 |
|-----|------|---------|---------|
| 1 | **扫码** | GD/LZ/SB 三码开工/报工/过站 | 操作工、品检、生管 |
| 2 | **仓储** | WL- 入库/出库 · PO 分批到货 · WW- 委外到货 | **仓管**、采购 |
| 3 | **待办** | 审批/检验/开工摘要 | 全员 |
| 4 | **消息** | 7 类消息 + 返修预警 | 全员 |
| 5 | **我的** | 离线缓存 / 工具入口 / 退出 | 全员 |

> **仓管**（纯 WAREHOUSE 角色）底部 Tab：**仓储 · 待办 · 消息 · 我的**（无「扫码」Tab；出入库在仓储 Tab 内完成）。  
> 全链路测试步骤：`docs/demo-e2e-flow-test.md`

## 5 类码体系（V1.3.5 收回区域码 · 3+1+1）

| 码 | 前缀 | 触发场景 | 模块 |
|----|------|---------|------|
| **GD-** 工单码 | `GD-YYYYMMDD-NNNN` | 开工 / 报工 / 转工序 | 首页扫码 |
| **LZ-** 流转码 | `LZ-GDxxx-NN` 或 `LZ-GDxxx-P{NN}`（工序委外）| 工序交接 | 首页扫码 |
| **SB-** 设备码 | `SB-CCC-NNN` | 开工选机台 | 首页扫码 |
| **WL-** 物料码 | 物料编码+批次+入库日期+流水 | 入库 / 出库 | 仓储 Tab |
| **WW-** 委外单码 | `WW-YYYYMMDD-NNNN` | **V1.3.5 仓管到货扫码** | 仓储 Tab |

## 源代码分层（V1.3.9 core/feature）

```
com.btsheng.erp/
├── ErpApplication.kt          ← Application（Hilt 入口 + WorkManager 调度）
├── core/                      ← 核心基础设施（无业务逻辑）
│   ├── data/
│   │   ├── local/             ← Room DAO/Entity（PendingScan/Message/Conflict）
│   │   └── sync/              ← PendingSyncWorker / SyncScheduler
│   ├── scan/                  ← QrCodeParser（5 类码路由表）
│   ├── security/              ← TokenStore（JWT in EncryptedSharedPreferences）
│   └── sync/                  ← ConflictResolver（SERVER_WINS/USER_CHOICE）
└── feature/                   ← 业务模块（按功能/版本分包）
    ├── MainActivity.kt        ← 4 大 TAB 入口（Compose NavigationBar）
    ├── auth/                  ← LoginScreen（指纹 + 账号密码）
    ├── message/               ← MessageListScreen + ViewModel
    ├── scan/                  ← ScanScreen（首页扫码 Compose）
    ├── v138/                  ← V1.3.8 Feature（3 个 Activity + 3 个 Fragment + ApiClient）
    └── v139/                  ← V1.3.9 Feature（图纸权限 / 标签预览 / 打印）
```

## V1.3.9 关键升级

1. **仓储 Tab**（V1.3.5 新增）：
   - "到货扫码"入口 → 扫 WW- 委外单码 → 触发 `SHIPPING → PENDING_INSPECTION`
   - 自动通知生管 + 品质（APP + PC 红点）

2. **离线扫码**（V1.3.5 强化）：
   - Room 表 `offline_scan_record` 缓存 500 条
   - WorkManager 定期同步（网络恢复后 30s 内）
   - 冲突解决策略：SERVER_WINS（默认）/ USER_CHOICE

3. **JWT 鉴权**（V1.3.6 升级）：
   - Token 存 `EncryptedSharedPreferences`（AndroidX Security）
   - Auto-login：Token 有效期内不重新登录
   - Biometric：指纹解锁复用 Token

4. **签字件下载**（V1.3.6）：
   - APP 不参与（仅 web 端 + PC 端有下载入口）
   - 但消息中心通知"新对账签字件待下载"

5. **质量红线**：
   - 5 类码识别通过 ZXing `MultiFormatReader` + 自研 prefix 路由表
   - 离线模式下扫码不卡顿（P95 ≤ 1s）

## V1.3.9 标准结构重构（2026-06-15）

| 变更点 | 之前（V1.3.8） | 之后（V1.3.9） |
|--------|---------------|---------------|
| 根 build.gradle.kts | 即 app 项目 | 仅声明插件版本（apply false） |
| app/ 子模块 | 不存在 | 标准 Android Studio module |
| keystore.properties | 根目录 | **app/keystore.properties**（与 build.gradle.kts 同 module） |
| release.keystore | 根目录 | **app/release.keystore** |
| proguard-rules.pro | 不存在 | **app/proguard-rules.pro**（Kotlin/Hilt/Retrofit/Room） |
| res/values/ | 缺失 | **strings/colors/themes 三件套** |
| res/xml/file_paths.xml | 缺失 | FileProvider 路径声明（PDF 打印分享） |
| res/mipmap-anydpi-v26/ | 缺失 | Adaptive Icon |
| AndroidManifest.xml | 3 个 v138 Activity | + MainActivity（4 大 TAB）+ FileProvider |
| MainActivity | 缺失 | **feature/MainActivity.kt**（4 大 TAB Compose Scaffold） |
| .archive-v138-dead-code/ | 历史 6 个 Activity/Fragment 备份 | **删除**（已重写到 feature/v138/） |
| build/ .gradle/ | 历史构建产物 | **删除**（首次 Sync 重新生成） |

## 关联

- **产品仓**：`../smart-workshop-erp`（PRD / 架构 / Epic YAML / OpenAPI / Figma 资产）
- **后端仓**：`../backend-impl`（Spring Cloud Alibaba · API 端点 74 个）
- **前端仓**：`../web-impl`（Vue 3 + Element Plus）

## 下一步

- SM 萧何 `*draft` 生成所有 android stories（1.4 / 4.1-4.2 / 5.2-5.3 / 7.x / 12.x）
- Dev agent 实现 Story 1.4（APP 端登录 + 扫码壳）
- architect 鲁班 `*create-mobile-architecture` 生成详细移动端架构
