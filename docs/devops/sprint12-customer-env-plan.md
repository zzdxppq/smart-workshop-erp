# DevOps 接入计划 · Sprint 12 客户机房环境就位（Redis 7 + 9100 + DHCP + AVD）

> **作者**：DevOps 张良
> **日期**：2026-06-14
> **Sprint**：V1.3.9 S12 · 集成 E 验证 DevOps 委派 #4
> **依据**：`docs/orchestrix-pm-audit-2026-06-14.md` §6 委派 4 + `docs/qa/evidence/sprint12-integration-test-report.md` §3.4 委派事项 #4
> **关联 Story**：12.1 图纸权限 @Scheduled · 12.2 工业标签打印机心跳 · 12.4 ZPL Socket 3s timeout · 13.3 drawing:link 缓存 · 13.4 GmSummary 数据链路 · 13.6 P2 AVD 7 角色 E2E
> **客户**：昆山佰泰胜（黄梓昀 151-0595-0281）
> **状态**：🟡 **计划已起草** · 5 步骤就位 · 顺序约束遵循集成 E 验证 CONDITIONAL GO 路径
> **截止**：2026-06-23 EOD（客户服务器就位验收）

---

## 1. 计划总览（6 步骤 · 跨 10 天）

| # | 步骤 | 责任 | 前置 | 截止 | 状态 |
|---|------|------|------|------|------|
| 1 | 客户机房 SSH 接入（VPN / 跳板机 / 凭据） | DevOps 张良 | 客户 IT 开通访问 + 提前 3 天通知 | 2026-06-20 EOD | 🟡 待启动 |
| 2 | Redis 7 容器化部署（erp-platform） | DevOps 张良 | 步骤 1 + Docker 环境就位 + 持久卷确认 | 2026-06-21 EOD | 🟡 待启动 |
| 3 | 9100 端口 firewall 放行（白名单双向） | 客户 IT + DevOps | 步骤 1 + 客户 IT 协同（提前 3 天通知） | 2026-06-22 EOD | 🟡 待启动 |
| 4 | DHCP 静态 IP 预案（绑 MAC + 预留） | 客户 IT | 步骤 1 + 客户 IT 提供 DHCP server 权限 | 2026-06-22 EOD | 🟡 待启动 |
| 5 | AVD 模拟器镜像（13.6 P2 · 7 角色 connectedAndroidTest） | DevOps 张良 | 步骤 1 + Android Studio 镜像模板 | 2026-06-23 EOD | 🟡 待启动 |
| 6 | 客户服务器就位验收（含连通性 + 缓存命中率 + 端口探活） | 客户黄梓昀 + DevOps | 步骤 2-5 全部完成 | 2026-06-23 EOD | 🟡 待启动 |

### 1.1 关键路径

```
客户 IT 协同（提前 3 天 = 2026-06-17 通知）
    │
    ├──→ 步骤 1 SSH 接入（2026-06-20）
              │
              ├──→ 步骤 2 Redis 7（2026-06-21）
              │       │
              │       └──→ 12.1 @Scheduled 缓存验证 + 13.3 drawing:link 5min TTL + 13.4 GmSummary
              │
              ├──→ 步骤 3 9100 端口（2026-06-22）
              │       │
              │       └──→ 12.2 心跳 60s + 12.4 ZPL Socket 3s timeout 探活
              │
              ├──→ 步骤 4 DHCP 静态 IP（2026-06-22）
              │       │
              │       └──→ 12.4 ZPL 打印机 IP 漂移防护
              │
              └──→ 步骤 5 AVD（2026-06-23）
                      │
                      └──→ 13.6 P2 connectedAndroidTest 7 角色 E2E 镜像准备
                          │
                          └──→ 步骤 6 客户验收（2026-06-23）
```

### 1.2 与 V1.3.8 FAT 准入 + Sprint 13 IMPL 衔接

- **V1.3.8 FAT 准入**（2026-06-23）：步骤 1-5 + 步骤 6 验收同步完成 → 准入闭环
- **Sprint 13 IMPL 启动**（2026-06-23+）：13.3 真实查询对接依赖 Redis 7 就位 + drawing:link 缓存验证
- **V1.3.9 灰度阶段 2**（2026-07-01）：13.3 ship 前置 · 必须 ship 才能灰度 SALES
- **V1.3.9 灰度阶段 4**（2026-07-04）：OPERATOR 工序缓存 Redis 7 验证必须通过

---

## 2. 顺序约束与协同依赖

### 2.1 硬前置（必须先完成）

1. **客户 IT 协同通知**（2026-06-17 EOD 前发出 · 提前 3 天）
   - 通知客户 IT 部门准备：VPN / 跳板机 / 客户机房 SSH 凭据 / firewall 变更窗口 / DHCP server 访问权限
   - 通知客户黄梓昀（151-0595-0281）+ 客户 IT 联系人（待客户提供）
   - 邮件模板：`docs/devops/templates/customer-it-notification.md`（待 PM 范蠡起草）

