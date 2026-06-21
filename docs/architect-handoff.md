# CNC 加工厂 ERP 系统 - 架构师交付文档（Architect Handoff V1.3.7）

> **架构师**：Architect agent（orchestrix · 鲁班）
> **对接输入**：`docs/prd.md` V1.3.7（PM 范蠡 · 2026-06-08）
> **文档版本**：V1.1（基于 V1.0 升级，对齐 PRD V1.3.3 → V1.3.7 共 6 个版本的增量）
> **编写日期**：2026-06-09
> **升级原因**：原 V1.0（2026-06-04）基于 PRD V1.3.1，落后 6 个版本（V1.3.2/3/4/5/6/7），主要漏：工序分配职责分离、对账不含"线下"描述、单一 163 邮箱、签字扫描件 AES-256、邮件投递三重防线、料号成本聚合、委外 7 状态机、仓管到货扫码。
> **目标读者**：后端工程师 / 前端工程师 / Android 工程师 / QA / 运维
> **配套文档**：`docs/handoff-upgrade-checklist-v1.3.7.md`（升级清单，本文件的变更依据）

---

## 0. 灵魂一致性声明（Architect V1.3.7 升级版）

**架构师的存在意义**：让"1 个后端工程师能独立完成 1 个 Epic 的端到端开发与部署"，而不是"11 个微服务 + 6 个中间件 + 3 人天才能发一个版本"。

**V1.3.7 升级带来的灵魂对齐**：
- **"职责分离"对架构的影响**：生管 vs 采购 API 严格分离 —— 不允许"生产服务"代采购选厂商（AD-1）
- **"对账不含线下"对架构的影响**：删除"采购带纸去厂商处"流程节点；厂商线下流程不在 PRD 描述 = 也不在架构中（AD-2）
- **"单一 163 邮箱"对架构的影响**：删除所有短信依赖（SDK/API/表/字段），收窄通知渠道为 SMTP（AD-3）

**灵魂一致性自评（V1.3.7）**：

| 维度 | 自评 | 说明 |
|------|------|------|
| WHY 优先 | 9 | 每次升级（V1.3.3→V1.3.7）都先答"为什么改"——工序分配、163 邮箱、签字件 AES-256 全部可追溯到客户原话 |
| 极致简化 | 9.5 | V1.3.5 收回区域码 AREA- / V1.3.6 删厂商微信 / V1.3.7 删短信 —— 越改越简 |
| 可落地 | 8.5 | 1 月工期 + 5 周 5 里程碑 + 工序分配 API 分离 + 163 邮箱 + 7 状态机 —— **M2 主链路 6 Epic 2 周并行仍是压力点** |
| 性能达标 | 8 | 扫码 P95 ≤ 1s、报表 P95 ≤ 10s、并发 100+50 全部有可量化路径；料号成本聚合走 Redis Stream 失效驱动避免重算 |
| 风险预判 | 9 | 6 大风险全部有缓解 + V1.3.7 新增 3 项：① 163 邮箱额度爆；② 签字扫描件丢；③ 工序分配数据不一致 |
| 数据安全 | 9.5 | 4 大原则 + 8 道防线全在架构层落地（DEK 甲方 IT 持有 / 字段级 AES-256 / 堡垒机 / MinIO 加密 / 下载审计）|
| **综合** | **8.92** | V1.3.7 后架构可驱动后端/前端/APP 三端开发 |

---

## 1. 架构愿景（≤200 字）

CNC 加工厂 ERP 采用 **粗粒度微服务 + Modulith（仓内多 Module）** 架构：3 个后端业务服务（business / production / platform）+ 1 个 API Gateway + 1 个 core 公共 Module，**全部位于一个 backend 仓内**。消息中间件用 **Redis Stream + XXL-JOB**（不引入 RabbitMQ）。通知渠道收敛为**单一 163 邮箱 SMTP**（V1.3.7 删短信）。委外对账**不含"线下"流程**（V1.3.7 客户原话），签字扫描件走 **MinIO + AES-256-GCM 加密存储**。目标是 **1 个后端工程师能独立完成 1 个 Epic 的端到端开发、测试、部署**；运维只需 1 台 8 核 32G 服务器 + Docker Compose。反例是"11 个微服务 + 6 个中间件 + K8s 集群"——CNC 单机部署场景下完全不需要。

---

## 2. 总体架构图（ASCII，V1.3.7）

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

## 3. 服务拆分详细设计（V1.3.7 升级版）

### 3.1 erp-platform（基础能力服务）

- **职责**：所有跨业务的基础能力，被其他服务**强依赖**。
- **包含模块**（V1.3.7 调整）：
  - `platform-auth`：用户/角色/部门/职位 + JWT 签发 + 权限拦截
  - `platform-workflow`：审批工作流引擎（基于 Flowable 改造，简化版）
  - `platform-sysparam`：系统参数（金额阈值、编号规则、APP 离线时长、V1.3.7 新增邮件配置）
  - `platform-dict`：数据字典（物料分类/工序类型/表处类型/班别/仓库/币种）
  - `platform-file`：MinIO 文件服务（**V1.3.6 新增签字扫描件桶 + AES-256-GCM 加密**）
  - `platform-message`：**V1.3.7 收窄为 APP 推送 / 企业微信 / 站内信**（删除短信）
  - `platform-audit`：审计日志（所有写操作前后值；**V1.3.6 新增签字件下载审计**）
- **依赖**：`core`
- **端口**：`8081`
- **数据库**：`erp_platform`
- **实例数**：1

### 3.2 erp-business（业务聚合服务）

- **职责**：所有业务 CRUD 聚合（CRM/销售/采购/仓储/品质/财务/人事/报表）+ **V1.3.6 对账模块 + V1.3.4 料号成本聚合**。
- **包含模块**（按业务域分 9 个包）：
  - `business-crm`：客户/联系人/洽谈/共享
  - `business-sales`：报价/订单/合同/回款
  - `business-purchase`：询价/采购订单/到货
  - `business-warehouse`：库存/入库/出库/库位/批次
  - `business-quality`：来料/过程/成品检 + FA + 三次元 + 不良品
  - `business-finance`：应收/应付/账龄/付款/利润
  - `business-hr`：员工/考勤/薪酬/绩效/招聘
  - `business-reporting`：跨域聚合查询/看板
  - **`business-cost-aggregator`（V1.3.4 新增）**：料号 5 段成本（材料/工时/表处/外协/管理费）+ Redis Stream 失效驱动 + 5 Tab 输出
- **依赖**：`core`、`erp-platform`（Feign）
- **端口**：`8082`
- **数据库**：`erp_business`
- **实例数**：2

### 3.3 erp-production（车间执行服务，V1.3.7 关键升级）

- **职责**：车间执行核心，独立部署、独立扩容（扫码写并发高）。
- **包含模块**（V1.3.7 调整）：
  - `production-workorder`：工单/排产/状态机/MRP
  - `production-process`：工序/工艺路线
  - `production-scan`：扫码三码识别（**V1.3.5 收回区域码 · 维持 3 类 GD/LZ/SB + WL-/WW-**）
  - `production-report`：报工/过站/计时/计件
  - `production-outsource`：**V1.3.7 改版** ——
    - `outsource-allocation`（**V1.3.7 新增**）：工序分配职责分离；生管 API 决定自/委，采购 API 选厂商
    - `outsource-order`：委外下单（WW- 单码生成）
    - `outsource-receive`（**V1.3.5 新增**）：仓管扫 WW- 到货（替代 V1.3.5 取消的 vendor-delivery 角色）
    - `outsource-state-machine`（**V1.3.4 新增**）：7 状态机（PENDING_SHIP / SHIPPING / PENDING_INSPECTION / INSPECTING / QUALIFIED_STORAGE / STORED / REPAIR_REQUESTED + NOTIFIED_REPAIR 衍生态）
    - `outsource-rework`（**V1.3.4 新增**）：返修闭环 + rework_count 累加 + ≥ 阈值（默认 2）自动预警
    - `outsource-history-eta`（**V1.3.4 新增**）：历史交期预估（最近 3 次中位数 + 50%/80%/100% 分位数）
  - `production-machine`：设备台账/机台负荷
- **依赖**：`core`、`erp-platform`（Feign）、`erp-business`（Feign：工单关联销售订单、扫码写业务库存）
- **端口**：`8083`
- **数据库**：`erp_production`
- **实例数**：2（高优先级，扫码高峰期可独立扩到 4 实例）
- **特殊优化**：
  - 扫码写先入 Redis Stream，**异步批量落库**
  - 关键接口只查 Redis 缓存（P95 ≤ 1s）
  - 报表查询走主库，读写分离强制读从

### 3.4 erp-gateway（API 网关）

- **职责**：**不写任何业务代码**，只做流量入口治理。
- **核心功能**：
  - 动态路由：`/api/v1/platform/**` → `erp-platform`，`/api/v1/business/**` → `erp-business`，`/api/v1/production/**` → `erp-production`
  - JWT 鉴权：解析 Token → 注入 `X-User-Id` / `X-Tenant-Id` Header → 转发
  - Redis 黑名单：踢出禁用用户
  - Sentinel 限流：按 IP / 用户 / 接口 QPS 三维度
  - 全局日志：记录 access log + 慢请求
  - TraceId 注入：`X-Trace-Id` Header 透传到下游
  - 灰度发布：`X-Gray-Tag: v2` Header 路由到灰度实例
- **依赖**：`core`
- **端口**：`8080`
- **部署**：2 实例

### 3.5 core（公共 Module，**非独立服务**，V1.3.7 升级）

> `core` 是 backend 仓内 **Maven Module**（与 `erp-business` / `erp-production` / `erp-platform` 同级），**不独立部署**。所有服务通过 Maven 依赖引入。

| 子 Module | 内容 | 关键类 |
|----------|------|--------|
| **common-dto** | API DTO、查询参数、分页响应 | `PageResponse<T>`、`Result<T>`、`BaseDTO` |
| **common-entity** | 基础实体 | `BaseDO`（id / createTime / updateTime / createBy / updateBy / version 乐观锁） |
| **common-util** | 通用工具 | `Money`（BigDecimal 金额，HALF_UP）、`SnowflakeIdGenerator`、`DictCache`、`AesGcmUtil`（**V1.3.6 新增 · 签字件加密**） |
| **common-web** | Web 通用 | `@RestControllerAdvice` 全局异常、`PageRequest` 参数校验、`@ApiLog`、`TraceIdFilter` |
| **common-redis** | Redis 封装 | `RedisStreamTemplate`、`DistributedLock`、`CacheTemplate` |
| **common-job** | XXL-JOB 基类 | `@XxlJob` 注解基类、统一异常处理 |
| **common-security** | 安全 | `JwtUtil`、`SecurityConfig`、`RBAC` 注解、`@DataScope` |
| **common-oss** | MinIO 封装 | `MinioTemplate`（**V1.3.6 新增 `putEncryptedObject` / `getDecryptedObject` · 签字件专用**） |
| **common-audit** | 审计 | `@AuditLog` 注解 + AOP（**V1.3.6 新增下载审计**）|
| **`common-email`**（**V1.3.7 新增**） | 163 邮件客户端 | `Email163Client`（SMTP）、`EmailRetryPolicy`（1h/6h/24h）、`EmailQuotaMonitor`（80% 告警）、`EmailSendLog`（90 天保留）|
| **`common-state-machine`**（**V1.3.4 新增**） | 通用状态机 | `StateMachine<S, E>`、委外 7 状态机配置、守卫 |
| **`common-cost-aggregator`**（**V1.3.4 新增**） | 料号成本聚合 | `CostAggregator`、5 段成本计算、Redis Stream 失效消息驱动、TTL 缓存 |
| **`common-outsub-eta`**（**V1.3.4 新增**） | 委外交期预估 | `OutsubEtaCalculator`、最近 3 次中位数 + 50%/80%/100% 分位数 |

