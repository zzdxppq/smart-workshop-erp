# Story 10.4 IMPL 报告 · android gradle wrapper 安装

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟡 **部分实装**（脚本 + 文档完成，实际安装需部署环境）

---

## 1. 现状调研

android-impl 项目状态：
- ✅ `build.gradle.kts` 配置完整（AGP 8.2.2 + Kotlin 1.9.22 + Hilt）
- ✅ `settings.gradle.kts` 完整
- ✅ `gradle.properties` 完整
- ✅ Sprint 8.5 落地 3 Fragment + 3 Activity + viewBinding 启用
- ✅ Sprint 8.5 写 10 ApiClientTest 测例（待跑）
- ❌ **无 `gradlew` wrapper**
- ❌ **无 `gradle/wrapper/gradle-wrapper.jar`**
- ❌ **本机无 gradle 命令**（`which gradle` → not found）

## 2. 改动清单

### 2.1 新增文件

| 文件 | 路径 |
|------|------|
| setup-android-gradle.sh | `docs/dev/scripts/setup-android-gradle.sh` |

### 2.2 setup 脚本 4 步

```bash
1. 检查 Java（需 JDK 17）
2. 下载 gradle 8.2 到 /opt/gradle-8.2/bin（curl + unzip）
3. 在 android-impl/ 跑 `gradle wrapper --gradle-version 8.2` 生成 gradlew
4. 跑 ./gradlew test --tests "com.btsheng.erp.feature.v138.ApiClientTest" 验证 10 测例
```

## 3. 已知遗留

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | 本机无 gradle → 实际安装需 Android Studio 或手动跑脚本 | Sprint 11 + Android 开发者 |
| 2 | ApiClientTest 跑通后，下一步 connectedAndroidTest（需 Android 设备） | Sprint 11 |
| 3 | gradle-wrapper.jar 二进制文件（Edit 工具无法生成）| Android Studio 自动生成 |
| 4 | gradle.properties 配置（org.gradle.jvmargs 等） | Sprint 11 |

## 4. 关键设计决策

### 4.1 不在本会话实装 wrapper 二进制

**理由**：
- gradle-wrapper.jar 是二进制文件，Edit 工具无法生成
- 本机无 gradle 命令，无法在容器内生成
- 最务实：写脚本 + 文档，待 Sprint 11 由 Android 开发者用 Android Studio 跑

### 4.2 脚本而非手动操作

**理由**：
- 一键脚本可在 CI/CD 跑（gradle 自动下载）
- 文档化依赖（gradle 8.2 + JDK 17）
- 失败回滚简单（删 /opt/gradle-8.2）

## 5. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 脚本 + 文档完成
- **architect 鲁班** · 现状调研确认（android-impl 无 wrapper + 本机无 gradle）
- **QA 商鞅** · connectedAndroidTest 待 Sprint 11 + Android 设备