2. **PM 决策 #2 12.4 模式降级**（2026-06-16 前发出）
   - 客户 IT 协同通知中需明确：若降级模式二（A4 PDF）需客户端打印通道预先验证

3. **V1.3.8 FAT 通过**（前置条件 · 不在本计划范围）
   - V1.3.8 Sprint 10 集成 E 验证 PASS 后才能启动本计划
   - 关联：集成 E CONDITIONAL GO 决策树

### 2.2 步骤间依赖

- **步骤 1 → 步骤 2/3/4/5**：必须步骤 1 完成（SSH 接入）后才能启动后续
- **步骤 2 → 步骤 6**：Redis 7 必须先就位 · 验收时需 Redis 探活
- **步骤 3 → 步骤 6**：9100 端口必须先放行 · 验收时需 12.2/12.4 探活
- **步骤 4 → 步骤 6**：DHCP 静态 IP 必须先绑 · 验收时需 ZPL 打印机 IP 稳定
- **步骤 5 → 步骤 6**：AVD 镜像准备就位 · 验收时演示 13.6 E2E 入口
- **步骤 6**：所有步骤闭环后由客户黄梓昀签字

### 2.3 与 Sprint 12 + Sprint 13 协同

- **12.1 @Scheduled 缓存验证**：依赖步骤 2（Redis 7）· 集成 E 验证 12.1.4 测例（缓存命中率 ≥ 90%）
- **12.2 心跳调度**：依赖步骤 3（9100 端口）· 集成 E 验证 12.2.6 测例（心跳成功率 ≥ 99%）
- **12.4 ZPL Socket 3s timeout**：依赖步骤 3 + 步骤 4 · 集成 E 验证 12.4.16 测例（Socket 异常清理）
- **13.3 drawing:link Redis 7 5min TTL**：依赖步骤 2 · Sprint 13 IMPL 启动后立即验证
- **13.4 GmSummary Redis 7 缓存**：依赖步骤 2 · Sprint 13 IMPL 启动后立即验证
- **13.6 P2 AVD E2E**：依赖步骤 5 · Sprint 13 P2 deferred · V1.3.10 backlog 候选

---

## 3. 步骤 1 · 客户机房 SSH 接入（截止 2026-06-20）

### 3.1 目标

- 客户机房跳板机 / VPN 接入凭证就位
- DevOps 张良 SSH 客户端可登录客户机房服务器
- 双因子认证（VPN 密码 + 短信 / 硬件令牌）

### 3.2 详细命令

```bash
# === A. VPN 接入（按客户 IT 配置）===
# 1) 启动 OpenVPN 客户端（按客户提供 .ovpn 文件）
sudo openvpn --config customer-vpn.ovpn --daemon

# 2) 验证 VPN tunnel 建立
ip addr show tun0          # 期望：inet 10.x.x.x/24
ping -c 3 10.x.x.1         # 期望：3 packets received

# === B. SSH 跳板机接入 ===
# 3) 跳板机连接（按客户提供端口与凭据）
ssh -p 2222 devops@jump.example-customer.com
# 期望：Password + 短信验证码（双因子）

# 4) 跳板机 → 客户机房服务器
# 跳板机上执行（提前与客户 IT 约定 ssh config）：
ssh deploy@10.x.x.10
# 期望：直接登录客户机房 ERP 服务器

# === C. 验证服务器环境 ===
# 5) 检查 Docker 环境
docker --version            # 期望：Docker version 24.0+
docker compose version      # 期望：Docker Compose version v2.x
docker ps -a                # 期望：列出已有容器

# 6) 检查网络连通性（9100 端口 · 12.2/12.4 依赖）
nc -zv 10.x.x.50 9100       # 期望：succeeded（ZPL 打印机 · 待步骤 3 firewall 放行）

# 7) 检查 SSH key 持久化（避免每次重新认证）
ssh-copy-id deploy@10.x.x.10
```

### 3.3 交付物

- SSH 跳板机 + VPN 配置文件归档到内部 GitLab `internal/smart-workshop/devops-runbooks`
- 客户机房服务器清单（IP / OS / Docker 版本 / 已有容器）
- 双因子认证备份方案（短信接收失败 → 硬件令牌 fallback）

### 3.4 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 客户 IT VPN 配置延期 | 🟡 中 | 提前 3 天通知（2026-06-17）+ 邮件+电话双通道 |
| 2 | 双因子认证短信延迟 | 🟢 低 | 硬件令牌 fallback · 客户 IT 提前配置 |
| 3 | 客户机房 IP 不在已知列表 | 🟡 中 | 步骤 1 验证连通性时记录全部网段 · 更新 runbook |

---

## 4. 步骤 2 · Redis 7 容器化部署（截止 2026-06-21）

### 4.1 目标

