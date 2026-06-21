# Merge baseline DDL + Flyway migrations into single init.sql
# V94 改造：去除 mock 数据（demo-flow-seed/V60b/V61/V62 mock seed/V72/V75/V78/V20/V21/V38-V48 seed）
# init.sql 现在仅含 DDL + 系统基础初始化（sys_user admin / sys_role / sys_menu / sys_dict / sys_workflow / mdm_process）+ 静态主数据（V6/V7/V8/V9/V10 静态 seed）
# 全量 mock 数据请走 init_data.sql（用 build-init-data.ps1 生成）
param(
    [string]$OutFile = (Join-Path $PSScriptRoot "init.sql")
)

$ErrorActionPreference = "Stop"
$DbRoot = $PSScriptRoot
$BaselineFile = Join-Path $DbRoot "init.baseline.sql"

function Read-Text([string]$Path) {
    if (-not (Test-Path $Path)) { throw "Missing: $Path" }
    return [System.IO.File]::ReadAllText($Path, [System.Text.UTF8Encoding]::new($false))
}

function Get-MigrationVersion([string]$Name) {
    if ($Name -match '^V(\d+)__') { return [int]$Matches[1] }
    return 99999
}

function Add-IncludeBlock([System.Collections.Generic.List[string]]$Lines, [string]$Label, [string]$Body) {
    $Lines.Add("")
    $Lines.Add("-- include: $Label")
    $Lines.Add($Body.Trim())
}

function Test-BaselineHasInclude([string]$Baseline, [string]$Label) {
    return $Baseline.Contains("-- include: $Label")
}

if (-not (Test-Path $BaselineFile)) {
    Copy-Item (Join-Path $DbRoot "init.sql") $BaselineFile
}

$baseline = Read-Text $BaselineFile
$ddlEnd = $baseline.IndexOf("INSERT INTO ``sys_user``")
if ($ddlEnd -lt 0) { throw "Cannot locate seed section in baseline" }
$ddlEnd = $baseline.LastIndexOf("--", $ddlEnd)

$footerIdx = $baseline.IndexOf("SET FOREIGN_KEY_CHECKS = 1;", $ddlEnd)
if ($footerIdx -lt 0) { throw "Cannot find FOREIGN_KEY_CHECKS footer in baseline" }

$ddl = $baseline.Substring(0, $ddlEnd).TrimEnd()
$seed = $baseline.Substring($ddlEnd, $footerIdx - $ddlEnd).TrimEnd()

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("-- ============================================================")
$lines.Add("-- 昆山佰泰胜专属 ERP · 数据库一键初始化（空库专用 · 无 mock）")
$lines.Add("-- 库名：cnc_platform / cnc_business / cnc_production")
$lines.Add("-- 用法：mysql -h HOST -u USER -p --default-character-set=utf8mb4 < backend/db/init.sql")
$lines.Add("-- 维护：改 init.baseline.sql 或 migrations 后执行 build-init.ps1")
$lines.Add("-- 含全量 Mock（50 订单 + 50 员工 + 演示工单）：mysql ... < backend/db/init_data.sql")
$lines.Add("-- ============================================================")
$lines.Add("")
$lines.Add("SET NAMES utf8mb4;")
$lines.Add("SET collation_connection = 'utf8mb4_unicode_ci';")
$lines.Add("SET FOREIGN_KEY_CHECKS = 0;")
$lines.Add("")
$lines.Add($ddl)
$lines.Add("")
$lines.Add($seed)

# V94 · init.baseline.sql 已经包含所有需要的基础 include 块；
# 下面只拼接 baseline 之外且不在 baseline 中的 V 文件（V94 之后新增的 migration 才会被追加）
$skipMigrations = @(
    'V58__drawing_link_partial_index.sql',
    'V60__cnc_production_schema.sql',
    'V60a__cnc_production_schema.sql',
    'V60b__cnc_production_data.sql'
)
Get-ChildItem (Join-Path $DbRoot "migrations\V*.sql") |
    Where-Object { $skipMigrations -notcontains $_.Name } |
    Sort-Object { Get-MigrationVersion $_.Name } |
    ForEach-Object {
        if (Test-BaselineHasInclude $baseline $_.Name) { return }
        Add-IncludeBlock $lines $_.Name (Read-Text $_.FullName)
        if ($_.Name -eq 'V62__prod_machine_and_workorder_process.sql') {
            $v58 = Join-Path $DbRoot "migrations\V58__drawing_link_partial_index.sql"
            if (Test-Path $v58) {
                Add-IncludeBlock $lines "V58__drawing_link_partial_index.sql (post-V62 indexes)" (Read-Text $v58)
            }
            $v60a = Join-Path $DbRoot "migrations\V60a__cnc_production_schema.sql"
            if (Test-Path $v60a) {
                Add-IncludeBlock $lines "V60a__cnc_production_schema.sql (post-V62 schema)" (Read-Text $v60a)
            }
        }
    }

# V94 · 不再拼接 demo-flow-seed.sql 与 V60b（mock 已从 baseline 移除）
# footer: 仅追加 cnc_production schema（DLL）确保 33 张生产域表存在
$v60aPath = Join-Path $DbRoot "migrations\V60a__cnc_production_schema.sql"
if (Test-Path -LiteralPath $v60aPath) {
    Add-IncludeBlock $lines "V60a__cnc_production_schema.sql (footer ensure)" (Read-Text $v60aPath)
}

$lines.Add("")
$lines.Add("SET FOREIGN_KEY_CHECKS = 1;")
$lines.Add("")
$lines.Add("SELECT 'cnc_platform' AS db, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'cnc_platform'")
$lines.Add("UNION ALL SELECT 'cnc_business', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_business'")
$lines.Add("UNION ALL SELECT 'cnc_production', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_production';")
$lines.Add("SELECT username, real_name FROM cnc_platform.sys_user WHERE id IN (1);")
$lines.Add("-- Full mock (50 orders + bulk seed): mysql ... < backend/db/init_data.sql")

$out = ($lines -join [Environment]::NewLine) + [Environment]::NewLine
[System.IO.File]::WriteAllText($OutFile, $out, [System.Text.UTF8Encoding]::new($false))

$annotator = Join-Path $DbRoot "tools\annotate-init-comments.py"
if (Test-Path $annotator) {
    python $annotator $OutFile
    if ($LASTEXITCODE -ne 0) { throw "annotate-init-comments.py failed with exit code $LASTEXITCODE" }
}

$kb = [math]::Round((Get-Item $OutFile).Length / 1KB, 1)
Write-Host ("Wrote " + $OutFile + " (" + $kb + " KB)")