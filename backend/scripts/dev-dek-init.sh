#!/usr/bin/env bash
# scripts/dev-dek-init.sh
# 生成 /etc/erp/dek.key (AES-256-GCM, 32 bytes base64)
# V1.3.6 fail-fast: backend 启动时如果 DEK 文件不存在 → 立即退出 1
set -euo pipefail

DEK_PATH="${DEK_FILE:-/etc/erp/dek.key}"

if [ -f "$DEK_PATH" ]; then
  echo "[DEK] 已存在: $DEK_PATH"
  exit 0
fi

echo "[DEK] 生成新 DEK -> $DEK_PATH"
sudo mkdir -p "$(dirname "$DEK_PATH")"
sudo openssl rand -base64 32 | sudo tee "$DEK_PATH" > /dev/null
sudo chmod 600 "$DEK_PATH"

echo "[DEK] OK · $(sudo ls -l "$DEK_PATH")"