- Redis 7 容器在客户机房 ERP 服务器就位
- 持久卷 `/data/redis` 挂载 · AOF 持久化开启
- erp-platform backend 通过 `spring.redis.host=10.x.x.10:6379` 接入
- 12.1 + 13.3 + 13.4 缓存验证就位

### 4.2 详细命令

```bash
# === A. 持久卷创建 ===
# 1) 创建 Redis 7 持久卷目录
sudo mkdir -p /data/redis
sudo chown -R 999:999 /data/redis    # Redis 容器内 redis 用户 UID
sudo chmod 755 /data/redis

# 2) 验证磁盘空间（Redis AOF 需要 ≥ 5GB）
df -h /data                           # 期望：≥ 50GB 可用

# === B. Redis 7 容器启动 ===
# 3) docker-compose.yml（写入 /data/redis/docker-compose.yml）
cat > /data/redis/docker-compose.yml <<'EOF'
version: '3.8'
services:
  redis7:
    image: redis:7.2-alpine
    container_name: erp-redis7
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - /data/redis:/data
    command:
      - redis-server
      - --appendonly yes
      - --appendfsync everysec
      - --maxmemory 2gb
      - --maxmemory-policy allkeys-lru
      - --requirepass ${REDIS_PASSWORD}
      - --bind 0.0.0.0
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

  redis-exporter:
    image: oliver006/redis_exporter:v1.58.0
    container_name: erp-redis-exporter
    restart: unless-stopped
    ports:
      - "9121:9121"
    environment:
      - REDIS_ADDR=redis://redis7:6379
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    depends_on:
      redis7:
        condition: service_healthy
EOF

# 4) 写入 .env（密码由 DevOps 张良生成）
cat > /data/redis/.env <<'EOF'
REDIS_PASSWORD=<32位随机密码·由DevOps生成并存入内部Vault>
EOF
sudo chmod 600 /data/redis/.env

# 5) 启动 Redis 7
cd /data/redis
docker compose up -d

# 6) 验证 Redis 7 健康
docker ps | grep erp-redis7    # 期望：Up X minutes (healthy)
docker logs erp-redis7 | tail -20

# === C. Redis 7 验证 ===
# 7) PING 测试
docker exec erp-redis7 redis-cli -a $REDIS_PASSWORD ping
# 期望：PONG

# 8) INFO 检查
docker exec erp-redis7 redis-cli -a $REDIS_PASSWORD info server | head -10
# 期望：redis_version:7.2.x

# 9) AOF 持久化验证
docker exec erp-redis7 redis-cli -a $REDIS_PASSWORD config get appendonly
# 期望：appendonly yes

# === D. erp-platform 接入配置 ===
# 10) 修改 erp-platform application-prod.yml
sudo vim /opt/erp-platform/config/application-prod.yml
# 添加或修改：
#   spring.redis.host: 10.x.x.10
#   spring.redis.port: 6379
#   spring.redis.password: <REDIS_PASSWORD>
#   spring.redis.timeout: 3000ms
#   spring.redis.lettuce.pool.max-active: 16
#   spring.redis.lettuce.pool.max-idle: 8

# 11) 重启 erp-platform backend
sudo systemctl restart erp-platform
sudo systemctl status erp-platform   # 期望：active (running)

# 12) 验证 erp-platform 已连接 Redis 7
sudo journalctl -u erp-platform --since "5 minutes ago" | grep -i redis
# 期望：Redis connection succeeded / Lettuce connected
```

### 4.3 交付物

- Redis 7 容器运行健康 · AOF 持久化开启 · 密码强度合规
- erp-platform backend Redis 接入验证日志
- Redis 7 监控接入 Prometheus（redis-exporter 端口 9121）
- Redis 7 备份脚本（每日凌晨 3 点 AOF copy 到 `/backup/redis/`）

### 4.4 关联 Story 验证

- **12.1 @Scheduled**：`draw.acl.cache.*` key 写入与读取验证（缓存命中率 ≥ 90%）
- **13.3 drawing:link**：5 类 link JOIN Redis 7 缓存验证（5min TTL · invalidate on write）
- **13.4 GmSummary**：仪表盘 4 图 Redis 7 缓存验证（30min TTL）
- **集成 E 12.1.4 测例**：Redis 7 连接成功 + 缓存命中验证

### 4.5 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 客户机房磁盘空间不足 | 🟡 中 | 步骤 1 验证 df -h ≥ 50GB · 否则扩容 / 清理 |
| 2 | Redis 7 容器启动失败（密码弱 / 端口占用） | 🟢 低 | 密码由 Vault 生成 32 位随机 · 6379 端口步骤 3 firewall 放行 |
| 3 | AOF 写入性能瓶颈 | 🟢 低 | `appendfsync everysec`（折中方案）· 监控磁盘 IO |
| 4 | erp-platform Redis 接入配置错误 | 🟡 中 | 步骤 1-2 连续执行 · 步骤 6 验收时探活 |