---

## 4. 数据架构

### 4.1 数据库分库分表策略

| 维度 | 策略 | 说明 |
|------|------|------|
| **物理库** | 3 个：`erp_platform` / `erp_business` / `erp_production` | 按服务物理隔离 |
| **逻辑库（schema）** | `erp_business` 内分 9 个 schema：`crm / sales / purchase / warehouse / quality / finance / hr / reporting / cost` | V1.3.4 新增 cost schema |
| **分表** | **不分表** | 单表数据量预估 < 500 万 |
| **主从** | 1 主 1 从 | 单台服务器部署 |
| **读写分离** | MyBatis 拦截器 + `@ReadOnly` 注解 | 写强制主库；默认走从库 |
| **字符集** | `utf8mb4` + `utf8mb4_unicode_ci` | |
| **引擎** | InnoDB | |

### 4.2 ER 图（核心实体，V1.3.7 升级版）

#### 用户域（erp_platform）
```
sys_user (id, username, password_hash, real_name, phone, email, dept_id, status, last_login_time)
sys_role (id, role_code, role_name, data_scope, amount_threshold, status)
sys_dept (id, parent_id, dept_name, sort, status)
sys_position (id, dept_id, position_name, sort)
sys_user_role (user_id, role_id)
sys_role_permission (role_id, menu_id, action)
sys_workflow (id, workflow_code, nodes_json, conditions_json, status)
sys_dict (id, dict_type, dict_code, dict_label, sort, status)
sys_file (id, bucket, object_key, original_name, size, mime, uploader_id, encryption_meta)  -- V1.3.6 加密元数据
sys_audit_log (id, user_id, module, action, before_json, after_json, ip, ts)
sys_download_log (id, file_id, user_id, ip, ts, action)  -- V1.3.6 新增（签字件下载审计）
```

#### 客户域（erp_business.crm）—— 无变化
```
crm_customer (id, customer_code, name, industry, credit_limit, owner_id, protect_until, status)
crm_contact (id, customer_id, name, position, phone, email, is_primary)
crm_follow_record (id, customer_id, owner_id, follow_time, content, stage, next_follow_time)
crm_customer_share (id, customer_id, shared_user_id, permission)
```

#### 销售域（erp_business.sales）—— 无变化
```
sales_quote (id, quote_no, customer_id, currency, delivery_date, total_amount, status, owner_id, approver_id)
sales_quote_item (id, quote_id, drawing_no, material, spec, qty, unit_price, amount, is_fa, is_new)
sales_order (id, order_no, customer_id, quote_id, order_type, is_fa, total_amount, status, owner_id)
sales_order_item (id, order_id, line_no, product_code, qty, unit_price, amount, delivery_date)
sales_order_change (id, order_id, change_type, before_json, after_json, changed_by, changed_at)
sales_contract (id, contract_no, order_id, file_id, signed_at, status)
sales_payment_plan (id, contract_id, period_no, plan_date, plan_amount, actual_amount, status)
sales_receipt (id, contract_id, receipt_date, amount, payer, remark)
```

#### 物料域（erp_business.warehouse + 共享）—— 无变化
```
mdm_material (id, material_code, name, category, spec, unit, safety_stock_min, safety_stock_max, price_limit)
mdm_bom_header (id, product_code, bom_version, type, status, published_at)
mdm_bom_line (id, bom_header_id, parent_code, child_code, qty, loss_rate, process_no)
mdm_process (id, process_code, process_name, std_time_min, machine_type, unit_price)
mdm_product_route (id, product_code, process_seq, process_code, is_outsource)
```

#### 生产域（erp_production）—— V1.3.4 升级
```
prod_workorder (id, workorder_no, order_id, product_code, qty, plan_start, plan_end, status, machine_id)
prod_workorder_process (id, workorder_id, process_seq, process_code, qty, is_outsource, assigned_by, assigned_at, status, start_at, end_at)
  -- V1.3.7 新增 is_outsource + assigned_by（生管/采购 ID 隔离）
prod_scan_start (id, workorder_id, process_seq, operator_id, machine_id, started_at)
prod_scan_report (id, workorder_id, process_seq, operator_id, qty_done, qty_ok, qty_scrap, reported_at)
prod_scan_transfer (id, workorder_id, from_process, to_process, transfer_code, qty, transferred_by, transferred_at)
```

#### 仓储域（erp_business.warehouse）—— 无变化
```
wms_warehouse (id, warehouse_code, warehouse_name, status)
wms_location (id, warehouse_id, location_code, capacity, status)
wms_inbound (id, inbound_no, type, source_order_id, warehouse_id, operator_id, status)
wms_inbound_item (id, inbound_id, material_id, batch_no, qty, location_id)
wms_outbound (id, outbound_no, type, source_order_id, warehouse_id, operator_id, status)
wms_outbound_item (id, outbound_id, material_id, batch_no, qty, location_id)
wms_inventory (id, material_id, warehouse_id, location_id, batch_no, qty_on_hand, qty_frozen)
wms_batch (id, material_id, batch_no, production_date, expiry_date, supplier_id)
```

#### 财务域（erp_business.finance）—— 无变化
```
fin_receivable (id, order_id, customer_id, amount, due_date, paid_amount, status)
fin_payable (id, source_order_id, source_type, vendor_id, amount, due_date, paid_amount, status)
fin_payment (id, payment_no, vendor_id, amount, approvers_json, status)
fin_receipt (id, receipt_no, customer_id, amount, contract_id, status)
fin_cost (id, workorder_id, material_cost, labor_cost, mfg_cost, outsource_cost, total_cost)
fin_ar_aging (snapshot_date, customer_id, bucket_0_30, bucket_30_60, bucket_60_90, bucket_90_plus)
```

#### **委外域（V1.3.7 重大改版）** —— AD-1 / AD-2 / AD-3 全部落地
```
-- V1.3.7 厂商资料：电话 V1.3.6 必填 → V1.3.7 选填（短信下线）
outsub_vendor (id, vendor_code, vendor_name, capabilities_json, credit_level, notify_channel,
               contact_email NOT NULL,         -- V1.3.7 仍必填（163 邮箱推送）
               contact_phone,                  -- V1.3.7 选填（短信下线后非关键路径）
               contact_name, default_recon_email,
               status, created_at, updated_at)

-- V1.3.4 升级：委外订单 7 状态机
outsub_order (id, outsource_no, workorder_id, vendor_id, process_code, qty, unit_price, total_amount,
              status ENUM('PENDING_SHIP','SHIPPING','PENDING_INSPECTION','INSPECTING',
                          'QUALIFIED_STORAGE','STORED','REPAIR_REQUESTED','NOTIFIED_REPAIR'),
              rework_count INT DEFAULT 0,           -- V1.3.4 新增
              original_outsub_order_id,             -- V1.3.4 新增（返修关联原单）
              is_rework_reinspection BOOL DEFAULT false,  -- V1.3.4 新增
              delivery_date, actual_delivery_date, created_at, updated_at)
outsub_order_item (id, outsource_id, material_id, qty, unit_price)
outsub_order_history (id, outsource_id, from_status, to_status, operator_id, ts, remark)  -- V1.3.4 新增（状态机日志）

-- V1.3.7 工序分配职责分离：生管勾工序归属，采购选厂商 —— 数据层就分开
outsub_allocation (id, workorder_id, process_seq, decision ENUM('INHOUSE','OUTSOURCE'),
                   decided_by_user_id, decided_at)  -- V1.3.7 新增（生管 ID）
outsub_allocation_vendor (id, allocation_id, vendor_id, unit_price, delivery_date,
                          selected_by_user_id, selected_at, status)  -- V1.3.7 新增（采购 ID + WW- 单号）

-- V1.3.7 收窄为单一 163 邮箱：删除 sms_* 字段
outsub_vendor_notify_log (id, vendor_id, channel ENUM('email_163'),  -- V1.3.7 收窄
                          subject, body, attachment_path, status, sent_at, retry_count)

-- V1.3.6 改版：月度对账不含"线下"描述，签字扫描件入 MinIO
outsub_reconcile (id, vendor_id, period, total_amount, freight, payable, status,
                  recon_pdf_file_id,                -- V1.3.6 采购生成对账单 PDF
                  signed_scan_file_id,              -- V1.3.6 厂商签字扫描件（MinIO + AES-256-GCM）
                  recon_sent_at,                    -- V1.3.6 邮件发送时间
                  recon_confirmed_at,               -- V1.3.6 采购上传签字件时间
                  payment_request_id, status_updated_at)
outsub_reconcile_line (id, reconcile_id, outsource_order_id, qty, unit_price, amount)

-- V1.3.4 委外历史交期预估
outsub_delivery_history (id, vendor_id, process_code, delivery_days, completed_at)  -- V1.3.4 新增

-- V1.3.4 料号成本聚合（5 段）
cost_part_aggregate (id, material_code, period,
                     material_cost, labor_cost, surface_cost, outsource_cost, mfg_cost, total_cost,
                     version, computed_at)  -- V1.3.4 新增
```

#### 设备域（erp_production）—— 无变化
```
prod_machine (id, machine_code, machine_name, machine_type, machine_no, status, last_maintenance)
prod_machine_load (id, machine_id, load_date, planned_hours, available_hours)
prod_machine_maintenance (id, machine_id, maintenance_type, performed_at, next_due)
```

#### **邮件域（V1.3.7 新增 erp_platform.email）**
```
email_send_log (id, to_address, subject, body, attachment_hash, smtp_response,
                status, retry_count, sent_at, created_at)  -- V1.3.7 新增
email_quota_daily (date, sent_count, quota, warn_threshold)  -- V1.3.7 新增
```

### 4.3 关键索引设计（V1.3.7 新增 6 项）

| 表 | 索引名 | 字段 | 类型 | 场景 | 版本 |
|----|--------|------|------|------|------|
| `prod_workorder` | `idx_customer_status_date` | `(customer_id, status, delivery_date)` | 普通 | 工单看板过滤 | V1.0 |
| `prod_scan_report` | `idx_workorder_process_time` | `(workorder_id, process_no, scan_time DESC)` | 普通 | 扫码记录查询 | V1.0 |
| `mdm_material` | `uniq_material_code` | `(material_code)` | **唯一** | 物料编码唯一 | V1.0 |
| `wms_inventory` | `uniq_material_wh_batch` | `(material_id, warehouse_id, batch_no)` | **唯一** | 库存唯一性 | V1.0 |
| `sales_quote` | `idx_owner_status_date` | `(owner_id, status, created_at DESC)` | 普通 | "我的报价"列表 | V1.0 |
| `outsub_order` | `idx_status_vendor` | `(status, vendor_id)` | 普通 | 7 状态机看板 | **V1.3.4 新增** |
| `outsub_order` | `idx_rework_count` | `(rework_count)` | 普通 | 返修预警查询 | **V1.3.4 新增** |
| `outsub_allocation` | `idx_workorder_process` | `(workorder_id, process_seq)` | **唯一** | 工序分配唯一性 | **V1.3.7 新增** |
| `outsub_reconcile` | `idx_vendor_period` | `(vendor_id, period)` | **唯一** | 月度对账唯一性 | **V1.3.6 新增** |
| `sys_file` | `idx_bucket_object` | `(bucket, object_key)` | **唯一** | MinIO 对象寻址 | V1.0 |
| `email_send_log` | `idx_status_sent_at` | `(status, sent_at)` | 普通 | 邮件发送日志查询 | **V1.3.7 新增** |

