#!/usr/bin/env bash
# 生产服务器运行脚本 · 工作目录 /opt/deploy
# 前置：env.prod、docker-compose.prod.yml 同目录；镜像已由本地 push-images 推到仓库
# Redis 7.x 由 compose 同机拉起（服务名 redis），不再使用外部 10.100.2.17
#
# 用法：
#   ./run-prod.sh start    # 拉取镜像并启动
#   ./run-prod.sh stop
#   ./run-prod.sh restart
#   ./run-prod.sh pull
#   ./run-prod.sh status
#   ./run-prod.sh logs [service]

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/deploy}"
COMPOSE_FILE="${COMPOSE_FILE:-${DEPLOY_DIR}/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/env.prod}"

cd "$DEPLOY_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少 env 文件: $ENV_FILE" >&2
  exit 1
fi
if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "缺少 compose 文件: $COMPOSE_FILE" >&2
  exit 1
fi

# docker compose 插件损坏时（exec format error）回退到 docker-compose 独立命令
COMPOSE_BIN=()
if docker compose version &>/dev/null; then
  COMPOSE_BIN=(docker compose)
elif command -v docker-compose &>/dev/null && docker-compose version &>/dev/null; then
  COMPOSE_BIN=(docker-compose)
else
  echo "未找到可用的 compose：请安装 docker compose 插件或 docker-compose" >&2
  echo "  插件异常时可执行: docker compose version" >&2
  echo "  或安装独立版: yum install -y docker-compose-plugin  /  pip install docker-compose" >&2
  exit 1
fi

compose() {
  "${COMPOSE_BIN[@]}" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

env_val() {
  grep -E "^${1}=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r'
}

wait_redis() {
  local pass
  pass="$(env_val REDIS_PASSWORD)"
  [[ -z "$pass" ]] && return 0

  echo ">>> wait Redis (erp-redis)..."
  for _ in $(seq 1 30); do
    if compose exec -T redis redis-cli -a "$pass" ping 2>/dev/null | grep -q PONG; then
      echo "Redis ready (Stream supported: Redis 7.x)"
      return 0
    fi
    sleep 1
  done
  echo "Redis 未在 30s 内就绪，请检查: docker logs erp-redis" >&2
  return 1
}

init_minio_bucket() {
  local bucket user pass port
  bucket="$(env_val MINIO_BUCKET_DRAWING)"
  user="$(env_val MINIO_ROOT_USER)"
  pass="$(env_val MINIO_ROOT_PASSWORD)"
  port="$(env_val MINIO_API_PORT)"
  port="${port:-9000}"
  [[ -z "$bucket" || -z "$user" ]] && return 0

  echo ">>> init MinIO bucket: ${bucket}"
  for _ in $(seq 1 30); do
    if curl -sf -o /dev/null "http://127.0.0.1:${port}/minio/health/live"; then
      break
    fi
    sleep 1
  done
  if curl -sf -X PUT "http://127.0.0.1:${port}/${bucket}" -u "${user}:${pass}"; then
    echo "MinIO bucket ready: ${bucket}"
  else
    echo "MinIO bucket init skipped (可能已存在)"
  fi
}

post_up() {
  wait_redis
  init_minio_bucket
  compose ps
  echo ""
  echo "Gateway: http://127.0.0.1:$(grep -E '^ERP_GATEWAY_PORT=' "$ENV_FILE" | cut -d= -f2)/actuator/health"
  echo "Redis:   compose 内 redis:6379（ERP 容器 REDIS_HOST=redis）"
}

cmd="${1:-start}"
shift || true

case "$cmd" in
  start)
    echo ">>> pull images..."
    compose pull
    echo ">>> up -d (ports from env.prod: ERP_*_PORT)..."
    compose up -d
    post_up
    ;;
  stop)
    compose down
    ;;
  restart)
    compose down
    compose pull
    compose up -d
    post_up
    ;;
  pull)
    compose pull "$@"
    ;;
  status|ps)
    compose ps
    ;;
  logs)
    compose logs -f --tail=200 "$@"
    ;;
  *)
    echo "用法: $0 {start|stop|restart|pull|status|logs [service]}" >&2
    exit 1
    ;;
esac