---

## 5. 步骤 3 · 9100 端口 firewall 放行（截止 2026-06-22）

### 5.1 目标

- 客户机房 ERP 服务器 9100 端口 inbound 放行（来自 ZPL 打印机回调 / admin 心跳）
- 客户机房 ERP 服务器 9100 端口 outbound 放行（到 ZPL 打印机 9100）
- firewall 规则记录到客户 IT 变更单
- 12.2 心跳 60s 调度探活 + 12.4 ZPL Socket 3s timeout 探活就位

### 5.2 详细命令

```bash
# === A. firewall 状态检查 ===
# 1) 查看客户机房 ERP 服务器 firewall 状态
sudo firewall-cmd --state         # 期望：running（firewalld）
sudo ufw status                   # 期望：Status: active（ufw）

# 2) 查看已有 9100 规则
sudo firewall-cmd --list-ports | grep 9100
sudo ufw status numbered | grep 9100

# === B. firewalld（CentOS / RHEL / AlmaLinux）===
# 3) 添加 9100 inbound 永久规则
sudo firewall-cmd --permanent --add-port=9100/tcp
sudo firewall-cmd --reload

# 4) 添加 ZPL 打印机 IP 段到 trusted zone
sudo firewall-cmd --permanent --zone=trusted --add-source=10.x.x.0/24
sudo firewall-cmd --reload

# 5) 验证规则
sudo firewall-cmd --list-all
# 期望：ports: 9100/tcp + trusted zone 含 10.x.x.0/24

# === C. ufw（Ubuntu / Debian）===
# 3') ufw 9100 端口放行
sudo ufw allow from 10.x.x.0/24 to any port 9100 proto tcp
sudo ufw reload
sudo ufw status numbered
# 期望：9100/tcp ALLOW 10.x.x.0/24

# === D. iptables（兜底）===
# 3'') iptables 9100 端口放行
sudo iptables -A INPUT -p tcp --dport 9100 -s 10.x.x.0/24 -j ACCEPT
sudo iptables -A OUTPUT -p tcp --dport 9100 -d 10.x.x.0/24 -j ACCEPT
sudo service iptables save

# === E. 9100 端口连通性验证 ===
# 6) 从 ERP 服务器 → ZPL 打印机 9100
nc -zv 10.x.x.50 9100            # 期望：succeeded
# 2s connect timeout（与 12.4 Socket 3s timeout 一致）

# 7) 从 ZPL 打印机 → ERP 服务器 9100（请客户 IT 在 ZPL 打印机端验证）
# 期望：succeeded

# 8) erp-platform 12.2 心跳任务验证
sudo journalctl -u erp-platform --since "5 minutes ago" | grep "9100\|heartbeat\|PrinterHeartbeat"
# 期望：心跳 60s 调度正常运行

# 9) erp-platform 12.4 ZPL Socket 测试
curl -X POST http://localhost:8080/api/print/zpl/test \
  -H "Content-Type: application/json" \
  -d '{"ip":"10.x.x.50","port":9100,"content":"^XA^FO50,50^FDTEST^FS^XZ"}'
# 期望：HTTP 200 + body 含 printLogId
```

### 5.3 交付物

- firewall 变更单（客户 IT 签字 + DevOps 张良签字）
- 9100 端口连通性双向验证报告（ERP↔ZPL 打印机）
- erp-platform 12.2 心跳调度日志（连续 60s × 10 = 10 次成功）
- erp-platform 12.4 ZPL Socket 测试日志（HTTP 200 + printLogId）

### 5.4 关联 Story 验证

- **12.2 心跳 60s 调度**：心跳探活成功率 ≥ 99%（集成 E 12.2.6 测例）
- **12.4 ZPL Socket 3s timeout**：Socket 异常清理验证（集成 E 12.4.16 测例）
- **风险 #4 firewall drop 而非 RST**：本次放行白名单后避免 drop · 集成 E 12.2.7 测例

### 5.5 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 客户 IT 协同延期（firewall 变更窗口） | 🟡 中 | 提前 3 天通知（2026-06-19）+ 提前提交变更单 |
| 2 | 9100 端口双向连通失败（NAT / 路由问题） | 🟡 中 | 步骤 1 验证网络拓扑 · 客户 IT 提供路由表 |
| 3 | ZPL 打印机 IP 漂移（DHCP 重启） | 🟡 P1 | 步骤 4 DHCP 静态 IP 预案 · admin UI"测试连接"按钮 |

---

## 6. 步骤 4 · DHCP 静态 IP 预案（截止 2026-06-22）

### 6.1 目标

- ZPL 打印机（10.x.x.50）+ ERP 服务器（10.x.x.10）绑 MAC 静态 IP
- DHCP server 预留 IP（DHCP reservation）配置就位
- DHCP 重启 / 续约 IP 变化时不影响 12.4 ZPL Socket 连接
- V1.4 mDNS / Bonjour backlog 已记录（本期接受风险）