---

## 5. 消息与异步架构

### 5.1 Redis Stream 主题设计（V1.3.7 升级）

| Stream Key | 消费者组 (Consumer Group) | 消费者实例 | 场景 | 消息示例 | 版本 |
|------------|---------------------------|-----------|------|----------|------|
| `stream:notify` | `notify-cg` | `app-push-consumer` / `workwechat-consumer` | 审批/逾期/异常/库存预警 | `{type: "APPROVAL", userId: 1, bizId: "BJ202606040001"}` | V1.0（V1.3.7 删 sms-consumer）|
| `stream:scan-sync` | `scan-cg` | `business-writer` | 扫码数据异步落库 | `{scanId, materialId, qty, type: "IN", operatorId}` | V1.0 |
| `stream:audit` | `audit-cg` | `audit-writer` | 审计日志批量落库 | `{userId, module, action, beforeJson, afterJson}` | V1.0 |
| `stream:outbox` | `outbox-cg` | `outbox-relay` | 业务→财务事件可靠投递 | `{eventId, bizType, bizId, payload}` | V1.0 |
| `stream:mrp-recalc` | `mrp-cg` | `production-mrp-runner` | MRP 重算广播 | `{trigger: "WORKORDER_CHANGE", workorderId}` | V1.0 |
| **`stream:cost-invalidate`** | `cost-cg` | `cost-aggregator` | **料号成本聚合失效驱动** | `{materialCode, reason: "OUTSUB_DONE"}` | **V1.3.4 新增** |
| **`stream:rework-alert`** | `rework-cg` | `rework-alert-consumer` | **返修次数 ≥ 阈值自动预警** | `{outsubOrderId, vendorId, reworkCount}` | **V1.3.4 新增** |
| **`stream:email-retry`** | `email-cg` | `email-retry-consumer` | **邮件重试 1h/6h/24h** | `{logId, retryRound, smtpResponse}` | **V1.3.7 新增** |

**Stream 关键配置**：
- 消息最大长度：`MAXLEN ~ 100000`
- 消费策略：`XREADGROUP > BLOCK 5000 COUNT 10`
- ACK 机制：消费成功后 `XACK`，失败重试 3 次后进 `stream:dead-letter`
- Pending 监控：消费者 lag > 1000 触发告警

### 5.2 XXL-JOB 任务清单（V1.3.7 新增 4 个，共 14 个）

| Job ID | Cron | 任务名 | 执行器 | 业务逻辑 | 版本 |
|--------|------|--------|--------|----------|------|
| `job-01` | `0 30 0 * * ?` | 数据库全量备份 | platform | `mysqldump` 全库，保留 30 天 | V1.0 |
| `job-02` | `0 */10 * * * ?` | 库存预警扫描 | business | 扫库存低于下限，生成预警 | V1.0 |
| `job-03` | `0 0 9 * * ?` | 回款到期提醒 | business | T-3/T-1/T+1/T+7 计划推送 | V1.0 |
| `job-04` | `0 0 8 * * ?` | 逾期订单推送 | business | 扫销售订单交期 < 当前日期 1/3/5 天 | V1.0 |
| `job-05` | `0 0 23 * * ?` | 成本核算批量计算 | business | 月底对已报工工单跑 `fin_cost` 归集 | V1.0 |
| `job-06` | `0 0 2 * * ?` | 日志清理 | platform | 清理 30 天前 Logback 本地日志 | V1.0 |
| `job-07` | `0 0 9 ? * MON` | 周报生成 | business | 生成上周生产/销售/品质周报 | V1.0 |
| `job-08` | 手动触发 | MRP 重算 | production | 接收参数 → MRP 重算 → 生成采购申请 | V1.0 |
| `job-09` | `0 */5 * * * ?` | 扫码待上传队列重试 | production | 重试失败/超时的 APP 扫码同步 | V1.0 |
| `job-10` | `0 0 1 * * ?` | Redis Stream Pending 重投 | platform | 扫所有 Stream Pending > 24h 重新投递 | V1.0 |
| **`job-11`** | `0 0 * * * ?` | **邮件重试（1h 轮）** | platform | 扫 `email_send_log.status=FAILED` 重发 | **V1.3.7 新增** |
| **`job-12`** | `0 0 */6 * * ?` | **邮件重试（6h 轮）** | platform | 同上（第二档重试） | **V1.3.7 新增** |
| **`job-13`** | `0 0 0 * * ?` | **邮件额度监控** | platform | 查 `email_quota_daily`，达 80% 告警 | **V1.3.7 新增** |
| **`job-14`** | `0 0 3 * * ?` | **邮件发送日志清理** | platform | 清理 90 天前 `email_send_log` | **V1.3.7 新增** |

### 5.3 幂等性保障

- **幂等键**：`{bizType}:{bizId}:{eventId}`
- **去重实现**：消费前 `SET key 1 NX EX 86400`
- **业务兜底**：核心表 unique key（如 `wms_inventory.uniq_material_wh_batch`、`outsub_reconcile.idx_vendor_period`）
- **重试策略**：3 次指数退避（1s / 5s / 30s）

---

## 5.4 Nacos 注册中心 / 配置中心（V1.3.7 重大升级）

### 5.4.1 Nacos 服务注册清单（V1.3.7 无变化）

| 服务名 | 端口 | 注册名 | 用途 |
|--------|------|--------|------|
| erp-gateway | 8080 | `erp-gateway` | 网关 |
| erp-platform | 8081 | `erp-platform` | 基础能力 |
| erp-business | 8082 | `erp-business` | 业务聚合 |
| erp-production | 8083 | `erp-production` | 车间执行 |
| xxl-job-executor | 9999 | `xxl-job-executor` | XXL-JOB 执行器 |

### 5.4.2 Nacos 配置管理（V1.3.7 新增大量配置）

**DataId 命名规范**：
```
nacos/
├─ erp-gateway.yaml
├─ erp-business.yaml
├─ erp-production.yaml
├─ erp-platform.yaml
└─ common-shared.yaml           # 跨服务共享
```

**热更新能力清单（V1.3.7 升级）**：

| 配置项 | 默认值 | 备注 | 版本 |
|--------|--------|------|------|
| `app.rate-limit.scan-qps` | 1000 | 扫码接口每秒上限 | V1.0 |
| `app.scan.offline-batch-size` | 500 | 离线扫码本地缓存条数 | V1.0 |
| `app.scan.conflict-strategy` | SERVER_WINS | 冲突解决策略 | V1.0 |
| `app.approval.quote-threshold-low` | 50000 | 报价 < 5万 业务员 | V1.0 |
| `app.approval.quote-threshold-high` | 200000 | 报价 > 20万 总经理 | V1.0 |
| `app.approval.purchase-threshold-high` | 50000 | 采购 > 5万 总经理 | V1.0 |
| `app.workflow.business-day-start` | 09:00 | 业务时间起点 | V1.0 |
| `feature.workorder.oee-enabled` | false | OEE 开关 | V1.0 |
| **`app.outsub.notify-channel`** | `['email_163']` | **V1.3.7 收窄为单 163 邮箱** | **V1.3.7 新增** |
| **`app.outsub.rework-alert-threshold`** | `2` | **返修次数 ≥ 此值自动预警** | **V1.3.4 新增** |
| **`app.outsub.vendor.require-phone`** | `false` | **V1.3.7 厂商电话改选填** | **V1.3.7 新增** |
| **`app.email.smtp.host`** | `smtp.163.com` | **163 SMTP 服务器** | **V1.3.7 新增** |
| **`app.email.smtp.port`** | `465` | **SSL 端口** | **V1.3.7 新增** |
| **`app.email.smtp.auth-code`** | `${EMAIL_163_AUTH_CODE}` | **授权码 KMS 注入（不写死）** | **V1.3.7 新增** |
| **`app.email.smtp.from-address`** | `noreply@yourcompany.com` | **发件人地址** | **V1.3.7 新增** |
| **`app.email.retry-policy`** | `1h,6h,24h` | **重试档位（XXL-JOB 调度）** | **V1.3.7 新增** |
| **`app.email.daily-quota`** | `5000` | **企业版日额度** | **V1.3.7 新增** |
| **`app.email.quota-warn-threshold`** | `0.8` | **达 80% 告警** | **V1.3.7 新增** |
| **`app.email.log-enabled`** | `true` | **发送日志开启** | **V1.3.7 新增** |
| **`app.email.log-retention-days`** | `90` | **发送日志保留 90 天** | **V1.3.7 新增** |
| **`app.email.attachment-max-size-mb`** | `10` | **附件最大 10MB（对账单 PDF / 签字扫描件）** | **V1.3.7 新增** |
| **`app.cost-cache-ttl`** | `3600` | **料号成本聚合缓存 1 小时** | **V1.3.4 新增** |
| **`app.cost.management-fee-allocation`** | `BY_LABOR_HOURS` | **管理费分摊维度（工时 OR 材料）** | **V1.3.4 新增** |
| **`app.security.field-encryption.algorithm`** | `AES-256-GCM` | **字段级加密算法（V1.3.6 升级 GCM）** | **V1.3.6 新增** |
| **`app.security.file-encryption.algorithm`** | `AES-256-GCM` | **MinIO 文件加密（同上）** | **V1.3.6 新增** |
| **`app.security.dek-storage`** | `/etc/erp/dek.key` | **DEK 文件路径（chmod 600）** | **V1.3.6 新增** |

### 5.4.3 命名空间隔离

| 命名空间 ID | 用途 |
|------------|------|
| `dev` | 开发环境 |
| `staging` | 预发环境 |
| `prod` | 生产环境 |
| `common` | 公共配置 |

### 5.4.4 部署与高可用

- **单机起步**：Nacos 2.3 standalone + 内嵌 Derby（建议外接 MySQL）
- **V1.1 高可用**：3 节点 Nacos 集群

### 5.4.5 安全

- 认证：Nacos 2.3 默认开启账号密码
- 命名空间权限：每个命名空间独立授权
- 审计：所有配置变更记录到 `config_history` 表，保留 90 天

---

## 6. 部署架构（Docker Compose，本地化部署）

> **V1.3.7 关键变化**：删除"阿里云 ACR / 阿里云 SLS / 阿里云 OSS"等公有云依赖；改为"本地 Docker Registry + 本地文件存储 + 异地冷备硬盘"。

