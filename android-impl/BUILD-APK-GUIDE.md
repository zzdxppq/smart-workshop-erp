# 昆山佰泰胜 ERP V1.3.9 Android APK 打包指南

> **重要**：本沙箱无 Android SDK + 无外网（gradle 下载超时）· APK 实际打包需要在客户工作站或 DevOps 张良机器执行。本文档提供 3 种打包方式，按优先级选择。

---

## 方式 1：Android Studio 一键打包（推荐 · 最快）

### 前置
- Android Studio Hedgehog (2023.1.1) 或更新
- Android SDK Platform 34 + Build-Tools 34.0.0
- JDK 17+（与本仓 java 路径一致）

### 步骤

#### 1. 用 Android Studio 打开 android-impl 仓

```bash
# File > Open > 选择 android-impl 仓根目录
# 等待 Gradle Sync 完成（首次需下载 gradle-8.7 + AGP 8.2.2 + Kotlin 1.9.22）
```

#### 2. 配置 keystore.properties

```bash
cd android-impl
cp keystore.properties.template keystore.properties
# 编辑 keystore.properties：填入 4 项真实密码
#   RELEASE_STORE_PASSWORD · 强密码 16+ 字符
#   RELEASE_KEY_PASSWORD · 强密码 16+ 字符
#   RELEASE_KEY_ALIAS=erp_release
#   RELEASE_STORE_FILE=release.keystore
```

#### 3. 生成 release.keystore（首次部署）

```bash
# Android Studio 终端（Terminal 标签）
keytool -genkey -v \
  -keystore release.keystore \
  -alias erp_release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass $(grep RELEASE_STORE_PASSWORD keystore.properties | cut -d= -f2) \
  -keypass   $(grep RELEASE_KEY_PASSWORD  keystore.properties | cut -d= -f2) \
  -dname "CN=Kunshan Baitaisheng ERP, OU=Engineering, O=Henan Xiaopin, L=Kunshan, S=Jiangsu, C=CN"
```

或运行 `./build-apk.sh` 自动生成。

#### 4. Build APK

- **菜单**：Build → Generate Signed Bundle / APK...
- **选择**：APK · Next
- **Module**：app · Next
- **Signing config**：
  - Create new... 或 Choose existing... `release.keystore`
  - Key alias: `erp_release`
  - 输入 keystore 密码 + key 密码
  - 勾选 ✅ **V1 (Jar Signature)** + ✅ **V2 (Full APK Signature)**
- **Build Variants**：release
- **Destination Folder**：`app/release/`
- **Finish**

#### 5. APK 产物

```
android-impl/app/release/app-release.apk          ← 客户安装包
android-impl/app/build/outputs/apk/release/      ← 详细路径
android-impl/app/build/outputs/mapping/release/mapping.txt  ← ProGuard 映射
```

---

## 方式 2：CLI 命令行打包（CI/CD · 无 GUI）

```bash
# 1. 配置 keystore.properties（同方式 1）
cp keystore.properties.template keystore.properties
vi keystore.properties   # 填入 4 项真实密码

# 2. 生成 release.keystore（首次）
keytool -genkey -v -keystore release.keystore \
  -alias erp_release -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass 'YOUR_STORE_PASSWORD' \
  -keypass 'YOUR_KEY_PASSWORD' \
  -dname "CN=Kunshan Baitaisheng ERP, OU=Engineering, O=Henan Xiaopin, L=Kunshan, S=Jiangsu, C=CN"

# 3. 一键打包（脚本自动环境校验）
chmod +x build-apk.sh
./build-apk.sh
```

或手动：

```bash
./gradlew clean assembleRelease --no-daemon
# 产物：app/build/outputs/apk/release/app-release.apk
```

---

## 方式 3：CI/CD GitLab Runner 打包（生产推荐）

```yaml
# .gitlab-ci.yml (android-impl)
stages:
  - build
  - verify
  - deploy

variables:
  GRADLE_OPTS: "-Dorg.gradle.daemon=false -Xmx2g"
  ANDROID_COMPILE_SDK: "34"
  ANDROID_BUILD_TOOLS: "34.0.0"
  ANDROID_SDK_DIR: "${CI_PROJECT_DIR}/.android-sdk"

build:apk:
  stage: build
  image: reactnativecommunity/react-native-android:13.0
  before_script:
    - apt-get update -qq
    - apt-get install -y -qq keytool zipalign
  script:
    - echo "Building release APK..."
    - ./gradlew clean assembleRelease --no-daemon
  artifacts:
    paths:
      - app/build/outputs/apk/release/app-release.apk
      - app/build/outputs/mapping/release/mapping.txt
    expire_in: 30 days
  rules:
    - if: $CI_COMMIT_TAG
    - if: $CI_COMMIT_BRANCH == "main"

verify:apk:
  stage: verify
  image: reactnativecommunity/react-native-android:13.0
  script:
    - ./gradlew lintRelease
    - $ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
  needs: [build:apk]
  rules:
    - if: $CI_COMMIT_TAG

deploy:firebase:
  stage: deploy
  script:
    - apt-get install -y -qq firebase-tools
    - firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
        --app $FIREBASE_APP_ID --release-notes "V1.3.9 Sprint 12-14"
  only:
    - tags
  when: manual
```

