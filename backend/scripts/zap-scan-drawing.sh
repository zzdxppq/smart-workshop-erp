#!/bin/bash
# ZAP · Story 1.7 · 图纸 6 类越权扫描
# 6 类：版本越权 / 加密绕过 / 二次密码绕过 / 状态机越权 / 签字越权 / PDF 越权

set -e

ZAP_PORT=${ZAP_PORT:-8080}
TARGET=${TARGET:-http://localhost:8082}
REPORT_DIR=${REPORT_DIR:-./zap-reports/drawing}

mkdir -p "${REPORT_DIR}"

echo "=========================================="
echo "Story 1.7 · ZAP 越权扫描"
echo "目标：${TARGET}"
echo "=========================================="

# 1. 版本越权：尝试 v1→v3 跳跃
echo "[1/6] 版本越权扫描 v1→v3 跳跃..."
curl -s -X POST "${TARGET}/api/v1/drawings/1/versions?operatorUserId=1001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"version":"v3","changeReason":"zap-bypass","pdfPath":"/tmp/zap.pdf"}' \
  -o "${REPORT_DIR}/01-version-bypass.json"
echo "  → 期望 40904 VERSION_NOT_STRICTLY_INCREASING"

# 2. 加密绕过：尝试读取未加密的签字扫描件
echo "[2/6] 加密绕过扫描..."
curl -s "${TARGET}/api/v1/drawings/1/signatures/raw" \
  -H "Authorization: Bearer ${TOKEN}" \
  -o "${REPORT_DIR}/02-encryption-bypass.json"
echo "  → 期望 404 NOT_FOUND（无 raw 端点）"

# 3. 二次密码绕过：FA 件 release 不带 adminPassword
echo "[3/6] 二次密码绕过扫描..."
curl -s -X POST "${TARGET}/api/v1/drawings/1/release?operatorUserId=1001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{}' \
  -o "${REPORT_DIR}/03-admin-password-bypass.json"
echo "  → 期望 40101 ADMIN_PASSWORD_REQUIRED"

# 4. 状态机越权：RELEASED → DRAFT 驳回
echo "[4/6] 状态机越权扫描..."
curl -s -X POST "${TARGET}/api/v1/drawings/2/reject?operatorUserId=1001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"reason":"zap-bypass"}' \
  -o "${REPORT_DIR}/04-state-machine-bypass.json"
echo "  → 期望 40904 DRAWING_STATE_INVALID（无 /reject 端点）"

# 5. 签字越权：尝试修改其他人的签字
echo "[5/6] 签字越权扫描..."
curl -s -X PUT "${TARGET}/api/v1/drawings/1/signatures/3?operatorUserId=1001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"signerUserId":9999,"signatureImagePath":"/tmp/zap.enc"}' \
  -o "${REPORT_DIR}/05-signature-bypass.json"
echo "  → 期望 404 NOT_FOUND（无 /signatures PUT 端点）"

# 6. PDF 越权：尝试导出未授权图纸
echo "[6/6] PDF 越权扫描..."
curl -s "${TARGET}/api/v1/drawings/export/9999?format=pdf" \
  -H "Authorization: Bearer ${TOKEN}" \
  -o "${REPORT_DIR}/06-pdf-bypass.json"
echo "  → 期望 40404 DRAWING_NOT_FOUND"

echo "=========================================="
echo "ZAP 扫描完成 · 报告：${REPORT_DIR}/"
echo "=========================================="
