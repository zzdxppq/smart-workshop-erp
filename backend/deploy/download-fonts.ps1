# 下载思源黑体 TTF 到 erp-platform resources（构建前执行 · push-images.ps1 会自动调用）
# OpenPDF 仅支持 TrueType(.ttf)，不支持 Subset OTF
$ErrorActionPreference = "Stop"
$FontDir = Join-Path (Split-Path -Parent $PSScriptRoot) "src\erp-platform\src\main\resources\fonts"
$TtfFile = Join-Path $FontDir "SourceHanSansCN-Normal.ttf"

function Test-FontFile([string]$path) {
    return (Test-Path $path) -and ((Get-Item $path).Length -gt 1MB)
}

if (Test-FontFile $TtfFile) {
    Write-Host "字体已存在: $TtfFile ($([math]::Round((Get-Item $TtfFile).Length / 1MB, 2)) MB)" -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $FontDir | Out-Null
$TmpTtf = Join-Path $env:TEMP "SourceHanSansCN-Normal-$PID.ttf"
# Adobe 思源黑体简体 · Variable TTF Subset（~17MB · OpenPDF + AWT 均可用）
$urls = @(
    "https://raw.githubusercontent.com/adobe-fonts/source-han-sans/release/Variable/TTF/Subset/SourceHanSansCN-VF.ttf"
)

foreach ($url in $urls) {
    Write-Host ">>> 下载字体 TTF: $url" -ForegroundColor Cyan
    try {
        if (Test-Path $TmpTtf) { Remove-Item -Force $TmpTtf }
        Invoke-WebRequest -Uri $url -OutFile $TmpTtf -UseBasicParsing -TimeoutSec 600
        if (Test-FontFile $TmpTtf) {
            Copy-Item -Force $TmpTtf $TtfFile
            Remove-Item -Force $TmpTtf -ErrorAction SilentlyContinue
            Write-Host "完成: $TtfFile ($([math]::Round((Get-Item $TtfFile).Length / 1MB, 2)) MB)" -ForegroundColor Green
            exit 0
        }
    } catch {
        Write-Host "  失败: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "字体下载失败，请手动下载 SourceHanSansCN-VF.ttf 保存为: $TtfFile" -ForegroundColor Red
exit 1