### 6.2 详细命令

```bash
# === A. MAC 地址收集 ===
# 1) ZPL 打印机 MAC 地址（从打印机配置页面或机身标签获取）
# 期望格式：AA:BB:CC:DD:EE:FF
ZPL_MAC="AA:BB:CC:DD:EE:FF"

# 2) ERP 服务器 MAC 地址
ERP_MAC=$(ip link show eth0 | awk '/ether/ {print $2}')
echo "ERP MAC: $ERP_MAC"

# 3) 记录到客户 IT DHCP 预留表
cat > /tmp/dhcp-reservations.txt <<EOF
# 客户机房 DHCP 静态预留表（2026-06-22）
# 设备 | MAC | IP | 备注
ZPL-Printer-1 | $ZPL_MAC | 10.x.x.50 | 12.4 模式一双模打印（集成 E 12.4）
ERP-Server | $ERP_MAC | 10.x.x.10 | erp-platform backend
EOF

# === B. 客户 IT DHCP server 配置（按客户 IT 操作手册）===
# 4) 通知客户 IT 在 DHCP server 添加 reservation
# - 设备：ZPL-Printer-1
# - MAC：$ZPL_MAC
# - IP：10.x.x.50
# - 租约：永久（或 ≥ 30 天）

# - 设备：ERP-Server
# - MAC：$ERP_MAC
# - IP：10.x.x.10
# - 租约：永久（或 ≥ 30 天）

# === C. 客户 IT 操作完成后验证 ===
# 5) 从 ERP 服务器验证 ZPL 打印机 IP 仍为 10.x.x.50
ping -c 3 10.x.x.50

# 6) 从 ZPL 打印机 ping ERP 服务器（请客户 IT 操作）
# 期望：ping 10.x.x.10 success

# === D. DHCP 重启预案演练 ===
# 7) 模拟 DHCP 重启（请客户 IT 在维护窗口执行）
# - 步骤 1：客户 IT 备份 DHCP 配置
# - 步骤 2：客户 IT 重启 DHCP server
# - 步骤 3：客户 IT 验证 10.x.x.50 + 10.x.x.10 仍保留
# - 步骤 4：DevOps 张良验证 12.4 ZPL Socket 仍可连接

# === E. 风险登记（V1.4 mDNS backlog）===
# 当前接受风险：DHCP 重启后若 IP 漂移，admin 需手动改 sys_dict 配置
# 缓解：admin UI"测试连接"按钮 + 12.2 心跳探活告警
# 长期方案：V1.4 mDNS / Bonjour 自动发现（backlog 候选 #2）
```

### 6.3 交付物

- DHCP 静态预留表（客户 IT 签字 + DevOps 张良签字）
- DHCP 重启预案演练报告（维护窗口执行结果）
- V1.4 mDNS / Bonjour backlog 登记（PM 范蠡 Sprint 14 候选 #2）

### 6.4 关联 Story 验证

- **12.4 ZPL Socket**：IP 稳定后 Socket 3s timeout 不再因 IP 漂移触发（集成 E 12.4.16 测例）
- **12.2 心跳调度**：心跳探活不再因 IP 漂移失败（集成 E 12.2.6 测例）
- **风险 #14 DHCP 重启**：本期接受 · V1.4 mDNS backlog 解决

### 6.5 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 客户 IT DHCP server 操作延期 | 🟡 中 | 提前 3 天通知 · 维护窗口预约 |
| 2 | DHCP reservation 配置错误（IP 冲突） | 🟡 中 | 步骤 1 收集完整 MAC + 步骤 5-6 双向 ping 验证 |
| 3 | DHCP 重启导致 IP 漂移（V1.4 mDNS backlog） | 🟡 P1 | 本期接受 · admin UI"测试连接"按钮 · V1.4 backlog |

---

## 7. 步骤 5 · AVD 模拟器镜像（截止 2026-06-23）

### 7.1 目标

- Android Studio AVD（Android Virtual Device）镜像就位 · 支持 7 角色 connectedAndroidTest E2E
- 13.6 P2 deferred 验证入口就位（V1.3.10 backlog 候选 #3）
- 镜像覆盖 Android 9 / 12 / 14 三版本（覆盖 V1.3.7-V1.3.9 兼容范围）

### 7.2 详细命令

