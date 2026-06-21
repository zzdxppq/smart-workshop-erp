# Sprint 11 IMPL 报告 · 5 项部署缺陷修复

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 5 项修复全部闭环

---

## 1. 修复明细

### 1.1 Story 11.1 web dist/index.html 标题 ✅

**改动**：
```html
<!-- Before -->
<title>昆山佰泰胜 ERP · V1.3.7</title>
<!-- After -->
<title>昆山佰泰胜 ERP · V1.3.8</title>
```

**影响**：下次 `npm run build` 后 dist/index.html 标题正确。

### 1.2 Story 11.2 web Login.vue 完整重做 ✅

**改动**：
- ❌ Before: 极简 `<style scoped>` 只有 1 行 `margin: 100px auto;`
- ✅ After: 卡片 + 渐变背景（linear-gradient #667eea → #764ba2）+ 居中 + 60px 品牌 LOGO + 22px 标题 + 13px 副标题 + 44px 渐变登录按钮 + 响应式 `@media (max-width: 480px)`
- ❌ Before: `useBaseStore().api.post(...)` + `r?.token || 'mock-token'`
- ✅ After: 真实 `/api/v1/auth/login` + token 兜底（无 mock）+ Loading 态 + 失败显式错误（msg/message 双兜底）

### 1.3 Story 11.3 web Login.vue mock-token 清理 ✅

合并入 11.2 完成。

### 1.4 Story 11.4 android release signingConfig ✅

**改动**：

#### build.gradle.kts
```kotlin
release {
    ...
    signingConfig = signingConfigs.getByName("release")  // 新增
}

signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = java.util.Properties().apply {
            if (keystorePropertiesFile.exists()) {
                load(keystorePropertiesFile.inputStream())
            }
        }
        storeFile = file(keystoreProperties.getProperty("RELEASE_STORE_FILE") ?: "release.keystore")
        storePassword = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
        keyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
        keyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
        enableV1Signing = true   // JAR 签名（Android < 7 兼容）
        enableV2Signing = true   // APK 签名 v2（Android 7+ 强制）
    }
}
```

#### 新增文件
- `android-impl/keystore.properties.template`（4 项配置占位）
- `docs/dev/scripts/generate-android-keystore.sh`（4 步脚本）

**部署流程**：
```bash
1. cp keystore.properties.template keystore.properties
2. ./generate-android-keystore.sh（生成 release.keystore + 填 keystore.properties）
3. ./gradlew assembleRelease
4. APK 自动 V1+V2 签名
```

### 1.5 Story 11.5 web package.json 版本 V1.3.8 ✅

**改动**：
```json
{
  "name": "erp-web",
  "version": "1.3.8",  // Before: "1.3.7"
  ...
  "description": "昆山佰泰胜专属 ERP 系统 V1.3.8 - Web Frontend (Vue 3) · 6 Story SHARDED (2.1/3.1/3.2/4.1/4.2/4.3)",
}
```

---

## 2. 5 项修复总览

| Story | 文件 | 改动量 |
|-------|------|--------|
| 11.1 | `web-impl/index.html` | 1 行 |
| 11.2 + 11.3 | `web-impl/src/views/auth/Login.vue` | 187 行（完整重做） |
| 11.4 | `android-impl/build.gradle.kts` + 2 新文件 | +30 行 + keystore.properties.template + generate-android-keystore.sh |
| 11.5 | `web-impl/package.json` | 2 行 |
| **合计** | **5 文件 + 2 新增** | **~220 行改动** |

---

## 3. 关键设计决策

### 3.1 Login.vue 卡片 + 渐变背景参考 V1.3.7 UX Handoff

- 背景：linear-gradient(135deg, #667eea 0%, #764ba2 100%)（与 Element Plus 默认蓝紫色匹配）
- 卡片：max-width 420px + border-radius 12px + 阴影 hover
- 品牌 LOGO：60×60 圆角渐变方块 + "ERP" 文字
- 响应式：480px 以下取消阴影 + 缩小标题

### 3.2 signingConfig 用 Properties 注入而非硬编码

```kotlin
val keystoreProperties = java.util.Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}
```

**理由**：
- keystore.properties 不入 git（密钥安全）
- 本地无 keystore.properties 时回退默认值，编译不报错
- 生产部署用环境变量或 ConfigMap 注入

### 3.3 保留 V1 + 启用 V2 签名

- enableV1Signing = true：兼容 Android 4-6
- enableV2Signing = true：Android 7+ 强制

## 4. 已知遗留

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | 实际 keystore 生成（需 keytool + 真实密钥） | 部署环境 |
| 2 | 登录页 gradient 调色与 V1.3.7 UX Handoff 实际值差异 | 设计对齐 |
| 3 | Sprint 11.6 PRD §5 Epic table 补全 | 后续 |
| 4 | Sprint 10.1/10.2 OpenAPI + Playwright | Sprint 12 |

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 5 项修复 + signingConfig
- **architect 鲁班** · 渐变 + 卡片接受
- **PO 范蠡** · dist 部署时验证 + 真实 keystore 生成
- **QA 商鞅** · 登录页 E2E 验证 + APK 签名验证（生产环境）