# Quote flow V2.1 - Live API E2E (PowerShell)
# Usage: .\e2e-quote-flow-v21.ps1 [-BusinessUrl http://127.0.0.1:9082] [-SkipEmail]

param(
    [string]$BusinessUrl = "http://127.0.0.1:9082",
    [string]$PlatformUrl = "http://127.0.0.1:9080",
    [string]$Token = "",
    [string]$Username = "sales",
    [string]$Password = "123456",
    [long]$CustomerId = 0,
    [switch]$SkipEmail
)

$ErrorActionPreference = "Stop"
$Passed = 0
$Failed = 0
$Skipped = 0
$script:CachedToken = $null

function Write-Step([string]$Msg) { Write-Host $Msg -ForegroundColor Cyan }
function Write-Pass([string]$Msg) { Write-Host "  [PASS] $Msg" -ForegroundColor Green; $script:Passed++ }
function Write-Fail([string]$Msg) { Write-Host "  [FAIL] $Msg" -ForegroundColor Red; $script:Failed++ }
function Write-Skip([string]$Msg) { Write-Host "  [SKIP] $Msg" -ForegroundColor Yellow; $script:Skipped++ }

function Get-AuthToken {
    if ($Token) { return $Token }
    if ($script:CachedToken) { return $script:CachedToken }
    try {
        $body = @{ username = $Username; password = $Password } | ConvertTo-Json
        $login = Invoke-RestMethod -Method POST -Uri "$PlatformUrl/erp-platform/auth/login" `
            -ContentType "application/json" -Body $body -TimeoutSec 8
        if ($login.code -eq 0 -and $login.data.accessToken) {
            $script:CachedToken = $login.data.accessToken
            return $script:CachedToken
        }
    } catch {
        Write-Host "  login skipped (dev X-User-Id): $($_.Exception.Message)" -ForegroundColor Yellow
    }
    return $null
}

function Invoke-ErpApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Query = @{},
        [switch]$RawBytes
    )
    $uri = "$BusinessUrl$Path"
    if ($Query.Count -gt 0) {
        $qs = ($Query.GetEnumerator() | ForEach-Object { "$($_.Key)=$([uri]::EscapeDataString([string]$_.Value))" }) -join "&"
        $uri = "$uri?$qs"
    }
    $headers = @{
        Accept = if ($RawBytes) { "*/*" } else { "application/json" }
        "X-User-Id" = "2"
    }
    $auth = Get-AuthToken
    if ($auth) { $headers["Authorization"] = "Bearer $auth" }
    if ($Body -ne $null -and -not $RawBytes) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers `
            -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 8)
    }
    if ($RawBytes) {
        return Invoke-WebRequest -Method $Method -Uri $uri -Headers $headers -UseBasicParsing
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
}

function Assert-Ok($resp, [string]$step) {
    if ($null -eq $resp) { throw "$step : empty response" }
    if ($resp.code -ne 0 -and $resp.code -ne $null) {
        throw "$step failed: code=$($resp.code) msg=$($resp.message)"
    }
}

Write-Step "=== Quote Flow V2.1 E2E ==="
Write-Host "Business: $BusinessUrl  Platform: $PlatformUrl"

try {
    $ping = Invoke-WebRequest -Uri "$BusinessUrl/quote-cost-items" -Headers @{ "X-User-Id" = "2" } -UseBasicParsing -TimeoutSec 5
} catch {
    Write-Skip "erp-business not reachable at $BusinessUrl ($($_.Exception.Message))"
    Write-Host "Offline checks: node backend/scripts/test-quote-flow-v21.mjs" -ForegroundColor Yellow
    exit 0
}

