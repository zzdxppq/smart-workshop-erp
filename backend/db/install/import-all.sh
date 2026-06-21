#!/usr/bin/env bash
# 一键导入（等同于仅执行合并后的 init.sql）
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-10.100.4.10}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-xm_admin}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-Xm@admin@123!}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INIT_SQL="$(cd "$SCRIPT_DIR/.." && pwd)/init.sql"

if [[ ! -f "$INIT_SQL" ]]; then
  echo "init.sql 不存在，请先在 backend/db 运行 ./build-init.ps1 或手动合并" >&2
  exit 1
fi

echo "目标: ${MYSQL_HOST}:${MYSQL_PORT} 用户: ${MYSQL_USER}"
echo ">>> init.sql (全量 DDL + 迁移 + Mock)"

mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  --default-character-set=utf8mb4 < "$INIT_SQL"

echo ""
echo "全部导入完成。"
echo "演示账号: admin / sales / warehouse / buyer / finance / gm 等 12 角色，密码均为 123456"
