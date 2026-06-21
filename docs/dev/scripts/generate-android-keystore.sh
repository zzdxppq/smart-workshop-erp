#!/usr/bin/env bash
# ============================================================
# V1.3.8 Sprint 11 Story 11.4 · android release keystore 生成脚本
#
# 用法：./generate-android-keystore.sh
# 前提：JDK 17（keytool 命令可用）
# 产出：android-impl/release.keystore + keystore.properties
# ============================================================

set -e

ANDROID_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../android-impl" && pwd)"
KEYSTORE_FILE="$ANDROID_DIR/release.keystore"
PROPS_FILE="$ANDROID_DIR/keystore.properties"
ALIAS="erp_release"
VALIDITY=10000
DNAME="CN=V1.3.8, OU=ERP, O=昆山佰泰胜, L=昆山, ST=江苏, C=CN"

log_info() { echo -e "\033[0;34m[INFO]\033[0m $*"; }
log_warn() { echo -e "\033[1;33m[WARN]\033[0m $*"; }
log_success() { echo -e "\033[0;32m[SUCCESS]\033[0m $*"; }

# 1. 检查 keystore 是否已存在
if [ -f "$KEYSTORE_FILE" ]; then
    log_warn "release.keystore 已存在，跳过生成（删除后重跑）"
    exit 0
fi

# 2. 询问密码（实际生产用环境变量注入，不用 read -s）
log_info "生成 RSA 2048 release.keystore（validity 10000 天）"
keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 \
    -validity "$VALIDITY" \
    -dname "$DNAME" \
    -keystore "$KEYSTORE_FILE" \
    -storepass changeit_in_production \
    -keypass changeit_in_production

# 3. 生成 keystore.properties
cat > "$PROPS_FILE" <<EOF
# V1.3.8 Sprint 11 Story 11.4 · 自动生成
# ⚠️ 不要 commit 到 git（已加入 .gitignore）
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=changeit_in_production
RELEASE_KEY_ALIAS=$ALIAS
RELEASE_KEY_PASSWORD=changeit_in_production
EOF

log_success "release.keystore + keystore.properties 生成完成"
log_info "位置：$ANDROID_DIR"
echo ""
echo "下一步："
echo "  cd $ANDROID_DIR"
echo "  ./gradlew assembleRelease"
echo ""
echo "⚠️ 警告：生产部署请修改默认密码 + 把 keystore 备份到安全位置"