```
                              [Internet] (V1.0.0 可选 · V1.0 局域网单机)
                                  |
                                  v
                          [Nginx :80/:443]
                              |       |
                  /web/*     |       |    /api/*
                              |       |
                  [Web SPA]  |       |    [Nginx upstream]
                              |       |        |
                              |       |        v
                              |       |    [erp-gateway × 2 :8080]
                              |       |        |
                              |       |        +---> [erp-platform × 1 :8081]
                              |       |        +---> [erp-business × 2 :8082]
                              |       |        +---> [erp-production × 2 :8083]
                              |       |
                  [APK 下载页]|       |    [Nginx upstream]
                              v       v
                          [MySQL Master :3306]
                                  |
                          async replication
                                  |
                          [MySQL Slave :3306 (read_only)]

+----------------+    +-----------------+    +-----------------+
| Redis :6379    |    | MinIO :9000     |    | XXL-JOB :8088   |
| - Cache        |    | :9001 console   |    | Admin Console   |
| - Stream       |    | - drawings      |    +-----------------+
| - Lock         |    | - contracts     |
+----------------+    | - reports       |
                      | - **对账签字扫描件桶 (V1.3.6 · AES-256-GCM · 5 年)** |
                      +-----------------+

+----------------+    +-----------------+    +-----------------+
| Nacos :8848    |    | Prometheus      |    | SkyWalking      |
| :9848 gRPC     |--->| :9090           |    | OAP + UI        |
| - Registry     |    | Grafana :3000   |    +-----------------+
| - Config       |    +-----------------+
+----------------+

（XXL-JOB 容器在 5.2 节已列出 · 14 个任务）
```

**资源分配（8 核 32G 服务器）**：

| 容器 | 内存限制 | CPU 限制 | 数量 |
|------|---------|---------|------|
| MySQL Master | 4 GB | 2 核 | 1 |
| MySQL Slave | 2 GB | 1 核 | 1 |
| Redis | 2 GB | 1 核 | 1 |
| MinIO | 2 GB | 0.5 核 | 1 |
| XXL-JOB | 1 GB | 0.5 核 | 1 |
| **Nacos** | **1 GB** | **0.3 核** | **1** |
| Nginx | 512 MB | 0.5 核 | 1 |
| erp-gateway | 1 GB | 0.5 核 | 2 |
| erp-business | 2 GB | 1 核 | 2 |
| erp-production | 2 GB | 1 核 | 2 |
| erp-platform | 1 GB | 0.5 核 | 1 |
| Prometheus | 1 GB | 0.5 核 | 1 |
| Grafana | 512 MB | 0.3 核 | 1 |
| SkyWalking | 2 GB | 0.5 核 | 1 |
| **合计** | **~22 GB** | **~9.3 核** | **16+ 容器** |

---

## 7. API 规范

### 7.1 URL 命名（V1.3.7 新增 3 个端点类）

- **基础**：`/api/v1/{service}/{resource}`
- **示例**：
  - `POST /api/v1/business/quotes`（创建报价）
  - `GET  /api/v1/production/workorders/{id}`（查询工单）
  - `PUT  /api/v1/platform/users/{id}`（更新用户）
- **批量**：`POST /api/v1/business/quotes/batch`
- **导出**：`GET /api/v1/business/quotes/export?format=xlsx&ids=1,2,3`
- **文件**：
  - `POST /api/v1/platform/files/upload`（multipart/form-data）
  - `GET  /api/v1/platform/files/{id}/download`（下载；**V1.3.6 签字件需 3 角色 + 审计**）
  - `GET  /api/v1/platform/files/{id}/preview`（在线预览，签名 URL 5 分钟过期）
- **特殊动作**：`POST /{resource}/{id}/{action}`
- **WebSocket**：`/api/v1/ws/dashboard`
- **V1.3.7 新增**：
  - `POST /api/v1/production/allocations`（**生管分配工序归属**）
  - `POST /api/v1/business/outsub-orders`（**采购选厂商创建 WW- 单**）
  - `POST /api/v1/production/outsub-orders/{id}/arrive`（**仓管扫 WW- 到货**）
  - `POST /api/v1/business/reconciles`（**采购生成月度对账单 PDF**）
  - `POST /api/v1/business/reconciles/{id}/send-email`（**163 邮箱推送对账单**）
  - `POST /api/v1/business/reconciles/{id}/upload-signed-scan`（**采购上传签字扫描件**）
  - `POST /api/v1/business/reconciles/{id}/confirm`（**对账已确认 → 触发付款申请**）
  - `GET  /api/v1/business/cost-aggregator/{materialCode}`（**料号 5 段成本**）

### 7.2 响应格式（不变）

成功：`{ "code": 0, "message": "ok", "data": {...}, "traceId": "..." }`
失败：`{ "code": 40001, "message": "...", "data": null, "traceId": "..." }`
分页：`{ "code": 0, "data": { "records": [...], "total": 100, "pageNum": 1, "pageSize": 20, "pages": 5 } }`

### 7.3 错误码体系（V1.3.7 删 90002 短信网关错误）

| 范围 | 类别 | 说明 |
|------|------|------|
| `0` | 成功 | - |
| `1xxxx` | 业务正常返回码 | |
| `40000-40099` | 参数错误 | |
| `40100-40199` | 认证错误 | |
| `40300-40399` | 授权错误 | `40304` **V1.3.7 新增 · 工序分配职责越权** |
| `40400-40499` | 资源不存在 | |
| `40900-40999` | 业务冲突 | `40904` **V1.3.4 新增 · 状态机不匹配**；`40905` **V1.3.6 新增 · 对账金额不一致** |
| `42900-42999` | 限流 | |
| `50000-50099` | 服务端错误 | |
| `50200-50299` | 下游错误 | `50203` **V1.3.7 新增 · 163 SMTP 调用失败**；`50204` **V1.3.7 新增 · 邮件额度耗尽** |
| `90000-99999` | 第三方错误 | `90001` 企业微信调用失败（V1.3.7 删 `90002` 短信网关）|

### 7.4 关键 API 端点（V1.3.7 新增 12 个，共 67+）

> 完整 OpenAPI 3.0 YAML 在 `backend/spec/openapi.yaml`（V1.3.7 升级后约 220+ 端点）

**V1.3.7 新增 API 端点（12 个）**：

| # | Method | Path | 说明 | 所属 Epic |
|---|--------|------|------|-----------|
| 56 | POST | `/api/v1/production/allocations` | **生管分配工序归属** | E5-S4 V1.3.7 |
| 57 | GET  | `/api/v1/production/allocations/pending` | **采购取待委外清单** | E5-S4 V1.3.7 |
| 58 | POST | `/api/v1/business/outsub-orders` | **采购选厂商创建 WW- 单** | E5-S4 V1.3.7 |
| 59 | POST | `/api/v1/production/outsub-orders/{id}/arrive` | **仓管扫 WW- 到货** | E12-S2 V1.3.5 |
| 60 | POST | `/api/v1/production/outsub-orders/{id}/state-transition` | **状态机转换** | E6 V1.3.4 |
| 61 | POST | `/api/v1/business/reconciles` | **采购生成对账单 PDF** | E6-S1 V1.3.6 |
| 62 | POST | `/api/v1/business/reconciles/{id}/send-email` | **163 邮箱推送对账单** | E6-S1 V1.3.6 |
| 63 | POST | `/api/v1/business/reconciles/{id}/upload-signed-scan` | **采购上传签字扫描件** | E6-S1 V1.3.6 |
| 64 | POST | `/api/v1/business/reconciles/{id}/confirm` | **对账已确认 → 触发付款** | E9-S3 V1.3.6 |
| 65 | GET  | `/api/v1/business/cost-aggregator/{materialCode}` | **料号 5 段成本** | E9-S5 V1.3.4 |
| 66 | GET  | `/api/v1/business/cost-aggregator/{materialCode}/export` | **导出 Excel/PDF** | E11-S5 V1.3.4 |
| 67 | GET  | `/api/v1/production/outsub-eta/{vendorId}/{processCode}` | **历史交期预估** | E6-S7 V1.3.4 |

---

## 8. 安全架构（V1.3.6 / V1.3.7 大幅升级）

| 维度 | 方案 | 实施细节 | 版本 |
|------|------|----------|------|
| **认证** | JWT (HS256) | access 2h + refresh 7d | V1.0 |
| **密码** | BCrypt(cost=12) | 禁用明文日志 | V1.0 |
| **Token 黑名单** | Redis | 用户禁用 / 改密 / 登出后写入黑名单 | V1.0 |
| **授权 (RBAC)** | 角色 + 菜单/数据/操作/金额四级权限 | `@PreAuthorize` + `@DataScope` | V1.0 |
| **金额阈值** | 角色配置 + 工作流条件 | 业务提交时路由 | V1.0 |
| **字段级加密** | **AES-256-GCM** | **身份证/手机号/银行卡；DEK 由甲方 IT 独立保管（chmod 600 文件）** | **V1.3.6 升级** |
| **文件级加密** | **AES-256-GCM** | **签字扫描件入 MinIO 时自动加密；下载时按 3 角色校验 + 审计** | **V1.3.6 新增** |
| **DEK 密钥管理** | **甲方 IT 单独保管** | **乙方不持有；配置文件分文件注入** | **V1.3.6 新增** |
| **传输加密** | HTTPS (TLS 1.3) | Nginx 终止 TLS | V1.0 |
| **SQL 注入** | MyBatis 预编译 | Code Review + SonarQube 禁止 `${}` | V1.0 |
| **XSS** | Vue 3 默认转义 + 后端 XSS Filter | 富文本场景白名单 | V1.0 |
| **CSRF** | SameSite=Strict + JWT in Header | APP/纯 API 不依赖 Cookie | V1.0 |
| **审计日志** | `@AuditLog` 注解 + AOP | 写操作前/后值；**V1.3.6 签字件下载 100% 留痕** | V1.0 + V1.3.6 |
| **防爆破** | 登录失败 5 次锁 30 分钟 | Redis 计数器 + IP/账号双维度 | V1.0 |
| **接口签名** | HMAC-SHA256 | 第三方扫码枪 | V1.0 |
| **网络隔离** | **VLAN 生产/办公/访客 + 堡垒机 + VPN + 双因子** | **V1.3.7 数据安全白皮书 8 道防线 #7** | **V1.3.7 新增** |
| **应急响应** | **数据泄露 1h 启动 + 4h 派员到场** | **V1.3.7 白皮书 8 道防线 #8** | **V1.3.7 新增** |

### 8.1 字段级加密实现（V1.3.6 升级版）

```java
// 注解驱动：@EncryptedField("phone")
@Mapper
public interface UserMapper {
    @Select("SELECT id, #{phone} AS phone_enc FROM sys_user")
    @EncryptedField("phone")
    User findById(@Param("id") Long id);
}

// MyBatis TypeHandler：AES-256-GCM 透明加解密
public class AesGcmTypeHandler extends BaseTypeHandler<String> {
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType type) {
        byte[] iv = SecureRandom.getSeed(12);
        byte[] cipher = AesGcmUtil.encrypt(DEK, iv, param.getBytes(UTF_8));
        ps.setBytes(i, iv + cipher);  // 存 12字节 IV + 密文
    }
    public String getNullableResult(ResultSet rs, String col) {
        byte[] blob = rs.getBytes(col);
        byte[] iv = Arrays.copyOfRange(blob, 0, 12);
        byte[] cipher = Arrays.copyOfRange(blob, 12, blob.length);
        return new String(AesGcmUtil.decrypt(DEK, iv, cipher), UTF_8);
    }
}
```

### 8.2 MinIO 签字件加密（V1.3.6 新增）

