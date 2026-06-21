# 本地 Maven 打包 + Docker 构建 + 推送到远程镜像仓库
# 用法：
#   $env:ERP_IMAGE_REGISTRY = "registry.cn-hangzhou.aliyuncs.com/kczj"
#   .\push-images.ps1
#   .\push-images.ps1 -Registry "registry.cn-hangzhou.aliyuncs.com/kczj" -Tag "1.3.7"

param(
    [string]$Registry = $env:ERP_IMAGE_REGISTRY,
    [string]$Tag = $(if ($env:ERP_IMAGE_TAG) { $env:ERP_IMAGE_TAG } else { "1.3.7" }),
    [switch]$SkipMaven
)

$ErrorActionPreference = "Stop"
$DeployDir = $PSScriptRoot
$BackendRoot = Split-Path -Parent $DeployDir

if (-not $Registry) {
    Write-Host "请设置镜像仓库前缀，例如：" -ForegroundColor Yellow
    Write-Host '  $env:ERP_IMAGE_REGISTRY = "registry.cn-hangzhou.aliyuncs.com/kczj"' -ForegroundColor Yellow
    Write-Host "  .\push-images.ps1" -ForegroundColor Yellow
    exit 1
}

$Registry = $Registry.TrimEnd("/")
$services = @("erp-gateway", "erp-platform", "erp-business", "erp-production")

Set-Location $BackendRoot

if (-not $SkipMaven) {
    Write-Host ">>> Download fonts..." -ForegroundColor Cyan
    & (Join-Path $DeployDir "download-fonts.ps1")
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host ">>> Maven install..." -ForegroundColor Cyan
    mvn clean install "-Dmaven.test.skip=true"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host ">>> Maven package (-Pdocker)..." -ForegroundColor Cyan
    foreach ($svc in $services) {
        mvn -pl "src/$svc" -Pdocker package "-Dmaven.test.skip=true"
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
}

Write-Host ">>> Docker build & push -> ${Registry}:*:${Tag}" -ForegroundColor Cyan
foreach ($svc in $services) {
    $ctx = Join-Path $BackendRoot "src\$svc"
    $localTag = "${Registry}/${svc}:${Tag}"
    $latestTag = "${Registry}/${svc}:latest"

    Write-Host "  build $svc -> $localTag"
    docker build -t $localTag -t $latestTag $ctx
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    docker push $localTag
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    docker push $latestTag
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$minioTag = if ($env:MINIO_IMAGE_TAG) { $env:MINIO_IMAGE_TAG } else { "RELEASE.2024-05-10T01-41-38Z" }
$redisTag = if ($env:REDIS_IMAGE_TAG) { $env:REDIS_IMAGE_TAG } else { "7.2" }
Write-Host ">>> Mirror MinIO base images -> ${Registry} (tag ${minioTag})" -ForegroundColor Cyan
@(
    @{ Hub = "minio/minio"; Name = "minio" },
    @{ Hub = "minio/mc"; Name = "minio-mc" }
) | ForEach-Object {
    $img = "${Registry}/$($_.Name):${minioTag}"
    Write-Host "  mirror $($_.Hub) -> $img"
    docker pull "$($_.Hub):${minioTag}"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  skip $($_.Hub) (optional, prod uses curl for bucket init)" -ForegroundColor Yellow
        return
    }
    docker tag "$($_.Hub):${minioTag}" $img
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    docker push $img
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host ">>> Mirror Redis -> ${Registry}/redis:${redisTag}" -ForegroundColor Cyan
docker pull "redis:${redisTag}"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
docker tag "redis:${redisTag}" "${Registry}/redis:${redisTag}"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
docker push "${Registry}/redis:${redisTag}"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Done. On server /opt/deploy:" -ForegroundColor Green
Write-Host "  1. copy docker-compose.prod.yml + run-prod.sh" -ForegroundColor Green
Write-Host "  2. set ERP_IMAGE_REGISTRY ERP_IMAGE_TAG MINIO_IMAGE_TAG REDIS_IMAGE in env.prod" -ForegroundColor Green
Write-Host "  3. ./run-prod.sh start" -ForegroundColor Green
