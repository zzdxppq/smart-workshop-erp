# 生产部署指南

## 目录结构（服务器 `/opt/deploy`）

```
/opt/deploy/
├── env.prod                 # 敏感配置（不入库，从 env.prod.example 复制）
├── docker-compose.prod.yml  # 从仓库复制
└── run-prod.sh              # 启动脚本
```

## 端口（9 开头）

| 服务 | 变量 | 默认 |
|------|------|------|
| erp-gateway | `ERP_GATEWAY_PORT` | **9080** |
| erp-platform | `ERP_PLATFORM_PORT` | **9081** |
| erp-business | `ERP_BUSINESS_PORT` | **9082** |
| erp-production | `ERP_PRODUCTION_PORT` | **9083** |
| redis（compose 内） | `REDIS_PORT` | **6379**（不映射宿主机） |

Nginx 反代 API 请指向 `http://127.0.0.1:9080`（不再用 8080）。

## 流程一：本地构建并推送镜像

推送 **ERP 四服务 + MinIO / Redis 基础镜像**（生产机不直连 Docker Hub 时）。

```powershell
cd backend\deploy

# 设置你的镜像仓库（Harbor / 阿里云 ACR 等）
$env:ERP_IMAGE_REGISTRY = "registry.cn-hangzhou.aliyuncs.com/kczj"
$env:ERP_IMAGE_TAG = "1.3.7"

.\push-images.ps1
# 或已打过包：.\push-images.ps1 -SkipMaven
# 仅补推 MinIO 基础镜像（本地能访问 Docker Hub 时）：
#   docker pull minio/minio:RELEASE.2024-05-10T01-41-38Z
#   docker tag ... registry.cn-hangzhou.aliyuncs.com/kczj/minio:RELEASE.2024-05-10T01-41-38Z
#   docker push registry.cn-hangzhou.aliyuncs.com/kczj/minio:RELEASE.2024-05-10T01-41-38Z
#   （minio/mc 同理，仓库名 minio-mc）
```

Linux/macOS：`./push-images.sh`（需 `export ERP_IMAGE_REGISTRY=...`）。

## 流程二：服务器拉取并启动

```bash
# 首次
sudo mkdir -p /opt/deploy
sudo cp docker-compose.prod.yml run-prod.sh /opt/deploy/
sudo cp env.prod.example /opt/deploy/env.prod   # 再编辑真实密码与 ERP_IMAGE_REGISTRY
sudo chmod +x /opt/deploy/run-prod.sh

cd /opt/deploy
./run-prod.sh start      # 或: bash run-prod.sh start
./run-prod.sh status
./run-prod.sh logs erp-gateway
./run-prod.sh restart
./run-prod.sh stop
```

`run-prod.sh` 会自动选用 `docker compose` 或 `docker-compose`，等价于：

```bash
docker compose -f /opt/deploy/docker-compose.prod.yml \
  --env-file /opt/deploy/env.prod up -d
```

### compose 报错排查

若出现 `unknown shorthand flag: 'f' in -f` 或 `compose ... exec format error`：

```bash
docker compose version      # 插件损坏时会失败
docker-compose version      # 看独立版是否可用
```

**临时启动**（独立版可用时）：

```bash
docker-compose -f /opt/deploy/docker-compose.prod.yml \
  --env-file /opt/deploy/env.prod pull
docker-compose -f /opt/deploy/docker-compose.prod.yml \
  --env-file /opt/deploy/env.prod up -d
```

**修复插件**（TencentOS / CentOS，按架构重装）：

```bash
uname -m                    # x86_64 或 aarch64
yum install -y docker-compose-plugin
# 或从 https://github.com/docker/compose/releases 下载对应架构的 docker-compose-linux-* 到 /usr/local/bin/docker-compose
```

## env.prod 必填项