```java
// 写入：自动加密
public String putSignedScanFile(String fileId, byte[] content) {
    byte[] iv = SecureRandom.getSeed(12);
    byte[] cipher = AesGcmUtil.encrypt(DEK, iv, content);
    String objectKey = "signed-scan/" + fileId + ".enc";
    minioClient.putObject(bucket, objectKey, new ByteArrayInputStream(iv + cipher), ...);
    // 存 sys_file.encryption_meta = "AES-256-GCM, iv=<base64>"
    return objectKey;
}

// 读取：权限校验 + 审计 + 解密
public byte[] getSignedScanFile(String fileId, Long userId) {
    // 1. 权限校验：仅总经理 / 财务总监 / 采购员可下载
    if (!hasAuthority(userId, "signed-scan:download")) {
        throw new BizException(40301, "无权限下载对账签字件");
    }
    // 2. 审计
    auditLog("signed-scan:download", fileId, userId);
    // 3. 读取 + 解密
    byte[] blob = minioClient.getObject(bucket, objectKey);
    return AesGcmUtil.decrypt(DEK, Arrays.copyOfRange(blob,0,12), Arrays.copyOfRange(blob,12,blob.length));
}
```

### 8.3 邮件安全（V1.3.7 新增）

```java
// 1. 发件人地址：白名单（仅限配置中预设）
// 2. 附件大小：≤ 10MB（app.email.attachment-max-size-mb）
// 3. 附件类型：白名单（PDF/JPG/PNG）
// 4. 收件人域名：可配置黑名单（防垃圾投诉）
// 5. 发送频率：1 封/分钟/收件人（同收件人 1 分钟内不重发）
```

---

## 9. 可观测性

### 9.1 日志（不变）

- 框架：Logback + Logstash JSON Encoder
- 存储：本地 7 天 + 远端保留 30 天 + 长期归档

### 9.2 监控指标（V1.3.7 新增邮件相关）

| 类别 | 指标 | 采集方式 | 版本 |
|------|------|----------|------|
| JVM | 堆内存/老年代/线程数/GC | Micrometer + Prometheus JMX | V1.0 |
| 应用 | TPS/P95/P99/错误率/慢 SQL | Micrometer `@Timed` | V1.0 |
| HTTP | 请求量/状态码/响应时间 | Actuator | V1.0 |
| 业务 | 在线用户/扫码 TPS/订单 TPS/审批待办 | MetricsService | V1.0 |
| MySQL | 慢查询/主从延迟/QPS | mysqld_exporter | V1.0 |
| Redis | 内存/命中率/Stream lag | redis_exporter | V1.0 |
| MinIO | 存储用量/请求量/错误率 | MinIO Prometheus | V1.0 |
| XXL-JOB | 任务执行次数/失败次数 | XXL-JOB Admin | V1.0 |
| **邮件** | **今日已发/额度/重试次数/失败率** | **EmailMetrics** | **V1.3.7 新增** |
| **委外** | **7 状态机各状态单量/返修次数分布/平均交期** | **OutsubMetrics** | **V1.3.4 新增** |
| **料号成本** | **聚合耗时/缓存命中率/失效驱动次数** | **CostMetrics** | **V1.3.4 新增** |

### 9.3 告警（V1.3.7 新增 3 项）

| 等级 | 响应时间 | 通知方式 | 触发条件 | 版本 |
|------|----------|----------|----------|------|
| P0 | 5 分钟 | 短信 + 电话（oncall） | 服务不可用 > 1 分钟；JVM OOM；DB 主从断开 | V1.0 |
| P1 | 15 分钟 | 企业微信群 + 钉钉 | 错误率 > 1%；P95 > 5 秒；Redis 内存 > 80% | V1.0 |
| P2 | 1 小时 | 邮件 | 慢查询 > 10 条/分钟；磁盘 > 80% | V1.0 |
| **P1** | **15 分钟** | **企业微信群** | **邮件日发送量达 80% 额度（job-13）** | **V1.3.7 新增** |
| **P1** | **15 分钟** | **企业微信群** | **委外返修次数 ≥ 2（stream:rework-alert）** | **V1.3.4 新增** |
| **P1** | **15 分钟** | **企业微信群** | **Stream cost-invalidate lag > 1000（料号成本失效堆积）** | **V1.3.4 新增** |

### 9.4 链路追踪（不变）

- 框架：SkyWalking 9

---

## 10. 性能与容量（V1.3.4 新增料号成本聚合性能目标）

| 指标 | 目标 | 备注 | 达成方案 | 版本 |
|------|------|------|----------|------|
| 普通查询 P95 | ≤ 2s | 索引覆盖 | 索引 + 缓存 + 读写分离 | V1.0 |
| 复杂报表 P95 | ≤ 10s | 异步 + 缓存 | XXL-JOB 预计算 + Redis 缓存 | V1.0 |
| 扫码响应 P95 | ≤ 1s | 简化校验 + Redis | 扫码写先入 Redis Stream | V1.0 |
| 列表分页 P95 | ≤ 1s | 限制 pageSize ≤ 100 | 强制 + UI 校验 | V1.0 |
| 导出 P95 | ≤ 30s | 异步 + 通知 | XXL-JOB 异步 | V1.0 |
| 并发用户 | Web 100 / APP 50 | 单台 8 核 32G | Nginx upstream + 服务 2 实例 | V1.0 |
| 并发扫码 TPS | ≥ 50 | 峰值场景 | Redis Stream 削峰 | V1.0 |
| **料号成本聚合** | **P95 ≤ 3s** | **5 段成本聚合** | **Redis TTL 缓存 1h + Stream 失效驱动** | **V1.3.4 新增** |
| **邮件发送** | **≤ 5s/封** | **SMTP 同步发送** | **异步队列 + 163 SMTP** | **V1.3.7 新增** |
| **对账 PDF 生成** | **≤ 10s/份** | **PDFKit/iText** | **异步 XXL-JOB** | **V1.3.6 新增** |
| 数据存储 | **本地永久存储** | **V1.3.7 客户原话** | **本地服务器（甲方自有）+ 异地冷备** | **V1.3.7 升级** |
| 可用性 | 99.5% | 月停机 ≤ 3.6 小时 | 2 实例 + 主从 + 异地备份 | V1.0 |
| 文件存储 | ≥ 2TB | 图纸为主 | MinIO 单点起步 | V1.0 |

---

## 11. 风险与缓解（V1.3.7 新增 3 项，共 13 项）

| # | 风险 | 等级 | 影响 | 缓解方案 | 版本 |
|---|------|------|------|----------|------|
| 1 | Redis Stream 消费者 lag | 🟡 中 | 通知延迟 | 监控 lag > 1000；多消费者并行 | V1.0 |
| 2 | XXL-JOB 单点 | 🟡 中 | 定时任务全停 | 集群模式 | V1.0 |
| 3 | MinIO 单点 | 🟡 中 | 文件服务不可用 | 分布式 4 节点（V1.1）| V1.0 |
| 4 | MySQL 主从延迟 | 🟡 中 | 写后读不一致 | 强制读主 | V1.0 |
| 5 | 扫码高峰期 DB 写压力 | 🔴 高 | 报工慢 | 扫码先写 Redis Stream | V1.0 |
| 6 | 大文件图纸上传慢 | 🟡 中 | 上传超时 | 分片上传 | V1.0 |
| 7 | 业务量增长后单库压力大 | 🟢 低 | 性能下降 | Sharding-JDBC（V1.1+）| V1.0 |
| 8 | 第三方扫码枪固件不兼容 | 🟡 中 | 部分设备无法对接 | 2 种协议 | V1.0 |
| 9 | Redis 内存增长失控 | 🟡 中 | OOM | maxmemory 8GB + LRU | V1.0 |
| 10 | 金额计算浮点精度 | 🔴 高 | 财务对账错位 | BigDecimal + Money | V1.0 |
| **11** | **163 邮箱额度耗尽** | 🔴 高 | **对账推送失败** | **企业版 5000 封/日 + 80% 告警 + 配额监控** | **V1.3.7 新增** |
| **12** | **签字扫描件丢/泄露** | 🔴 高 | **对账合规凭证缺失 / 数据泄露** | **MinIO AES-256-GCM + 5 年保留 + 3 角色下载限 + 全审计** | **V1.3.6 新增** |
| **13** | **工序分配数据不一致** | 🟡 中 | **生管/采购踢皮球** | **`outsub_allocation` 唯一索引 `(workorder_id, process_seq)` + 状态机守卫** | **V1.3.7 新增** |

---

## 12. 交付物清单（V1.3.7 升级）

| # | 交付物 | 位置 | 责任人 | 版本 |
|---|--------|------|--------|------|
| 1 | OpenAPI 3.0 规范（YAML）| `backend/spec/openapi.yaml` | 后端 + Architect | V1.0 → **V1.3.7 升级**（67+ → 220+ 端点）|
| 2 | ER 图（drawio）| `docs/architecture/er.drawio` | Architect + 后端 | V1.0 → **V1.3.7 升级**（+13 张新表）|
| 3 | 架构图（drawio / PPT）| `docs/architecture/architecture.drawio` | Architect | V1.0 → **V1.3.7 升级** |
| 4 | Docker Compose 配置 | `backend/deploy/docker-compose.yml` | 运维 + 后端 | V1.0（**V1.3.7 删阿里云 ACR**）|
| 5 | XXL-JOB 任务脚本 | `backend/xxl-job/jobs/` | 后端 | V1.0 → **V1.3.7 升级**（10 → 14 个）|
| 6 | 数据库初始化 SQL | `backend/db/init.sql` | 后端 DBA | V1.0 → **V1.3.7 升级**（+13 张新表 + V1.3.6 字段加密元数据）|
| 7 | Prometheus 告警规则 | `backend/deploy/prometheus/alerts.yml` | 运维 | V1.0 → **V1.3.7 升级**（+3 告警）|
| 8 | 部署文档（README）| `backend/deploy/README.md` | 运维 | V1.0 |
| 9 | 性能测试脚本（JMeter）| `backend/test/jmeter/` | QA | V1.0 |
| 10 | 链路追踪拓扑截图 | `docs/architecture/skywalking.png` | 运维 | V1.0 |
| 11 | 服务依赖矩阵 | `docs/architecture/service-matrix.md` | Architect | V1.0 |
| 12 | 安全白皮书 | `docs/architecture/security.md` | Architect | V1.0 → **V1.3.7 升级**（**对齐数据安全白皮书 4+8 防线**）|
| **13** | **升级清单** | `docs/handoff-upgrade-checklist-v1.3.7.md` | **Architect + PM** | **V1.3.7 新增** |
| **14** | **签字件加密实施手册** | `backend/docs/signed-scan-encryption.md` | **Architect + 后端** | **V1.3.6 新增** |
| **15** | **DEK 密钥管理 SOP** | `backend/docs/dek-management.md` | **Architect + 甲方 IT** | **V1.3.6 新增** |
| **16** | **163 邮箱实施手册** | `backend/docs/email-163-setup.md` | **Architect + 后端** | **V1.3.7 新增** |
| **17** | **返修闭环 + 委外 7 状态机测试用例** | `backend/test/state-machine/` | **QA** | **V1.3.4 新增** |
| **18** | **料号成本聚合测试用例** | `backend/test/cost-aggregator/` | **QA** | **V1.3.4 新增** |

---