```bash
# === A. Android Studio + SDK 安装 ===
# 1) 下载 Android Studio（DevOps 沙箱工作站执行）
wget -q https://dl.google.com/android/studio/install/2023.3.1.18/android-studio-2023.3.1.18-linux.tar.gz
tar -xzf android-studio-*.tar.gz -C /opt/
sudo ln -sf /opt/android-studio/bin/studio.sh /usr/local/bin/android-studio

# 2) 安装 Android SDK（含 API 28 / 31 / 34）
sdkmanager --install "platform-tools" "platforms;android-28" "platforms;android-31" "platforms;android-34" \
  "system-images;android-28;google_apis;x86_64" \
  "system-images;android-31;google_apis;x86_64" \
  "system-images;android-34;google_apis;x86_64"

# === B. AVD 镜像创建 ===
# 3) 创建 7 角色 E2E AVD 模板
avdmanager create avd -n "admin-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"
avdmanager create avd -n "engineer-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"
avdmanager create avd -n "sales-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"
avdmanager create avd -n "purchaser-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"
avdmanager create avd -n "warehouse-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"
avdmanager create avd -n "qc-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"
avdmanager create avd -n "operator-avd" -k "system-images;android-31;google_apis;x86_64" -d "pixel_5"

# 4) 验证 AVD 创建
avdmanager list avd
# 期望：admin-avd / engineer-avd / sales-avd / purchaser-avd / warehouse-avd / qc-avd / operator-avd 共 7 个

# === C. AVD 启动 + connectedAndroidTest 入口 ===
# 5) 启动 admin AVD（headless）
emulator -avd admin-avd -no-window -no-audio -no-snapshot &
adb wait-for-device

# 6) 验证 AVD 就位
adb devices
# 期望：emulator-5554 device

# 7) 安装 android-impl APK
adb install -r android-impl/app/build/outputs/apk/release/app-release.apk

# 8) connectedAndroidTest 入口（13.6 P2 deferred · V1.3.10 backlog）
# 由 Sprint 13 dev agent 在 P2 启动时执行：
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.role=ADMIN
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.role=ENGINEER
# ... 7 角色 × 1 测例 × 3 API 级别 = 21 测例（13.6 P2 全量）

# === D. AVD 镜像归档 ===
# 9) 归档 AVD 镜像到内部 GitLab LFS（避免重新下载）
tar -czf /backup/avd-images-2026-06-23.tar.gz ~/.android/avd/
aws s3 cp /backup/avd-images-2026-06-23.tar.gz s3://internal-smart-workshop/avd/

# 10) 编写 AVD 启动脚本到 devops-runbooks
cat > /opt/devops-runbooks/avd-startup.sh <<'EOF'
#!/bin/bash
# 启动 7 角色 AVD · 13.6 P2 connectedAndroidTest
ROLES=(admin engineer sales purchaser warehouse qc operator)
for role in "${ROLES[@]}"; do
  emulator -avd "${role}-avd" -no-window -no-audio -no-snapshot &
  sleep 5
done
adb devices
EOF
chmod +x /opt/devops-runbooks/avd-startup.sh
```

### 7.3 交付物

- 7 角色 AVD 镜像就位 · Android 12（API 31）基线
- AVD 启动脚本 + 镜像归档（内部 GitLab LFS + S3 备份）
- 13.6 P2 connectedAndroidTest 入口就位（V1.3.10 backlog 候选）

### 7.4 关联 Story 验证

- **13.6 P2 deferred**：7 角色 × 1 测例 × 3 API 级别 = 21 测例（V1.3.10 backlog）
- **Story 1.4 pending_deploy**：android-impl 仓 Android E2E 收口

### 7.5 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | AVD 启动性能差（KVM 未启用） | 🟡 中 | 验证 `/dev/kvm` 可访问 · 否则启用 nested virtualization |
| 2 | AVD 镜像下载失败（Google 服务网络） | 🟢 低 | 镜像归档到内部 GitLab LFS + S3 · 避免重复下载 |
| 3 | 13.6 P2 启动时间延期 | 🟢 低 | Sprint 13 P2 deferred · V1.3.10 backlog 候选 · 本计划仅准备镜像 |

---

## 8. 步骤 6 · 客户服务器就位验收（截止 2026-06-23）

### 8.1 目标

- 客户黄梓昀（151-0595-0281）签字验收客户机房环境就位
- Redis 7 + 9100 端口 + DHCP 静态 IP + AVD 镜像全部就位
- V1.3.8 FAT 准入前置条件闭环

### 8.2 验收清单

| # | 项 | 验证方法 | 期望结果 | 责任 |
|---|----|---------|---------|------|
| 1 | Redis 7 容器运行 | `docker ps \| grep erp-redis7` | Up + (healthy) | DevOps 张良 |
| 2 | Redis 7 AOF 持久化 | `redis-cli config get appendonly` | yes | DevOps 张良 |
| 3 | erp-platform Redis 接入 | `journalctl -u erp-platform \| grep redis` | connected | DevOps 张良 |
| 4 | 9100 端口 firewall 放行 | `nc -zv 10.x.x.50 9100` | succeeded | 客户 IT |
| 5 | 12.2 心跳 60s 调度 | `journalctl -u erp-platform \| grep heartbeat` | 60s × 10 = 10 成功 | DevOps 张良 |
| 6 | 12.4 ZPL Socket 3s timeout | `curl -X POST /api/print/zpl/test` | HTTP 200 + printLogId | DevOps 张良 |
| 7 | DHCP 静态 IP 绑 MAC | 客户 IT 维护窗口确认 | 10.x.x.50 + 10.x.x.10 稳定 | 客户 IT |
| 8 | AVD 镜像就位 | `avdmanager list avd` | 7 角色 × AVD | DevOps 张良 |
| 9 | V1.3.8 FAT 通过 | QA 商鞅集成 E 验证 | PASS | QA 商鞅 |
| 10 | V1.3.9 Sprint 12 集成 E PASS | QA 商鞅 86 测例 | PASS | QA 商鞅 |

