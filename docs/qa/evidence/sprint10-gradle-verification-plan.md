# Sprint 10 · Gradle Wrapper 实机验证计划

**项目**：smart-workshop-erp (Orchestrix)
**编写**：QA 商鞅
**编写日期**：2026-06-13
**状态**：待 Sprint 11 启动时执行
**Story 关联**：10.4（android-impl 仓 gradle wrapper 落盘）
**上下文**：
- Story 10.4 dev log：`docs/dev/logs/10.4-dev-log.md`
- 集成 E 验证报告：`docs/qa/evidence/sprint10-integration-test-report.md`

---

## 1. 计划总览

| 维度 | 内容 |
|------|------|
| 验证目标 | 10.4 沙箱 N/A 的 V3 / V4 / V7 实机验证（gradle wrapper 在真实工作站的可用性） |
| 验证范围 | `android-impl` 仓的 gradle wrapper 产物（4 文件 + sha256 + distributionUrl） |
| 验证方法 | 在 Linux / macOS / Windows 工作站分别执行命令链，逐一断言 V1–V9 |
| 截止时间 | Sprint 11 启动时（约 2026-06-23 ± 1 周） |
| 阻塞性 | **不阻塞** V1.3.8 FAT；Sprint 11 启动前置 |
| 失败回退 | AGP 8.2.2 + Gradle 8.7 实机不兼容 → 回退 Gradle 8.5 |
| 执行人 | dev 在 Sprint 11 启动工作站的"装机清单"中执行，QA 商鞅签字验收 |

---

## 2. 验证命令链

### 2.1 Linux / macOS 工作站

```bash
# 进入仓
cd android-impl

# V1 · 4 文件存在性
ls -la gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties

# V2 · SHA256 校验
sha256sum -c .gradle-wrapper-sha256.txt

# V3 · gradlew --version（期望 Gradle 8.7 + JVM 17 + Kotlin 1.9.x）
chmod +x gradlew
./gradlew --version

# V5 · 行尾检查（gradlew LF，gradlew.bat CRLF）
file gradlew gradlew.bat

# V6 · distributionUrl 锁定 8.7
grep distributionUrl gradle/wrapper/gradle-wrapper.properties

# V7 · 版本锁生效（模拟主机 gradle 8.10 漂移）
sdk install gradle 8.10
sdk use gradle 8.10
gradle --version          # 应显示 Gradle 8.10
./gradlew --version       # 应仍显示 Gradle 8.7（wrapper 锁生效）

# V8 · 额外 · assembleDebug
./gradlew assembleDebug
ls -la app/build/outputs/apk/debug/app-debug.apk

# V9 · 额外 · 8.5 八角色 E2E（connectedAndroidTest）
./gradlew connectedAndroidTest
```

### 2.2 Windows 工作站

```cmd
cd android-impl

REM V1 · 4 文件存在性
dir gradlew.bat gradle\wrapper\gradle-wrapper.jar gradle\wrapper\gradle-wrapper.properties gradlew

REM V2 · SHA256 校验（PowerShell 也可：Get-FileHash）
certutil -hashfile gradle\wrapper\gradle-wrapper.jar SHA256
type .gradle-wrapper-sha256.txt

REM V4 · gradlew.bat --version
gradlew.bat --version

REM V5 · 行尾
powershell -Command "Get-Content gradlew.bat | Select-Object -First 1 | Format-Hex"

REM V6 · distributionUrl
findstr distributionUrl gradle\wrapper\gradle-wrapper.properties

REM V8 · assembleDebug
gradlew.bat assembleDebug
dir app\build\outputs\apk\debug\app-debug.apk
```

---

## 3. 验证项清单

| 编号 | 项 | 命令 | 期望 |
|------|----|------|------|
| V1 | 4 文件存在 | `ls -la` / `dir` | `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` / `gradle-wrapper.properties` 全部存在 |
| V2 | SHA256 校验 | `sha256sum -c` / `certutil -hashfile` | `.gradle-wrapper-sha256.txt` 全部 `OK` |
| V3 | gradlew --version | `./gradlew --version` | 退出码 0；输出含 `Gradle 8.7` + `JVM: 17` + `Kotlin: 1.9.x` |
| V4 | gradlew.bat --version | `gradlew.bat --version` | 同 V3 |
| V5 | 行尾正确 | `file` / `Format-Hex` | `gradlew` = `LF`（无 CRLF）；`gradlew.bat` = `CRLF` |
| V6 | distributionUrl 锁定 | `grep` / `findstr` | `https\://services.gradle.org/distributions/gradle-8.7-bin.zip` |
| V7 | 版本锁生效 | `sdk install 8.10` + `./gradlew --version` | 主机 gradle 切到 8.10 后，`./gradlew --version` **仍**显示 8.7 |
| V8 | assembleDebug | `./gradlew assembleDebug` / `gradlew.bat assembleDebug` | 退出码 0；`app/build/outputs/apk/debug/app-debug.apk` 存在 |
| V9 | connectedAndroidTest | `./gradlew connectedAndroidTest` | 8.5 八角色 E2E 4 测例全部 PASS |