## 13. 验收清单（DoD，V1.3.7 升级）

- [ ] **1. 服务启动**：4 个服务（gateway × 2 / business × 2 / production × 2 / platform × 1）+ core module 全部启动成功
- [ ] **2. API 契约**：OpenAPI 3.0 规范生成（**220+ 端点**），前后端通过 `springdoc-openapi` + `openapi-typescript-codegen` 自动生成客户端
- [ ] **3. 一键部署**：`docker compose up -d` 一键起 16+ 容器
- [ ] **4. 性能达标**：
  - Web 100 并发 P95 ≤ 2s
  - APP 50 并发扫码 P95 ≤ 1s
  - 报表 P95 ≤ 10s
  - **料号成本聚合 P95 ≤ 3s（V1.3.4 新增）**
  - **邮件发送 ≤ 5s/封（V1.3.7 新增）**
- [ ] **5. 可观测性**：4 个 dashboard（应用/MySQL/Redis/Stream）+ **V1.3.7 邮件 dashboard + V1.3.4 委外 7 状态机 dashboard**
- [ ] **6. 高可用演练**：MySQL 主从切换、Redis Stream 重启、任意服务 kill -9 自动恢复
- [ ] **7. 安全合规**：
  - SonarQube 无 Critical
  - SQL 注入 / XSS 渗透测试通过
  - **V1.3.6 字段级 AES-256-GCM 加密 + 签字件加密 + DEK 甲方持有 + 下载审计（V1.3.7 数据安全白皮书 8 道防线 100% 落地）**
- [ ] **8. 文档完备**：18 项交付物全部交付并 review 通过
- [ ] **9. E2E 关键路径**：Web 报价→订单→工单 流程 + APP 扫码三码流程全部通过
- [ ] **10. 备份恢复**：数据库全量备份 + 恢复演练（RPO ≤ 1h，RTO ≤ 4h）
- [ ] **11. V1.3.7 新增 DoD**：
  - **11.1 工序分配职责 API 隔离测试**：生管 API 不能选厂商；采购 API 不能改工序归属
  - **11.2 对账不含"线下"描述验证**：搜索代码无"采购带纸去厂商处"等线下动作
  - **11.3 单一 163 邮箱验证**：搜索代码无短信 SDK/API/表/字段引用
  - **11.4 邮件重试 1h/6h/24h 演练**：手动 fail 一封 → 验证 XXL-JOB 3 轮重发
  - **11.5 邮件额度 80% 告警演练**：模拟发 4000 封 → 验证告警
  - **11.6 返修 ≥ 2 自动预警演练**：造数据 → 验证 stream:rework-alert 触发
  - **11.7 签字扫描件下载审计**：下载一封 → 验证 sys_download_log 留痕
  - **11.8 签字扫描件 3 角色下载限**：业务员下载 → 40301 拒绝

---

## 14. 给后端 / 前端 / APP 团队的 Handoff 提示（V1.3.7 升级版）

### 14.1 后端工程师

1. **代码生成**：MyBatis Generator 按 schema 生成
2. **统一异常**：`@RestControllerAdvice` 统一处理
3. **统一日志**：`@ApiLog` 注解
4. **统一审计**：`@AuditLog` 注解
5. **统一鉴权**：`@PreAuthorize` 或 `@DataScope`
6. **Stream 消费**：使用 `RedisStreamTemplate`，**必须**实现幂等键
7. **分布式锁**：`DistributedLock`
8. **金额**：**永远用 `BigDecimal`，禁止 `double` / `float`**
9. **V1.3.7 新增**：
   - 工序分配职责 API 严格分离：`OutsubAllocationService`（生管）和 `OutsubOrderService`（采购）**不互相依赖**
   - 163 邮箱调用统一走 `Email163Client`，**不要在业务代码里直接 new JavaMailSender**
   - 字段级加密统一走 `@EncryptedField` + `AesGcmTypeHandler`，**不要在 Service 里手动 encrypt/decrypt**
   - 委外状态转换统一走 `StateMachine.transition()`，**不要 if-else 判断状态**

### 14.2 前端工程师

1-7 不变
8. **V1.3.7 新增**：
   - 对账模块：采购生成对账单 → 发邮件 → 上传签字扫描件 三步走，**不要写"采购带纸去厂商处"等线下流程**
   - 邮件配置后台：163 邮箱授权码字段（input type=password）+ 测试连接按钮
   - 委外面板：7 状态机高亮（即将逾期/返修≥2/待检/已完成）
   - 料号成本检索：5 Tab 切换（价格/材料/工时/外协/总成本）

### 14.3 Android 工程师

1-8 不变
9. **V1.3.7 新增**：
   - APP "到货扫码"入口：扫 WW- 委外单码（**V1.3.5 新增**）
   - APP 消息中心：返修次数 ≥ 2 时强提醒（**V1.3.4**）
   - APP 离线缓存：物料码/工单码/委外单码 三类统一缓存

---

## 15. 灵魂一致性自评（V1.3.7 升级版）

**Q：V1.3.7 升级后，1 台 8 核 32G 服务器还能扛住吗？**

> **A：能扛住**。V1.0 测算 9 核 21GB 余量 30%，V1.3.7 新增 4 个 XXL-JOB 任务（CPU < 0.1 核/任务）+ 1 个 email_send_log 表（每天 200-500 条）+ 1 个 cost_part_aggregate 表（每月 10000 级别聚合）—— 增量资源 < 5%，仍在余量内。**关键依赖项**：① 客户 M1 启动会前开通 163 企业版邮箱（5000 封/日）；② 甲方 IT 独立保管 DEK；③ 异地冷备硬盘由甲方第二地点保管。

---

**文档版本**：V1.1（基于 V1.0 升级，对齐 PRD V1.3.7）
**生成时间**：2026-06-09
**生成人**：Architect agent（orchestrix · 鲁班）
**升级依据**：`docs/handoff-upgrade-checklist-v1.3.7.md`
**下一步**：M1 启动会（2026-06-12）→ 后端 / 前端 / Android 三团队按 V1.3.7 handoff 启动 Sprint 0（环境搭建 + OpenAPI 契约 + 数据库 init.sql）

---

## V1.3.9 增量（基于 PRD V1.3.9 Sprint 12 客户反馈）

> **架构师**：Architect agent（orchestrix · 鲁班）
> **对接输入**：`docs/prd.md` V1.3.9（PM 范蠡 · 2026-06-14）+ `docs/prd-feedback-v1.3.9.md`（4 条客户反馈）
> **增量范围**：Sprint 12 4 Story（12.1 / 12.2 / 12.3 / 12.4）+ 灰度时序约束
> **基线**：本文件 V1.1（PRD V1.3.7 升级版），**V1.3.8 段未在本仓落地**（PM 巡检 #4 建议补齐 V1.3.9 增量后回头补 V1.3.8 段）
> **客户**：昆山佰泰胜精密加工（黄梓昀 151-0595-0281 / 潘强 158-3710-7264）
> **预计启动**：2026-06-15（Sprint 12 启动）→ 2026-06-30+（客户灰度）

---

### 1. 4 条客户反馈（Sprint 12 全部采纳）

PM 范蠡 2026-06-14 早会接收的客户（昆山佰泰胜）4 条反馈，全部 P0/P1 采纳，列入 V1.3.9 Sprint 12：

| # | 客户反馈 | 优先级 | 复杂度 | Story | 端点数 | 关键架构决策 |
|---|---------|--------|--------|-------|--------|-------------|
| 1 | 图纸查看与打印权限（7 角色 × 5 操作矩阵） | 🔴 P0 | M | **12.1** | 3 | backend `DrawAclService` + SpEL `@PreAuthorize` 扩展 + 两套 ACL（图纸 vs 金额）互不干扰 |
| 2 | 打印机管理（普通 + 工业标签） | 🟡 P1 | S | **12.2** | 6 | `sys_printer` 字典表 + `@Scheduled` 60s TCP 心跳 + 浏览器→后端 Socket 9100 代理（**CORS 不可能直连**）|
| 3 | 标签模板（4 种 GD-/LZ-/SB-/WW-） | 🔴 P0 | M | **12.3** | 2 | 50mm×30mm 三区布局 + SB- 复用 GD- 模板（仅色条色不同） + QR 内容按前缀路由（与 V1.3.8 APP 扫码壳 5 类码一致）|
| 4 | 双模式打印（ZPL 直连 + A4 PDF 浏览器） | 🔴 P0 | L | **12.4** | 7 | 模式一 `ZplSocketClient`（后端代理 9100）+ 模式二 OpenPDF A4 3×10 = 30 标签/页 + 新增 `sys_print_log` 共表（与 12.1 打印留痕共用）|

**汇总**：4 Story · 🔴 P0 × 3 + 🟡 P1 × 1 · 工时 14-20 天 · 端点 18 · 测例 86 · Flyway 迁移 V54-V57。

**parallel_group**：
- **A**：12.1 + 12.4（**共用 `sys_print_log` 表** · 顺序启动）
- **B**：12.2（独立 · 但前置 12.3/12.4 的打印机选择）
- **C**：12.3（独立 · 但前置 12.4 的模板渲染）

**启动顺序建议**（PM 范蠡 2026-06-14）：**12.2 (B) → 12.3 (C) → 12.1 + 12.4 (A 并行)**。

---

### 2. 18 端点契约概览（12.1 + 12.2 + 12.3 + 12.4）

> 完整 OpenAPI 3.0 YAML 在 `backend/spec/openapi.yaml`（V1.3.9 升级后约 238+ 端点 · 较 V1.3.7 220+ 新增 18 个）

#### 2.1 Story 12.1 图纸权限矩阵（3 端点）

| # | Method | Path | 说明 | 鉴权 |
|---|--------|------|------|------|
| 68 | GET | `/api/v1/drawings/{id}/preview` | 图纸预览（带 ACL · `draw:preview` + 角色关联过滤） | `@PreAuthorize("hasAuthority('draw:preview') and #id.matchesOwnScope()")` |
| 69 | POST | `/api/v1/drawings/{id}/print-ticket` | 打印留痕（与 12.4 共 `sys_print_log` 表） | `draw:print` |
| 70 | GET | `/api/v1/drawings/permissions/matrix` | admin 查 7 角色 × 5 操作矩阵 | `admin` |

**权限定义（5 类）**：`draw:preview` / `draw:print` / `draw:download` / `draw:upload` / `draw:delete`

**角色映射（7 角色 × 5 操作 = 35 个权限单元）**：

| 角色 | preview | print | download | upload | delete | 关联过滤 |
|------|---------|-------|----------|--------|--------|----------|
| ENGINEER | ✅ | ✅ | ✅ | ✅ | ✅ | 无（全可见）|
| PROD_PLANNER | ✅ | ✅ | ❌ | ❌ | ❌ | 无 |
| SALES | ✅ | ✅ | ❌ | ❌ | ❌ | `order_id` 关联 |
| PURCHASER | ✅ | ✅ | ❌ | ❌ | ❌ | `po_id` 关联 |
| WAREHOUSE | ✅ | ✅ | ❌ | ❌ | ❌ | `inbound_id` 关联 |
| QC | ✅ | ✅ | ❌ | ❌ | ❌ | `inspection_id` 关联 |
| OPERATOR | ✅ | ❌ | ❌ | ❌ | ❌ | `process_id` 关联（APP 端当前工序）|
| FINANCE | ❌ | ❌ | ❌ | ❌ | ❌ | **无图纸权限**（与金额 4 阈值路由独立）|