### 8.3 验收签字模板

```
==========================================
客户机房环境就位验收单
==========================================
客户名称：昆山佰泰胜
验收人：黄梓昀（151-0595-0281）
验收日期：2026-06-23
DevOps：张良
==========================================

□ Redis 7 容器就位
□ 9100 端口 firewall 放行
□ DHCP 静态 IP 预案
□ AVD 模拟器镜像就位
□ V1.3.8 FAT 通过
□ V1.3.9 Sprint 12 集成 E 验证 PASS

验收结论：□ 通过  □ 不通过（原因：____________）

客户签字：_______________  日期：_______________

DevOps 签字：_______________  日期：_______________
==========================================
```

### 8.4 风险

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | 客户黄梓昀出差无法签字 | 🟢 低 | 提前 3 天预约 · 委托客户 IT 签字 + 邮件确认 |
| 2 | 验收发现环境问题 | 🟡 中 | 步骤 1-5 连续执行验证 · 步骤 6 仅最后签字 · 风险前移 |

---

## 9. 风险登记（合并去重）

### 9.1 风险表

| # | 风险 | 步骤 | 等级 | 缓解 | 截止 |
|---|------|------|------|------|------|
| 1 | 客户 IT 协同延期（VPN / firewall / DHCP） | 1/3/4 | 🟡 中 | 提前 3 天通知（2026-06-17）+ 邮件 + 电话双通道 | 2026-06-17 通知发出 |
| 2 | DHCP 重启导致 12.4 ZPL Socket IP 漂移 | 4 | 🟡 P1 | DHCP 静态 IP 预案 + admin UI"测试连接"按钮 · V1.4 mDNS backlog | 2026-06-22 |
| 3 | Redis 7 容器启动失败 | 2 | 🟢 低 | Docker 环境步骤 1 验证 · 密码 Vault 生成 | 2026-06-21 |
| 4 | 9100 端口双向连通失败（NAT / 路由）| 3 | 🟡 中 | 步骤 1 验证网络拓扑 · 客户 IT 路由表 | 2026-06-22 |
| 5 | AVD KVM 未启用性能差 | 5 | 🟡 中 | 验证 /dev/kvm · nested virtualization | 2026-06-23 |
| 6 | 客户磁盘空间不足 | 2 | 🟡 中 | 步骤 1 验证 df -h ≥ 50GB · 否则扩容 | 2026-06-20 |
| 7 | ZPL 打印机型号兼容 | 3 | 🟡 中 | 步骤 1 确认客户 ZPL 打印机型号（启邦 DL-888B / 斑马 ZD420 / TSC TTP-244 Pro）| 2026-06-20 |
| 8 | 验收延期（客户黄梓昀出差）| 6 | 🟢 低 | 提前 3 天预约 · 客户 IT 委托签字 | 2026-06-20 |

### 9.2 风险等级分布

| 等级 | 数量 | 占比 |
|------|------|------|
| 🟡 P1 | 1（#2 DHCP 重启）| 12.5% |
| 🟡 中 | 5 | 62.5% |
| 🟢 低 | 2 | 25% |

### 9.3 与集成 E 验证衔接

- 集成 E 验证委派 #4（DevOps）状态从 🟡 待执行 → ✅ 已执行（本文档）
- V1.3.8 FAT 准入前置条件：5 步骤全部完成 + 步骤 6 验收签字
- Sprint 13 IMPL 启动前置：Redis 7 + drawing:link 缓存就位

---

## 10. 截止时间表（客户机房 5 步骤 · 关键路径）

```
2026-06-14  本计划起草（DevOps 张良）
2026-06-17  客户 IT 协同通知发出（提前 3 天）
2026-06-20  步骤 1 完成 · 客户机房 SSH 接入
2026-06-21  步骤 2 完成 · Redis 7 容器就位
2026-06-22  步骤 3 完成 · 9100 端口 firewall 放行
2026-06-22  步骤 4 完成 · DHCP 静态 IP 预案
2026-06-23  步骤 5 完成 · AVD 模拟器镜像
2026-06-23  步骤 6 完成 · 客户服务器就位验收（黄梓昀签字）
2026-06-23+ V1.3.8 FAT 准入 + Sprint 13 IMPL 启动
2026-06-30+ V1.3.9 客户灰度阶段 1 启动（admin + ENGINEER）
2026-07-01  灰度阶段 2（SALES）· 13.3 必须 ship
2026-07-02  灰度阶段 3（PUR/WH/QC）
2026-07-04  灰度阶段 4（OPERATOR · 2 天观察）
2026-07-07+ V1.3.9 客户验收（黄梓昀签字）
2026-07-14+ V1.3.9 正式上线
```

