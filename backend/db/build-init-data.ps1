# Merge baseline + migrations + demo seeds + bulk mock into init_data.sql
param(
    [string]$OutFile = (Join-Path $PSScriptRoot "init_data.sql")
)

$ErrorActionPreference = "Stop"
$DbRoot = $PSScriptRoot
$BaselineFile = Join-Path $DbRoot "init.baseline.sql"
$InstallDir = Join-Path $DbRoot "install"

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

function Strip-DemoWrapper([string]$Body) {
    $b = [regex]::Replace($Body, '(?s)^[\s\S]*?SET FOREIGN_KEY_CHECKS = 0;\s*', '')
    $b = [regex]::Replace($b, '(?s)\s*SET FOREIGN_KEY_CHECKS = 1;[\s\S]*$', '')
    return $b.Trim()
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
$lines.Add("-- Kunshan BTS ERP - init_data.sql (schema + full mock demo data)")
$lines.Add("-- DB: cnc_platform / cnc_business / cnc_production")
$lines.Add("-- Usage: mysql ... --default-character-set=utf8mb4 < backend/db/init_data.sql")
$lines.Add("-- Rebuild: cd backend/db && powershell -File build-init-data.ps1")
$lines.Add("-- WARNING: empty DB only - contains DROP TABLE")
$lines.Add("-- ============================================================")
$lines.Add("")
$lines.Add("SET NAMES utf8mb4;")
$lines.Add("SET collation_connection = 'utf8mb4_unicode_ci';")
$lines.Add("SET FOREIGN_KEY_CHECKS = 0;")
$lines.Add("")
$lines.Add($ddl)
$lines.Add("")
$lines.Add($seed)

$extraFiles = @(
    (Join-Path $DbRoot "..\src\main\resources\db\migration\V2__workflow_split.sql"),
    (Join-Path $DbRoot "..\src\main\resources\db\migration\V3__approval_record.sql")
)
foreach ($f in $extraFiles) {
    if (-not (Test-Path $f)) { continue }
    $leaf = Split-Path $f -Leaf
    if (Test-BaselineHasInclude $baseline $leaf) { continue }
    Add-IncludeBlock $lines $leaf (Read-Text $f)
}

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

$dataV54 = Join-Path $DbRoot "migrations\data\V54__migrate_drawing_link.sql"
if ((Test-Path $dataV54) -and -not (Test-BaselineHasInclude $baseline "V54__migrate_drawing_link.sql (data)")) {
    Add-IncludeBlock $lines "V54__migrate_drawing_link.sql (data)" (Read-Text $dataV54)
}

# demo-flow-seed
Add-IncludeBlock $lines "demo-flow-seed.sql" (Strip-DemoWrapper (Read-Text (Join-Path $InstallDir "demo-flow-seed.sql")))

# demo-finance-rfq-seed
$financeSeed = Join-Path $InstallDir "demo-finance-rfq-seed.sql"
if (Test-Path $financeSeed) {
    $fin = Read-Text $financeSeed
    $fin = [regex]::Replace($fin, '(?s)^[\s\S]*?USE `cnc_business`;\s*', '')
    Add-IncludeBlock $lines "demo-finance-rfq-seed.sql" $fin.Trim()
}

# bulk mock (50 orders + 50 employees) — 内联 SQL，勿用 SOURCE（Navicat/GUI 不支持）
$genScript = Join-Path $InstallDir "generate-mock-bulk-data.py"
if (-not (Test-Path $genScript)) { throw "Missing $genScript" }
python $genScript
if ($LASTEXITCODE -ne 0) { throw "generate-mock-bulk-data.py failed" }
$mockBulk = Join-Path $InstallDir "mock-bulk-seed.sql"
Add-IncludeBlock $lines "mock-bulk-seed.sql (inlined)" (Strip-DemoWrapper (Read-Text $mockBulk))

$v60b = Join-Path $DbRoot "migrations\V60b__cnc_production_data.sql"
if ((Test-Path $v60b) -and -not (Test-BaselineHasInclude $baseline "V60b__cnc_production_data.sql (post-demo sync)")) {
    Add-IncludeBlock $lines "V60b__cnc_production_data.sql (post-demo sync)" (Read-Text $v60b)
}

$v60aPath = Join-Path $DbRoot "migrations\V60a__cnc_production_schema.sql"
$v60bPath = Join-Path $DbRoot "migrations\V60b__cnc_production_data.sql"
if ((Test-Path -LiteralPath $v60aPath) -and -not (Test-BaselineHasInclude $baseline "V60a__cnc_production_schema.sql (footer ensure)")) {
    Add-IncludeBlock $lines "V60a__cnc_production_schema.sql (footer ensure)" (Read-Text $v60aPath)
}
if ((Test-Path -LiteralPath $v60bPath) -and -not (Test-BaselineHasInclude $baseline "V60b__cnc_production_data.sql (footer ensure)")) {
    Add-IncludeBlock $lines "V60b__cnc_production_data.sql (footer ensure)" (Read-Text $v60bPath)
}

# V1.4.0 · 近 30 日报工 + 绩效日聚合 mock
$v140Mock = Join-Path $InstallDir "v140-performance-mock-seed.sql"
if (Test-Path $v140Mock) {
    Add-IncludeBlock $lines "v140-performance-mock-seed.sql" (Read-Text $v140Mock)
}

$lines.Add("")
$lines.Add("USE ``cnc_platform``;")
$lines.Add("INSERT IGNORE INTO ``sys_role_permission`` (``role_id``, ``menu_id``, ``action``)")
$lines.Add("SELECT r.id, 112, 'view' FROM ``sys_role`` r")
$lines.Add("WHERE r.role_code IN ('PROD_MGR', 'PRODUCTION_MANAGER', 'OPERATOR', 'GM', 'ADMIN', 'SYS_ADMIN');")

$lines.Add("")
$lines.Add("SET FOREIGN_KEY_CHECKS = 1;")
$lines.Add("")
$lines.Add("SELECT 'cnc_platform' AS db, COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'cnc_platform'")
$lines.Add("UNION ALL SELECT 'cnc_business', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_business'")
$lines.Add("UNION ALL SELECT 'cnc_production', COUNT(*) FROM information_schema.tables WHERE table_schema = 'cnc_production';")
$lines.Add("SELECT COUNT(*) AS mock_orders FROM cnc_business.crm_order WHERE order_no LIKE 'XS-MOCK-%';")
$lines.Add("SELECT COUNT(*) AS mock_employees FROM cnc_business.crm_hr_employee WHERE employee_no LIKE 'EM-MOCK-%';")
$lines.Add("SELECT username, real_name FROM cnc_platform.sys_user WHERE id IN (1,2,3,4,5);")
$lines.Add("SELECT 'GD-20260615-0001 -> LZ-GD001-P01 -> SB-CNC-001' AS scan_flow;")
$lines.Add("SELECT 'XS-MOCK-0001 .. XS-MOCK-0050' AS bulk_order_range;")

$out = ($lines -join [Environment]::NewLine) + [Environment]::NewLine
[System.IO.File]::WriteAllText($OutFile, $out, [System.Text.UTF8Encoding]::new($false))

$annotator = Join-Path $DbRoot "tools\annotate-init-comments.py"
if (Test-Path $annotator) {
    python $annotator $OutFile
    if ($LASTEXITCODE -ne 0) { throw "annotate-init-comments.py failed" }
}

$kb = [math]::Round((Get-Item $OutFile).Length / 1KB, 1)
Write-Host ("Wrote " + $OutFile + " (" + $kb + " KB)")
