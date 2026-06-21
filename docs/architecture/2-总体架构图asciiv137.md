# 2. 总体架构图（ASCII，V1.3.7）

```
+---------------------------------------------------------------------------+
|                          客户端层 (Client Tier)                            |
|                                                                           |
|   [Nginx 80/443]                                                           |
|      |                                                                    |
|      |--- /web/* -----------> [Web (Vue 3 SPA) on Nginx 静态]              |
|      |--- /api/* -----------> upstream erp-gateway                        |
|      |--- /apk/* -----------> [APP 下载页]                                |
|                                                                           |
|   [Android APP (Kotlin)] --HTTPS+JSON--> Nginx --> Gateway                |
|   [PDA / 工业扫码枪 (HTTP POST)]      --> Nginx --> Gateway              |
+---------------------------------------------------------------------------+
                                    |
                                    v
+---------------------------------------------------------------------------+
|                       API 网关层 (Gateway Tier)                            |
|                                                                           |
|   [erp-gateway × 2 实例]  (Spring Cloud Gateway, 8080)                    |
|      - JWT 鉴权 + Redis 黑名单踢出                                        |
|      - Sentinel 限流（QPS/IP/接口）                                       |
|      - 动态路由（按 path 前缀 /api/v1/{service}/）                        |
|      - 灰度发布（Header: X-Gray-Tag）                                     |
|      - 全局日志 + TraceId 注入                                            |
|      - 服务发现：Nacos 8848（每 5s 心跳）                                 |
+---------------------------------------------------------------------------+
                                    |
                +-------------------+-------------------+
                |                   |                   |
                v                   v                   v
+-------------------------+ +-------------------+ +------------------------+
|  erp-platform × 1        | | erp-business × 2   | | erp-production × 2     |
|  基础能力服务 (8081)      | | 业务聚合 (8082)    | | 车间执行 (8083)         |
|  - 用户/角色/部门/职位   | | - 客户/报价/订单   | | - 工单/工序/扫码/报工   |
|  - 审批工作流引擎         | | - 合同/回款/利润   | | - 委外下单/扫到货       |
|  - 数据字典 / 系统参数    | | - 采购/仓储/品质   | | - 工序分配职责分离      |
|  - 文件服务 / 消息服务    | | - 财务/人事/报表   | |   (生管自/委,采购选厂商) |
|  - 邮件服务 (V1.3.7 新)  | | - 料号成本聚合     | | - 委外 7 状态机         |
+-------------------------+ +-------------------+ +------------------------+
                |                   |                   |
                +---------+---------+---------+---------+
                                    |
                                    v
+---------------------------------------------------------------------------+
|              注册中心 / 配置中心 (Nacos 2.3+, 8848)                         |
|                                                                           |
|   - 服务注册发现：4 个后端服务 + 1 个 XXL-JOB 执行器                       |
|   - 动态配置（V1.3.7 新增）：                                              |
|       app.email.smtp.host=smtp.163.com                                    |
|       app.email.smtp.port=465                                             |
|       app.email.smtp.auth-code=${EMAIL_163_AUTH_CODE}  # KMS 注入           |
|       app.email.retry-policy=1h,6h,24h                                    |
|       app.email.daily-quota=5000                                          |
|       app.email.quota-warn-threshold=0.8                                 |
|       app.email.log-retention-days=90                                     |
|       app.outsub.notify-channel=email_163                                 |
|       app.outsub.rework-alert-threshold=2                                 |
|       app.cost-cache-ttl=3600                                             |
|   - 命名空间隔离：dev / staging / prod                                   |
|   - 集群模式：单机起步，V1.1 评估 3 节点                                   |
+---------------------------------------------------------------------------+
                          |                   |
                          v                   v
+---------------------------------------------------------------------------+
|                       公共 Module (core - 仓内共享)                        |
|                                                                           |
|  common-dto / common-entity / common-util / common-web                   |
|  common-redis (Stream + 分布式锁) / common-job (XXL-JOB)                  |
|  common-security (JWT + Spring Security + AES-256 + RBAC)                |
|  common-oss (MinIO + 签字扫描件 AES-256-GCM)                              |
|  common-email (V1.3.7 新: 163 SMTP + 重试 + 额度监控 + 发送日志)          |
|  common-state-machine (V1.3.4 新: 委外 7 状态机 + 返修闭环)                |
|  common-cost-aggregator (V1.3.4 新: 料号 5 段成本 + 失效驱动)              |
|  common-outsub-eta (V1.3.4 新: 历史交期预估中位数 + 分位数)                |
+---------------------------------------------------------------------------+
                                    |
                                    v
+---------------------------------------------------------------------------+
|                          数据层 (Data Tier)                                |
|                                                                           |
|  [MySQL 8.0 Master:3306]  ──async──>  [MySQL Slave:3306 (read_only)]      |
|      |                                                                  |
|      |--- 库: erp_platform / erp_business / erp_production               |
|                                                                           |
|  [Redis 7 :6379]                                                                |
|      |--- Cache (业务缓存) + Session + 分布式锁 (Redisson)                |
|      |--- Stream: notify / scan-sync / audit / outbox / cost-invalidate   |
|                                                                           |
|  [MinIO :9000/:9001]  (S3 兼容, AES-256-GCM 加密存储)                     |
|      |--- 图纸 PDF / DWG / STEP                                           |
|      |--- 合同 / 检测报告                                                 |
|      |--- **对账签字扫描件（V1.3.6 新 · 5 年保留 · 下载限 3 角色）**         |
|                                                                           |
|  [XXL-JOB Admin :8088]                                                     |
|      |--- 邮件重试 1h/6h/24h (V1.3.7 新)                                  |
|      |--- 邮件额度监控 80% 告警 (V1.3.7 新)                                 |
|      |--- 返修次数预警 (V1.3.4)                                            |
|      |--- 料号成本失效重算 (V1.3.4)                                        |
|      |--- 邮件发送日志清理 90 天 (V1.3.7 新)                                |
+---------------------------------------------------------------------------+
                                    |
                                    v
+---------------------------------------------------------------------------+
|                        运维层 (Ops Tier)                                   |
|                                                                           |
|  [Prometheus :9090]  ──scrape──>  [Grafana :3000]  ──alert──>  企业微信    |
|                                                                           |
|  [SkyWalking OAP]  ──trace──>  [SkyWalking UI]                            |
|                                                                           |
|  [GitHub Actions CI/CD]  ──push──>  [本地 Docker Registry]                 |
+---------------------------------------------------------------------------+
```

> **V1.3.7 关键变化（与 V1.0 对比）**：
> 1. **删除** platform-message 中的"短信"通道（V1.3.7 删短信渠道）
> 2. **新增** common-email Module（163 SMTP + 重试 + 额度 + 日志）
> 3. **新增** common-state-machine / common-cost-aggregator / common-outsub-eta 三个 core 子模块
> 4. **新增** MinIO 中"对账签字扫描件"桶（AES-256-GCM 加密）
> 5. **新增** 4 个 XXL-JOB 任务（邮件重试 / 额度 / 失效重算 / 日志清理）
> 6. **阿里云 ACR → 本地 Docker Registry**（本地化部署，不上公有云）

---
