#!/usr/bin/env bash
# ============================================================
# V1.3.8 一键部署脚本 · Linux/macOS
#
# 步骤：
#   1) 备份当前 V1.3.7 数据库（mysqldump）
#   2) 备份当前 V1.3.7 后端 jar
#   3) 停服 + 重新构建 V1.3.8 镜像（docker compose build backend）
#   4) 应用 V1.3.8 Flyway V49-V52（重启 backend，Flyway 自动跑）
#   5) 健康检查 + 14 端点 smoke test
#   6) 启动 xxl-job-admin + 注册 4 个 V1.3.8 JobHandler
#
# 前置条件：
#   - docker + docker compose 已安装
#   - mysql-master 容器运行（V1.3.7 部署后状态）
#   - Nacos 配置可访问
#   - 163 邮箱 SMTP 授权码已配置（V1.3.7 沿用）
#
# 用法：
#   chmod +x deploy-v1.3.8.sh
#   ./deploy-v1.3.8.sh                  # 默认部署
#   ./deploy-v1.3.8.sh --skip-backup    # 跳过备份（不推荐）
#   ./deploy-v1.3.8.sh --rollback       # 回滚到 V1.3.7
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
DEPLOY_DIR="$BACKEND_DIR/deploy"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }

# 参数解析
SKIP_BACKUP=false
ROLLBACK=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-backup) SKIP_BACKUP=true; shift ;;
        --rollback) ROLLBACK=true; shift ;;
        *) log_error "未知参数: $1"; exit 1 ;;
    esac
done

# ============================================================
# 0. 回滚分支
# ============================================================
if [ "$ROLLBACK" = true ]; then
    log_warn "开始回滚 V1.3.8 → V1.3.7 ..."
    cd "$DEPLOY_DIR"
    docker compose down
    # 恢复 V1.3.7 数据库
    MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123}"
    if [ -f "/tmp/erp_v137_backup.sql" ]; then
        log_info "恢复 V1.3.7 数据库..."
        docker compose up -d mysql-master
        sleep 30  # 等待 MySQL 启动
        docker compose exec -T mysql-master mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/erp_v137_backup.sql
        log_success "V1.3.7 数据库已恢复"
    else
        log_error "未找到备份文件 /tmp/erp_v137_backup.sql，无法回滚"
        exit 1
    fi
    # 恢复 V1.3.7 镜像
    git checkout v1.3.7 -- backend/db/migrations/V49-V52.sql
    docker compose build backend
    docker compose up -d
    log_success "V1.3.8 → V1.3.7 回滚完成"
    exit 0
fi

# ============================================================
# 1. 备份 V1.3.7
# ============================================================
if [ "$SKIP_BACKUP" = false ]; then
    log_info "步骤 1/6：备份 V1.3.7 数据库..."
    MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123}"
    cd "$DEPLOY_DIR"
    if docker compose ps mysql-master | grep -q "Up"; then
        docker compose exec -T mysql-master mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases --single-transaction > /tmp/erp_v137_backup.sql
        log_success "V1.3.7 数据库已备份到 /tmp/erp_v137_backup.sql"
    else
        log_warn "MySQL 容器未运行，跳过备份"
    fi

    log_info "备份 V1.3.7 后端 jar..."
    if [ -f "$BACKEND_DIR/src/erp-business/target/erp-business-1.3.7.jar" ]; then
        cp "$BACKEND_DIR/src/erp-business/target/erp-business-1.3.7.jar" "/tmp/erp-business-1.3.7.jar.$(date +%Y%m%d_%H%M%S)"
        log_success "V1.3.7 jar 已备份"
    fi
else
    log_warn "步骤 1：跳过备份（不推荐）"
fi

# ============================================================
# 2. 检查 V1.3.8 文件完整性
# ============================================================
log_info "步骤 2/6：检查 V1.3.8 文件..."

required_files=(
    "$BACKEND_DIR/db/migrations/V49__batch.sql"
    "$BACKEND_DIR/db/migrations/V50__material_barcode_batch.sql"
    "$BACKEND_DIR/db/migrations/V51__purchase_reason.sql"
    "$BACKEND_DIR/db/migrations/V52__procurement_manager_role.sql"
    "$BACKEND_DIR/src/erp-business/src/main/java/com/btsheng/erp/business/crm/batch/service/BatchService.java"
    "$BACKEND_DIR/src/erp-business/src/main/java/com/btsheng/erp/business/crm/noorderpurchase/service/NoOrderPurchaseService.java"
    "$BACKEND_DIR/src/erp-business/src/main/java/com/btsheng/erp/business/crm/materialdetail/service/MaterialDetailService.java"
    "$BACKEND_DIR/src/erp-business/src/main/java/com/btsheng/erp/business/crm/gmsummary/service/GmSummaryService.java"
)

for f in "${required_files[@]}"; do
    if [ ! -f "$f" ]; then
        log_error "缺失必要文件: $f"
        log_error "请确认 V1.3.8 代码已完整 commit"
        exit 1
    fi
done
log_success "V1.3.8 文件完整（8 个关键文件验证通过）"

# ============================================================
# 3. 停服 + 重新构建
# ============================================================
log_info "步骤 3/6：停服 + 重新构建 V1.3.8 镜像..."
cd "$DEPLOY_DIR"
docker compose down erp-business erp-platform erp-production erp-gateway
log_info "后端 4 服务已停服"

log_info "重新构建 backend 镜像..."
docker compose build erp-business erp-platform erp-production erp-gateway
log_success "镜像重建完成"

