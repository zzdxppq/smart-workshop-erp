# 5.4 Nacos 注册中心 / 配置中心（V1.3.7 重大升级）

## 5.4.1 Nacos 服务注册清单（V1.3.7 无变化）

| 服务名 | 端口 | 注册名 | 用途 |
|--------|------|--------|------|
| erp-gateway | 8080 | `erp-gateway` | 网关 |
| erp-platform | 8081 | `erp-platform` | 基础能力 |
| erp-business | 8082 | `erp-business` | 业务聚合 |
| erp-production | 8083 | `erp-production` | 车间执行 |
| xxl-job-executor | 9999 | `xxl-job-executor` | XXL-JOB 执行器 |

## 5.4.2 Nacos 配置管理（V1.3.7 新增大量配置）

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

## 5.4.3 命名空间隔离

| 命名空间 ID | 用途 |
|------------|------|
| `dev` | 开发环境 |
| `staging` | 预发环境 |
| `prod` | 生产环境 |
| `common` | 公共配置 |

## 5.4.4 部署与高可用

- **单机起步**：Nacos 2.3 standalone + 内嵌 Derby（建议外接 MySQL）
- **V1.1 高可用**：3 节点 Nacos 集群

## 5.4.5 安全

- 认证：Nacos 2.3 默认开启账号密码
- 命名空间权限：每个命名空间独立授权
- 审计：所有配置变更记录到 `config_history` 表，保留 90 天

---
