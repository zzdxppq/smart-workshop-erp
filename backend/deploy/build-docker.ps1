# Maven 打包 + Docker 镜像（生产 profile）
# 用法：.\build-docker.ps1

$ErrorActionPreference = "Stop"
$BackendRoot = Split-Path -Parent $PSScriptRoot
Set-Location $BackendRoot

Write-Host ">>> Download fonts (Source Han Sans)..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "download-fonts.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ">>> Maven install (library jars)..." -ForegroundColor Cyan
mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ">>> Maven repackage (-Pdocker, per service)..." -ForegroundColor Cyan
$services = @("erp-gateway", "erp-platform", "erp-business", "erp-production")
foreach ($svc in $services) {
    mvn -pl "src/$svc" -Pdocker package -DskipTests
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host ">>> Docker build..." -ForegroundColor Cyan
foreach ($svc in $services) {
    $ctx = Join-Path $BackendRoot "src\$svc"
    Write-Host "  building $svc"
    docker build -t "btsheng/${svc}:1.3.7" $ctx
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "`n完成。本地推送: .\push-images.ps1" -ForegroundColor Green
Write-Host "服务器启动: cd /opt/deploy && ./run-prod.sh start" -ForegroundColor Green