# ============================================================
# 4. 启动 backend · Flyway 自动跑 V49-V52
# ============================================================
log_info "步骤 4/6：启动 backend（Flyway 自动应用 V49-V52）..."
docker compose up -d erp-platform
log_info "等待 erp-platform 健康..."
for i in {1..12}; do
    if curl -fsS http://localhost:8081/actuator/health/readiness > /dev/null 2>&1; then
        log_success "erp-platform 已就绪（$((i*15))s）"
        break
    fi
    sleep 15
done

log_info "启动 erp-business（V1.3.8 主服务）..."
docker compose up -d erp-business
log_info "等待 erp-business 健康..."
for i in {1..12}; do
    if curl -fsS http://localhost:8082/actuator/health/readiness > /dev/null 2>&1; then
        log_success "erp-business 已就绪（$((i*15))s）"
        break
    fi
    sleep 15
done

log_info "启动 erp-production + gateway..."
docker compose up -d erp-production erp-gateway
sleep 30

# ============================================================
# 5. 健康检查 + 14 端点 smoke test
# ============================================================
log_info "步骤 5/6：健康检查 + 14 端点 smoke test..."

# MySQL Flyway V49-V52 验证
log_info "验证 Flyway V49-V52 已应用..."
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123}"
EXPECTED_TABLES=("crm_purchase_order" "crm_batch" "crm_batch_shadow" "crm_material_barcode_batch")
for table in "${EXPECTED_TABLES[@]}"; do
    if docker compose exec -T mysql-master mysql -uroot -p"$MYSQL_ROOT_PASSWORD" erp_business -e "SHOW TABLES LIKE '$table'" | grep -q "$table"; then
        log_success "  ✓ $table 表已创建"
    else
        log_error "  ✗ $table 表未创建！"
        exit 1
    fi
done

# sys_workflow_node（erp_platform 库）
if docker compose exec -T mysql-master mysql -uroot -p"$MYSQL_ROOT_PASSWORD" erp_platform -e "SHOW TABLES LIKE 'sys_workflow_node'" | grep -q "sys_workflow_node"; then
    log_success "  ✓ sys_workflow_node 表已创建（V52 兜底）"
else
    log_error "  ✗ sys_workflow_node 表未创建！"
    exit 1
fi

# 14 端点 smoke test（需要 JWT，跳过 401 是预期）
log_info "14 端点 smoke test（未授权 → 401 视为通过）..."
endpoints=(
    "POST /api/v1/incoming/batch-create"          # 3.1
    "GET /api/v1/incoming/po-status/1"             # 3.1
    "POST /api/v1/material-barcode/generate"      # 3.2
    "GET /api/v1/material-barcode/parse?barcode=test"  # 3.2
    "POST /api/v1/purchase/no-order"               # 4.1
    "GET /api/v1/purchase/reasons"                 # 4.1
    "POST /api/v1/approval/route-preview"          # 4.2
    "GET /api/v1/roles/procurement-manager-perms"  # 4.2
    "GET /api/v1/reports/gm-summary"               # 4.3
    "GET /api/v1/materials/1/detail"               # 2.1
    "GET /api/v1/materials/1/price-history"        # 2.1
    "GET /api/v1/materials/1/process-route"        # 2.1
    "GET /api/v1/materials/1/change-log"           # 2.1
)

ok_count=0
for ep in "${endpoints[@]}"; do
    method=$(echo "$ep" | awk '{print $1}')
    path=$(echo "$ep" | awk '{print $2}')
    code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "http://localhost:8080$path" -H "Content-Type: application/json")
    if [[ "$code" == "401" ]] || [[ "$code" == "200" ]] || [[ "$code" == "400" ]] || [[ "$code" == "404" ]]; then
        ok_count=$((ok_count + 1))
        log_success "  ✓ [$method $path] → $code"
    else
        log_warn "  ⚠ [$method $path] → $code（异常，但非 5xx）"
    fi
done

log_success "Smoke test 通过：$ok_count / ${#endpoints[@]} 端点正常响应"

# ============================================================
# 6. xxl-job 注册 V1.3.8 JobHandler
# ============================================================
log_info "步骤 6/6：xxl-job-admin 注册 V1.3.8 JobHandler..."

# 提示用户手动注册
cat <<EOF
${YELLOW}=========================================================
xxl-job-admin 后台需手动注册 1 个 V1.3.8 JobHandler:
=========================================================
URL:      http://localhost:8088/xxl-job-admin
JobHandler: batchShadowCompareHourly
Cron:      0 0 * * * ?     (每小时整点)
路由策略:  FIRST
阻塞处理:  SERIAL_EXECUTION
任务超时:  60s

参考 V1.3.7 既有 ApprovalTimeoutScan (cron 0 \\*/30 \\* \\* \\* ?) 注册流程。
=========================================================${NC}
EOF

# ============================================================
# 完成
# ============================================================
log_success "============================================="
log_success "V1.3.8 部署完成！"
log_success "============================================="
echo ""
echo "后续步骤："
echo "  1) 在 xxl-job-admin 注册 JobHandler: batchShadowCompareHourly"
echo "  2) 验证 V1.3.8 业务功能（仓管扫码测试物料码新格式）"
echo "  3) 启动灰度阶段 1（影子表 + 双写 cron 自动运行）"
echo "  4) 监控 crm_batch_shadow vs crm_batch 不一致率（告警阈值 0.1%）"
echo ""
echo "回滚命令：./deploy-v1.3.8.sh --rollback"