**两套 ACL 边界**（避免新人混淆）：
- **图纸 ACL**（V1.3.9 新增）：按 `crm_drawing_link` 5 类关联 + 角色 5 类操作
- **金额 ACL**（V1.3.7 1.3 `sys_global_threshold`）：按金额 4 阈值路由审批人
- **互不干扰**：财务在金额 ACL 内可签批 ≥ 20 万订单 · 但在图纸 ACL 下零权限

#### 2.2 Story 12.2 打印机管理（6 端点）

| # | Method | Path | 说明 |
|---|--------|------|------|
| 71 | GET    | `/api/v1/printers` | 列表（含 status/heartbeat） |
| 72 | POST   | `/api/v1/printers` | 新增（admin only · 校验 protocol ∈ {ZPL, TSPL, PDF_BROWSER}）|
| 73 | PUT    | `/api/v1/printers/{id}` | 更新（admin only）|
| 74 | DELETE | `/api/v1/printers/{id}` | 删除（admin only · 仅 OFFLINE 可删）|
| 75 | POST   | `/api/v1/printers/{id}/test` | TCP Socket 探活（9100 端口）|
| 76 | GET    | `/api/v1/printers/available?type=LABEL` | 查询当前可用同类型（`status=ONLINE`）|

**协议支持**：NORMAL（PDF_BROWSER · 走浏览器打印）/ LABEL（ZPL · 斑马 ZD420 + TSPL · TSC TTP-244 Pro）/ **不支持 ESC/POS**（V1.3.10 backlog）。

#### 2.3 Story 12.3 标签模板（2 端点）

| # | Method | Path | 说明 |
|---|--------|------|------|
| 77 | GET  | `/api/v1/labels/templates` | 列 4 模板元数据（GD-/LZ-/SB-/WW- · 50mm×30mm）|
| 78 | POST | `/api/v1/labels/preview` | 生成 PNG/PDF 预览（QR 内容 + 明文 6 行 + 厂名）|

**4 模板规格统一 50mm × 30mm · 三区布局**（顶行色条+厂名 / 中央 QR / 下方明文 6 行）：

| 类型 | 前缀 | 色条色 | QR 示例 | 复用 |
|------|------|--------|---------|------|
| 工单码 | `GD-` | 蓝色 #1E40AF | `GD-260614-001` | 独立 |
| 流转码 | `LZ-` | 绿色 #16A34A | `LZ-260613-001-P03` | 独立 |
| 设备码 | `SB-` | 灰色 #6B7280 | `SB-260614-001` | **复用 GD- 模板**（仅色条色不同）|
| 委外单码 | `WW-` | 橙色 #EA580C | `WW-260614-001` | 独立 |
| 物料码 | `WL-` | （V1.3.8 既有 · 保持）| `WL-...` | — |

**厂名可配置**：复用 1.3 `sys_dict` 新增 `sys_company_name` 项 · 默认"昆山佰泰胜精密加工"。

#### 2.4 Story 12.4 双模式打印（7 端点）

| # | Method | Path | 说明 |
|---|--------|------|------|
| 79 | POST | `/api/v1/print/labels/zpl` | 模式一 ZPL 直连（后端 Socket 9100 代理）|
| 80 | POST | `/api/v1/print/labels/pdf-a4` | 模式二 A4 PDF 浏览器打印（3 列 × 10 行 = 30 标签/页）|
| 81 | GET  | `/api/v1/print/logs` | 打印历史（分页 · 多维过滤）|
| 82 | POST | `/api/v1/print/logs/{id}/replay` | 一键补打（SUCCESS 状态可重放 · FAILED 不可）|
| 83 | GET  | `/api/v1/print/statistics` | 按月/人/类型聚合 |
| 84 | POST | `/api/v1/print/labels/zpl/fallback-pdf` | 模式一失败 → 自动降级模式二（前端可选触发）|
| 85 | GET  | `/api/v1/print/templates/{codeType}/binding` | 查询某类码的当前激活模板（GD/LZ/SB/WW/WL）|

**架构决策**：
- **浏览器无法跨域调 Socket 9100** → **后端做 Socket 代理**（架构明确 · CORS + 浏览器安全模型双重限制）
- **模式一失败处理**：抛 `PRINT_CONNECTION_FAILED` (40950) · 前端弹"请检查打印机连接" · 提供 `fallback-pdf` 端点做自动降级
- **`ProtocolAdapter` 抽象**：ZPL 与 TSPL 指令集部分字段不兼容 · 抽象层做字段映射

---

### 3. 4 个 Flyway 迁移（V54-V57）

> 迁移文件位于 `backend/db/migrations/`（已存在 · V1.3.9 期间生成）

| 迁移 | 文件 | Story | 表 / 变更 | 关键索引 |
|------|------|-------|-----------|---------|
| **V54** | `V54__crm_drawing_link.sql` | 12.1 | 新增 `crm_drawing_link`（id / drawing_id / link_type[ORDER/PO/INBOUND/INSPECTION/PROCESS] / link_id / created_at）+ V1.3.7 `crm_drawing` 表 5 类关联字段 | `uniq_drawing_link (drawing_id, link_type, link_id)` |
| **V55** | `sys_printer.sql` | 12.2 | 新增 `sys_printer`（id / name / type[NORMAL/LABEL] / protocol[ZPL/TSPL/PDF_BROWSER] / ip / port[9100] / model_suggestion / status[ONLINE/OFFLINE] / last_heartbeat_at）| `idx_status_type (status, type)` |
| **V56** | `label_template.sql` | 12.3 | 新增 `label_template`（id / code_type[GD/LZ/SB/WW/WL] / width_mm / height_mm / bar_color / template_json / is_active / created_at）| `uniq_code_type_active (code_type, is_active)` |
| **V57** | `sys_print_log.sql` | 12.4 | 新增 `sys_print_log`（id / operator_user_id / printed_at / code_type / code_value / count / printer_id / printer_ip / printer_name / mode[ZPL_DIRECT/PDF_BROWSER] / status[SUCCESS/FAILED] / error_msg / replay_count）—— **与 12.1 打印留痕共用**| `idx_operator_printed_at (operator_user_id, printed_at DESC)` · `idx_code_type_status (code_type, status)` |

**额外迁移（V1.3.9 Sprint 13 阶段）**：
- **V58** `drawing_link_partial_index.sql`（已存在）：为 `crm_drawing_link` 加部分索引 `WHERE link_type = 'PROCESS'` · 优化 OPERATOR 角色扫码查询

**与 V1.3.7 既有表的差异**：
- `crm_drawing`（V1.3.7 1.7 ship）原本无关联字段 → V54 新增 5 类关联（order_id/po_id/inbound_id/inspection_id/process_id）
- 全部命名空间 `sys_`（sys_printer / sys_print_log）遵循 V1.3.7 8.3 `sys_workflow_event` 约定

---

### 4. 1 Story 灰度中（12.1 OPERATOR APP 端）

**Story 12.1 的 OPERATOR 角色（APP 端）当前处于"灰度观察"状态**（PM 巡检 #3 委派 · 6/14 EOD 由 dev agent 标记）。

#### 4.1 灰度原因

| 风险点 | 等级 | 原因 | 缓解 |
|--------|------|------|------|
| OPERATOR 当前工序查询占位（`userId → processId`）| 🟡 中 | V1.3.9 12.1 dev 占位逻辑 `username.hashCode() 取模` · **未对接真实生产数据** | 13.3 `findOperatorProcessIds` 真实查询对接（**关键依赖 · 见第 5 节灰度时序**）|
| Redis 7 缓存策略未验证 | 🟡 中 | OPERATOR 高频扫码 → 缓存击穿风险 | 13.3 同时上线 Redis 7 缓存（缓存 5 分钟 + 主动失效）|
| 5 类关联数据回填率待验 | 🟡 中 | V54 数据迁移后 `crm_drawing_link` 5 类关联回填率需 ≥ 95% | devops 部署后跑 `validate_link_backfill.sql` 验证脚本 |

#### 4.2 灰度范围（仅 OPERATOR 角色 APP 端）

- **不在范围**：admin / ENGINEER / PROD_PLANNER / SALES / PURCHASER / WAREHOUSE / QC（这些角色走 12.1 阶段 1-3 灰度，见第 5 节）
- **在范围**：仅 OPERATOR 角色 + 仅 APP 端 + 仅 `process_id` 关联过滤
- **观察指标**：
  - OPERATOR 扫码成功率（应保持 ≥ 99%）
  - APP 端"图纸 Tab"加载 P95（应 ≤ 2s · 与 12.1 web 端一致）
  - 误拦截投诉（应 = 0 · 占位逻辑可能误判）

#### 4.3 灰度解除条件

- ✅ 13.3 `findOperatorProcessIds` ship 且 `findSalesOrderIds` / `findPurchaseOrderIds` / `findInboundOrderIds` / `findInspectionOrderIds` 上线
- ✅ V54 数据回填率 ≥ 95%
- ✅ 灰度观察 1 周（预计 2026-07-04 → 2026-07-11）无业务投诉
- ✅ Redis 7 缓存命中率 ≥ 90%

---

### 5. 灰度时序（13.3 ship ≤ 12.1 阶段 2）

> **关键约束**（PM 范蠡 2026-06-14 巡检 #5 确认）：**13.3 必须早于或同步于 12.1 阶段 2-4 灰度开启** · 占位逻辑未替换会导致灰度阶段业务冲击。

#### 5.1 12.1 灰度 4 阶段

| 阶段 | 角色 | 启动日期 | 前置条件 | 13.3 ship 要求 |
|------|------|----------|----------|----------------|
| **阶段 1** | admin + ENGINEER | 2026-06-30+ | V1.3.8 FAT 通过 | **不强制**（admin/ENGINEER 不受占位逻辑影响）|
| **阶段 2** | SALES | 2026-07-01 | 阶段 1 稳定 1 天 | **必须 ship**（`findSalesOrderIds` 上线）|
| **阶段 3** | PURCHASER + WAREHOUSE + QC | 2026-07-02 | 阶段 2 稳定 1 天 | **必须 ship**（3 类真实查询上线）|
| **阶段 4** | OPERATOR（APP 端）| 2026-07-04 | 阶段 3 稳定 2 天 | **必须 ship**（`findOperatorProcessIds` 上线 + Redis 7 缓存验证）|

#### 5.2 13.3 ship 协调（不可逆约束）

| 阶段 | 13.3 ship 状态 | 影响 |
|------|----------------|------|
| 阶段 1 启动（6/30）| 可不 ship | admin/ENGINEER 全可见图纸 · 占位逻辑未触发 |
| 阶段 2 启动（7/1）| **必须 ship** | SALES 角色若 13.3 未 ship → 业务员看不到关联订单图纸 = 业务冲击 |
| 阶段 3 启动（7/2）| **必须 ship** | PUR/WH/QC 3 类关联未对接 → 3 类角色全部"看不到关联图纸" |
| 阶段 4 启动（7/4）| **必须 ship** | OPERATOR 占位逻辑上线 → 与真实生产数据不一致 → 误拦截 |

