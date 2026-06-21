# erp-backend · 昆山佰泰胜专属 ERP V1.3.7 (Implementation Repo)

> **角色**: multi-repo `backend` 实现仓
> **产品仓**: `../smart-workshop-erp` (含 PRD V1.3.7 / 架构 V1.1 / OpenAPI 契约 / ER 图 / 合同)
> **架构依据**: [`docs/architecture.md`](docs/architecture.md) V1.3.7-backend-detail (94 KB)

## 1. 仓内结构 (Spring Boot × 3 Services + Gateway × 1 + core × 1)

```
backend/                                本仓
├── .orchestrix-core/core-config.yaml   multi-repo backend 配置
├── pom.xml                             顶层 Maven 父 POM (5 modules)
├── docs/architecture.md                详细后端架构 (94 KB)
├── db/
│   ├── init.sql                        产品仓契约: 70 表 DDL
│   └── migrations/                     Flyway 迁移 (来自 backend-impl)
├── spec/openapi.yaml                   产品仓契约: 74 端点 OpenAPI
├── src/
│   ├── core/                           仓内 core Module (12 common-* 子包)
│   ├── erp-platform/                   服务 1 · 8081 · 基础能力
│   ├── erp-business/                   服务 2 · 8082 · 业务聚合
│   ├── erp-production/                 服务 3 · 8083 · 车间执行
│   ├── erp-gateway/                    服务 4 · 8080 · Spring Cloud Gateway
│   ├── test/                           Testcontainers 共享 E2E
│   └── main/                           兼容遗留 ErpApplication.java
├── deploy/
│   ├── docker-compose.yml              16 容器一键起
│   └── prometheus/alerts.yml           V1.3.7 告警规则
├── scripts/
│   ├── dev-dek-init.sh                 生成 /etc/erp/dek.key (V1.3.6 fail-fast)
│   └── verify-all-v137-redlines.sh     5 条 V1.3.7 UI 红线 grep
└── utils/                              产品仓 PO 房玄龄切片脚本 (保留)
```

## 2. 快速开始

```bash
# 1. 启动依赖中间件 (16 容器)
docker compose -f deploy/docker-compose.yml up -d

# 2. 初始化数据库 (从产品仓 init.sql, 70 表 / 3 库)
mysql -h 127.0.0.1 -uroot -proot123 < db/init.sql

# 3. 准备 DEK 密钥 (V1.3.6 fail-fast)
sudo bash scripts/dev-dek-init.sh

# 4. 启动 backend (任选一个服务)
cd src/erp-platform && mvn spring-boot:run

# 5. 验证红线
bash scripts/verify-all-v137-redlines.sh

# 6. 健康检查
curl http://localhost:8080/api/v1/platform/health  # 经 gateway
curl http://localhost:8081/api/v1/platform/health  # 直连 platform
```

## 3. 4 大服务模块

| 端口 | Module        | 职责 |
|------|---------------|------|
| 8080 | erp-gateway   | Spring Cloud Gateway (JWT/限流/灰度) |
| 8081 | erp-platform  | 用户/角色/审批/字典/文件/邮件 (V1.3.7 新) |
| 8082 | erp-business  | CRM/销售/采购/仓储/品质/财务/人事/报表/料号成本/对账 |
| 8083 | erp-production| 工单/工序/扫码/报工/委外/设备 |

> 实际部署: gateway 独立进程; platform/business/production 可同进程或拆分。

## 4. 关联仓

- **产品仓**: `../smart-workshop-erp` (PRD V1.3.7 / 架构 V1.1 / OpenAPI / ER / 合同)
- **前端仓**: `../web-impl` (Vue 3 + Element Plus)
- **移动端仓**: `../android-impl` (Kotlin + Jetpack)

## 5. V1.3.7 关键升级

1. 工序分配职责严格分离 (生管 API vs 采购 API)
2. 对账不含"线下"动作 (V1.3.6/7)
3. 单一 163 邮箱通知 (删短信)
4. 签字扫描件 AES-256-GCM (DEK 独立保管)
5. 料号 5 段成本 (Redis Stream 失效驱动)
6. 委外 7 状态机 + 返修闭环 (V1.3.4)

详见 `docs/architecture.md` §1.1。