```ini
ERP_IMAGE_REGISTRY=registry.cn-hangzhou.aliyuncs.com/kczj
ERP_IMAGE_TAG=1.3.7
ERP_GATEWAY_PORT=9080
ERP_PLATFORM_PORT=9081
ERP_BUSINESS_PORT=9082
ERP_PRODUCTION_PORT=9083
# erp-business 调 erp-platform 创建登录账号（Feign · HR 新建员工）
PLATFORM_SERVICE_URL=http://erp-platform:9081
PRODUCTION_SERVICE_URL=http://erp-production:9083
MYSQL_HOST=...
REDIS_HOST=redis
REDIS_PASSWORD=...
JWT_SECRET=...
```

## 健康检查

Gateway 使用 **Nacos lb://** 路由，对外 URL：`/{serviceId}/auth/login`

```bash
curl http://127.0.0.1:9080/actuator/health
curl http://127.0.0.1:9080/erp-platform/platform/health
curl -X POST http://127.0.0.1:9080/erp-platform/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Nginx 只需反代 `/erp-platform/`、`/erp-business/`、`/erp-production/` 到 Gateway（见 `deploy/nginx/bts.51xiaoping.com.conf`）。

JWT_SECRET=change-me

## 宿主机服务（Nacos / XXL-JOB）

ERP 跑在 Docker 里时，**容器内的 `127.0.0.1` 不是宿主机**。本环境 Nacos / XXL-JOB 在同机内网 `10.100.6.14`：

```ini
NACOS_SERVER=10.100.6.14:8848
XXL_JOB_ADMIN=http://10.100.6.14:8080/xxl-job-admin
```

改完 `env.prod` 后重启：

```bash
cd /opt/deploy
./run-prod.sh restart
```

**验证**（在容器内测 Nacos 是否可达）：

```bash
docker exec erp-business sh -c 'wget -qO- http://10.100.6.14:8848/nacos/v1/console/health/readiness || true'
```

若仍 `Connection refused`，检查 Nacos 是否监听 `0.0.0.0:8848`（及 gRPC `9848`），防火墙是否放行。

## MinIO（同机 compose 已包含）

| 用途 | 地址 |
|------|------|
| ERP 容器内 API | `http://minio:9000`（`MINIO_ENDPOINT`） |
| 宿主机/内网控制台 | `http://10.100.6.14:9001` |
| 桶 | `erp-drawing` |

账号见 `env.prod` 中 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`。**不要用 127.0.0.1** 给 ERP 容器配 MinIO（127 指向容器自身）。桶 `erp-drawing` 由 `run-prod.sh` 启动后自动创建，**无需拉取 minio/mc 镜像**。

## Redis（同机 compose 已包含 · Redis 7.2）

不再使用外部 `10.100.2.17`（Redis 4.0 不支持 Stream）。ERP 与 Redis 同 compose 网络：

| 用途 | 地址 |
|------|------|
| ERP 容器内 | `redis:6379`（`REDIS_HOST=redis`） |
| 版本 | Redis **7.2**（支持 Stream：`XREADGROUP` / 审批集成） |
| 数据卷 | `redis-data`（AOF 持久化） |

`env.prod` 必填：

```ini
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_DATABASE=0
REDIS_PASSWORD=你的强密码
REDIS_IMAGE=redis:7.2
```

生产机无法拉 Docker Hub 时，本地执行 `push-images.ps1` 会同步推送 `redis:7.2` 到 `ERP_IMAGE_REGISTRY`，服务器 `env.prod` 设置 `REDIS_IMAGE=${ERP_IMAGE_REGISTRY}/redis:7.2`。

**验证**（启动后）：

```bash
docker exec erp-redis redis-cli -a "$(grep '^REDIS_PASSWORD=' env.prod | cut -d= -f2)" INFO server | grep redis_version
docker exec erp-business sh -c 'wget -qO- http://127.0.0.1:9082/actuator/health || true'
```

## 数据库

见 `backend/db/install/README.md`（空库执行 `init.sql`）。

## 演示账号

`admin` / `sales` / `operator` / `prod_mgr`，密码 `admin123`。