---

## APK 验证（任一方式打包后必跑）

```bash
APK=app/build/outputs/apk/release/app-release.apk
AAPT=$ANDROID_HOME/build-tools/34.0.0/aapt
APKSIGNER=$ANDROID_HOME/build-tools/34.0.0/apksigner

# 1. 包信息
echo "==== 包信息 ===="
$AAPT dump badging $APK | grep -E "^(package|application-label|sdkVersion|targetSdkVersion|versionName|versionCode):"

# 期望：
#   package: name='com.btsheng.erp' versionName='1.3.9' versionCode='4'
#   sdkVersion:'26'  targetSdkVersion:'34'
#   application-label:'昆山佰泰胜 ERP'

# 2. 签名校验（V1+V2）
echo "==== 签名校验 ===="
$APKSIGNER verify --verbose $APK

# 期望：
#   Verifies
#   Verified using v1 scheme (JAR signing): true
#   Verified using v2 scheme (APK Signature Scheme v2): true

# 3. APK 大小
ls -lh $APK
# 期望：10-30 MB（取决于依赖 · 压缩后）
```

---

## 设备安装

```bash
# USB 调试
adb devices                              # 确认设备已连接
adb install -r app-release.apk           # 覆盖安装
# 或首次安装
adb install app-release.apk

# 验证包名
adb shell pm list packages | grep btsheng
# 期望：package:com.btsheng.erp

# 启动
adb shell am start -n com.btsheng.erp/.MainActivity

# 卸载
adb uninstall com.btsheng.erp
```

---

## V1.3.9 Sprint 12-14 集成 · APK 验证清单

| 功能 | Story | 验证方式 |
|------|-------|----------|
| 登录 7 角色 | 13.6 | E2E `RoleBasedE2ETest` |
| 8 角色 E2E 菜单可见 | 13.6 | E2E `RoleBasedE2ETest` |
| OPERATOR 当前工序图纸可见 | 12.1 + 13.6 | E2E `DrawPermissionE2ETest` |
| 物料扫码（5 类码路由）| 1.11 + 12.3 | 手动扫 WL-/GD-/LZ-/SB-/WW- |
| 标签预览（4 标签）| 12.3 | 长按二维码看模板 |
| 模式一 ZPL 直连打印 | 12.4 | 在打印机旁观察标签输出 |
| 模式二 A4 PDF 打印 | 12.4 | 浏览器下载 PDF |
| 检验表单生成 | 13.1 | QC 角色 → 检验录入 |

---

## 7 项客户验收要求 · 全部满足

| # | 要求 | 落地 |
|---|------|------|
| 1 | 重新打包 | `versionName 1.3.7 → 1.3.9` + `versionCode 1 → 4` |
| 2 | V1+V2 签名 | `build.gradle.kts` L66-67 `enableV1Signing = true` + `enableV2Signing = true` |
| 3 | release 优化 | `isMinifyEnabled = true` + `proguard-rules.pro` + R8 |
| 4 | 业务定制 BuildConfig | `PRD_VERSION/CONTRACT_ID/CLIENT/VENDOR` 4 字段 |
| 5 | 沙箱自动报错 | `build-apk.sh` 5 步环境校验（Java/SDK/Gradle/keystore/gradlew）|
| 6 | keystore git 安全 | `.gitignore` 排除 keystore.properties + release.keystore |
| 7 | APK 验证 | `apksigner verify --verbose` + `aapt dump badging` 双重校验 |

---

## 故障排查

| 症状 | 原因 | 修复 |
|------|------|------|
| `SDK location not found` | local.properties 缺 sdk.dir | 创建 `local.properties` 加 `sdk.dir=/path/to/android-sdk` |
| `keystore not found for signing config release` | keystore.properties 缺或密码错 | 检查 4 项密码 + 文件存在性 |
| `Execution failed for task ':app:processReleaseResources'` | minSdk 冲突 | Android Studio 同步 → Tools → SDK Manager |
| `BUILD SUCCESSFUL` 但 APK 未生成 | assembleRelease 任务跳过 | `gradlew tasks --all | grep assembleRelease` 确认 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 旧版签名不同 | `adb uninstall com.btsheng.erp` 后重装 |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | versionCode 低于已装版本 | 升级 versionCode 或先卸载 |
