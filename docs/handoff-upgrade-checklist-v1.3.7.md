# 架构师 Handoff 升级清单（V1.3.1 → V1.3.7）

> **致**：鲁班（orchestrix Architect agent）
> **发自**：潘强（orchestrix PM agent 范蠡）
> **日期**：2026-06-09
> **目的**：当前 `docs/architect-handoff.md` 仍为 **V1.0（基于 PRD V1.3.1）**，落后 6 个版本（V1.3.2 → V1.3.7），本清单明确告诉你**要改哪些章节、改什么、怎么改**。请于 **2026-06-10** 前完成升级。

---

## 〇、为什么必须升级

PM 评审 8 维度 **一致性 8.5 / 10** —— 唯一扣分点就是 architect-handoff 与 PRD V1.3.7 不对齐。架构依据落后 = 团队按旧 handoff 写代码 = 6 个版本的增量全部漏掉（工序分配、7 状态机、料号成本、163 邮箱、签字件加密、邮件重试）。

---

## 一、6 项必须升级的架构决策

| # | 决策 | 升级位置（handoff 章节）| 升级要点 |
|---|------|--------------------------|----------|
| **AD-1** | **工序分配职责严格分离**（生管 vs 采购） | §4 业务能力地图 + §6 微服务边界 | E5-S4 改版；新增 `process-allocation` 限界上下文；生管 API（`/api/v1/production/processes/{id}/assign-insourcing`）与采购 API（`/api/v1/sourcing/outsub-orders`）**严格分离**；不可在生产服务里直接"代采购选厂商" |
| **AD-2** | **对账流程不含"线下"** | §3 业务架构图 + §5 核心流程 | E6-S1 / FR-12-3 改版；删除"采购带纸去厂商处 → 厂商签字 → 拍照上传"流程；**只保留**：① 采购点"发对账单邮件" → ② 系统邮件推送 → ③ 采购上传签字扫描件（PDF/JPG ≤10MB）→ ④ MinIO 加密存储 |
| **AD-3** | **通知厂商渠道收敛为单一 163 邮箱** | §2.4 消息中间件 + §7 配置中心（Nacos 配置）| **删除**：阿里云/腾讯云 SMS SDK 依赖；`/api/v1/notifications/sms` API；`sms_templates` 数据表；短信模板管理模块（FR-1-3 字典删除）。**新增**：`app.email` 配置块（`smtp.163.com:465 SSL` + 授权码 KMS 注入）|
| **AD-4** | **厂商资料电话改选填** | §7.2 数据模型 + §4 业务能力 | `data_model.vendor.contact_phone` 字段约束 V1.3.6 NOT NULL → V1.3.7 NULL；Nacos `app.outsub.vendor.require-phone = false` |
| **AD-5** | **邮件投递保障三重防线** | §2.4 消息中间件 + §7 配置中心 | **新增**：`app.email.retry-policy`（1h/6h/24h XXL-JOB 调度）、`daily-quota`（企业版 5000 封/日）、`quota-warn-threshold`（80% 告警）、`log-retention-days`（90 天）；新增 `email_send_log` 表 |
| **AD-6** | **数据安全白皮书 v1.0 已签** | §10 安全架构 + 新增 §13 数据安全细则 | 字段级 AES-256-GCM；DEK 由甲方 IT 独立保管（乙方不持有）；MinIO 存储签字扫描件（AES-256 + 5 年保留 + 下载限 3 角色 + 审计）；网络隔离（VLAN 生产/办公/访客 + 堡垒机 + VPN + 双因子）|

## 二、7 项必须新增/改写的服务/模块

| # | 模块 | 类型 | 关键改动 |
|---|------|------|---------|
| **M1** | `module: outsub-allocation` | 新增 | 工序分配职责的 API 入口与权限隔离；生管"工序划分"和采购"厂商选择"在数据层就分开 |
| **M2** | `module: outsub-billing` | 新增 | 月度对账：汇总 WW- 单 → 生成 PDF（含签字栏位）→ 163 邮箱推送 → 接收签字扫描件 → 入 MinIO（AES-256）|
| **M3** | `module: email-service` | 新增 | 163 SMTP 客户端 + 重试 1h/6h/24h + 额度监控 + 发送日志（90 天） |
| **M4** | `module: rework-tracking` | 新增 | 返修单关联原委外单 + rework_count 累加 + 阈值（默认 2）自动预警（APP/PC 红点/企业微信 webhook）|
| **M5** | `core: outsub-state-machine` | 改写 | 7 主状态 + NOTIFIED_REPAIR 衍生态；状态机配置 + 守卫；与 E12-S2 仓管到货扫码、E12-S3 品质领料联动 |
| **M6** | `core: cost-aggregator` | 新增 | 料号成本聚合（材料/工时/表处/外协/管理费 5 段）+ Redis Stream 失效消息驱动 + 5 Tab 输出 + 权限隔离 |
| **M7** | `core: outsub-history-eta` | 新增 | 委外历史交期预估（最近 3 次中位数 + 50%/80%/100% 分位数）|

