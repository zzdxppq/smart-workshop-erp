# 工程师核心链路 · 端到端 API 联调脚本（PowerShell）
# 用法: .\e2e-engineer-workflow.ps1 [-BaseUrl http://127.0.0.1:9082] [-Token <jwt>]
# 前置: MySQL + erp-business (+ erp-production 可选) 已启动

param(
    [string]$BaseUrl = "http://127.0.0.1:9082",
    [string]$Token = "",
    [long]$DrawingId = 0
)

$ErrorActionPreference = "Stop"

function Invoke-ErpApi {
    param([string]$Method, [string]$Path, [object]$Body = $null, [hashtable]$Query = @{}, [switch]$Multipart)
    $uri = "$BaseUrl$Path"
    if ($Query.Count -gt 0) {
        $qs = ($Query.GetEnumerator() | ForEach-Object { "$($_.Key)=$([uri]::EscapeDataString([string]$_.Value))" }) -join "&"
        $uri = "$uri?$qs"
    }
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    if ($Multipart) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Form $Body
    }
    if ($Body -ne $null) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 6)
    }
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
}

function Assert-Ok($resp, [string]$step) {
    if ($null -eq $resp) { throw "$step : empty response" }
    if ($resp.code -ne 0 -and $resp.code -ne $null) {
        throw "$step failed: code=$($resp.code) msg=$($resp.message)"
    }
}

Write-Host "=== 工程师 E2E 联调 ===" -ForegroundColor Cyan
Write-Host "API: $BaseUrl"

# 1) 选图纸
if ($DrawingId -le 0) {
    $list = Invoke-ErpApi GET "/drawings" @{ page = 0; size = 5; status = "RELEASED" }
    Assert-Ok $list "list drawings"
    $DrawingId = [long]$list.data.list[0].id
}
Write-Host "[1] 图纸 ID: $DrawingId"

# 2) CAD/CAM 附件
$tmp = New-TemporaryFile
Set-Content -Path $tmp -Value "SECTION`nHEADER`nENDSEC" -NoNewline
$form = @{ file = Get-Item $tmp }
$attach = Invoke-ErpApi POST "/drawings/$DrawingId/attachments" -Query @{ operatorUserId = 1001 } -Multipart -Body $form
Assert-Ok $attach "upload CAD attachment"
$attachId = $attach.data.id
Write-Host "[2] CAD 附件已上传: id=$attachId file=$($attach.data.fileName)"

$attachList = Invoke-ErpApi GET "/drawings/$DrawingId/attachments"
Assert-Ok $attachList "list attachments"
Write-Host "    附件数: $($attachList.data.Count)"

# 3) 工程转化
$convBody = @{ bomType = "STANDARD"; targetQty = 5; engineerName = "E2E测试" }
try {
    $conv = Invoke-ErpApi POST "/drawings/$DrawingId/convert" -Body $convBody
    Assert-Ok $conv "engineering conversion"
    $bomId = $conv.data.bomId
    $materialCode = $conv.data.materialCode
    Write-Host "[3] 转化成功: material=$materialCode bomId=$bomId bomNo=$($conv.data.bomNo)"
} catch {
    if ($_.Exception.Message -match "40905|CONVERSION_ALREADY") {
        Write-Host "[3] 图纸已转化，跳过" -ForegroundColor Yellow
        $boms = Invoke-ErpApi GET "/boms" @{ keyword = ""; size = 20 }
        $hit = $boms.data.list | Where-Object { $_.drawingId -eq $DrawingId } | Select-Object -First 1
        $bomId = $hit.id
        $materialCode = $hit.materialCode
    } else { throw }
}

# 4) BOM 子件
$treeBody = @{
    bomId = [long]$bomId
    lines = @(
        @{ materialCode = "RM-001"; materialName = "45#圆钢"; qty = 2; unit = "kg"; scrapRate = 5; itemLevel = 1; itemNo = 1 }
    )
}
$save = Invoke-ErpApi POST "/boms/save-tree" -Body $treeBody
Assert-Ok $save "save BOM tree"
Write-Host "[4] BOM 子件已保存: lines=$($save.data.lineCount)"

# 5) 料号 lookup + 工艺预览（需 erp-production）
try {
    $lookup = Invoke-ErpApi GET "/materials/lookup" @{ code = $materialCode }
    Assert-Ok $lookup "material lookup"
    $matId = $lookup.data.id
    $route = Invoke-ErpApi GET "/materials/$matId/process-route"
    if ($route.data.Count -eq 0) {
        Write-Host "[5] 工艺路线未发布（预览为空，符合预期）" -ForegroundColor Yellow
    } else {
        Write-Host "[5] 已发布工艺路线工序数: $($route.data.Count)" -ForegroundColor Green
    }
} catch {
    Write-Host "[5] 工艺路线 API 跳过（erp-production 未启动?）: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n=== E2E 联调完成 ===" -ForegroundColor Green
Write-Host "BOM 编辑页: /material/boms/edit?bomId=$bomId"
