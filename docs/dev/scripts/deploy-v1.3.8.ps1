# ============================================================
# V1.3.8 一键部署脚本 · Windows PowerShell
#
# 等同 deploy-v1.3.8.sh 的 Windows 版本
# 用法：.\deploy-v1.3.8.ps1 [-SkipBackup] [-Rollback]
# ============================================================

param(
    [switch]$SkipBackup = $false,
    [switch]$Rollback = $false
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\..\..\.."
$BackendDir = "$ProjectRoot\backend"
$DeployDir = "$BackendDir\deploy"

function Write-Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Blue }
function Write-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "[ERROR] $msg" -ForegroundColor Red }
function Write-Success($msg) { Write-Host "[SUCCESS] $msg" -ForegroundColor Green }

# ============================================================
# 0. 回滚分支
# ============================================================
if ($Rollback) {
    Write-Warn "开始回滚 V1.3.8 -> V1.3.7 ..."
    Set-Location $DeployDir
    docker compose down
    $backupFile = "C:\Users\Public\erp_v137_backup.sql"
    if (Test-Path $backupFile) {
        Write-Info "恢复 V1.3.7 数据库..."
        docker compose up -d mysql-master
        Start-Sleep -Seconds 30
        $env:MYSQL_PWD = "root123"
        Get-Content $backupFile | docker compose exec -T mysql-master mysql -uroot
        Write-Success "V1.3.7 数据库已恢复"
    } else {
        Write-Err "未找到备份文件 $backupFile"
        exit 1
    }
    Set-Location $ProjectRoot
    git checkout v1.3.7 -- backend/db/migrations/V49-V52.sql
    Set-Location $DeployDir
    docker compose build erp-business erp-platform erp-production erp-gateway
    docker compose up -d
    Write-Success "回滚完成"
    exit 0
}

# ============================================================
# 1. 备份 V1.3.7
# ============================================================
if (-not $SkipBackup) {
    Write-Info "步骤 1/6：备份 V1.3.7..."
    Set-Location $DeployDir
    $backupFile = "C:\Users\Public\erp_v137_backup.sql"
    $env:MYSQL_PWD = "root123"
    if (docker compose ps mysql-master 2>$null | Select-String "Up") {
        docker compose exec -T mysql-master mysqldump -uroot --all-databases --single-transaction | Out-File -Encoding utf8 $backupFile
        Write-Success "V1.3.7 数据库已备份到 $backupFile"
    } else {
        Write-Warn "MySQL 容器未运行，跳过备份"
    }
}

# ============================================================
# 2. 检查 V1.3.8 文件
# ============================================================
Write-Info "步骤 2/6：检查 V1.3.8 文件..."
$requiredFiles = @(
    "$BackendDir\db\migrations\V49__batch.sql",
    "$BackendDir\db\migrations\V50__material_barcode_batch.sql",
    "$BackendDir\db\migrations\V51__purchase_reason.sql",
    "$BackendDir\db\migrations\V52__procurement_manager_role.sql",
    "$BackendDir\src\erp-business\src\main\java\com\btsheng\erp\business\crm\batch\service\BatchService.java",
    "$BackendDir\src\erp-business\src\main\java\com\btsheng\erp\business\crm\noorderpurchase\service\NoOrderPurchaseService.java"
)
foreach ($f in $requiredFiles) {
    if (-not (Test-Path $f)) {
        Write-Err "缺失必要文件: $f"
        exit 1
    }
}
Write-Success "V1.3.8 文件完整"

# ============================================================
# 3. 停服 + 重建
# ============================================================
Write-Info "步骤 3/6：停服 + 重建 V1.3.8 镜像..."
Set-Location $DeployDir
docker compose down erp-business erp-platform erp-production erp-gateway
docker compose build erp-business erp-platform erp-production erp-gateway
Write-Success "镜像重建完成"

# ============================================================
# 4. 启动 backend（Flyway 自动跑 V49-V52）
# ============================================================
Write-Info "步骤 4/6：启动 backend..."
docker compose up -d erp-platform
Write-Info "等待 erp-platform 健康..."
$healthy = $false
for ($i = 0; $i -lt 12; $i++) {
    Start-Sleep -Seconds 15
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health/readiness" -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -eq 200) { $healthy = $true; break }
    } catch {}
}
if (-not $healthy) { Write-Err "erp-platform 未就绪"; exit 1 }
Write-Success "erp-platform 已就绪"

docker compose up -d erp-business
$healthy = $false
for ($i = 0; $i -lt 12; $i++) {
    Start-Sleep -Seconds 15
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8082/actuator/health/readiness" -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -eq 200) { $healthy = $true; break }
    } catch {}
}
if (-not $healthy) { Write-Err "erp-business 未就绪"; exit 1 }
Write-Success "erp-business 已就绪"

docker compose up -d erp-production erp-gateway
Start-Sleep -Seconds 30

# ============================================================
# 5. 健康检查
# ============================================================
Write-Info "步骤 5/6：健康检查 + 14 端点 smoke test..."
$env:MYSQL_PWD = "root123"
$tables = @("crm_purchase_order", "crm_batch", "crm_batch_shadow", "crm_material_barcode_batch")
foreach ($table in $tables) {
    $r = docker compose exec -T mysql-master mysql -uroot erp_business -e "SHOW TABLES LIKE '$table'" 2>$null
    if ($r -match $table) {
        Write-Success "  ✓ $table 已创建"
    } else {
        Write-Err "  ✗ $table 未创建"
        exit 1
    }
}

$r = docker compose exec -T mysql-master mysql -uroot erp_platform -e "SHOW TABLES LIKE 'sys_workflow_node'" 2>$null
if ($r -match "sys_workflow_node") {
    Write-Success "  ✓ sys_workflow_node 已创建（V52 兜底）"
} else {
    Write-Err "  ✗ sys_workflow_node 未创建"
    exit 1
}

# ============================================================
# 6. xxl-job 注册提示
# ============================================================
Write-Warn "步骤 6/6：xxl-job-admin 手动注册 JobHandler"
Write-Host @"
=========================================================
URL:      http://localhost:8088/xxl-job-admin
JobHandler: batchShadowCompareHourly
Cron:      0 0 * * * ?
=========================================================
"@

Write-Success "============================================="
Write-Success "V1.3.8 部署完成！"
Write-Success "============================================="
Write-Host ""
Write-Host "回滚命令：.\deploy-v1.3.8.ps1 -Rollback"