## 三、6 项必须删除的旧实现

| # | 旧实现 | 删除原因 | 删除位置 |
|---|--------|----------|----------|
| **D1** | `region-code` 模块（V1.3.4 引入，V1.3.5 已删） | 客户二次反馈"取消区域码"——送货员角色下线，区域码失去服务对象 | §4 业务能力地图、§6 服务边界、数据模型 |
| **D2** | `vendor-delivery` 角色（送货员 APP） | V1.3.5 删；改由仓管到货扫码 | §6 用户/角色矩阵、§4 业务能力 |
| **D3** | `wechat-recon` 模块（V1.3.6 引入，V1.3.6 整章改版） | V1.3.6 客户原话"本地部署，厂商没有微信操作"；改 163 邮箱 | §4 业务能力地图、§6 服务边界 |
| **D4** | 阿里云 SMS / 腾讯云 SMS 依赖 | V1.3.7 删短信渠道 | §2.4 消息中间件、§3 外部依赖 |
| **D5** | 短信模板管理 API（`/api/v1/notifications/sms`） | V1.3.7 删 | §6.2 API 列表 |
| **D6** | 厂商微信对账功能 | V1.3.6 整章改版 | §5 核心流程 |

## 四、4 个**关键提醒**给鲁班

1. **架构师鲁班 6/10 前升级完成**——M1 启动会 6/12 之前必须让全员读到 V1.3.7 架构版，否则后端 / 前端 / Android 三团队按旧 handoff 开工即返工。
2. **core Module 优先打稳**——M1 第 1 周必须先有 `common-dto`（统一响应/错误码）、`common-util`（雪花 ID/AES/邮件客户端）、`common-web`（鉴权注解）、`common-job`（XXL-JOB 封装）；别等业务服务开工后再回头改 core。
3. **DEK 的 KMS 方案要落**——PM 评审指出的唯一缺口；推荐**甲方 IT 单独保管 + 配置文件分文件注入**（`/etc/erp/dek.key` chmod 600），不要把 DEK 写进环境变量或 Nacos。
4. **163 邮箱企业版（5000 封/日）需客户 M1 启动会前开通**——授权码 `${EMAIL_163_AUTH_CODE}` 由乙方 KMS 注入，不要把明文写进 application.yml。

## 五、升级后的 handoff 必须包含的 5 项产出

1. ✅ 架构图（更新到 V1.3.7，含工序分配职责 / 163 邮箱 / 签字件 / 7 状态机）
2. ✅ 微服务/Module 边界图（新增 outsub-allocation / outsub-billing / email-service / rework-tracking / cost-aggregator / outsub-history-eta）
3. ✅ OpenAPI 3.0 契约 yaml（13 Epic 全量接口，至少 200+ endpoint）
4. ✅ 数据库 ER 图（13 Epic 主表 + 关系，6 张核心表：工单/工序/委外单/对账单/签字件/邮件日志）
5. ✅ Nacos 配置清单（`app.outsub.*` / `app.email.*` / `app.security.*` / `app.cost-cache-ttl` 等）

## 六、不需要改的部分（保留 V1.0）

- §0 灵魂一致性声明
- §1 架构愿景（粗粒度微服务 + Modulith）
- §2 总体架构图（3 服务 + 1 网关 + 1 core）
- §5.1 服务注册发现
- §8 部署架构（Docker Compose / 8C32G / Nacos 单机起步）
- §9 监控 / 日志 / 链路追踪
- §11 性能目标（P95 ≤ 1s 扫码等）

---

## 七、验收方式

PM 将在升级后用以下清单验收：

- [ ] 6 项 AD（架构决策）全部反映在 handoff
- [ ] 7 项模块新增/改写全部反映
- [ ] 6 项旧实现全部从 handoff 删除
- [ ] 5 项产出全部完成（架构图/边界/OpenAPI/ER/Nacos）
- [ ] 4 项关键提醒鲁班签字确认

> 升级完成后，PM 将此清单放入 `docs/architect-handoff-v1.3.7-handoff.md` 作为交接记录，团队即可开始 Sprint 0。

---

**签收**

| 角色 | 姓名 | 签字 | 日期 |
|------|------|------|------|
| PM | 潘强（范蠡）| _____________ | 2026/06/09 |
| Architect | 鲁班 | _____________ | ____/____/____ |
