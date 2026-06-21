#!/usr/bin/env bash
# ============================================================
# V1.3.8 Sprint 10 Story 10.4 · android gradle wrapper 安装脚本
#
# 现状：android-impl/ 没有 gradlew wrapper + 本机没 gradle
# 目标：装 gradle 8.2 + 生成 wrapper + 跑 10 ApiClientTest
#
# 前置：JDK 17（已装）+ curl 可用
# ============================================================

set -e

ANDROID_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../android-impl" && pwd)"
GRADLE_VERSION="8.2"

log_info() { echo -e "\033[0;34m[INFO]\033[0m $*"; }
log_success() { echo -e "\033[0;32m[SUCCESS]\033[0m $*"; }

# 1. 检查 Java
log_info "步骤 1/4：检查 Java"
if ! command -v java >/dev/null 2>&1; then
    echo "请先安装 JDK 17"
    exit 1
fi
java -version 2>&1 | head -1

# 2. 安装 gradle（如未装）
log_info "步骤 2/4：检查/安装 gradle $GRADLE_VERSION"
if ! command -v gradle >/dev/null 2>&1; then
    log_info "下载 gradle $GRADLE_VERSION..."
    curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip
    unzip -q /tmp/gradle.zip -d /opt/
    export PATH="/opt/gradle-${GRADLE_VERSION}/bin:$PATH"
    echo "export PATH=\"/opt/gradle-${GRADLE_VERSION}/bin:\$PATH\"" >> ~/.bashrc
    log_success "gradle $GRADLE_VERSION 安装到 /opt/gradle-$GRADLE_VERSION"
else
    log_success "gradle 已安装：$(gradle --version | grep ^Gradle)"
fi

# 3. 生成 wrapper
log_info "步骤 3/4：在 $ANDROID_DIR 生成 gradle wrapper"
cd "$ANDROID_DIR"
gradle wrapper --gradle-version "$GRADLE_VERSION"
ls -la gradlew gradle/wrapper/ 2>/dev/null | head -10

# 4. 跑测例
log_info "步骤 4/4：跑 ApiClientTest 10 测例"
./gradlew test --tests "com.btsheng.erp.feature.v138.ApiClientTest" --info 2>&1 | tail -20

log_success "============================================="
log_success "android-impl gradle wrapper 安装完成"
log_success "============================================="
echo ""
echo "后续："
echo "  - 跑全测例：cd android-impl && ./gradlew test"
echo "  - 跑连接测：./gradlew connectedAndroidTest（需 Android 设备）"