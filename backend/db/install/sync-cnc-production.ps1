# 补建 cnc_production（cnc_business 已存在 · 0 表时使用）
param(
    [string]$MysqlHost = "10.100.4.10",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "xm_admin",
    [string]$MysqlPassword = "Xm@admin@123!"
)

$ErrorActionPreference = "Stop"
$DbRoot = Join-Path $PSScriptRoot ".."
$files = @(
    (Join-Path $DbRoot "migrations\V60a__cnc_production_schema.sql"),
    (Join-Path $DbRoot "migrations\V60b__cnc_production_data.sql")
)

Write-Host ">>> sync cnc_production -> ${MysqlHost}:${MysqlPort}" -ForegroundColor Cyan
foreach ($f in $files) {
    if (-not (Test-Path $f)) { throw "Missing: $f" }
    Write-Host "    $(Split-Path $f -Leaf)" -ForegroundColor DarkGray
    Get-Content -Path $f -Raw -Encoding UTF8 |
        & mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword" --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw "Failed: $f" }
}

& mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword" -e `
    "SELECT table_schema, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='cnc_production' GROUP BY table_schema;"
Write-Host "Done." -ForegroundColor Green
