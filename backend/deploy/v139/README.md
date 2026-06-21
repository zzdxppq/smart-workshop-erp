# 昆山佰泰胜 ERP V1.3.9 部署指南

## 概述

V1.3.9 提供两个单台服务器部署脚本（Ubuntu 24.04 · Docker 24+ · 8 核 16G 起步）：

| 脚本 | 用途 | 端口 | 资源 |
|------|------|------|------|
| `docker-compose.cloud.yml` | 云服务器（公网）| 80+443 SSL | MySQL 2G / Redis 512M / Nacos 512M / SkyWalking 1G / Prometheus 512M / Grafana 512M / erp-platform 1.5G / erp-business 1.5G / erp-gateway 512M / Nginx 256M = 8.8C/9.3G |
| `docker-compose.local.yml` | 客户机内网 | 80 HTTP | 同 cloud.yml（SkyWalking 存储用 H2 简化）|

## 7 项客户验收要求 · 全部满足

| 要求 | 落地 |
|------|------|
| ✅ `restart: unless-stopped` 所有服务 | `x-restart: &restart_policy` YAML anchor 复用 |
| ✅ 密码改用 .env（compose 无明文）| 所有 `MYSQL_PASSWORD` / `REDIS_PASSWORD` / `NACOS_AUTH_TOKEN` 等用 `${...}` 引用 `.env.{cloud,local}` |
| ✅ 资源限制 | 10 个服务全部 `deploy.resources.limits + reservations`（cpu + memory）|
| ✅ 监控数据持久化 | `prometheus-data` / `grafana-data` / `skywalking-data` / `skywalking-logs` 4 个 volume 全部持久化 |
| ✅ MySQL Slave 暂时注释 | `mysql-slave` 完整保留为注释（避免无配置主从误导）|
| ✅ Nginx SSL/非 SSL 区分 | cloud.conf SSL 443 + HTTP 80 → 443 重定向 · local.conf 纯 HTTP 80 |

## 部署步骤

### 1. 准备环境（Ubuntu 24.04）

```bash
# 安装 Docker 24+
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 安装 acme.sh（云部署 SSL 签发 · 本地部署跳过）
curl https://get.acme.sh | sh
```

### 2. 准备 .env 文件

```bash
cd backend/deploy/v139
cp .env.cloud.example .env.cloud   # 或 .env.local.example .env.local
vi .env.cloud                       # 修改所有 ChangeMe_* 密码
chmod 600 .env.cloud
```

### 3. 准备 Nginx SSL 证书（仅云部署）

```bash
# acme.sh 签发（Cloudflare DNS API 或 HTTP-01）
export CF_Token="..."
.acme.sh/acme.sh --issue -d erp.example.com --dns dns_cf

# 复制到 ssl 目录
mkdir -p nginx/ssl
.acme.sh/acme.sh --install-cert -d erp.example.com \
  --key-file       nginx/ssl/privkey.pem \
  --fullchain-file nginx/ssl/fullchain.pem
```

### 4. 启动

```bash
# 云部署
docker compose -f docker-compose.cloud.yml --env-file .env.cloud up -d
docker compose -f docker-compose.cloud.yml ps
docker compose -f docker-compose.cloud.yml logs -f erp-business

# 本地部署
docker compose -f docker-compose.local.yml --env-file .env.local up -d
```

### 5. 验证

```bash
# 健康检查
docker compose -f docker-compose.cloud.yml ps   # 9 个核心服务全 healthy
curl -k https://localhost/health               # 200 OK
curl http://localhost:9090/-/healthy            # Prometheus 200

# 访问面板（通过 nginx 反代）
https://erp.example.com/                       # ERP 主站
https://erp.example.com/grafana/                # 监控
https://erp.example.com/skywalking/             # APM
https://erp.example.com/nacos/                 # 服务发现
```

## 启动顺序

```
mysql-master (health) → redis (health) → nacos (health)
  → skywalking-oap (health) → prometheus (health) → grafana (health)
  → erp-platform (depends on nacos+mysql+redis healthy)
  → erp-business (depends on erp-platform healthy)
  → erp-gateway (depends on erp-platform+erp-business healthy)
  → nginx (depends on erp-gateway healthy)
```

## 备份与维护

```bash
# MySQL 备份
docker exec erp-mysql-master sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --all-databases' > backup.sql

# Prometheus 数据保留 30 天（默认）
# Grafana dashboards 通过 provisioning 目录自动导入

# SkyWalking H2 存储升级到 ES（云部署）：修改 SW_STORAGE=elasticsearch + 部署 ES 容器
```

## 资源估算

8 核 16G 单台服务器：
- 全部 10 容器：8.8 CPU / 9.3G RAM（limit）
- 实际预留：~6 CPU / ~5G RAM（reservation）
- 剩余：~2 CPU / ~11G 给 OS + buffer + 突发

## 文件清单

```
backend/deploy/v139/
├── docker-compose.cloud.yml          # 云部署 · 9 核心 + 监控
├── docker-compose.local.yml          # 本地部署 · 9 核心 + 监控
├── .env.cloud.example                # 云部署环境变量模板（git 安全）
├── .env.local.example                # 本地部署环境变量模板
├── README.md                          # 本文档
├── prometheus/
│   ├── prometheus.yml                # 9 抓取目标
│   └── alerts.yml                    # 告警规则（预留）
├── nginx/
│   ├── cloud.conf                    # SSL 443 + HTTP→HTTPS 重定向
│   └── local.conf                    # 纯 HTTP 80
└── skywalking/
    └── agent/                        # Java agent 挂载目录
        └── skywalking-agent.jar      # （需从 Apache 下载或 Maven 拉取）
```

## V1.3.9 Sprint 12/13/14 集成

- **12.2 sys_printer** 端口 9100 直连模式：Nginx 转发 + Gateway 路由可达
- **12.4 ZPL Socket**：erp-business 容器内 Socket 直连客户机房（生产环境）· 需要安全组放行
- **13.4 sys_workflow_event 仪表盘**：Grafana 配 SkyWalking 数据源即可视化
- **13.6 7 角色 E2E**：本地版建议加 android 模拟器（与 erp-gateway 隔离内网）

## 与 MySQL Slave 启用

```bash
# 1. 取消 mysql-slave 注释
# 2. 配置 server-id + log_bin + binlog_format
# 3. 主库创建复制账号
docker exec erp-mysql-master mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;"
# 4. 从库启动 + CHANGE MASTER TO
docker exec erp-mysql-slave mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
CHANGE MASTER TO MASTER_HOST='mysql-master', MASTER_USER='repl', MASTER_PASSWORD='repl_password', MASTER_LOG_FILE='mysql-bin.000001', MASTER_LOG_POS=0;
START SLAVE;"
```

## 升级到 V1.4

```bash
# 1. 备份当前 compose
cp docker-compose.cloud.yml docker-compose.cloud.v139.backup.yml
cp .env.cloud .env.cloud.v139.backup

# 2. 拉 V1.4 镜像
docker pull erp/platform:1.4.0
docker pull erp/business:1.4.0
docker pull erp/gateway:1.4.0

# 3. 更新 compose + env · 跑 Flyway V58+ · 验证 health
# 4. 蓝绿切换（先起 v140 容器 · 健康检查通过后切 nginx upstream）
```