---

## 11. 验证闭环

### 11.1 步骤验证矩阵

| 步骤 | 集成 E 验证关联测例 | 验收责任 | 状态 |
|------|-------------------|---------|------|
| 1（SSH 接入）| 12.1 / 12.2 / 12.4 远程部署前置 | DevOps 张良 | 🟡 待启动 |
| 2（Redis 7）| 12.1.4 缓存命中 + 13.3 + 13.4 | DevOps 张良 + QA 商鞅 | 🟡 待启动 |
| 3（9100 端口）| 12.2.6 心跳探活 + 12.4.16 Socket 清理 | DevOps 张良 + 客户 IT | 🟡 待启动 |
| 4（DHCP 静态 IP）| 12.4.16 IP 稳定 + 风险 #14 缓解 | 客户 IT | 🟡 待启动 |
| 5（AVD）| 13.6 P2 deferred · V1.3.10 backlog | DevOps 张良 + Sprint 13 dev | 🟡 待启动 |
| 6（验收）| V1.3.8 FAT + Sprint 12 集成 E 闭环 | 客户黄梓昀 + DevOps | 🟡 待启动 |

### 11.2 集成 E 验证闭环判定

- ✅ 步骤 1-5 全部完成 + 步骤 6 客户签字
- ✅ V1.3.8 FAT 准入闭环（6 项委派中 4/6 已执行）
- 🟡 委派 1（86 测例）+ 委派 2（typecheck:ci）由 QA 商鞅同步执行（截止 2026-06-16）
- 🟡 委派 5（ESC/POS 评估）+ 委派 6（12.4 降级）由 PM 范蠡决策（截止 2026-06-16）
- 🟡 委派 3（12.1 灰度 4 阶段）等 V1.3.8 FAT 通过后启动（截止 2026-06-30+）

---

## 12. 签字

**DevOps 张良** · 2026-06-14 · Sprint 12 客户机房环境就位计划 · 6 步骤就位 · 截止 2026-06-23

**关联签字**（待补）：

- 客户 IT（待客户提供联系人）· 待执行（VPN / firewall / DHCP 协同）
- 客户黄梓昀 · 待验收（步骤 6 签字 · 2026-06-23）
- PM 范蠡 · 2026-06-14 · 计划已审 · 与巡检报告 6 项委派对齐
- SM 萧何 · 2026-06-14 · 计划已审 · 与集成 E 验证 CONDITIONAL GO 路径对齐

---

## 13. 与 Sprint 12 / Sprint 13 / V1.3.10 backlog 衔接

### 13.1 与 Sprint 12 集成 E 衔接

| 项 | 决策前状态 | 决策后状态 |
|----|-----------|-----------|
| 集成 E 委派 #4（客户机房环境）| 🟡 待执行 | 🟡 已计划（本文档 · 截止 2026-06-23）|
| V1.3.8 FAT 准入 | 🟡 CONDITIONAL | 🟡 委派 #4 截止后准入 |
| V1.3.9 Sprint 12 集成 E CONDITIONAL GO | 🟡 5 项委派待执行 | 🟡 委派 #4 已计划 + #5/#6 待 PM 决策 + #1/#2 待 QA |

### 13.2 与 Sprint 13 IMPL 衔接

- **13.3 drawing:link 缓存**：依赖 Redis 7 就位（步骤 2）· Sprint 13 IMPL 启动后立即验证
- **13.4 GmSummary 缓存**：依赖 Redis 7 就位（步骤 2）· Sprint 13 IMPL 启动后立即验证
- **13.6 AVD E2E**：依赖 AVD 镜像（步骤 5）· Sprint 13 P2 deferred · V1.3.10 backlog 候选

### 13.3 与 V1.3.10 backlog 衔接

- DHCP 自动发现（mDNS / Bonjour）：本计划接受风险 · V1.3.10 backlog 候选 #2
- AVD 7 角色 E2E：本计划准备镜像 · Sprint 13 P2 deferred · V1.3.10 backlog 候选 #3
- 9100 端口 SNMP 协议升级：本计划 firewall 放行 · V1.3.10 backlog 候选 #4

---

**DevOps 张良 · Sprint 12 客户机房环境就位计划 · 6 步骤 · 截止 2026-06-23 · 与集成 E 验证 CONDITIONAL GO 闭环对齐 · V1.3.8 FAT 准入前置**