try {
Write-Step "[1] Customer contact email"
if ($CustomerId -le 0) {
    $custList = Invoke-ErpApi -Method "GET" -Path "/customers" -Query @{ pageNum = 1; pageSize = 5 }
    Assert-Ok $custList "list customers"
    $CustomerId = [long]$custList.data.items[0].id
}
$cust = Invoke-ErpApi -Method "GET" -Path "/customers/$CustomerId"
Assert-Ok $cust "get customer"
$testEmail = "e2e-quote-$(Get-Date -Format 'yyyyMMddHHmmss')@example.com"
$upd = Invoke-ErpApi -Method "PUT" -Path "/customers/$CustomerId" -Body @{
    contactName = "E2E Contact"
    contactPhone = "13800000001"
    contactEmail = $testEmail
}
Assert-Ok $upd "update customer email"
Write-Pass "customer $CustomerId email=$testEmail"

Write-Step "[2] Create quote with customerDrawingNo"
$customerDrawingNo = "615-E2E-$(Get-Date -Format 'HHmmss')"
$createBody = @{
    quote = @{
        customerId = $CustomerId
        customerName = $cust.data.name
        ownerUserId = 2
        comment = "E2E quote flow test"
    }
    items = @(
        @{
            customerDrawingNo = $customerDrawingNo
            drawingNo = "DWG-E2E-TEST"
            material = "AL6061"
            spec = "100x50x20"
            unitWeight = 0.35
            quantity = 2
            productName = "E2E part"
        }
    )
}
$created = Invoke-ErpApi -Method "POST" -Path "/quotes" -Body $createBody
Assert-Ok $created "create quote"
$quoteId = [long]$created.data.id
Write-Pass "quote id=$quoteId no=$($created.data.quoteNo)"

Write-Step "[3] Submit to engineer"
$eng = Invoke-ErpApi -Method "POST" -Path "/quotes/$quoteId/submit-to-engineer"
Assert-Ok $eng "submit to engineer"
if ($eng.data.status -ne "PENDING_ENG") { throw "expected PENDING_ENG" }
Write-Pass "status PENDING_ENG"

Write-Step "[4] Engineer process + calculate"
$detail = Invoke-ErpApi -Method "GET" -Path "/quotes/$quoteId"
Assert-Ok $detail "get quote detail"
$itemId = [long]$detail.data.items[0].id
$procBody = @{
    processes = @(
        @{
            processCode = "CNC"
            processName = "CNC"
            machineType = "CNC"
            unitTimeMinutes = 90
            costPerHour = 120
            outsourceFlag = 0
        }
    )
}
$fill = Invoke-ErpApi -Method "POST" -Path "/quotes/items/$itemId/process" -Body $procBody
Assert-Ok $fill "fill process"
$calc = Invoke-ErpApi -Method "POST" -Path "/quotes/items/$itemId/calculate"
Assert-Ok $calc "calculate quote item"
Write-Pass "item $itemId calculated unitPrice=$($calc.data.unitPrice)"

Write-Step "[5] Submit for approval"
$sub = Invoke-ErpApi -Method "POST" -Path "/quotes/$quoteId/submit"
Assert-Ok $sub "submit approval"
if ($sub.data.status -ne "PENDING_APPROVAL") { throw "expected PENDING_APPROVAL" }
Write-Pass "status PENDING_APPROVAL node=$($sub.data.currentNode)"

Write-Step "[6] Approve"
$appr = Invoke-ErpApi -Method "POST" -Path "/quotes/$quoteId/approve" -Body @{}
Assert-Ok $appr "approve quote"
if ($appr.data.status -ne "APPROVED") { throw "expected APPROVED" }
Write-Pass "status APPROVED"

Write-Step "[7] Export PDF"
$pdfResp = Invoke-ErpApi -Method "GET" -Path "/quotes/export/$quoteId" -Query @{ format = "pdf" } -RawBytes
$pdfText = [System.Text.Encoding]::UTF8.GetString($pdfResp.Content)
$byteCount = $pdfResp.Content.Length
if ($pdfText -notmatch [regex]::Escape($customerDrawingNo)) {
    Write-Fail "PDF missing customerDrawingNo $customerDrawingNo"
} else {
    Write-Pass "PDF contains $customerDrawingNo ($byteCount bytes)"
}

Write-Step "[8] Send customer email"
if ($SkipEmail) {
    Write-Skip "-SkipEmail specified"
} else {
    try {
        $mail = Invoke-ErpApi -Method "POST" -Path "/quotes/$quoteId/send-email" -Body @{}
        if ($mail.code -eq 0) {
            Write-Pass "email sent to $($mail.data.toAddress)"
        } else {
            Write-Skip "SMTP not configured: $($mail.message)"
        }
    } catch {
        Write-Skip "email skipped: $($_.Exception.Message)"
    }
}

Write-Step "[9] Cost items catalog"
$costItems = Invoke-ErpApi -Method "GET" -Path "/quote-cost-items"
Assert-Ok $costItems "list cost items"
$count = @($costItems.data).Count
if ($count -lt 1) { Write-Fail "no cost items" } else { Write-Pass "cost items count=$count" }

Write-Host ""
Write-Host "=== SUMMARY PASS=$Passed FAIL=$Failed SKIP=$Skipped ===" -ForegroundColor $(if ($Failed -eq 0) { "Green" } else { "Red" })
if ($Failed -gt 0) { exit 1 }
exit 0
} catch {
    Write-Fail "Live E2E aborted: $($_.Exception.Message)"
    Write-Host "Start MySQL + erp-business on $BusinessUrl then re-run." -ForegroundColor Yellow
    Write-Host "Offline checks: node backend/scripts/test-quote-flow-v21.mjs" -ForegroundColor Yellow
    exit 1
}