---

## 4. 失败处理

| 失败项 | 根因 | 处理动作 |
|--------|------|----------|
| V1 | 4 文件缺失 | dev 按 10.4 dev log 重新生成 wrapper（`gradle wrapper --gradle-version 8.7`） |
| V2 | sha256 不匹配 | `gradle-wrapper.jar` 损坏 → 从 services.gradle.org/distributions/gradle-8.7-bin.zip 重新解出 wrapper jar，重算 sha256 |
| V3 / V4 | wrapper jar 损坏或 JDK 不匹配 | 装 OpenJDK 17（Temurin 17.0.x），重下 wrapper jar，回归 V3/V4 |
| V5 | git autocrlf 污染 | `git config core.autocrlf input`（linux/mac）/ `true`（windows）后重新 `git checkout` 4 文件 |
| V6 | distributionUrl 写错或漂移 | 改回 `https://services.gradle.org/distributions/gradle-8.7-bin.zip` |
| V7 | distributionUrl 配置错误 | 同 V6，回归 V3 + V7 |
| V8 | AGP 8.2.2 + Gradle 8.7 实机不兼容 | 通知 PM 范蠡；回退 Gradle 8.5（`distributionUrl` 改 8.5），重跑 V8 |
| V9 | 8.5 八角色 E2E 漂移 | 8.5 dev 修（功能漂移与 wrapper 无关）；wrapper 验证仍记 PASS |

---

## 5. 顺序约束

1. **不需要 PM 决策前置**——10.4 是独立交付物，仅技术验收。
2. **Sprint 11 启动时执行**——验收窗口：2026-06-23 ± 1 周。
3. **验收顺序**：V1 → V2 → V3/V4 → V5 → V6 → V7 → V8 → V9；任一前置失败则阻断后续。
4. **通过判据**：V1–V8 全部 PASS；V9 为额外项（不在 10.4 验收边界，但作为 Sprint 11 联动健康度探针）。
5. **通过后**：android-impl 可被 dev 直接用 `./gradlew` 构建，无需本地装 gradle。

---

## 6. 截止时间

**Sprint 11 启动时（约 2026-06-23 ± 1 周）**

- 不阻塞 V1.3.8 FAT（V1.3.8 FAT 仅依赖 8.5 八角色 E2E + 集成 E）。
- 不阻塞 Sprint 10 收尾（10.4 dev log 已签字归档，wrapper 已落盘）。
- 必须 Sprint 11 启动会前完成，否则 dev 在新工作站首日无 `./gradlew` 可用。

---

## 7. 风险与阻塞

| 类型 | 描述 | 应对 |
|------|------|------|
| 硬阻塞 | **0 项** | — |
| 风险 | AGP 8.2.2 + Gradle 8.7 实机兼容性（dev 沙箱 N/A，未跑过真机） | 若 V8 失败 → 回退 Gradle 8.5（distributionUrl 改 8.5-bin.zip），8.5 是 AGP 8.2.2 官方兼容基线 |
| 风险 | 工作站 JDK 不是 17（项目要求 JVM 17） | 提前在装机清单写明 Temurin 17 |
| 风险 | `sdkman` 未装（V7 漂移模拟失败） | V7 改为 `export PATH=/tmp/gradle-8.10/bin:$PATH` 用临时解压替代 |

---

## 8. 签字

**QA 商鞅 · 2026-06-13**

- 计划已提交：Sprint 11 启动工作流
- 复核：集成 E 验证报告（V3/V4/V7 在沙箱 N/A 已显式标注，留待本计划实机收口）
- 复核：10.4 dev log（4 文件 + sha256 + distributionUrl 已落盘）
- 签字意见：执行无前置依赖，准予 Sprint 11 启动时执行
