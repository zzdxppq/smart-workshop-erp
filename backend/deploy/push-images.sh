#!/usr/bin/env bash
# 本地 Maven 打包 + Docker 构建 + 推送（Linux/macOS）
# 用法：
#   export ERP_IMAGE_REGISTRY=registry.cn-hangzhou.aliyuncs.com/kczj
#   ./push-images.sh

set -euo pipefail

REGISTRY="${ERP_IMAGE_REGISTRY:-}"
TAG="${ERP_IMAGE_TAG:-1.3.7}"
SKIP_MAVEN="${SKIP_MAVEN:-0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVICES=(erp-gateway erp-platform erp-business erp-production)

if [[ -z "$REGISTRY" ]]; then
  echo "请设置 ERP_IMAGE_REGISTRY，例如: export ERP_IMAGE_REGISTRY=registry.cn-hangzhou.aliyuncs.com/kczj" >&2
  exit 1
fi
REGISTRY="${REGISTRY%/}"

cd "$BACKEND_ROOT"

if [[ "$SKIP_MAVEN" != "1" ]]; then
  echo ">>> Maven install..."
  mvn clean install -DskipTests
  echo ">>> Maven package (-Pdocker)..."
  for svc in "${SERVICES[@]}"; do
    mvn -pl "src/$svc" -Pdocker package -DskipTests
  done
fi

echo ">>> Docker build & push -> ${REGISTRY}:*:${TAG}"
for svc in "${SERVICES[@]}"; do
  ctx="$BACKEND_ROOT/src/$svc"
  img="${REGISTRY}/${svc}:${TAG}"
  latest="${REGISTRY}/${svc}:latest"
  docker build -t "$img" -t "$latest" "$ctx"
  docker push "$img"
  docker push "$latest"
done

MINIO_TAG="${MINIO_IMAGE_TAG:-RELEASE.2024-05-10T01-41-38Z}"
echo ">>> Mirror MinIO base images -> ${REGISTRY} (tag ${MINIO_TAG})"
for pair in "minio/minio:minio" "minio/mc:minio-mc"; do
  hub="${pair%%:*}"
  name="${pair##*:}"
  img="${REGISTRY}/${name}:${MINIO_TAG}"
  docker pull "${hub}:${MINIO_TAG}"
  docker tag "${hub}:${MINIO_TAG}" "$img"
  docker push "$img"
done

REDIS_TAG="${REDIS_IMAGE_TAG:-7.2}"
echo ">>> Mirror Redis -> ${REGISTRY}/redis:${REDIS_TAG}"
docker pull "redis:${REDIS_TAG}"
docker tag "redis:${REDIS_TAG}" "${REGISTRY}/redis:${REDIS_TAG}"
docker push "${REGISTRY}/redis:${REDIS_TAG}"

echo "完成。服务器: cd /opt/deploy && ./run-prod.sh start"
