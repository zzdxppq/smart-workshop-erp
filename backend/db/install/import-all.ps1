# 一键导入（等同于仅执行合并后的 init.sql）
# 用法：.\import-all.ps1
#       .\import-all.ps1 -MysqlHost 127.0.0.1 -MysqlUser root -MysqlPassword secret

param(
    [string]$MysqlHost = "10.100.4.10",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "xm_admin",
    [string]$MysqlPassword = "Xm@admin@123!"
)

$ErrorActionPreference = "Stop"
$InitSql = Join-Path $PSScriptRoot "..\init.sql"
$SyncScript = Join-Path $PSScriptRoot "sync-cnc-production.ps1"

if (-not (Test-Path $InitSql)) {
    throw "init.sql 不存在，请先在 backend/db 运行 .\build-init.ps1"
}

Write-Host "目标: ${MysqlHost}:${MysqlPort} 用户: $MysqlUser" -ForegroundColor Green
Write-Host ">>> init.sql (全量 DDL + 迁移 + Mock)" -ForegroundColor Cyan

Get-Content -Path $InitSql -Raw -Encoding UTF8 |
    & mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword" --default-character-set=utf8mb4

if ($LASTEXITCODE -ne 0) {
    Write-Host "init.sql 有报错，尝试补建 cnc_production ..." -ForegroundColor Yellow
}

if (Test-Path $SyncScript) {
    & $SyncScript -MysqlHost $MysqlHost -MysqlPort $MysqlPort -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
}

& mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword" -e `
    "SELECT table_schema, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema IN ('cnc_platform','cnc_business','cnc_production') GROUP BY table_schema;"

Write-Host "`n全部导入完成。" -ForegroundColor Green
Write-Host "演示账号: admin / sales / warehouse / buyer / finance / gm 等 12 角色，密码均为 123456"