**协调结论**：
- **13.3 最晚 ship 日期 = 2026-07-01**（阶段 2 启动前 1 天）
- **dev agent 13.3 启动窗口 = 2026-06-22 → 2026-06-30**（8 天 · 工时 5-7 天 · 留 1 天 buffer）
- **rollback 预案**：若 13.3 ship 后发现生产查询性能不达标 → 12.1 阶段 2-4 灰度延后 · 但不撤回 13.3（避免占位逻辑长期存在）

#### 5.3 灰度时序甘特图（V1.3.9 Sprint 12 + Sprint 13 联动）

```
6/14  6/15  6/22  6/30  7/1   7/2   7/4   7/11  7/14
 │     │     │     │     │     │     │     │     │
 │  ├─PM 委派─Sprint 12 启动─┐
 │  │   (12.1/12.2/12.3/12.4 dev)
 │  │                         │
 │  │                    ├─12.1 阶段 1 (admin+ENGINEER)
 │  │                    │     ├─12.1 阶段 2 (SALES) ← 13.3 必须 ship
 │  │                    │     │   ├─12.1 阶段 3 (PUR/WH/QC)
 │  │                    │     │   │   ├─12.1 阶段 4 (OPERATOR) ← 13.3 ship + Redis 7
 │  │                    │     │   │   │     ├─灰度观察 1 周─┐
 │  │                    │     │   │   │     │               ├─客户验收 1 周─┐
 │  │  ├─13.3 dev 启动─┐ │     │   │   │     │               │               │
 │  │  │               ├─┼─────┼───┼───┼─────┼───────────────┼───────────────┤
 │  │  │               └─13.3 ship（最晚 6/30）─┘
```

#### 5.4 灰度时序变更影响（相对 V1.3.7 / V1.3.8）

| 维度 | V1.3.7 / V1.3.8 | V1.3.9 |
|------|-----------------|--------|
| 灰度对象 | 服务级（gateway / business / production）| 角色级（7 角色分 4 阶段）|
| 灰度粒度 | `X-Gray-Tag: v2` Header 路由 | 角色 → 7 类关联过滤 + 5 类操作 |
| 跨 Story 依赖 | 弱（多为 Epic 内）| **强**（13.3 ↔ 12.1 · 12.2 → 12.3 → 12.4 顺序）|
| 灰度入口 | erp-gateway 灰度发布 | erp-gateway 灰度 + `DrawAclService` SpEL |
| 关键风险 | 字段级 AES-256 加密漏配 | 占位逻辑未替换 / 5 类关联数据回填率不足 |

---

### 6. V1.3.9 增量验收清单（DoD · 4 项 · 增补至第 13 节）

- [ ] **V1.3.9.1 端点契约**：OpenAPI 3.0 YAML 升级至 238+ 端点（V1.3.7 220+ + V1.3.9 18 新增）· 前后端自动生成客户端通过
- [ ] **V1.3.9.2 Flyway 迁移**：V54-V57 + V58 全部在 dev/staging/prod 三环境执行成功 · V54 `crm_drawing_link` 数据回填率 ≥ 95%
- [ ] **V1.3.9.3 两套 ACL 边界**：图纸 ACL（12.1 7 角色 × 5 操作）与金额 ACL（V1.3.7 1.3 sys_global_threshold 4 阈值）测试互不干扰 · 财务在金额 ACL 内可签批 ≥ 20 万订单 · 但在图纸 ACL 下零权限
- [ ] **V1.3.9.4 灰度时序**：13.3 最晚 2026-07-01 ship · 12.1 4 阶段按 6/30 / 7/1 / 7/2 / 7/4 启动 · 阶段 4 OPERATOR 灰度观察 1 周无业务投诉

---

### 7. 给后端 / 前端 / APP 团队的 Handoff 提示（V1.3.9 增量版）

#### 7.1 后端工程师（增补至 14.1）

10. **V1.3.9 新增**：
    - **12.1 图纸 ACL**：用 `@PreAuthorize("hasAuthority('draw:preview') and #id.matchesOwnScope()")` SpEL · 自定义 `matchesOwnScope()` 走 `DrawAclService` 查 5 类关联 · **不要在 Service 层 if-else 判断角色**（易漏 · 测试覆盖难）
    - **12.2 心跳调度**：`@Scheduled(fixedRate = 60000)` + TCP Socket `new Socket(ip, port)` + 容差 2 次失败再标 OFFLINE · 客户端 try-with-resources 关连接
    - **12.3 模板渲染**：抽 `BaseLabelTemplate.render(BarColor)` 父类 · SB- 继承 GD- 仅覆盖色条色 · **不要 4 个独立类**
    - **12.4 Socket 代理**：`ZplSocketClient.sendZpl(ip, port, zpl)` 端口 9100 · `OutputStream.write(zpl.getBytes(UTF_8))` · 失败抛 `BizException(40950, "PRINT_CONNECTION_FAILED")` · 浏览器调后端 → 后端再调打印机 · **不要尝试让浏览器直连 Socket**
    - **12.4 模式降级**：`fallback-pdf` 端点接受同 ZPL 请求体 · 复用 `LabelRenderService.renderZpl` 输出 · 转 `renderPdfA4` · 返回 PDF 流

11. **13.3 真实查询**（Sprint 13）：
    - `findSalesOrderIds(userId)` · `findPurchaseOrderIds(userId)` · `findInboundOrderIds(userId)` · `findInspectionOrderIds(userId)` · `findOperatorProcessIds(userId)` 五个方法
    - 实现走 MyBatis `selectIn_` 模式 · Redis 7 缓存 5 分钟（key: `acl:biz_ids:{userId}:{type}`）
    - **最晚 ship 日期 2026-07-01**（12.1 阶段 2 启动前）

#### 7.2 前端工程师（增补至 14.2）

9. **V1.3.9 新增**：
    - **12.1 图纸查看器**：web-impl `<DrawingViewer>` 组件 + Element Plus `<el-image-preview>` 包权限拦截 · 命中 40301 时弹"您没有该图纸的查看权限"
    - **12.2 admin 打印机管理页**：复用 1.3 sys_dict admin 页风格 · 表格 + 新增/编辑/测试/删除按钮 · OFFLINE 标红
    - **12.3 标签预览组件**：web-impl `<LabelPreview>` Vue 组件 + Element Plus `<el-image>` 显示 PNG · 调用 `/api/v1/labels/preview` 后端渲染
    - **12.4 打印按钮**：`PrintButton` 组件（V1.3.8 10.1/10.5 已 ship · 集成）· 点击 → 12.2 逻辑（1 台直打 / 多台选 / 未配提示）· 模式一失败弹"请检查打印机连接" + 提供"切换 A4 打印"按钮

#### 7.3 Android 工程师（增补至 14.3）

10. **V1.3.9 新增**：
    - **12.1 APP 图纸权限拦截**：`DrawPermissionInterceptor`（共用拦截器）· 应用到 `MaterialBarcodeScan` + `NoOrderPurchase` 两个 Fragment · OPERATOR 角色 `process_id` 关联过滤 · 命中 40301 时 Toast"您没有该图纸的查看权限"
    - **12.2 打印机选择**：`PrintDialogFragment`（V1.3.8 已 ship 8 角色）· 调用 `/api/v1/printers/available?type=LABEL` 取列表 · 1 台直打 / 多台单选 / 未配 Toast
    - **12.3 标签预览**：`LabelPreviewFragment` + ZXing 渲染 QR · 调用 `/api/v1/labels/preview`
    - **12.4 打印模式**：`PrintDialogFragment` 新增"模式选择"区（ZPL 直连 / A4 浏览器）· 默认记住上次选择 · 模式一失败提示 + 切换 A4 按钮
    - **12.4 补打入口**：`PrintHistoryFragment`（新增）· 调用 `/api/v1/print/logs` · SUCCESS 状态显示"补打"按钮 · FAILED 状态显示"重试"按钮

---

### 8. 灵魂一致性自评（V1.3.9 增量版）

**Q：V1.3.9 在 V1.3.7 架构基础上增加了 4 Story · 1 台 8 核 32G 服务器还扛得住吗？**

> **A：扛得住**。V1.3.9 增量资源评估：
> - 4 个 Flyway 迁移（V54-V57 + V58 部分索引）：CPU 瞬时 < 0.2 核 · 内存 < 200MB（`crm_drawing_link` 回填期间）
> - 18 端点：均在 V1.3.7 网关层下 · 路由配置增量 < 1% · 不新增微服务
> - 4 XXL-JOB 任务沿用 V1.3.7 14 个调度器 · **不新增**（打印机心跳用 Spring `@Scheduled` 而非 XXL-JOB）
> - `sys_print_log` 预估每天 200-500 条（与 `email_send_log` 同量级）· 90 天保留约 30 万行 · 索引命中 OK
> - **关键依赖项**：① 客户机房打印机 IP 固定（不支持 DHCP 自动发现 · V1.4 backlog）② 13.3 Redis 7 缓存命中率 ≥ 90% ③ V54 数据回填率 ≥ 95%

**Q：V1.3.9 与 V1.3.7 架构灵魂一致性？**

| 维度 | V1.3.7 评分 | V1.3.9 增量后评分 | 说明 |
|------|------------|------------------|------|
| WHY 优先 | 9 | 9.2 | 4 客户反馈全部可追溯到 6/14 早会原话 · 灰度时序追溯到 PM 巡检 #5 委派 |
| 极致简化 | 9.5 | 9.5 | SB- 复用 GD- 模板 · 后端 Socket 代理避免浏览器跨域 · 两套 ACL 边界明确 |
| 可落地 | 8.5 | 8.3 | 14-20 天工时 + 18 端点 + 4 迁移 + 86 测例 · **13.3 跨 Sprint 依赖是关键风险** |
| 性能达标 | 8 | 8 | 扫码 P95 ≤ 1s · 报表 P95 ≤ 10s · 全部沿用 V1.3.7 路径 · 新增 `sys_print_log` 索引覆盖 |
| 风险预判 | 9 | 9.2 | **新增 4 项**：① 浏览器无法跨域 Socket ② V54 数据回填率 ③ 13.3 占位逻辑未替换 ④ 灰度时序不可逆 |
| 数据安全 | 9.5 | 9.5 | 沿用 V1.3.6/1.3.7 AES-256-GCM + DEK 甲方持有 · 12.1 打印留痕共 `sys_print_log` 全审计 |
| **综合** | **8.92** | **8.95** | V1.3.9 增量后架构仍可驱动后端/前端/APP 三端开发 · 风险点更明确 |

---

**文档版本**：V1.2（V1.1 基础上叠加 V1.3.9 增量）
**生成时间**：2026-06-14
**生成人**：Architect agent（orchestrix · 鲁班）
**升级依据**：`docs/prd-feedback-v1.3.9.md`（PM 范蠡 · 2026-06-14）+ PM 巡检 10 项建议 #4
**V1.3.8 段说明**：本仓 `docs/architect-handoff.md` 当前未含 V1.3.8 段（V1.0 → V1.1 直跳 V1.3.7）· V1.3.8 Sprint 7-10 19 Story 增量内容建议作为单独 V1.3.8 段补齐（PM 巡检 #4 子项 · 由 architect 鲁班后续委派）
**下一步**：Sprint 12 启动会（2026-06-15）→ 4 团队按 V1.3.9 handoff 启动 12.2 (B) → 12.3 (C) → 12.1 + 12.4 (A 并行) → 2026-06-30+ 客户灰度

