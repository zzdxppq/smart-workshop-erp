# 6. Epics（核心，完整 YAML）

> **YAML 字段说明**：
> - `id`：`E{epic}-S{story}`，例如 `E2-S1` 表示 Epic 2 第 1 个 Story。
> - `repository_type`：该 Story 主要代码所属仓库。
> - `estimated_complexity`：S（< 3 天）/ M（3-7 天）/ L（7-14 天）/ XL（> 14 天）。
> - `priority`：P0（必做）/ P1（重要）/ P2（增值）。
> - `acceptance_criteria`：GIVEN/WHEN/THEN 格式 + 业务规则 + 数据验证 + 错误处理 + 示例。
> - `provides_apis` / `consumes_apis`：RESTful 路径；版本前缀 `/api/v1/`。

### Epic 1: 基础设施与权限

#### E1: 基础设施与权限（完整 YAML）

```yaml
epic:
  id: E1
  title: 基础设施与权限
  version: V1.0
  target_platform: [backend, web, android]   # core 是 backend 内部 Module，不作为 target_platform
  why_exist: |
    WHY：其他所有 Epic 都依赖"谁能登入、谁能操作、谁能审批"。
         没有这套底座，每个业务模块都要重复造权限轮子，永远统一不起来。
  what_it_is:
    - 用户/角色/部门/职位的统一管理
    - 菜单/数据/操作/金额四级权限
    - 审批工作流引擎（拖拽配置）
    - 系统参数 + 数据字典
    - APP 端登录/消息/扫码壳
  what_it_is_not:
    - 不是业务功能（CRM/ERP 业务在 Epic 2-10）
    - 不是日志分析平台（操作日志只保留查询）
    - 不是工作流执行器（只做审批类，不做 BPMN 全套）
  reuse_analysis:
    can_reuse:
      - SaaS 鉴权框架（如 Sa-Token）做会话
      - Flowable / Camunda 做工作流
    must_build:
      - 金额分级权限引擎（与业务深度耦合）
      - APP 端扫码壳（自研，统一识别）
  depends_on_epics: []
  stories_count: 4
  stories:
    - id: E1-S1
      title: 用户与角色权限
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      why_first: |
        WHY：权限是 0 号基础设施，先做它，后续 11 个 Epic 都能站在这上面。
        客户诉求："业务员不能改单价，经理才能看毛利"——本质就是角色权限。
      acceptance_criteria:
        - id: AC-1.1.1
          given: 管理员已登录系统
          when: 创建新用户「张三」，分配「业务员」角色，密码 = `Pass1234`
          then: |
            系统创建成功，弹出"用户创建成功"提示；
            张三可以登录；登录后只能看到 CRM/报价/订单菜单；
            张三看不到采购单价、毛利率、合同金额。
          business_rules:
            - 用户名全局唯一（不区分大小写）
            - 密码 ≥ 8 位，必须含大小写字母+数字
            - 密码 bcrypt 加密存储
            - 用户被禁用后，JWT 立即失效（Redis 黑名单 5 分钟内踢出）
          data_validation:
            - 用户名：必填，3-20 字符，仅字母数字下划线
            - 姓名：必填，1-50 字符
            - 手机号：可选，11 位数字，校验格式
            - 邮箱：可选，校验格式
          error_handling:
            - 用户名重复 → "用户名已存在"
            - 密码强度不足 → "密码必须 ≥ 8 位含大小写数字"
            - 角色未分配 → "请至少分配一个角色"
          examples:
            - valid: { username: "zhangsan", password: "Pass1234", role: "业务员" }
            - invalid: { username: "zs", password: "123" } # 长度不足
        - id: AC-1.1.2
          given: 管理员在角色管理中编辑「业务员」角色
          when: 设置「订单模块-查看全部客户」权限为关闭
          then: |
            保存后，所有"业务员"角色用户登录后，看不到"全部客户"列表页；
            仅能看到"我的客户"列表。
          business_rules:
            - 菜单权限：树形勾选，全选/全不选/反选
            - 数据权限：本部门 / 本人及下属 / 全部 / 自定义
            - 操作权限：增 / 改 / 删 / 审 / 导 / 打（独立勾选）
            - 角色删除：若已分配用户，禁止删除
          data_validation:
            - 角色名称：必填，1-20 字符，全局唯一
            - 角色编码：必填，字母数字下划线
        - id: AC-1.1.3
          given: 「业务员」角色设置了金额阈值 = 50000
          when: 业务员「张三」提交一笔 60000 元报价
          then: |
            系统自动跳过业务员审批，路由到「部门经理」角色第一个用户；
            张三的"我的待办"看不到这笔单。
          business_rules:
            - 金额阈值定义在角色上（见 FR-1-3-3 全局配置）
            - 审批路由：报价阈值 < 5万业务员、5-20万部门经理、> 20万总经理
            - 同一审批节点多人时，按"部门经理"角色取第一个有效用户
      provides_apis:
        - POST /api/v1/auth/login
        - POST /api/v1/auth/logout
        - GET  /api/v1/auth/me
        - POST /api/v1/users
        - GET  /api/v1/users/{id}
        - PUT  /api/v1/users/{id}
        - DELETE /api/v1/users/{id}
        - GET  /api/v1/users?page=0&size=20&keyword=...
        - POST /api/v1/roles
        - GET  /api/v1/roles
        - PUT  /api/v1/roles/{id}
        - DELETE /api/v1/roles/{id}
        - POST /api/v1/roles/{id}/permissions
      consumes_apis: []
      depends_on_stories: []

    - id: E1-S2
      title: 审批工作流配置
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-1.2.1
          given: 管理员在工作流配置页
          when: 新建「订单审批」流程，节点：业务员提交 → 部门经理 → 总经理（条件：金额>20万）
          then: |
            流程保存成功，状态=「启用」；
            后续订单提交时按此路由审批。
          business_rules:
            - 节点类型：审批人 / 审批角色 / 发起人自选 / 部门经理
            - 条件分支支持：金额区间、客户类型、订单类型
            - 内置 4 套模板：报价/订单/采购/付款（不可删除，可禁用）
            - 流程启用前必须经过"流程测试"（用历史单据走一遍）
          data_validation:
            - 流程名称：必填，1-30 字符
            - 节点至少 2 个（发起 + 审批）
            - 金额条件用闭区间 [50000, 200000)
        - id: AC-1.2.2
          given: 订单 10001 处于"部门经理审批"节点已 24 小时
          when: 系统检测超时
          then: |
            推送企业微信消息给部门经理（"您有 1 笔订单审批超时"）；
            同步在 APP 消息中心置顶。
          business_rules:
            - 阈值：24h 提醒、48h 升级、72h 邮件（V1.1）
            - 提醒内容：单号、金额、申请人、申请时间
      provides_apis:
        - POST /api/v1/workflows
        - GET  /api/v1/workflows
        - PUT  /api/v1/workflows/{id}
        - POST /api/v1/workflows/{id}/test
        - GET  /api/v1/approvals/pending
        - POST /api/v1/approvals/{id}/approve
        - POST /api/v1/approvals/{id}/reject
      consumes_apis:
        - GET /api/v1/users  # 审批人选择
        - GET /api/v1/roles
      depends_on_stories: [E1-S1]

    - id: E1-S3
      title: 系统参数与数据字典
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      acceptance_criteria:
        - id: AC-1.3.1
          given: 管理员在「数据字典」管理页
          when: 维护「物料分类」字典：原材料/半成品/成品/辅料/包材
          then: |
            物料管理页下拉框立即可用；
            任意模块可引用，引用后字典项不可删除（只能禁用）。
          business_rules:
            - 字典分类：物料分类/工序类型/表处类型/班别/仓库/币种/单位
            - 字典项被引用后禁止物理删除
            - 字典项有"启用/禁用"状态，禁用后下拉框不可见
        - id: AC-1.3.2
          given: 管理员在「系统参数」配置报价单号生成规则 = `BJ+YYYYMMDD+4位流水`
          when: 业务员新建一笔报价
          then: 报价单号 = `BJ202606040001`（当天第 1 笔）
          business_rules:
            - 参数分组：单据编号/打印模板/审批阈值/APP 设置
            - 变更走变更日志（操作人/前后值/时间/IP）
      provides_apis:
        - GET  /api/v1/dict/{type}
        - POST /api/v1/dict/{type}
        - PUT  /api/v1/dict/{type}/{id}
        - GET  /api/v1/system/params
        - PUT  /api/v1/system/params
        - GET  /api/v1/system/serial/{bizType}  # 获取下一个单据号
      consumes_apis: []
      depends_on_stories: [E1-S1]

    - id: E1-S4
      title: APP 端基础（登录/消息/扫码壳）
      repository_type: android
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-1.4.1
          given: 操作工在车间打开 APP
          when: 输入账号 zhangsan + 密码 Pass1234
          then: |
            登录成功进入 APP 首页（80% 区域是扫码框）；
            本地缓存登录态，30 天内免登录（可配置）。
          business_rules:
            - 登录支持账号密码（V1.0）/ 指纹（V1.0）/ 人脸（V1.1）
            - 登录失败 5 次锁定 30 分钟
            - 离线模式：未登录也可扫码（本地缓存），登录后自动同步
        - id: AC-1.4.2
          given: 操作工在 APP 首页
          when: 点击"扫码"按钮，对准工单二维码
          then: |
            1 秒内识别码类型（工单码 / 流转码 / 设备码 / 物料码 / **委外订单二维码 V1.3.5**）；
            自动路由到对应业务页（开工 / 报工 / 过站 / 出入库 / **到货扫码 V1.3.5**）。
          business_rules:
            - 码前缀识别：GD-工单码、LZ-流转码、SB-设备码、WL-物料码、**WW-委外订单二维码（V1.3.5 新增到货扫码入口）**
            - **V1.3.5 收回**：V1.3.4 引入的 AREA-区域码整章删除；扫码路由只剩 4 类
            - WW-委外单码路由：识别后由仓管角色触发"到货确认"流程（V1.3.5 E12-S2 / E4-S2 增补）
            - 识别失败：显示"未知码，请确认"
            - 同时支持一维码（Code 128）/ 二维码（QR）
        - id: AC-1.4.3
          given: 操作工在车间扫码报工后断网
          when: 继续扫码 5 次（5 条记录）
          then: |
            5 条记录全部保存到本地 SQLite（带本地时间戳）；
            顶部出现"离线模式"红条提示；
            网络恢复 30 秒内自动同步到服务端，弹窗显示同步结果。
          business_rules:
            - 离线缓存上限 500 条
            - 冲突解决：以服务端最新状态为准；
              冲突时弹窗让用户选"覆盖"或"合并"；
              合并按"时间戳晚的覆盖早的"。
            - 本地时间戳格式：ISO 8601 + 本地时区
      provides_apis:
        - POST /api/v1/app/login
        - POST /api/v1/app/sync  # 批量同步离线记录
        - GET  /api/v1/app/messages
        - POST /api/v1/app/messages/{id}/read
        - GET  /api/v1/app/scan/route  # 扫码路由（前端用）
      consumes_apis:
        - POST /api/v1/auth/login
        - GET  /api/v1/auth/me
      depends_on_stories: [E1-S1]
```

#### E2: 客户与销售（完整 YAML）

```yaml
epic:
  id: E2
  title: 客户与销售
  version: V1.0
  target_platform: [backend, web, android]
  why_exist: |
    WHY：销售是工厂的"血液"。业务员每天花 3 小时在客户跟进、报价、订单上，
         但传统 ERP 让他们在 5 个系统间切换。本 Epic 让业务员从询价到回款
         在一个系统内闭环。
  what_it_is:
    - 客户档案 + CRM（领用/保护/共享/洽谈）
    - 报价单（BOM 报价 + 工艺预估 + 多级审批）
    - 销售订单（审批 + 变更 + 状态机）
    - 合同 + 回款计划 + 利润核算
  what_it_is_not:
    - 不是客户社交平台（不做朋友圈/打卡/签到）
    - 不是销售自动化（不做邮件群发/短信营销）
    - 不是 CRM 中的 SFA 完整版（V1.0 简版，V1.1 深化）
  reuse_analysis:
    can_reuse:
      - 客户保护机制：参考纷享销客"30 天保护期"
      - 报价审批：复用 E1-S2 工作流引擎
    must_build:
      - 客户-联系人-洽谈三件套数据模型
      - 报价 BOM 成本计算引擎
  depends_on_epics: [E1]
  stories_count: 4
  stories:
    - id: E2-S1
      title: 客户档案与 CRM
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-2.1.1
          given: 业务员「张三」登录系统
          when: 新建客户「上海某科技公司」，行业=「机械制造」，信用额度=500000
          then: |
            客户编号自动生成 KH202606040001；
            张三自动成为该客户的"主负责人"；
            客户进入 30 天保护期，他人只读。
          business_rules:
            - 客户编码规则：KH+YYYYMMDD+4 位流水
            - 保护期：默认 30 天，可在系统参数配置
            - 信用额度：0 表示无额度
            - 同一客户名 24h 内不允许重复创建（防误录）
          data_validation:
            - 客户名称：必填，1-100 字符，全局唯一（区分大小写）
            - 行业：必填，引用数据字典
            - 信用额度：≥ 0，金额
            - 税号：可选，15-20 位
            - 开票地址/电话/账号：可选，每项 1-200 字符
        - id: AC-2.1.2
          given: 业务员「李四」想查看「上海某科技公司」
          when: 李四进入客户详情页
          then: |
            看到的是"只读视图"（带水印"保护中"）；
            不能编辑/转客户/领用。
          business_rules:
            - 保护期内：他人只读 + 不能领用
            - 保护期满：所有人可领用
            - 管理员可强制收回/转移
        - id: AC-2.1.3
          given: 业务员「张三」对该客户添加洽谈记录
          when: 跟进内容="客户对报价有兴趣，预计下周确认"，下次跟进=2026-06-10
          then: |
            洽谈时间线新增一条记录；
            2026-06-10 早上 9 点系统推送提醒。
          business_rules:
            - 进展阶段：初步接触 / 需求确认 / 报价中 / 议价中 / 已签约 / 已流失
            - 跟进时间不能早于当前时间
            - 下次跟进提醒：APP 消息中心 + 企业微信
      provides_apis:
        - POST /api/v1/customers
        - GET  /api/v1/customers/{id}
        - PUT  /api/v1/customers/{id}
        - GET  /api/v1/customers?page=0&size=20&keyword=&owner=&protectStatus=
        - POST /api/v1/customers/{id}/claim  # 领用
        - POST /api/v1/customers/{id}/release  # 释放
        - POST /api/v1/customers/{id}/transfer  # 转移
        - GET  /api/v1/customers/{id}/contacts
        - POST /api/v1/customers/{id}/contacts
        - GET  /api/v1/customers/{id}/followups
        - POST /api/v1/customers/{id}/followups
      consumes_apis:
        - GET /api/v1/dict/industry
        - GET /api/v1/users
      depends_on_stories: [E1-S1]

    - id: E2-S2
      title: 报价与多级审批
      repository_type: backend+web
      estimated_complexity: XL
      priority: P0
      acceptance_criteria:
        - id: AC-2.2.1
          given: 业务员「张三」客户「上海某科技公司」要询价
          when: 创建报价单，1 条明细：图号 P001，材质 45#，数量 100，单价 80，金额 8000
          then: |
            报价单号 BJ202606040001；
            状态=「待审」；
            自动路由到「业务员」角色审批（金额 < 5 万）。
          business_rules:
            - 报价单号：BJ+YYYYMMDD+4 位流水
            - 审批阈值：< 5万业务员、5-20万部门经理、> 20万总经理
            - 客户为黑名单 → 直接驳回
            - 报价单从「草稿」→「待审」后不可改数量/单价，只能撤销重报
          data_validation:
            - 客户：必填，引用客户档案
            - 交期：必填，≥ 当前日期 + 3 天
            - 币种：必填，默认 CNY
            - 明细：至少 1 条
            - 单价/数量：> 0，金额 = 单价 × 数量
        - id: AC-2.2.2
          given: 报价 BJ202606040001 处于「待审」状态
          when: 业务员「张三」点击「通过」
          then: |
            状态变「已审」；
            "审批通过"按钮置灰；
            出现在"我的报价-已审"列表。
          business_rules:
            - 审批意见：必填，≥ 5 字符（V1.0 软提示，V1.1 强校验）
            - 驳回：必须填写驳回原因（≥ 5 字符），单据回到「草稿」
        - id: AC-2.2.3
          given: 报价 BJ202606040001 已审通过
          when: 业务员点击「转订单」
          then: |
            系统创建销售订单，订单号 XS202606040001；
            客户/明细/图纸/数量/单价自动带出；
            业务员可微调交期/币种，提交订单审批。
          business_rules:
            - 报价 → 订单：明细行 1:1 复制
            - 同一报价只能转 1 次订单
            - 报价状态变「已转订单」
      provides_apis:
        - POST /api/v1/quotes
        - GET  /api/v1/quotes/{id}
        - PUT  /api/v1/quotes/{id}
        - POST /api/v1/quotes/{id}/submit
        - POST /api/v1/quotes/{id}/approve
        - POST /api/v1/quotes/{id}/reject
        - POST /api/v1/quotes/{id}/convert-to-order
        - GET  /api/v1/quotes/export/{id}  # PDF
      consumes_apis:
        - GET /api/v1/customers
        - POST /api/v1/files/upload  # 图纸
        - POST /api/v1/orders  # 转订单
      depends_on_stories: [E2-S1, E1-S2]

    - id: E2-S3
      title: 订单与合同
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-2.3.1
          given: 业务员新建销售订单
          when: 客户「上海某科技公司」，明细金额合计 300000
          then: |
            订单号 XS202606040001；
            状态=「待审」；
            信用额度校验：(应收账款 + 300000) > 信用额度 500000？
            - 若否：路由到「部门经理」（5-20 万阈值）
            - 若是：升级到「总经理」审批
          business_rules:
            - 订单号：XS+YYYYMMDD+4 位流水
            - 审批阈值：同报价
            - 信用额度超限升级：未收金额 + 本次金额 > 信用额度
            - 订单类型：标准/急单/试制（影响后续排产优先级）
            - 合同附件：支持 PDF/图片
          state_machine:
            - 草稿 → 待审（提交）
            - 待审 → 已审（通过）/ 草稿（驳回）
            - 已审 → 生产中（订单下发）
            - 生产中 → 部分发货 / 已发货
            - 已发货 → 已结算
            - 已结算 → 已关闭
            - 任意状态 → 已取消（管理员权限）
        - id: AC-2.3.2
          given: 订单 XS202606040001 状态 =「生产中」
          when: 业务员提交订单变更：交期从 2026-07-01 改到 2026-07-15
          then: |
            系统记录变更前/变更后值；
            变更后自动通知生管员/客户。
          business_rules:
            - 变更类型：数量/交期/工艺/客户
            - 变更前值/后值都保留
            - 变更后通知：APP 消息 + 企业微信
            - 数量变更超过 ±20% 需重新审批
      provides_apis:
        - POST /api/v1/orders
        - GET  /api/v1/orders/{id}
        - PUT  /api/v1/orders/{id}
        - POST /api/v1/orders/{id}/submit
        - POST /api/v1/orders/{id}/approve
        - POST /api/v1/orders/{id}/changes  # 变更记录
        - GET  /api/v1/orders/{id}/timeline  # 订单时间线
      consumes_apis:
        - GET /api/v1/customers
        - POST /api/v1/quotes/{id}/convert-to-order
      depends_on_stories: [E2-S2, E1-S2]

    - id: E2-S4
      title: 合同回款与利润
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-2.4.1
          given: 销售订单 XS202606040001 总金额 300000
          when: 创建回款计划：30%/40%/30% = 90000/120000/90000，日期 2026-07-01/2026-08-01/2026-09-01
          then: |
            自动生成 3 期回款计划；
            到期前 3 天 APP 推送业务员和财务。
          business_rules:
            - 计划期数：1-N 期（N ≤ 12）
            - 计划日期：> 订单日期
            - 计划金额合计 = 订单金额
        - id: AC-2.4.2
          given: 订单 XS202606040001 已收 200000，未收 100000
          when: 财务录入收款 50000
          then: |
            已收 = 250000；未收 = 50000；
            自动匹配最早一期待回款计划。
          business_rules:
            - 收款大于未收：超出部分记为预付款
            - 收款日期：≤ 当前日期
            - 收款必须关联订单/对账单
        - id: AC-2.4.3
          given: 订单已结案
          when: 财务查看订单利润
          then: |
            利润 = 总收款 - 总成本（材料+人工+委外+费用）
            利润率 = 利润 / 总收款 × 100%
            若利润率 < 5% → 标红。
          business_rules:
            - 成本来源：材料（BOM 领料）、人工（计件/计时）、委外（委外订单）、费用（费用报销）
            - 利润率 = (总收款 - 总成本) / 总收款
            - 利润率 < 5% 标红；< 0 标深红（亏损）
      provides_apis:
        - POST /api/v1/orders/{id}/payment-plans
        - GET  /api/v1/orders/{id}/payment-plans
        - POST /api/v1/receipts  # 收款记录
        - GET  /api/v1/orders/{id}/profit
      consumes_apis:
        - GET /api/v1/orders/{id}
      depends_on_stories: [E2-S3, E9-S1]
```

#### E3: 图纸与物料（完整 YAML）

```yaml
epic:
  id: E3
  title: 图纸与物料
  version: V1.0
  target_platform: [backend, web]
  why_exist: |
    WHY：CNC 加工厂的"图纸"是核心资产。一张图错了，整批报废。
         老师傅脑子里有 1000 张图，新人来了要跟一年。
         本 Epic 把图号、BOM、工艺做成可复用资产。
  what_it_is:
    - 图纸档案 + 版本管理
    - 客户图纸 → 厂内图纸工程转化
    - BOM 多级维护（不限层级）
    - 工艺库 + 工序路线
  what_it_is_not:
    - 不是 CAD/CAM 软件（不编辑图纸，只管理文件）
    - 不是 PLM（不做项目管理/变更控制 ECN 全套）
    - 不是 MES（不做工艺执行）
  reuse_analysis:
    can_reuse:
      - pdf.js 做在线预览
      - jstree 做 BOM 树形展示
    must_build:
      - BOM 多级展开 + 损耗计算引擎
      - 工艺路线编辑器
  depends_on_epics: [E1]
  stories_count: 3   # V1.3.5 从 4 减为 3：E3-S5 区域码已删除
  stories:
    - id: E3-S1
      title: 图纸与版本管理
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      acceptance_criteria:
        - id: AC-3.1.1
          given: 工程师上传客户图纸 P001-V1.pdf，关联图号 P001
          when: 提交保存
          then: |
            图纸档案生成，记录版本 V1.0；
            文件存到 MinIO 桶；
            工程师可在线预览。
          business_rules:
            - 图号：客户图号 + 厂内图号（可同可不同）
            - 版本号格式：V{major}.{minor}（V1.0, V1.1, V2.0）
            - 文件格式：PDF / DWG / STEP / NC
            - 文件大小：≤ 50MB
          data_validation:
            - 图号：必填，1-50 字符，全局唯一
            - 文件：必填，PDF/DWG/STEP/NC
            - 备注：可选
        - id: AC-3.1.2
          given: 图纸 P001-V1.0 已存在
          when: 上传 P001-V2.pdf，变更说明="增加 0.5mm 公差"
          then: |
            生成新版本 V2.0；
            V1.0 变为"历史版本"（可查不可改）；
            变更说明必填（≥ 10 字符）。
          business_rules:
            - 历史版本：保留 ≥ 5 年
            - 变更说明：≥ 10 字符
      provides_apis:
        - POST /api/v1/drawings
        - GET  /api/v1/drawings/{id}
        - GET  /api/v1/drawings?page=0&size=20&keyword=&version=
        - GET  /api/v1/drawings/{id}/versions
        - POST /api/v1/drawings/{id}/versions
        - GET  /api/v1/drawings/{id}/preview
      consumes_apis:
        - POST /api/v1/files/upload
      depends_on_stories: [E1-S1]

    - id: E3-S2
      title: 工程转化
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      acceptance_criteria:
        - id: AC-3.2.1
          given: 客户图纸 P001-V1.0
          when: 工程师点击「工程转化」→ 新建厂内图号 N001，复制原图
          then: |
            新图纸 N001-V1.0 创建；
            关联原客户图号 P001；
            工程师在 PDF 预览上添加批注（保存为新版本）。
          business_rules:
            - 客户图号 → 厂内图号映射记录
            - 厂内图纸支持"挂载"多个客户图号
            - 批注：使用 pdf.js annotation
      provides_apis:
        - POST /api/v1/drawings/{id}/convert
        - POST /api/v1/drawings/{id}/annotations
      consumes_apis:
        - GET /api/v1/drawings/{id}
      depends_on_stories: [E3-S1]

    - id: E3-S3
      title: BOM 多级维护
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-3.3.1
          given: 工程师为产品 P001 维护 BOM
          when: 一级子件：原材料 1（用量 2）、半成品 A（用量 1）；半成品 A 又有子件 原材料 2（用量 3）
          then: |
            BOM 树形展示：P001 → [原材料 1, 半成品 A → [原材料 2]]
            损耗系数：原材料 1 损耗 5%，原材料 2 损耗 8%
          business_rules:
            - BOM 层级不限（理论无限，实测 ≤ 5 层）
            - 损耗系数：按层级独立维护，0-50%
            - BOM 版本：V1.0 启用后 V1.0 不可改
            - 子件引用物料必须存在
          data_validation:
            - 父件/子件：必填
            - 用量：> 0
            - 损耗系数：0-50%
            - 工序号：1-N 顺序
        - id: AC-3.3.2
          given: 工程师有设计 BOM P001-V1.0
          when: 点击「一键转生产 BOM」
          then: |
            复制为生产 BOM P001-V1.0-P（生产）；
            工程师可微调用量/损耗，发布后生效。
          business_rules:
            - 设计 BOM → 生产 BOM：复制整棵树
            - 生产 BOM 状态：草稿 / 已发布 / 已废弃
            - 已发布 BOM 被订单引用后不可改
      provides_apis:
        - POST /api/v1/boms
        - GET  /api/v1/boms/{id}
        - GET  /api/v1/boms/{id}/tree
        - POST /api/v1/boms/{id}/convert-to-production
        - POST /api/v1/boms/{id}/publish
      consumes_apis:
        - GET /api/v1/materials
      depends_on_stories: [E3-S4, E1-S1]

    - id: E3-S4
      title: 工艺库与工序
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      acceptance_criteria:
        - id: AC-3.4.1
          given: 工程师在工序库新建「车外圆」
          when: 名称=车外圆，标准工时=10 min/件，设备类型=车床，单价=2 元/件
          then: |
            工序库新增；后续工艺路线可引用。
          business_rules:
            - 工序名称：全局唯一
            - 标准工时：单位 min/件
            - 设备类型：车床/CNC/铣床/钻床/钳工/表处/热处理/三次元/清洗/包装
            - 单价：元/件
        - id: AC-3.4.2
          given: 产品 P001 工艺路线：车外圆 → CNC → 检验 → 包装
          when: 工程师拖拽排序并保存
          then: |
            工艺路线保存；总工时 = 工序工时 × 数量。
          business_rules:
            - 工序顺序：1-N
            - 加工类型：自加工 / 委外
            - 复制历史工艺：选历史产品 → 一键复制
      provides_apis:
        - POST /api/v1/processes
        - GET  /api/v1/processes
        - POST /api/v1/products/{id}/routes
        - GET  /api/v1/products/{id}/routes
        - POST /api/v1/products/{id}/routes/copy-from/{srcProductId}
      consumes_apis: []
      depends_on_stories: []

    # ===== E3-S5 已删除（V1.3.5 客户收回区域码） =====
    # 原 V1.3.4 引入的"固定区域二维码管理"整章删除。
    # 理由：V1.3.5 客户反馈"取消区域码"——送货员角色也删除后，
    # 区域码失去了服务对象（无厂外人员扫），少一类码少一套贴牌少一份维护。
    # 一码到底的灵魂不靠"多一类码"维持，而靠"每一类码全程贯穿"。
    # 替代方案：到货扫码由仓管扫"委外订单二维码"实现（见 V1.3.5 E12-S1 / E12-S2 / E4-S2 增补）。
  ```

#### E4: 扫码仓储一体化（完整 YAML）

```yaml
epic:
  id: E4
  title: 扫码仓储一体化
  version: V1.0
  target_platform: [backend, web, android]
  why_exist: |
    WHY：仓管员每天花在"找料、记账、对数"上 3 小时以上。
         APP 扫码让"扫一下、按一下"就能完成出入库；
         老师傅不用教，新人 10 分钟上手。
  what_it_is:
    - 物料条码生成 + 标签打印
    - APP 扫码出入库（采购/生产/委外/其他）
    - 多仓库 + 库位 + 批次
    - 安全库存 + 库存预警
  what_it_is_not:
    - 不是 WMS 全套（不做 RF 全套策略、AGV 调度）
    - 不是 TMS（不做物流跟踪）
    - 不是多组织多公司（单公司单组织）
  reuse_analysis:
    can_reuse:
      - zxing-android-embedded 做扫码
      - iText + JasperReports 做标签打印
    must_build:
      - 离线扫码冲突解决
      - 多仓库库存核算
  depends_on_epics: [E1, E3]
  stories_count: 4
  stories:
    - id: E4-S1
      title: 物料条码生成
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      acceptance_criteria:
        - id: AC-4.1.1
          given: 物料 45#圆料 直径 80 采购入库 100kg
          when: 仓管员入库时点击"生成条码"
          then: |
            物料码 = WL-YL45D80-20260604-0001；
            可选择 50mm×30mm 标签批量打印。
          business_rules:
            - 物料码规则：WL-{物料编码}-{入库日期}-{4位流水}
            - 支持一维码（Code 128）/ 二维码（QR）
            - 标签内容：物料码、物料名称、规格、批次、入库日期
        - id: AC-4.1.2
          given: 仓管员选中 50 个物料码
          when: 点击「批量打印」
          then: |
            PDF 文件生成，按 50mm×30mm 排版；
            每页 4×6 = 24 个标签；
            下载到本地打印。
          business_rules:
            - 标签模板：可配置
            - 打印份数：默认 1 份
      provides_apis:
        - POST /api/v1/materials/{id}/barcodes
        - GET  /api/v1/materials/{id}/barcodes
        - POST /api/v1/barcodes/print
      consumes_apis:
        - GET /api/v1/materials
      depends_on_stories: [E3-S3]

    - id: E4-S2
      title: APP 扫码出入库
      repository_type: android
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-4.2.1
          given: 仓管员打开 APP 扫码
          when: 扫物料码 WL-YL45D80-20260604-0001
          then: |
            自动识别为"物料码"；
            弹出页面：入库类型（采购/生产/委外/其他）、库位、数量；
            默认填入 1.0，可修改。
          business_rules:
            - 码前缀识别：WL-物料码（详见 E1-S4 AC-1.4.2）
            - 入库类型：采购（关联采购订单）/生产（关联工单）/委外（关联委外订单）/其他
            - 数量：> 0
            - 库位：必选
        - id: AC-4.2.2
          given: 仓管员扫描后断网
          when: 提交扫码入库 5 次
          then: |
            5 条记录保存到本地；
            顶部"离线"红条；
            网络恢复 30s 内自动同步。
          business_rules:
            - 详见 E1-S4 AC-1.4.3 离线策略
            - 冲突解决：同物料码入库两次 → 后者提示"该批次已入库，是否合并？"
      provides_apis:
        - POST /api/v1/stock/in
        - POST /api/v1/stock/out
        - POST /api/v1/app/sync  # 复用
      consumes_apis:
        - GET /api/v1/materials/{barcode}
      depends_on_stories: [E4-S1, E1-S4]

    - id: E4-S3
      title: 库位批次与多仓库
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-4.3.1
          given: 管理员维护仓库：一号仓、二号仓
          when: 一号仓下设置库区 A、B；A 区下设置库位 A-01-01、A-01-02
          then: |
            库位编码：A-01-01（A 区 01 排 01 位）
            库存查询可按库位过滤。
          business_rules:
            - 库位编码：W-A-01-03（一号仓 A 区 01 排 03 位）
            - 库位容量：可选，重量/体积
            - 库位状态：空闲/占用/锁定
        - id: AC-4.3.2
          given: 物料 YL45D80 入库批次 B20260604-001
          when: 出库时未指定批次
          then: |
            系统按先进先出（FIFO）自动选择最早批次。
          business_rules:
            - 批次号：YYYYMMDD-流水
            - 默认 FIFO；可手动指定
            - 批次追溯：扫码查批次 → 关联工单/订单
        - id: AC-4.3.3
          given: 仓管员/生管员/业务员需要查"历史订单 + 成本明细"
          when: 在仓储/订单全局搜索框输入"图号 P001" 或"料号 YL45D80" 或选日期范围/状态多选
          then: |
            系统返回所有匹配的历史订单 + 工单 + 成本明细一屏聚合；
            支持二次过滤（按客户/业务员/供应商）；
            支持导出 Excel。
          business_rules:
            - 检索维度：图号 / 料号 / 日期范围 / 订单状态（多选）/ 客户 / 业务员 / 供应商
            - 模糊检索：图号/料号支持前缀 + 包含匹配
            - 索引：MySQL 联合索引 + Redis 二级缓存（热门图号 5 分钟）
            - 性能：P95 ≤ 2s（10 万订单数据集）
            - 权限：管理层可见成本明细列；一线角色看不到成本（同 §1.1 数据权限）
      provides_apis:
        - POST /api/v1/warehouses
        - GET  /api/v1/warehouses
        - POST /api/v1/warehouses/{id}/locations
        - GET  /api/v1/warehouses/{id}/locations
        - GET  /api/v1/stock?materialId=&warehouseId=&locationId=&batchNo=
        - GET  /api/v1/search/orders?drawingNo=&materialNo=&dateFrom=&dateTo=&status=&customerId=&page=
        - GET  /api/v1/search/orders/{id}/cost-detail   # 含权限校验
        - GET  /api/v1/search/orders/export?...         # 导出 Excel
      consumes_apis: []
      depends_on_stories: [E1-S1]

    - id: E4-S4
      title: 安全库存与预警
      repository_type: backend+web
      estimated_complexity: S
      priority: P1
      acceptance_criteria:
        - id: AC-4.4.1
          given: 物料 YL45D80 安全库存上限 = 1000kg，下限 = 100kg
          when: 库存 = 80kg
          then: |
            系统生成预警，APP 推送仓管/生管；
            库存报表中标红。
          business_rules:
            - 预警阈值：可按物料配置上下限
            - 预警推送：APP + 企业微信
            - 预警生成频率：每小时一次
            - 预警去重：同一物料 24h 内不重复推
      provides_apis:
        - PUT  /api/v1/materials/{id}/safety-stock
        - GET  /api/v1/alerts/stock
        - POST /api/v1/alerts/{id}/read
      consumes_apis:
        - GET /api/v1/materials
      depends_on_stories: [E4-S3]
```

#### E5: 生产执行核心（完整 YAML）

```yaml
epic:
  id: E5
  title: 生产执行核心
  version: V1.0
  target_platform: [backend, web, android]
  why_exist: |
    WHY：生产车间是 ERP 的"心脏"。老师傅凭经验排产、记工时，
         老板月底才知道哪个订单亏了。本 Epic 用扫码三码
         把"开工→加工→报工→过站"全流程数字化。
  what_it_is:
    - 工单 + 排产（Gantt 视图）
    - 扫码开工/报工/过站（三码区分）
    - 物料需求分析 MRP
    - 委外下单基础
    - 设备与机台排产
  what_it_is_not:
    - 不是 APS 高级排产（不做有限能力精细排程）
    - 不是 MES 完整版（不做设备数据采集、刀具寿命）
    - 不是车间大屏看板（V1.1 考虑）
  reuse_analysis:
    can_reuse:
      - ECharts Gantt 做排产
      - E2 订单模块（订单转工单）
    must_build:
      - 扫码三码区分（工单码/流转码/设备码）
      - 离线扫码冲突解决
  depends_on_epics: [E1, E2, E3, E4]
  stories_count: 5
  stories:
    - id: E5-S1
      title: 工单与排产
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      acceptance_criteria:
        - id: AC-5.1.1
          given: 销售订单 XS202606040001 状态=「已审」
          when: 生管员点击「转工单」
          then: |
            工单 GD202606040001 自动创建；
            产品/数量/工艺路线从订单/BOM 自动带出；
            状态=「待排产」。
          business_rules:
            - 工单号：GD+YYYYMMDD+4 位流水
            - 一张订单可拆多张工单
            - 工单引用 BOM（来自 E3-S3）
          state_machine:
            - 草稿 → 待排产（提交）
            - 待排产 → 已排产（排产确认）
            - 已排产 → 加工中（扫码开工）
            - 加工中 → 已报工（扫码报工）
            - 已报工 → 已检验（品检通过）
            - 已检验 → 已入库（成品入库）
            - 已入库 → 已关闭
            - 任意状态 → 已取消
          state_field_design_note: |
            【V1.3.3 客户评审澄清 / V1.3.4 正式收回】
            原"77 个生命状态"释义来自 V1.3.3 会议记录笔误，
            客户 2026-06-05 第二次反馈中已确认："Q1（77 状态）= 会议记录错误，不处理"。
            本 PRD V1.3.4 保留 7 个工单主状态（草稿/待排产/已排产/加工中/已报工/已检验/已入库/已关闭/已取消），
            不再额外强调"70 子字段"释义；
            实现策略仍为：状态机用 7 主状态枚举（DB enum + Spring State Machine），
                         字段值用 work_order_detail 表行级展开（每道工序一行 × 多列字段）。
            **委外订单的真正核心是 7 状态机**（详见 E6-S5 + 附录 B-2），与本 note 无关。
        - id: AC-5.1.2
          given: 工单 GD202606040001 状态=「待排产」
          when: 生管员拖拽工单到 6 月 10 日 CNC-01 机台
          then: |
            工单状态=「已排产」；
            计划开始 = 2026-06-10 08:00；
            计划结束 = 2026-06-10 18:00（按工时计算）。
          business_rules:
            - 排产视图：甘特图（X 轴时间，Y 轴机台）
            - 拖拽约束：不能拖到机台已占用时段
            - 工时计算：工序标准工时 × 数量 + 准备时间
        - id: AC-5.1.3
          given: 工单 GD202606040001 计划开始 = 2026-06-10
          when: 当前日期 = 2026-06-09，BOM 物料 YL45D80 库存 = 50kg，需求 = 100kg
          then: |
            MRP 自动生成缺料清单：YL45D80 缺 50kg；
            可一键转采购申请。
          business_rules:
            - MRP 计算：工单 BOM 展开 × 数量 - 库存 - 在途 = 缺料
            - MRP 重算频率：每小时 / 工单变更时
      provides_apis:
        - POST /api/v1/workorders
        - GET  /api/v1/workorders/{id}
        - GET  /api/v1/workorders?page=0&size=20&status=&machineId=
        - PUT  /api/v1/workorders/{id}/schedule
        - GET  /api/v1/workorders/{id}/timeline
        - POST /api/v1/workorders/{id}/mrp  # MRP 计算
        - POST /api/v1/workorders/{id}/purchase-requests
      consumes_apis:
        - GET /api/v1/orders/{id}
        - GET /api/v1/boms/{id}/tree
        - GET /api/v1/machines
      depends_on_stories: [E2-S3, E3-S3]

    - id: E5-S2
      title: 扫码开工/报工/过站
      repository_type: android+backend
      estimated_complexity: XL
      priority: P0
      acceptance_criteria:
        - id: AC-5.2.1
          given: 操作工「王师傅」在机台 CNC-01 前
          when: 打开 APP 扫工单码 GD202606040001
          then: |
            识别为「工单码」；
            自动识别操作人=王师傅，机台=CNC-01（可选扫设备码自动填）；
            弹出"开工"按钮，点击后工单状态=「加工中」；
            记录开工时间=2026-06-10 08:05:23。
          business_rules:
            - 扫码三码区分（V1.3.5 收回区域码，回到 3 类 + 委外单码）：
              - 工单码 GD-：记录加工人与时间
              - 流转码 LZ-：记录物料去向；**工序委外时每道委外工序生成独立 LZ-（V1.3.4，详见 E5-S6）**
              - 设备码 SB-：快速录入机台号（可选）
              - **委外订单二维码 WW-（V1.3.5 强调）**：由仓管扫触发"到货扫码"流程（详见 E12-S2）
            - **V1.3.5 收回**：V1.3.4 引入的 AREA-区域码删除；扫码类型由 4 类降为 3 类 + 委外单码
            - 权限：操作工只能扫分配给自己的工单
            - 状态校验：必须「已排产」才能开工
        - id: AC-5.2.2
          given: 王师傅在 CNC-01 完成加工
          when: 扫工单码 GD202606040001，录入完工=10，合格=9，报废=1
          then: |
            校验：合格(9) + 报废(1) = 完工(10) ✓
            工单进度更新：完成 10/100；
            自动通知品检员进行过程检。
          business_rules:
            - 报工数量 ≤ 派工数量
            - 合格 + 报废 = 完工
            - 报废率 > 10% 触发品检介入
            - 报工时间 = 当前时间（不可改）
        - id: AC-5.2.3
          given: CNC-01 工序检验合格
          when: 王师傅扫流转码 LZ-202606040001，录入下工序=钳工，交接数量=9
          then: |
            生成下工序开工触发；
            钳工位操作工 APP 收到"待开工"提醒。
          business_rules:
            - 流转码在上工序报工合格后自动生成
            - 流转码规则：LZ+YYYYMMDD+4 位流水
            - 流转码贴在物料包装上，跟随物料流转
      provides_apis:
        - POST /api/v1/app/workorders/{barcode}/start
        - POST /api/v1/app/workorders/{barcode}/report
        - POST /api/v1/app/transfer/{barcode}/next
        - GET  /api/v1/app/workorders/pending
      consumes_apis:
        - GET /api/v1/workorders/{barcode}
        - GET /api/v1/machines
      depends_on_stories: [E5-S1, E1-S4]

    - id: E5-S3
      title: 物料需求分析 MRP
      repository_type: backend
      estimated_complexity: M
      priority: P0
      acceptance_criteria:
        - id: AC-5.3.1
          given: 5 个工单都在 6 月 10 日开工
          when: MRP 重算
          then: |
            汇总 5 个工单的物料需求；
            减去在库 + 在途 = 净需求；
            生成采购申请清单（按物料汇总）。
          business_rules:
            - 重算触发：定时（每小时）+ 工单变更时
            - 净需求 > 0 才生成采购申请
            - 同一物料 24h 内不重复生成（去重）
      provides_apis:
        - POST /api/v1/mrp/run
        - GET  /api/v1/mrp/results?date=
      consumes_apis:
        - GET /api/v1/workorders
        - GET /api/v1/stock
        - GET /api/v1/purchase-orders  # 在途
      depends_on_stories: [E5-S1]

    - id: E5-S4
      title: 委外下单基础（V1.3.7 改版：生管/采购分工严格分离）
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      why_first: |
        WHY：**V1.3.7 客户第五次反馈原话**——
             "由**生管**决定哪些工序自制、哪些工序委外，
              然后委外到**哪家由采购决定**。"
             工序划分是生产计划问题（生管懂），厂商选择是供应链问题（采购懂）。
             V1.3.6 之前"业务/生管划分、采购选厂商"的描述模糊，V1.3.7 严格分离：
             · 生管：只决定"工序是厂内还是委外"，**不指定厂商**
             · 采购：接收生管提交的"待委外工序清单"，**为每道工序选厂商**
             职责清晰 = 避免"生管指定某厂商但采购拿不到好价格"
                       或"采购选了厂商但生管没安排工序"的脱节。
      acceptance_criteria:
        - id: AC-5.4.1
          title: 生管完成工序划分（V1.3.7 新增阶段）
          given: 工单 GD202606040001 工艺路线：车 → CNC → 表处 → 钳工 → 包装（5 道工序）
          when: 生管员在 PC 端"工单详情"页 → 工序划分 → 勾选「工序 1 厂内 / 工序 2 厂内 / 工序 3 委外 / 工序 4 厂内 / 工序 5 厂内」→ 提交
          then: |
            1. 工单工序状态记录每道工序的"厂内/委外"归属（work_order_process.outsource_flag）；
            2. 委外工序（此处 = 工序 3）进入"待委外工序清单"，状态 = 「待委外分配」；
            3. 系统自动推送「待委外工序清单」给采购员（APP + PC 端红点 + 站内信）；
            4. **不指定厂商**（生管视角不选厂商）。
        - id: AC-5.4.2
          title: 采购为每道委外工序选厂商并创建委外单（V1.3.7 改版）
          given: 采购员收到生管提交的"待委外工序清单"（含工序 3 表处，数量 100 件，工单 GD202606040001）
          when: 采购员在 PC 端"委外管理 → 待委外清单" → 选「工序 3 表处」→ 选厂商「A 表处厂」→ 自动带出历史价 8 元/件 → 录入数量 100 / 交期 2026-06-15 → 提交
          then: |
            1. 生成委外订单 WW202606040001；
            2. 状态 = 「待审」；
            3. 路由到生管员自审（金额 800 元 < 5 万）；
            4. 系统通过 163 邮箱 SMTP 推送通知至 A 表处厂"业务邮箱"（V1.3.7 单渠道，无短信）；
            5. 邮件含委外单号 / 工序 / 数量 / 单价 / 交期 / 二维码 WW-。
          business_rules:
            - 委外订单号：WW+YYYYMMDD+4 位流水
            - 自动调取历史价：取该厂商该工序最近 3 次成交价中位数
            - 历史价为空时显示"暂无历史价，请询价"
            - **V1.3.7**：通知渠道固定为 163 邮箱（Nacos `app.outsub.notify-channel = ['email_163']`），删除短信渠道
        - id: AC-5.4.3
          given: 委外订单 WW202606040001 已审
          when: 仓管员扫流转码出库物料 100 件
          then: |
            库存 -100；委外订单状态=「已出库」；
            关联外协厂商开始加工。
          business_rules:
            - 委外出库数量 = 发货数量（委外订单数量）
            - 委外入库数量 ≤ 发货数量（允许多次入库）
      provides_apis:
        - POST /api/v1/workorders/{id}/processes/{seq}/outsource-flag    # V1.3.7 新增：生管勾选工序归属
        - GET  /api/v1/outsource/pending-list                            # V1.3.7 新增：采购查看待委外清单
        - POST /api/v1/outsource                                          # V1.3.7 改：采购为每道委外工序创建委外单
        - GET  /api/v1/outsource/{id}
        - POST /api/v1/outsource/{id}/submit
        - POST /api/v1/outsource/{id}/stock-out
        - POST /api/v1/outsource/{id}/stock-in
      consumes_apis:
        - GET /api/v1/vendors
        - GET /api/v1/workorders/{id}
        - POST /api/v1/notifications/email                               # V1.3.7 单渠道（163 邮箱）
      depends_on_stories: [E5-S1]

    - id: E5-S5
      title: 设备与机台排产
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-5.5.1
          given: 管理员维护设备台账
          when: 新增 CNC-01（机台号 01，类型 CNC，状态 空闲）
          then: |
            设备档案创建；可被排产引用。
          business_rules:
            - 设备编码：SB+4 位流水
            - 设备类型：车床/CNC/铣床/钻床/...
            - 状态：空闲/加工中/维护/故障
            - 维护周期：可选，按小时或按日期
        - id: AC-5.5.2
          given: CNC-01 在 6 月 10 日已排产 8 小时
          when: 排产员尝试再排 6 小时
          then: |
            系统提示"该机台当日已满载，请选择其他机台或日期"。
          business_rules:
            - 负荷查看：按日展示已排工时/可用工时
            - 超载预警：已排 > 12h（按班别）预警
      provides_apis:
        - POST /api/v1/machines
        - GET  /api/v1/machines
        - PUT  /api/v1/machines/{id}
        - GET  /api/v1/machines/{id}/load?date=
      consumes_apis: []
      depends_on_stories: [E1-S1]

    - id: E5-S6
      title: 工序委外切片（V1.3.4 新增 / V1.3.5 强化"自动接续"）
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      why_first: |
        WHY：客户反馈"**目前工序委外订单多一些**"（V1.3.5 客户原话），"委外厂商送货回来，**大部分都还要流转**"。
             业务现实是：每张工序委外单的下一道工序几乎都不是"入库"，
             而是"下一道厂内工序"或"另一道委外工序"。
             这意味着：委外单完成不能直接"结案入库"，必须**自动触发下一道工序的接续码生成 + 锁机台 + 推岗位**。
             V1.3.4 引入切片 + 自动接续；V1.3.5 把"自动接续"从"可选项"升级为"必选项 + 工单完不成则阻塞提醒"，
             因为客户的业务现实是 100% 委外后还要流转，不是 80%。
             傻瓜测试：生管打开工单详情 → 一眼看到"第 3 道在 A 表处厂、预计 6/10 回来、第 4 道已锁机台等回流"。
      acceptance_criteria:
        - id: AC-5.6.1
          given: 工单 GD202606040001 工艺路线：车 → CNC → 表处 → 钳工 → 包装（5 道工序）
          when: 生管员选第 3 道「表处」→ 厂商「A 表处厂」→ 提交工序委外
          then: |
            该工序生成工序委外切片 GD202606040001-P03；
            自动生成独立流转码 LZ-GD001-P03；
            该工序进入委外 7 状态机的 PENDING_SHIP 态（详见 E6-S5）；
            工序 1/2 仍走厂内三码（GD/LZ/SB），工序 4/5 等待第 3 道回流后自动接续；
            **V1.3.5 强化**：工序 4 自动锁定机台 + 推送"待开工"预备通知到岗位 APP
            （预备 ≠ 实际开工，等第 3 道 STORED 完才正式触发）。
          business_rules:
            - 每道委外工序生成独立 LZ-{工单短码}-P{工序号} 流转码
            - 工序切片在工单详情页树形展示，进度条按工序粒度
            - 厂内 ↔ 厂外接续：第 N 道委外完成后，第 N+1 道工序自动通知接续岗位（无论厂内/厂外）
            - 复用 E5-S2 三码：进出厂、过站、报工流程不变，仅"加工人"换为"委外厂商"
            - **V1.3.5 新增**：委外单完成（无论合格不合格 REPAIR）→ 100% 触发"工序接续"，不接续即视为异常（系统告警）
            - **V1.3.5 新增**：客户业务现实"大部分都还要流转"→ 接续工序若为厂内 → 自动锁机台 + 预排工时；
              若为另一道委外 → 自动生成新委外单草稿（生管确认后下发）
          data_validation:
            - 工序号：必须是该工单未完成且未委外的工序
            - 厂商：必填，必须具备该工序能力（厂商档案中加工能力字段）
          error_handling:
            - 工序已委外重复提交 → "该工序已委外给 XX 厂商"
            - 厂商不具备该工序能力 → "该厂商无表处能力，请选其他厂商"
            - **V1.3.5 新增**：接续工序未在 24h 内被认领 → 系统红色提醒生管 + 推送"工序接续超时"
        - id: AC-5.6.2
          given: 工序委外切片 GD202606040001-P03 状态 = STORED 已入库（V1.3.5 不区分合格/不合格都触发接续）
          when: 系统检测到第 3 道完成（无论 QUALIFIED_STORAGE 还是 REPAIR_REQUESTED 都触发）
          then: |
            自动触发第 4 道「钳工」工序的"待开工"提醒；
            分配给已锁定的钳工机台 + 操作工；
            生管员看板上第 3 道进度条变绿、第 4 道变蓝（待开工）。
          business_rules:
            - 接续触发：上一道（无论厂内/厂外）完成即推下一道
            - **V1.3.5 强化**：即使是 REPAIR_REQUESTED（待返修），第 3 道不算"完成"，第 4 道接续**暂停**直到返修完成入库
            - 接续锁定：拖拽排产时可勾选"锁定后续机台"，避免委外回来无机台可用
            - 时间预估：第 4 道计划开始 = 第 3 道实际完成 + 1h 缓冲（可配置）
        - id: AC-5.6.3
          given: 生管员打开工单 GD202606040001 详情页
          when: 查看"工序进度"
          then: |
            一屏树形显示 5 道工序：
              工序 1 车 [厂内·已完成·王师傅·06/08 14:20]
              工序 2 CNC [厂内·已完成·李师傅·06/09 10:30]
              工序 3 表处 [厂外·A 表处厂·送货中·预计回流 06/12·LZ-GD001-P03]
              工序 4 钳工 [待开工·已锁 CNC-04·等第 3 道]
              工序 5 包装 [待开工·等第 4 道]
            点击工序 3 可下钻看委外 7 状态机详情 + 历史交期分位数（来自 E6-S7）。
          business_rules:
            - 视图统一：厂内/厂外工序同一进度条，颜色区分（厂内蓝 / 厂外橙 / 已完成绿 / 逾期红）
            - 下钻深度：工序详情 → 委外单 → 厂商 → 历史交期
            - **V1.3.5 强调**：客户业务现实"大部分还要流转"→ 进度条默认显示"下一道工序接续状态"（已锁机台 / 已生成新委外单 / 等待接续）
      provides_apis:
        - POST /api/v1/workorders/{id}/processes/{seq}/outsource-slice
        - GET  /api/v1/workorders/{id}/processes        # 工序进度树
        - POST /api/v1/workorders/{id}/processes/{seq}/lock-next-machine
        - GET  /api/v1/workorders/{id}/processes/{seq}/handoff
        - POST /api/v1/workorders/{id}/processes/{seq}/auto-continue   # V1.3.5 新增：强制接续下一道
        - GET  /api/v1/workorders/{id}/process-handoff-status          # V1.3.5 新增：接续状态查询
      consumes_apis:
        - GET /api/v1/workorders/{id}
        - GET /api/v1/vendors?capability=
        - GET /api/v1/outsource/history-price
      depends_on_stories: [E5-S1, E5-S2, E5-S4, E6-S5, E6-S7]
```

#### E6: 委外加工深化（完整 YAML）

```yaml
epic:
  id: E6
  title: 委外加工深化
  version: V1.1
  target_platform: [backend, web]
  why_exist: |
    WHY：CNC 加工厂 30%-50% 工序是委外（氧化/电镀/喷涂/热处理/三次元）。
         对账是最大的痛点：3 个月微信记录翻一遍。V1.1 让对账
         像网购退货一样简单。

         【V1.3.6 改版】E6 是我方视角的委外深化（下单/质检/对账/付款）；
         E12 是**面向厂外**的协同（V1.3.5 起无 APP 账号；**V1.3.6 改为邮件/短信 + 线下对账**）。
         二者形成完整链路：
           委外下单（E6/E5-S4 + **邮件/短信通知厂商 E12-V1.3.6**）
           → 仓管到货扫码（V1.3.5 E12-S2）→ 品质领料质检（V1.3.5 E12-S3）→ 委外入库 → 对账单生成（E6-S1）
           → 采购**邮件发对账单**（V1.3.6 E6-S1 AC-6.1.1）→ 采购**线下对账 + 厂商签字盖章**（V1.3.6 E6-S1 AC-6.1.2）
           → 采购**拍照上传签字扫描件**（V1.3.6 E6-S1 AC-6.1.3）→ 财务生成付款申请（V1.3.6 触发条件升级）→ 付款审批（E9-S3）。
  what_it_is:
    - **V1.3.6** 委外对账 + 月度对账单 PDF + 邮件推送厂商 + 线下签字 + 拍照上传
    - 工序委外 / 整单委外灵活切换
    - 自动调取委外历史价
    - 委外质检
  what_it_is_not:
    - **V1.3.6** 不是供应商 APP 端协同（本地部署，厂外无系统账号；通知走邮件/短信）
    - **V1.3.6** 不做线上对账（无任何线上对账入口；改为"线下 + 拍照上传"）
    - 不是 SRM（不做供应商评估全功能）
  reuse_analysis:
    can_reuse:
      - E5-S4 委外订单基础
      - E9 财务付款
      - E1-S1 用户/角色（"采购员" / "财务" / "生管" 复用）
    must_build:
      - 对账单 PDF 生成（自动汇总 WW- 单 + 含签字栏位）
      - 邮件推送（按 Nacos `app.outsub.notify-channel` 多渠道）
      - 签字扫描件上传（MinIO + AES-256 加密 + 5 年保留 + 下载审计）
      - 委外/自制切换
  depends_on_epics: [E5, E9]
  stories_count: 8   # V1.3.4 从 4 升至 7（新增 S5 7 状态机 / S6 返修闭环 / S7 历史交期预估）；V1.3.6 从 7 升至 8（S1 整章改版为"月度对账 + 线下 + 拍照上传" + 新增 S8 厂商资料完善）
  stories:
    - id: E6-S1
      title: 月度对账 + 厂商签字拍照上传（V1.3.6 整章改版）
      repository_type: backend+web
      estimated_complexity: L
      priority: P0
      why_first: |
        WHY：客户 2026-06-08 第四次反馈原话——
             "因为是**本地部署**，所以厂商没有微信操作这一部。
              通知厂商可选择**邮件推送/短信推送**，需要**采购线下拿对账单跟厂商确认**，
              然后将厂商**签字确认后拍照上传系统**流转至财务，其余流程不变。"
        V1.3.5 留下的"E6-S1 月度对账 + E12-S3 供应商线上对账"在本地部署下不可行：
        (1) 厂商是"厂外组织"，本地部署无公网入口，开放 APP 账号 / 微信链接 = 增加 IT 负担 + 安全风险；
        (2) 对账是**法律行为**，必须"白纸黑字 + 签字盖章"才合规——线上"勾选确认"在法务上不成立；
        (3) 厂商档案之前"邮箱可选"，现在"邮件推送"成为主渠道，必须升级为必填。
        V1.3.6 把"线上对账"拆为：
          · 系统动作（我方 PC 端）：生成 PDF → 邮件发送 → 状态置"待对账"
          · 物理动作（线下）：采购带纸去 → 厂商核对 → 厂商签字盖章
          · 系统动作（我方 PC 端）：采购拍照上传 → 状态置"对账已确认" → 通知财务
        V1.3.7 进一步简化：删除"采购带纸去厂商处"描述，**厂商线下流程不在 PRD 描述**——PRD 只管系统内动作；保留"签字扫描件上传系统"作为合规凭证，保留付款触发三条件。
        善良设计：让"对账的法律凭证"（签字扫描件）沉淀进系统，但不让厂外承担任何 IT 负担；让 PRD 描述边界更清晰——PRD 描述"我方系统做什么"，不描述"厂商线下怎么做"。
      acceptance_criteria:
        - id: AC-6.1.1
          title: 采购生成并发送对账单邮件
          given: 2026 年 5 月「A 表处厂」有 10 张已 STORED 的委外订单
          when: 采购员在 PC 端"委外对账"模块 → 选对账月份 = 2026-05 + 厂商 = A 表处厂 → 点"生成对账单" → 系统自动汇总 → 点"发送对账单邮件"
          then: |
            1. 生成对账单 PDF，含：厂商名称 / 对账月份 / 10 张 WW- 单明细 / 加工费合计 / 运杂费 / 应付合计 / **厂商签字栏位（空白）**
            2. 系统按 Nacos `app.outsub.notify-channel = ['email_163']` 配置（V1.3.7 单渠道，删除短信）：
               · **163 邮箱**：发送对账单 PDF 至厂商档案"默认对账邮箱"，主题「【××精密机械】2026 年 5 月对账单」
               · ~~短信：~~  V1.3.7 删除（不再发送 140 字短信）
            3. 对账单状态：「待对账」
            4. 记录邮件发送日志（时间 / 收件人 / 附件 hash / SMTP 响应 / 投递状态）—— V1.3.7 新增额度监控
            5. 状态机：委外订单 → 月度对账数据归集
          business_rules:
            - id: BR-6.1.1.1
              rule: "对账周期：月度（可配）"
            - id: BR-6.1.1.2
              rule: "对账维度：厂商 + 月份"
            - id: BR-6.1.1.3
              rule: "汇总范围：只汇总当月已 STORED 的委外订单"
            - id: BR-6.1.1.4
              rule: "对账单 PDF 必须含厂商签字栏位（空白）+ 我方采购员签字栏位（已签）+ 公司盖章（已盖）"
            - id: BR-6.1.1.5
              rule: "邮件发送失败自动重试 3 次（间隔 1h/6h/24h，XXL-JOB 调度）"
            - id: BR-6.1.1.6
              rule: "邮件发送失败后通知采购（短信 + PC 端红点）"
          data_validation:
            - 厂商：必填，引用厂商档案
            - 对账月份：必填，YYYY-MM 格式，不能为未来月份
            - 厂商"默认对账邮箱"：必填，校验邮箱格式
          error_handling:
            - scenario: "厂商档案无邮箱"
              code: "400"
              message: "厂商档案未填写默认对账邮箱，请先到厂商档案维护"
              action: "拒绝发送，提示维护"
            - scenario: "对账月份无已 STORED 订单"
              code: "404"
              message: "该月份该厂商无已入库委外订单"
              action: "拒绝生成"
            - scenario: "对账单已存在（重复生成）"
              code: "409"
              message: "该月份该厂商对账单已存在（编号 DZ202605-A001）"
              action: "提示查看现有对账单"
        - id: AC-6.1.2
          title: 采购上传厂商签字扫描件（V1.3.7 删除"线下"描述）
          given: 对账单 DZ202605-A001 状态 = 「待对账」，厂商已通过任意方式（微信 / 邮件 / 线下带回等）将签字盖章版对账单回传给采购（**厂商线下流程不在 PRD 描述**）
          when: 采购员在 PC 端"委外对账" → 找到该对账单 → 点「上传厂商签字版」 → 选择扫描件/照片文件 → 录入对账确认日期 → 点「提交对账」
          then: |
            1. 附件入库（MinIO + AES-256 加密，Key = `reconcile-signature/{reconcileId}/{filename}`）
            2. 状态「待对账」→「对账已确认」
            3. 记录对账确认日期 + 采购员 ID + 附件 hash + 上传时间
            4. 自动通知财务（PC 端"待生成付款"红点 + 短信「A 表处厂 5 月对账已确认，金额 8200 元，请生成付款申请」，可选）
            5. 自动通知生管（PC 端"该厂商本月对账完成"提示）
          business_rules:
            - id: BR-6.1.2.1
              rule: "签字扫描件作为对账法律凭证，必须 AES-256 加密 + MinIO 存储"
            - id: BR-6.1.2.2
              rule: "附件保留期 ≥ 5 年（财务合规）"
            - id: BR-6.1.2.3
              rule: "对账确认日期不能早于对账月最后一天（校验：signed_at >= last_day_of(reconcile_month)）"
            - id: BR-6.1.2.4
              rule: "允许格式：PDF / JPG / JPEG / PNG；最大 10MB"
            - id: BR-6.1.2.5
              rule: "采购员必须 = 该对账单创建人（避免越权上传）"
            - id: BR-6.1.2.6
              rule: "下载需总经理 / 财务总监权限（其他角色仅可预览）"
            - id: BR-6.1.2.7
              rule: "下载 / 预览触发审计日志（用户 / IP / 时间 / 操作类型）"
            - id: BR-6.1.2.8
              rule: "下载操作额外通知审计员（企业微信 webhook，可选）"
          data_validation:
            - field: "对账确认日期"
              type: "date"
              required: true
              rules: "不能早于对账月最后一天，不能晚于当前日期"
              error_message: "对账确认日期异常：必须 ≥ 对账月最后一天，且 ≤ 当前日期"
            - field: "签字扫描件"
              type: "file"
              required: true
              rules: "PDF/JPG/JPEG/PNG, ≤10MB, 至少 1 个文件"
              error_message: "请上传厂商签字盖章的对账单扫描件或照片"
          error_handling:
            - scenario: "文件格式不支持"
              code: "400"
              message: "仅支持 PDF/JPG/PNG 格式"
              action: "拒绝上传，提示重新选择"
            - scenario: "文件超过 10MB"
              code: "413"
              message: "文件过大，请压缩后重新上传"
              action: "拒绝上传"
            - scenario: "对账确认日期早于对账月最后一天"
              code: "400"
              message: "对账确认日期异常"
              action: "提示检查日期"
            - scenario: "状态非「待对账」"
              code: "409"
              message: "该对账单状态 = {state}，无法上传"
              action: "提示查看对账单详情"
        - id: AC-6.1.3
          title: 财务看到签字附件 + 生成付款申请（V1.3.6 触发条件升级）
          given: 对账单 DZ202605-A001 状态 = 「对账已确认」+ 签字附件已上传 + 双方对账金额一致
          when: 财务员在"付款管理"模块看到「对账已确认」红点 → 点「生成付款申请」
          then: |
            1. 校验：签字附件存在（BR-6.1.2.1）
            2. 校验：对账确认日期合法（BR-6.1.2.3）
            3. 校验：双方对账金额一致（采购提交金额 = 厂商签字版金额，差异 0 元）
            4. 生成付款单，关联对账单 + 厂商银行账户
            5. 状态「对账已确认」→「付款中」
            6. 走原有 >10万双签流程（>10万 总经理 + 财务总监双签 / ≤10万 财务总监单签）
          business_rules:
            - id: BR-6.1.3.1
              rule: "V1.3.6 触发条件升级：必须同时满足「对账已确认 + 签字附件已上传 + 双方对账金额一致」才可生成付款申请"
            - id: BR-6.1.3.2
              rule: "金额差异处理：若采购录入金额 ≠ 厂商签字版金额 → 弹差异确认框，由财务决定（接受差异 / 退回采购重对）"
            - id: BR-6.1.3.3
              rule: "单笔 > 10 万：总经理 + 财务总监双签"
            - id: BR-6.1.3.4
              rule: "单笔 ≤ 10 万：财务总监单签"
          error_handling:
            - scenario: "签字附件缺失"
              code: "400"
              message: "该对账单尚未上传厂商签字版，无法生成付款申请"
              action: "提示联系采购员"
            - scenario: "对账金额不一致"
              code: "409"
              message: "对账金额不一致：系统 {sys} 元 vs 厂商签字 {vendor} 元，差异 {diff} 元"
              action: "弹差异确认框：接受差异 / 退回采购重对"
            - scenario: "对账单状态非「对账已确认」"
              code: "409"
              message: "对账单状态 = {state}，未达付款前置条件"
              action: "提示查看对账单详情"
        - id: AC-6.1.4
          title: 签字扫描件存档与审计（V1.3.6 安全合规）
          given: 签字附件已上传（MinIO + AES-256）
          when: 审计员 / 财务总监 / 总经理 / 采购员 查询 / 预览 / 下载
          then: |
            1. PDF 内嵌预览（pdf.js） / JPG/PNG 图片显示
            2. 记录查询日志（用户、IP、时间、操作类型、附件 ID）
            3. 附件保留期 ≥ 5 年（财务合规 + 法律诉讼时效）
            4. 下载仅限总经理 / 财务总监 / 采购员（其他角色仅可预览）
          business_rules:
            - id: BR-6.1.4.1
              rule: "预览权限：财务、采购、生管、总经理、财务总监（只读，不可下载）"
            - id: BR-6.1.4.2
              rule: "下载权限：总经理、财务总监、采购员（对账单创建人）"
            - id: BR-6.1.4.3
              rule: "下载触发审计日志 + 企业微信通知审计员（可配）"
            - id: BR-6.1.4.4
              rule: "附件保留期 5 年（可配，Nacos `app.reconciliation.retention-years`）"
            - id: BR-6.1.4.5
              rule: "附件加密：AES-256-GCM（密钥由 KMS 托管，季度轮换）"
      provides_apis:
        - POST /api/v1/outsource/reconcile                           # 生成对账单
        - GET  /api/v1/outsource/reconcile?month=&vendorId=          # 对账单列表
        - GET  /api/v1/outsource/reconcile/{id}                      # 对账单详情（含附件预览）
        - POST /api/v1/outsource/reconcile/{id}/send-email           # 发送对账单邮件（V1.3.6 / V1.3.7 单 163 邮箱）
        - POST /api/v1/outsource/reconcile/{id}/upload-signature     # 上传厂商签字版（V1.3.6 / V1.3.7 维持）
        - GET  /api/v1/outsource/reconcile/{id}/signature-preview    # 预览签字扫描件（V1.3.6 / V1.3.7 维持）
        - GET  /api/v1/outsource/reconcile/{id}/signature-download   # 下载签字扫描件（V1.3.6 / V1.3.7 维持，限权限）
        - POST /api/v1/outsource/reconcile/{id}/payment              # 财务生成付款申请（V1.3.6 触发条件升级 / V1.3.7 维持）
        - GET  /api/v1/outsource/reconcile/templates/email           # 邮件模板管理（V1.3.6 / V1.3.7 维持）
        - PUT  /api/v1/outsource/reconcile/templates/email/{key}     # 更新邮件模板（V1.3.6 / V1.3.7 维持）
        - GET  /api/v1/outsource/reconcile/audit-log/{id}            # 审计日志查询（V1.3.6 / V1.3.7 维持）
        # ~~V1.3.7 删除短信模板管理 API：~~ GET /api/v1/outsource/reconcile/templates/sms
        # ~~V1.3.7 删除短信模板管理 API：~~ PUT /api/v1/outsource/reconcile/templates/sms/{key}
      consumes_apis:
        - GET /api/v1/outsource?month=&vendorId=&status=STORED       # 拉取已入库订单
        - GET /api/v1/vendors/{id}                                   # 厂商档案
        - POST /api/v1/notifications/email                           # 邮件发送（V1.3.7 单 163 邮箱渠道，删除 sms）
        # ~~V1.3.7 删除短信发送 API：~~ POST /api/v1/notifications/sms
        - POST /api/v1/storage/encrypt-upload                         # MinIO + AES-256 上传（V1.3.7 维持）
      depends_on_stories: [E5-S4, E9-S3, E1-S3]
      email_templates:  # V1.3.6 邮件模板
        outsource_order:
          subject: "【××精密机械】新委外订单 {outsourceId}"
          body: |
            ×× 公司（厂商名）：

            我司有一笔新的委外加工订单，请查收详情：

            委外单号：{outsourceId}
            工序：第 {processSeq} 道 {processName}
            数量：{quantity} 件
            单价：{unitPrice} 元 / 件
            总金额：{totalAmount} 元
            计划交期：{deliveryDate}

            附件：委外订单详情 PDF

            如有疑问请联系：
            采购员 {buyerName}
            电话：{buyerPhone}
            邮箱：{buyerEmail}

            ××精密机械 ERP 系统
        monthly_reconcile:
          subject: "【××精密机械】{reconcileMonth} 对账单"
          body: |
            ×× 公司：

            附件为我司 {reconcileMonth} 月度对账单，请核对：
            加工费合计：{processAmount} 元
            运杂费：{shippingFee} 元
            应付合计：{totalAmount} 元
            委外单明细：{orderCount} 张（详见附件 PDF）

            核对无误请签字盖章后回传我方采购员。
            如有疑问请联系采购员 {buyerName}（{buyerPhone}）。

            ××精密机械 ERP 系统
      # ~~V1.3.7 删除短信模板管理模块：~~  sms_templates 字段整段删除（短信渠道已下线）

    - id: E6-S2
      title: 工序/整单委外灵活切换
      repository_type: backend
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-6.2.1
          given: 工单 GD202606040001 原计划"全部自加工"
          when: 生管员选择第 3 道工序 → 委外
          then: |
            工单工序 3 标记为"委外"；
            自动创建委外订单（关联工序 3）；
            工序 1/2/4 仍自加工。
          business_rules:
            - 工序级委外：单工序可单独委外
            - 整单委外：整张工单委外
            - 切换留痕：记录切换人/时间/原因
      provides_apis:
        - POST /api/v1/workorders/{id}/process/{seq}/outsource
        - POST /api/v1/workorders/{id}/outsource-all
      consumes_apis:
        - GET /api/v1/workorders/{id}
      depends_on_stories: [E5-S1]

    - id: E6-S3
      title: 自动调取委外历史价
      repository_type: backend
      estimated_complexity: S
      priority: P1
      acceptance_criteria:
        - id: AC-6.3.1
          given: 厂商「A 表处厂」工序「氧化」历史 5 次价格：8/8.5/8/8.2/8
          when: 生管员选择该厂商 + 工序 + 数量 1000
          then: |
            自动带出建议价 = 8 元（中位数）；
            实际录入可改，差异 > 10% 提示。
          business_rules:
            - 历史价范围：最近 3 次成交价
            - 中位数计算
            - 差异 > 10% 提示，但不强制
      provides_apis:
        - GET /api/v1/outsource/history-price?vendorId=&processName=
      consumes_apis: []
      depends_on_stories: [E5-S4]

    - id: E6-S4
      title: 委外质检
      repository_type: backend+web
      estimated_complexity: S
      priority: P1
      acceptance_criteria:
        - id: AC-6.4.1
          given: 委外订单 WW202606040001 已入库
          when: 仓管员扫码入库
          then: |
            自动生成委外质检任务；
            推送品检员。
          business_rules:
            - 委外入库 = 必检
            - 不合格走不良品流程
            - **【V1.3.4】返修品复检**：若该委外单 `is_rework_reinspection = true`，质检任务标题前缀"[返修复检]"，自动分配资深品检员
      provides_apis:
        - POST /api/v1/outsource/{id}/inspect
      consumes_apis:
        - GET /api/v1/outsource/{id}
      depends_on_stories: [E5-S4, E7-S1]

    - id: E6-S5
      title: 委外订单 7 状态机（V1.3.4 新增）
      repository_type: backend+web+android
      estimated_complexity: L
      priority: P0
      why_first: |
        WHY：客户原话："送货员到工厂打开 APP 扫码 → 状态变待检 → 通知品管"。
             原 PRD 委外订单状态机只有"草稿/已审/已出库/委外加工中/已入库/已对账/已关闭"，
             无法承载"送货中/扫码到达/待检/质检中/合格入库/退货维修/已通知返修"等关键节点。
             本 Story 用 7 主状态机 + 转换矩阵 + 通知规则一次性收口，
             把"3 个月翻微信记录对账"变为"扫一下、推一下、走一段"。
             傻瓜测试：送货员奶奶能学会（扫一下区域码 → 弹确认 → 按提交）。
      acceptance_criteria:
        - id: AC-6.5.1
          given: 委外订单 WW202606040001 已审（生管下单）
          when: 系统自动创建送货单 + 通知供应商 APP
          then: |
            状态 = PENDING_SHIP 待发货；
            供应商 APP 收到"待接单"任务；
            供应商点击"接单" → 状态变 SHIPPING 送货中（启动送货员任务）。
          business_rules:
            - 主状态枚举（7 主态 + 1 衍生态）：
              · PENDING_SHIP 待发货（下单 → 厂商待接单）
              · SHIPPING 送货中（厂商已接单 → 送货员出发到厂）
              · PENDING_INSPECTION 待检（扫区域码到达待检区）
              · INSPECTING 质检中（生管安排质检）
              · QUALIFIED_STORAGE 待入库（质检合格 → 待仓管扫货位）
              · STORED 已入库（仓管扫码货位 + 录入良品数）
              · REPAIR_REQUESTED 待返修（质检不合格 → 品管填不良原因）
              · NOTIFIED_REPAIR 已通知返修（采购生成返修单 → 通知厂商拉回 / 现场维修）
            - 转换矩阵详见附录 B-2
            - 状态变更必须有触发者（人 / 扫码事件 / 定时任务）+ 留痕
          state_transitions:
            - from: PENDING_SHIP, to: SHIPPING, trigger: 厂商电话/系统外接单 + 仓管代录 (V1.3.5 收回送货员 APP，详见 E12-S1)
            - from: SHIPPING, to: PENDING_INSPECTION, trigger: **仓管员在 APP "到货扫码"扫委外单码 (V1.3.5 新流程，E12-S2)**
            - from: PENDING_INSPECTION, to: INSPECTING, trigger: **品质员去仓库领料 (V1.3.5 简化，E12-S3)；不再走"生管排程"**
            - from: INSPECTING, to: QUALIFIED_STORAGE, trigger: 品管录入"合格"
            - from: INSPECTING, to: REPAIR_REQUESTED, trigger: 品管录入"不合格 + 不良原因 + 处置=退回维修"
            - from: QUALIFIED_STORAGE, to: STORED, trigger: 仓管扫物料码 (WL-) 选库位 + 录良品数 (V1.3.5 收回 AREA-STORAGE)
            - from: REPAIR_REQUESTED, to: NOTIFIED_REPAIR, trigger: 采购生成返修单 (E6-S6)
            - from: NOTIFIED_REPAIR, to: SHIPPING, trigger: 厂商再送货 (is_rework_reinspection=true)
        - id: AC-6.5.2
          given: 仓管员「王仓管」收到「A 表处厂」送来的委外单 WW202606040001
          when: APP 打开"仓储 → 到货扫码"扫送货单上的委外订单二维码
          then: |
            APP 弹出"已到达一号仓 待检区，委外单 WW202606040001（A 表处厂，料号 P001，数量 100），是否确认到货？"；
            点确认 → 委外单状态 SHIPPING → PENDING_INSPECTION；
            自动通知工厂品管员（APP 推送 + 企业微信，"请去仓库领料"）+ 生管员（PC 红点 + APP 推送）。
          business_rules:
            - 扫码权限（V1.3.5）：仓管员必须 = 该委外单的主/副仓管（E12-S1 AC-12.1.1）；**不再校验 vendor_id**（送货员角色已删除）
            - 通知对象（V1.3.5）：**生管 + 品质**（原"送货员 → 品管 + 生管"措辞已收回）
            - 通知通道：APP 推送 + 企业微信 + PC 端红点；可在系统参数关闭部分通道
            - 时间记录：到达时间 = 扫码时间，写入委外单 `arrived_at` 字段（用于供应商交期统计）
            - 离线扫码：复用 E1-S4 离线缓存策略（500 条上限）
        - id: AC-6.5.3
          given: 品管员对委外单 WW202606040001 录入质检结果
          when: 不合格 10 件，不良原因 = "尺寸超差"，处置 = 退回维修
          then: |
            状态 INSPECTING → REPAIR_REQUESTED；
            自动通知采购员"请生成返修单"；
            进入 E6-S6 返修闭环流程；
            质检报告自动归档（PDF）。
          business_rules:
            - 不良原因枚举：尺寸超差 / 外观划伤 / 材质不符 / 数量短缺 / 其他（自定义）
            - 处置方式：退回维修 / 让步接收 / 报废
            - 让步接收：状态直接跳到 STORED，但标记"让步"+ 记入供应商质量档案（E8 P2）
            - 报废：状态跳到 STORED，良品数 = 0，记入成本（E9）
        - id: AC-6.5.4
          given: 仓管员对质检合格的委外单 WW202606040001 录入入库
          when: APP 扫物料码 WL-P001-20260604-0001 + 选库位 + 录入良品数 = 90 件
          then: |
            状态 QUALIFIED_STORAGE → STORED；
            库存 +90；
            自动触发 E5-S6 的下一道工序接续通知（若是工序委外）；
            自动触发 E6-S1 月度对账数据归集。
          business_rules:
            - 良品数 ≤ 委外发出数量
            - 入库货位必须扫码（物料码或库位码均可，不允许手输库位）
            - **V1.3.5 收回**：原"扫 AREA-STORAGE 区域码"改为"扫物料码 WL- 选库位"（V1.3.4 引入的区域码删除）
            - 若是工序委外切片，状态变更同步刷新工单工序进度树（E5-S6 AC-5.6.3）
      provides_apis:
        - GET  /api/v1/outsource/{id}/state-machine     # 当前状态 + 可用转换
        - POST /api/v1/outsource/{id}/transit           # 状态转换（带 trigger 校验）
        - GET  /api/v1/outsource/state-matrix           # 全量转换矩阵（仅管理员）
        - POST /api/v1/outsource/{id}/quality-result    # 品管录入结果
        - POST /api/v1/outsource/{id}/inbound           # 仓管扫码入库
      consumes_apis:
        - POST /api/v1/app/outsource/arrival-scan       # V1.3.5 仓管到货扫码触发
        - POST /api/v1/app/messages                     # 通知品管 / 生管 / 仓管
      depends_on_stories: [E5-S4, E12-S1, E12-S2]

    - id: E6-S6
      title: 返修闭环 + 次数预警（V1.3.4 新增）
      repository_type: backend+web+android
      estimated_complexity: M
      priority: P0
      why_first: |
        WHY：客户原话："返修次数 ≥ 2 → 自动触发预警给高层"。
             返修是委外质量的"血压计"——同一供应商同一委外单返修 2 次以上，
             不是偶然，是供应商能力或我方图纸有问题。
             必须在第 2 次发生时立刻把信号推到高层，避免"老板月底才知道这家厂老返修"。
             善良设计：预警推给高层 + 采购 + 生管，不压一线品管。
      acceptance_criteria:
        - id: AC-6.6.1
          given: 委外单 WW202606040001 状态 = REPAIR_REQUESTED
          when: 采购员点击"生成返修单"
          then: |
            创建返修单 RW202606040001，关联原委外单 `original_outsub_order_id = WW202606040001`；
            原委外单 `rework_count` 自增 1；
            状态 REPAIR_REQUESTED → NOTIFIED_REPAIR；
            通知厂商"请拉回维修 / 派人现场维修"（**邮件 + 短信，V1.3.6 取代 V1.3.5 的"APP + 企业微信"——V1.3.6 厂商无系统账号，渠道统一走邮件/短信**）。
          business_rules:
            - 返修单字段：原委外单 / 不良数 / 不良原因 / 维修方式（拉回 / 现场）/ 期望返回时间
            - 维修方式 = 拉回：厂商派人来运回，过程同送货流程逆向
            - 维修方式 = 现场：厂商派工程师来厂维修，**V1.3.6 不再"复用 AREA-"**——AREA- 区域码 V1.3.5 已删，**改为"工程师到厂后由我方仓管现场登记"**
            - 返修单状态：已通知 → 维修中 → 已返厂 → 待复检（衔接 SHIPPING）
        - id: AC-6.6.2
          given: 委外单 WW202606040001 已有 rework_count = 1
          when: 第 2 次质检又不合格 → 采购员生成第 2 张返修单 → rework_count 变为 2
          then: |
            **触发预警（≥ 阈值 2）**：
              · APP 推送：高层（总经理 / 生产副总）+ 采购主管 + 生管主管
              · PC 端：委外面板（E11-S4）该单标记深红 + 顶部预警条
              · 短信：通过企业微信 webhook 发送（系统参数可关闭）
            预警内容："厂商 A 表处厂 委外单 WW20260604001 已返修 2 次（阈值 2），请关注"
            点预警 → 直达委外单详情 + 历史返修记录 + 厂商所有未结订单。
          business_rules:
            - 阈值参数 `app.outsub.rework-alert-threshold`：默认 2，Nacos 热更新，1-10 整数
            - 同一委外单同一预警 24h 内不重复推（去重）
            - 短信通道：默认走企业微信群机器人 webhook，可在系统参数切换（钉钉 / 飞书 / 实际 SMS）
            - 高层接收对象：从角色"高层"中取所有用户（不硬编码姓名）
        - id: AC-6.6.3
          given: 厂商「A 表处厂」最近 90 天返修单 ≥ 5 张
          when: 系统每日 09:00 自动扫描
          then: |
            生成"厂商高返修预警"，推送采购主管 + 总经理；
            建议"启动厂商整改谈话"流程；
            该厂商档案标记橙色（E8 供应商绩效 P2 范围）。
          business_rules:
            - 厂商级阈值：默认 90 天 ≥ 5 张，可在系统参数调
            - 推送对象：采购主管 + 总经理；不推一线
            - 厂商档案橙色：直到该厂商连续 30 天无新返修自动恢复
      provides_apis:
        - POST /api/v1/outsource/{id}/rework            # 生成返修单
        - GET  /api/v1/outsource/{id}/rework-history    # 该委外单返修历史
        - GET  /api/v1/outsource/rework-alerts          # 预警列表
        - POST /api/v1/outsource/rework-alerts/{id}/ack # 高层确认已处理
        - PUT  /api/v1/outsource/rework-alert-config    # 调阈值
      consumes_apis:
        - POST /api/v1/app/messages
        - POST /api/v1/notifications/sms                # webhook SMS
      depends_on_stories: [E6-S5, E1-S3]

    - id: E6-S7
      title: 委外历史交期预估（V1.3.4 新增）
      repository_type: backend+web
      estimated_complexity: S
      priority: P1
      why_first: |
        WHY：生管下委外单时最常问"这家厂这次能几天回来"。
             答案不该靠经验，应该靠数据。本 Story 复用 E3-S3 历史工艺库的思路，
             用最近 3 次交期的中位数 + 50%/80%/100% 分位数给出"既乐观也保守"的预估。
             把"猜"变成"算"，让生管排产更靠谱。
      acceptance_criteria:
        - id: AC-6.7.1
          given: 厂商「A 表处厂」工序「氧化」最近 3 次委外交期分别为 5 / 6 / 7 天
          when: 生管下新委外单
          then: |
            预估完成时间 = 下单日 + 6 天（中位数）；
            页面同时显示分位数提示：
              · 50% 分位（中位）= 6 天
              · 80% 分位 = 6.6 天
              · 100% 分位（最坏）= 7 天
            生管可参考分位数选择"保守交期"或"乐观交期"。
          business_rules:
            - 历史范围：最近 3 次成交（同 E6-S3 历史价范围一致）
            - 历史样本不足 3 次：显示"历史样本不足，使用厂商档案默认交期"
            - 计算公式：`estimated_completion = order_date + median(recent_3_orders.delivery_days)`
            - 分位数计算：标准百分位数算法（线性插值）
            - 节假日处理：仅算工作日（与企业日历联动）
        - id: AC-6.7.2
          given: 委外单 WW202606040001 已下单，预估 6/12 完成
          when: 实际完成日 = 6/15（延误 3 天）
          then: |
            写入历史样本表，下次计算该厂商该工序时纳入；
            若延误 > 50% 分位 → 厂商档案"按期率"指标 -1；
            按期率 < 80% → 厂商档案标黄警告（采购员可见）。
          business_rules:
            - 按期率统计窗口：最近 12 个月（可配置）
            - 延误判定：实际完成日 > 计划完成日 1 天即算延误
            - 按期率 < 80% 的厂商在新下单时弹"该厂商按期率较低，请谨慎"提示
      provides_apis:
        - GET /api/v1/outsource/estimated-delivery?vendorId=&processName=&orderDate=
        - GET /api/v1/vendors/{id}/delivery-stats?processName=&window=12m
        - PUT /api/v1/vendors/{id}/delivery-sample      # 写入新样本（系统自动调用）
      consumes_apis:
        - GET /api/v1/outsource?vendorId=&processName=&limit=3
      depends_on_stories: [E5-S4]

    - id: E6-S8
      title: 厂商资料完善（V1.3.6 新增 / V1.3.7 改版：电话改选填 + 通知偏好固定 163 邮箱）
      repository_type: backend+web
      estimated_complexity: S
      priority: P0
      why_first: |
        WHY：V1.3.6 客户原话"通知厂商可选择**邮件推送/短信推送**"——
             V1.3.7 客户原话"通知厂商改为 **163 邮箱邮件通知**"——
             通知渠道由"邮件+短信多选"收敛为"163 邮箱单渠道"，厂商资料字段随之调整：
             · 邮箱 = 163 邮箱接收（含对账单 PDF 附件）的唯一入口，**V1.3.6 必填 → V1.3.7 维持必填**
             · 电话 = ~~短信接收的入口~~（V1.3.7 不再需要），**V1.3.6 必填 → V1.3.7 改选填**
             · 通知偏好 ~~['email', 'sms'] 多选~~ → V1.3.7 固定为 ['email_163']
             · 默认对账邮箱 = 部分厂商"业务联系邮箱"与"财务对账邮箱"不同，V1.3.7 维持
        善良设计：把"对账凭证投递到正确的人"，通过系统约束保证（邮箱必填 + 校验 + 默认值带出）；
                    删掉"电话必填"约束（V1.3.6 短信渠道下线后，电话不再是通知关键路径）。
      acceptance_criteria:
        - id: AC-6.8.1
          given: 采购员在"厂商档案"新增 / 编辑厂商
          when: 保存厂商资料
          then: |
            1. 邮箱字段：V1.3.6 升级为**必填** → V1.3.7 维持必填（Nacos `app.outsub.vendor.require-email = true`）
            2. 校验邮箱格式（标准 RFC 5322 简化校验）
            3. 通知偏好字段：V1.3.6 多选 ['email', 'sms'] → V1.3.7 **固定为 ['email_163']**（单值）
            4. 默认对账邮箱字段：V1.3.6 新增 → V1.3.7 维持（可与厂商邮箱不同）
            5. 电话字段：V1.3.6 必填 → V1.3.7 **改选填**（Nacos `app.outsub.vendor.require-phone = false`）
          business_rules:
            - id: BR-6.8.1.1
              rule: "V1.3.5 已存在厂商数据迁移：邮箱为空时，下一次保存触发"邮箱必填"提示（不强制阻塞旧数据）"
            - id: BR-6.8.1.2
              rule: "V1.3.7 新创建厂商：邮箱必填，电话选填；电话为空的厂商不会收到任何电话通知，但 163 邮箱渠道仍正常推送"
            - id: BR-6.8.1.3
              rule: "默认对账邮箱为空时，发送对账单邮件时取厂商邮箱 + 提示"建议填写独立对账邮箱""
            - id: BR-6.8.1.4
              rule: "通知偏好 = ['email_163'] 固定值，UI 不再提供"通知偏好"勾选框（V1.3.6 改版）"
            - id: BR-6.8.1.5
              rule: "V1.3.7：数据迁移时若厂商 phone 为空 + email 已有 → 标黄"电话已选填，建议补充"+ 不阻塞业务"
          data_validation:
            - 厂商名称：必填，1-100 字符
            - 联系人：必填，1-50 字符
            - 联系电话：**V1.3.7 选填**，11 位手机号格式校验（如填写）
            - 邮箱：必填，RFC 5322 简化校验（必填生效日 = V1.3.6 上线日）
            - 默认对账邮箱：可选，邮箱格式校验；为空时回退到厂商邮箱
            - 通知偏好：固定值 ['email_163']，V1.3.7 不允许编辑
            - 加工能力分类：必填
            - 银行账户：必填
            - 地址：必填（送货导航用）
          error_handling:
            - 邮箱格式错误 → "邮箱格式错误，请检查"
            - 邮箱为空（V1.3.6 上线后新厂商）→ "V1.3.6 起，邮箱为必填项（用于接收对账单）"
            - 联系电话格式错误（如填写）→ "联系电话格式错误，应为 11 位手机号"
        - id: AC-6.8.2
          given: 厂商档案已包含邮箱 / 电话（可选）/ 默认对账邮箱
          when: 采购员在"厂商档案"列表搜索
          then: |
            1. 列表显示：厂商名称 / 联系人 / 联系电话（选填）/ 邮箱 / 默认对账邮箱 / 合作状态
            2. 邮箱缺失的旧厂商标黄"V1.3.6 起必填"提醒
            3. ~~V1.3.7 删除~~：不再提供"按通知偏好过滤"（通知偏好已固定为 163 邮箱，无过滤价值）
          business_rules:
            - 权限：采购员 / 采购主管可见全部；财务可见"默认对账邮箱"；其他角色不可见邮箱（防泄漏）
            - 邮箱字段脱敏：列表显示 `wang***@xx.com`（仅前 3 + 后缀）
      provides_apis:
        - POST /api/v1/vendors                          # 新增厂商
        - GET  /api/v1/vendors                          # 厂商列表
        - GET  /api/v1/vendors/{id}                     # 厂商详情
        - PUT  /api/v1/vendors/{id}                     # 更新厂商（V1.3.7 强制校验邮箱必填、电话选填）
      consumes_apis: []
      depends_on_stories: [E5-S4]
      data_model:  # V1.3.7 厂商资料实体
        vendor:
          - vendor_code           # 厂商编码（自动生成 VS+年月日+3 位流水）
          - vendor_name           # 厂商名称
          - contact_person        # 联系人
          - contact_phone         # 联系电话（V1.3.7 改选填，短信渠道已下线）
          - contact_email         # 邮箱（V1.3.6 必填 → V1.3.7 维持必填）
          - default_reconcile_email  # 默认对账邮箱（V1.3.6 新增 / V1.3.7 维持，可选）
          - notify_channel        # 通知偏好 固定为 ['email_163']（V1.3.7 改单值）
          - process_capability    # 加工能力分类
          - bank_account          # 银行账户
          - address               # 地址
          - created_at            # 创建时间
          - cooperation_status    # 合作状态
          - email_receipt_enabled # V1.3.7 新增：邮件回执确认（v1.0 暂不启用，v1.1 评估）
```

#### E7: 品质管控（完整 YAML）

```yaml
epic:
  id: E7
  title: 品质管控
  version: V1.1
  target_platform: [backend, web, android]
  why_exist: |
    WHY：CNC 加工的"质量"是工厂的命脉。一次批量报废 = 几十万损失。
         本 Epic 把"来料检、过程检、成品检、FA 首件、三次元"
         全部数字化，质量问题可追溯到人、机、料。
  what_it_is:
    - 来料/过程/成品检验
    - FA 首件确认
    - 三次元检测
    - 不良品处理（返工/报废/让步接收）
  what_it_is_not:
    - 不是 QMS 全套（不做 CAPA、8D 报告）
    - 不是 SPC（不做统计过程控制）
    - 不是实验室管理（LIMS）
  reuse_analysis:
    can_reuse:
      - ECharts 做检验数据可视化
      - pdf.js 做检测报告预览
    must_build:
      - 检验方案配置
      - 不良品处理流程
  depends_on_epics: [E5]
  stories_count: 4
  stories:
    - id: E7-S1
      title: 来料/过程/成品检验
      repository_type: backend+web
      estimated_complexity: L
      priority: P1
      acceptance_criteria:
        - id: AC-7.1.1
          given: 物料 YL45D80 检验方案：抽检 10%，检验项目=直径/长度/外观
          when: 仓管员入库时点"生成来料检单"
          then: |
            抽检数量 = 入库数量 × 10%；
            品检员 APP 收到任务；
            检验项目逐项录入：合格/不合格。
          business_rules:
            - 检验类型：抽检/全检/免检
            - 抽检比例可按物料配置
            - 检验项目：尺寸/外观/硬度/材质等
            - 公差范围：可配置上下限
        - id: AC-7.1.2
          given: 关键工序 CNC 完成后
          when: 系统自动触发过程检
          then: |
            品检员收到 APP 推送；
            巡检录入检验结果。
          business_rules:
            - 关键工序定义在工艺路线
            - 巡检可手动触发
        - id: AC-7.1.3
          given: 工单 GD202606040001 已报工
          when: 生管员点击"成品入库"
          then: |
            必须先过成品检；
            合格 → 入库；不合格 → 不良品。
          business_rules:
            - 成品检：必检
            - 合格标准：检验方案全部项目通过
      provides_apis:
        - POST /api/v1/inspections
        - GET  /api/v1/inspections?type=&status=
        - POST /api/v1/inspections/{id}/result
        - GET  /api/v1/inspection-schemes
        - POST /api/v1/inspection-schemes
      consumes_apis:
        - GET /api/v1/materials/{id}
        - GET /api/v1/workorders/{id}
      depends_on_stories: [E5-S2]

    - id: E7-S2
      title: FA 首件
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-7.2.1
          given: 订单勾选"是否 FA = 是"
          when: 工单首次报工（首件）
          then: |
            自动生成 FA 检验单；
            品检 + 工程师双签确认后，才允许批量生产。
          business_rules:
            - FA 检验：必检
            - 签字：品检员 + 工程师
            - 不通过：不能继续报工
      provides_apis:
        - POST /api/v1/inspections/fa
        - POST /api/v1/inspections/fa/{id}/sign
      consumes_apis:
        - GET /api/v1/workorders/{id}
      depends_on_stories: [E7-S1]

    - id: E7-S3
      title: 三次元检测
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-7.3.1
          given: 工单 GD202606040001 需要三次元检测
          when: 生管员指派三次元检测任务 → 选品检员 → 上传检测报告 PDF
          then: |
            检测记录保存；
            关联工单，检测结果=合格/不合格/让步接收。
          business_rules:
            - 检测报告：PDF/Excel，≤ 20MB
            - 检测项目：可自定义
            - 检测结果：决定工单是否能继续
      provides_apis:
        - POST /api/v1/inspections/3d
        - POST /api/v1/inspections/3d/{id}/report
      consumes_apis:
        - POST /api/v1/files/upload
      depends_on_stories: [E7-S1]

    - id: E7-S4
      title: 不良品处理
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-7.4.1
          given: 检验结果 = 不合格，10 件
          when: 品检员选择"返工"
          then: |
            不良品记录生成；
            自动生成返工工单（数量=10）；
            原工单减扣 10 件。
          business_rules:
            - 处理方式：返工/报废/让步接收
            - 原因分类：材料/工艺/设备/人为
            - 返工：自动生成新工单
            - 报废：扣减库存 + 记成本
      provides_apis:
        - POST /api/v1/defects
        - GET  /api/v1/defects?status=&reason=
        - POST /api/v1/defects/{id}/rework
        - POST /api/v1/defects/{id}/scrap
      consumes_apis:
        - GET /api/v1/workorders/{id}
      depends_on_stories: [E7-S1]
```

#### E8: 采购管理（完整 YAML）

```yaml
epic:
  id: E8
  title: 采购管理
  version: V1.1
  target_platform: [backend, web, android]
  why_exist: |
    WHY：采购成本占 CNC 加工厂 60% 以上。采购员"拍脑袋定价"是行业常态。
         V1.1 让询价、比价、限价、对账全流程透明。
  what_it_is:
    - 询价比价（多供应商对比）
    - 价格控制（限价规则）
    - 到货提醒
    - 来料质检
  what_it_is_not:
    - 不是 SRM 全套（不做供应商评估/绩效全功能）
    - 不是电子采购平台（不做在线招投标）
  reuse_analysis:
    can_reuse:
      - E1-S2 审批工作流
      - E7-S1 来料检验
    must_build:
      - 价格控制规则
      - 到货提醒
  depends_on_epics: [E1, E7]
  stories_count: 4
  stories:
    - id: E8-S1
      title: 询价比价
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-8.1.1
          given: 采购需求 YL45D80 100kg
          when: 采购员选 3 家供应商发送询价
          then: |
            3 家供应商收到询价（邮件 / 微信 / 系统内）；
            收到报价后系统自动对比：最低/最高/均价。
          business_rules:
            - 询价单：物料 + 数量 + 期望交期
            - 供应商数 ≥ 2
            - 自动对比：最低/最高/均价
            - 议价记录：可记录议价过程
      provides_apis:
        - POST /api/v1/rfqs
        - GET  /api/v1/rfqs/{id}
        - POST /api/v1/rfqs/{id}/quotes
        - GET  /api/v1/rfqs/{id}/compare
      consumes_apis:
        - GET /api/v1/vendors
      depends_on_stories: [E1-S1]

    - id: E8-S2
      title: 价格控制
      repository_type: backend
      estimated_complexity: S
      priority: P1
      acceptance_criteria:
        - id: AC-8.2.1
          given: 物料 YL45D80 限价 = 10 元/kg
          when: 采购员录入采购单价 = 12 元/kg
          then: |
            系统提示"高于限价 20%，需采购主管审批"。
          business_rules:
            - 限价规则：物料维度
            - 高于限价：需主管审批
            - 审批阈值：< 1万采购员、1-5万采购主管、> 5万总经理
        - id: AC-8.2.2
          given: 采购订单 8000 元
          when: 采购员提交
          then: |
            状态=「待审」；
            路由到采购员自审（< 1 万）。
      provides_apis:
        - PUT  /api/v1/materials/{id}/price-limit
        - POST /api/v1/purchase-orders
        - POST /api/v1/purchase-orders/{id}/submit
      consumes_apis:
        - GET /api/v1/materials/{id}
      depends_on_stories: [E1-S2]

    - id: E8-S3
      title: 到货提醒
      repository_type: backend
      estimated_complexity: S
      priority: P1
      acceptance_criteria:
        - id: AC-8.3.1
          given: 采购订单约定交期 = 2026-06-10
          when: 当前日期 = 2026-06-07
          then: |
            系统提醒：3 天后到期，物料 YL45D80 100kg。
          business_rules:
            - 提醒时间点：到期前 3 天、当天、逾期 1/3/7 天
            - 提醒对象：采购员 + 仓管员
            - 提醒方式：APP + 企业微信
      provides_apis:
        - GET /api/v1/purchase-orders/incoming
        - GET /api/v1/purchase-orders/overdue
      consumes_apis:
        - GET /api/v1/purchase-orders
      depends_on_stories: [E8-S2]

    - id: E8-S4
      title: 来料质检
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-8.4.1
          given: 物料 YL45D80 质检方案 = 抽检 10%
          when: 采购入库 100kg
          then: |
            自动生成 10 件来料检单；
            品检员检验后录入结果；
            合格入库/不合格退货/让步接收。
          business_rules:
            - 质检方案：抽检/全检/免检
            - 批量生成：每张采购订单一张
            - 检验后：合格入库 / 不合格退货 / 让步接收
      provides_apis:
        - POST /api/v1/inspections/incoming
        - GET  /api/v1/inspections/incoming/{id}
      consumes_apis:
        - POST /api/v1/stock/in
      depends_on_stories: [E7-S1, E8-S2]
```

#### E9: 财务与成本（完整 YAML）

```yaml
epic:
  id: E9
  title: 财务与成本
  version: V1.1
  target_platform: [backend, web]
  why_exist: |
    WHY：财务最痛的是"业务数据和财务数据对不上"。
         V1.1 让销售订单自动转应收、采购订单自动转应付、工单自动算成本。
  what_it_is:
    - 应收应付
    - 工单成本核算
    - 回款控制
    - 利润分析
  what_it_is_not:
    - 不是 ERP 财务全模块（不做总账/凭证/税务申报）
    - 不是 BI 分析平台（不做多维 OLAP）
  reuse_analysis:
    can_reuse:
      - E2 订单/合同
      - E5 报工
    must_build:
      - 工单成本归集引擎
      - 利润分析
  depends_on_epics: [E2, E5]
  stories_count: 5   # V1.3.4 从 4 升至 5（新增 S5 料号成本聚合）
  stories:
    - id: E9-S1
      title: 应收应付
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-9.1.1
          given: 销售订单 XS202606040001 金额 300000
          when: 订单状态=「已发货」
          then: |
            自动生成应收账款；
            应收金额 = 订单金额 - 已收款。
          business_rules:
            - 应收生成时机：订单发货后
            - 应收金额 = 订单金额 - 已收款
            - 账期：合同约定（默认 30 天）
        - id: AC-9.1.2
          given: 应收账款 100000 元，账期 30 天
          when: 当前日期 - 订单发货日期 = 35 天
          then: |
            账龄 = 35 天，归入「30-60 天」区间；
            财务 APP 收到提醒。
          business_rules:
            - 账龄区间：0-30/30-60/60-90/90+
            - 提醒：30/60/90 天
      provides_apis:
        - GET  /api/v1/finance/receivables
        - GET  /api/v1/finance/payables
        - GET  /api/v1/finance/aging
      consumes_apis:
        - GET /api/v1/orders
        - GET /api/v1/purchase-orders
      depends_on_stories: [E2-S3, E8-S2]

    - id: E9-S2
      title: 成本核算
      repository_type: backend
      estimated_complexity: L
      priority: P1
      acceptance_criteria:
        - id: AC-9.2.1
          given: 工单 GD202606040001 已报工 100 件
          when: 月底点击"成本核算"
          then: |
            材料成本 = Σ(物料领用量 × 单价)；
            人工成本 = 合格件 × 单价 + 工时补差；
            制造费用 = 工时 × 制造费率；
            委外费用 = Σ(委外订单金额)；
            总成本 = 材料 + 人工 + 制造 + 委外。
          business_rules:
            - 计件工资 = 合格件 × 单价 + 工时补差
            - 工时补差 = (计时工时 - 标准工时) × 时薪
            - 制造费用按工时比例分摊
            - 成本归集到工单
      provides_apis:
        - GET  /api/v1/workorders/{id}/cost
        - POST /api/v1/cost/calculate
        - GET  /api/v1/cost/{workorderId}
      consumes_apis:
        - GET /api/v1/workorders/{id}
        - GET /api/v1/stock/out?workorderId=
      depends_on_stories: [E5-S2]

    - id: E9-S3
      title: 回款控制
      repository_type: backend
      estimated_complexity: S
      priority: P1
      acceptance_criteria:
        - id: AC-9.3.1
          given: 应收账款 150000 元，到期 2026-06-10
          when: 当前日期 = 2026-06-07
          then: |
            系统提醒：3 天后到期，业务员 + 财务；
            逾期后 7 天升级到总经理。
          business_rules:
            - 到期前 3 天：业务员 + 财务
            - 逾期 7 天：升级总经理
            - 逾期 30 天：暂停接单
        - id: AC-9.3.2
          given: 付款单 150000 元
          when: 财务提交
          then: |
            走付款审批：> 10 万总经理 + 财务总监双签。
          business_rules:
            - 单笔 > 10 万：总经理 + 财务总监双签
            - 单笔 ≤ 10 万：财务总监单签
      provides_apis:
        - POST /api/v1/payments
        - POST /api/v1/payments/{id}/submit
        - GET  /api/v1/payments/pending
      consumes_apis:
        - GET /api/v1/finance/payables
      depends_on_stories: [E9-S1, E1-S2]

    - id: E9-S4
      title: 利润分析
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-9.4.1
          given: 2026 年 5 月所有订单已结案
          when: 财务查看 5 月利润分析
          then: |
            按订单/客户/产品三个维度展示；
            **【V1.3.3 客户追加】利润率分级预警**：
              · 毛利率 < 10% 标黄（预警："薄利，需关注成本"）；
              · 毛利率 < 5% 标红（警示："已临近成本线"）；
              · 毛利率 < 0% 标深红 + 老板驾驶舱强提示（严重："亏损订单"）；
            同比 4 月 ±%。
          business_rules:
            - 利润 = 总收款 - 总成本
            - 利润率 = 利润 / 总收款
            - 同比/环比自动计算
            - 标红阈值（V1.3.3 调整）：
                毛利率 ≥ 10%  → 正常绿色
                10% > 毛利率 ≥ 5%  → 黄色"薄利预警"
                5%  > 毛利率 ≥ 0%  → 红色"接近亏损"
                0%  > 毛利率       → 深红"亏损" + 老板驾驶舱推送 + 当月强提示
            - 阈值集中在系统参数（E1-S3）配置，便于客户根据行业周期微调
        - id: AC-9.4.2
          given: 销售订单 XS202606040001 报价时利润率预估 12%
          when: 工单加工中物料/委外/人工实际成本不断累加，**当前利润率跌破 10%**
          then: |
            系统在订单/工单详情顶部出现"利润率预警黄条"；
            自动推送业务员 + 生管 + 老板（APP + 企业微信）；
            点预警可下钻看成本明细，定位"哪笔成本超支"。
          business_rules:
            - 实时预警基于当前已发生成本 + 剩余工序标准成本
            - 预警频率：每小时重算 + 关键事件触发（领料/报工/委外入库）
            - 同一订单同一预警等级 24h 不重复推送（去重）
      provides_apis:
        - GET /api/v1/profit/orders?month=
        - GET /api/v1/profit/customers?month=
        - GET /api/v1/profit/products?month=
        - GET /api/v1/profit/alerts?level=        # 黄/红/深红预警列表
        - PUT /api/v1/profit/thresholds           # 调整预警阈值（管理员）
      consumes_apis:
        - GET /api/v1/orders
      depends_on_stories: [E9-S1, E9-S2]

    - id: E9-S5
      title: 料号成本聚合视图（V1.3.4 新增）
      repository_type: backend
      estimated_complexity: L
      priority: P0
      why_first: |
        WHY：客户原话："客户想通过直接搜料号，查特定料号的价格和成本明细，材料成本整个明细"。
             原 PRD 成本核算粒度在"工单"而非"料号"，财务看不出"这个料号到底赚不赚"。
             本 Story 把"工单 → 料号"反向聚合，让一个料号能看到自己跨订单的完整成本结构。
             这是 G10「料号维度数据洞察」的后端引擎，前端面板见 E11-S5。
             傻瓜测试：财务搜一个料号，5 秒内看到价格曲线 + 5 类成本明细。
      acceptance_criteria:
        - id: AC-9.5.1
          given: 料号 YL45D80 在最近 12 个月有 23 个销售订单
          when: 调用聚合 API GET /api/v1/cost/by-material?materialNo=YL45D80&from=2025-06&to=2026-06
          then: |
            返回 JSON：
            {
              "materialNo": "YL45D80",
              "priceHistory": [{orderId, date, unitPrice, customer}, ...],  // 23 条
              "avgPrice12m": 82.5,
              "costBreakdown": {
                "material": { items: [{bomItemNo, size, unitPrice, lossRate, totalCost}, ...], total: 23000 },
                "labor": { items: [{processName, avgHours, hourlyRate, totalCost}, ...], total: 8500 },
                "surface": { items: [{type, basis, totalCost}, ...], total: 1200 },
                "outsource": { items: [{outsubId, vendor, totalCost}, ...], total: 3400 },
                "overhead": { method: "labor-ratio", ratio: 0.15, totalCost: 5400 }
              },
              "unitCostTotal": 41.5
            }
            P95 ≤ 2s（命中缓存 ≤ 200ms）。
          business_rules:
            - 聚合时间范围：默认最近 12 个月，可传 from/to 自定义
            - 价格历史：所有销售订单单价（去除作废）
            - 材料成本：BOM 展开 × 实时单价 × (1 + 损耗系数)
            - 工时成本：按工序聚合，工时 = avg(该料号该工序所有历史报工工时)
            - 表处成本：按表处类型聚合（按面积/重量/件数其一，BOM 内定义）
            - 外协成本：聚合所有关联委外单的实际付款金额
            - 管理费分摊：默认按工时占比（labor-ratio），可在系统参数切为 material-ratio
            - 缓存策略：Redis Key `cost:material:{materialNo}:{from}:{to}`，TTL 见 FR-9-5-2（默认 3600s）
            - 失效触发（Redis Stream 广播）：
              · 委外单 STORED → 失效该料号所有缓存
              · 工时录入 → 失效该料号 labor 段
              · 物料价格变更 → 失效该料号 material 段
              · BOM 发布新版本 → 失效该料号所有缓存
          data_validation:
            - materialNo：必填，引用物料编码
            - from/to：可选，ISO 8601 yyyy-MM；from ≤ to
          error_handling:
            - 料号不存在 → 404 "料号未注册"
            - 时间范围超过 36 个月 → 400 "时间范围过大，最多 36 个月"
        - id: AC-9.5.2
          given: 用户角色 = "操作工"
          when: 调用 GET /api/v1/cost/by-material?materialNo=YL45D80
          then: |
            返回数据中 labor 段仅包含该用户所属工序的工时；
            其他段（material / surface / outsource / overhead / unitCostTotal）= null；
            响应头 `X-Permission-Limited: true`；
            前端面板提示"您仅可见自己工序的工时成本，其他成本请联系管理层"。
          business_rules:
            - 权限隔离规则（同 G7）：
              · 高层 / 管理层 / 财务：全量
              · 生管 / 业务员 / 工程师：全量但仅查看，不可导出明细
              · 操作工 / 仓管 / 品检 / 送货员：仅自己工序工时，其他段 null
            - 权限判定在 Service 层（注解 @CostScope），不依赖前端隐藏
            - 审计：所有料号成本查询写入审计日志（操作人 / 料号 / 时间 / IP）
      provides_apis:
        - GET /api/v1/cost/by-material?materialNo=&from=&to=&customerId=
        - POST /api/v1/cost/by-material/refresh         # 强制刷新缓存（管理员）
        - GET /api/v1/cost/by-material/export?...&format=excel|pdf
      consumes_apis:
        - GET /api/v1/materials/{id}
        - GET /api/v1/boms/{id}/tree
        - GET /api/v1/workorders?materialNo=
        - GET /api/v1/outsource?materialNo=
      depends_on_stories: [E9-S2, E3-S3, E5-S2, E5-S4]
```

#### E10: 人事管理（完整 YAML）

```yaml
epic:
  id: E10
  title: 人事管理
  version: V1.1
  target_platform: [backend, web, android]
  why_exist: |
    WHY：HR 每月花在考勤统计、工资条制作上 5 天以上。
         V1.1 让考勤自动算工资，HR 从 Excel 中解放。
  what_it_is:
    - 员工档案 + 考勤
    - 薪酬自动核算
    - 绩效 + 招聘
  what_it_is_not:
    - 不是 HRM 全套（不做事假管理、组织架构、离职全流程）
    - 不是招聘 ATS（不做候选人门户）
  reuse_analysis:
    can_reuse:
      - E1-S1 用户/角色
      - E9-S2 计件工资归集
    must_build:
      - 工资账套配置
      - 考勤规则
  depends_on_epics: [E1, E9]
  stories_count: 3
  stories:
    - id: E10-S1
      title: 员工档案与考勤
      repository_type: backend+web+android
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-10.1.1
          given: HR 新建员工「王师傅」
          when: 部门=车间，职位=CNC 操作工，入职日期=2026-06-01
          then: |
            员工档案创建；自动关联用户账号；
            员工号 = EMP+YYYYMM+4 位流水。
          business_rules:
            - 员工号：EMP+YYYYMM+4 位流水
            - 身份证号加密存储（AES-256）
            - 状态：在职/试用期/离职
        - id: AC-10.1.2
          given: 王师傅上班打卡
          when: 早上 8:00 打开 APP 打卡
          then: |
            上班时间 = 08:00:00；
            考勤状态 = 正常。
          business_rules:
            - 打卡方式：APP WiFi/蓝牙定位（GPS P2/可选）
            - 迟到：> 上班时间 30 分钟
            - 早退：< 下班时间 30 分钟
        - id: AC-10.1.3
          given: 王师傅请假 1 天
          when: APP 提交请假申请
          then: |
            走审批流；HR 审批通过后考勤状态 = 请假。
          business_rules:
            - 请假类型：事假/病假/年假/调休/婚假/产假
            - 年假：按工龄自动计算
      provides_apis:
        - POST /api/v1/employees
        - GET  /api/v1/employees/{id}
        - GET  /api/v1/employees
        - POST /api/v1/attendance
        - POST /api/v1/attendance/leave
        - POST /api/v1/attendance/overtime
        - GET  /api/v1/attendance/{employeeId}?month=
      consumes_apis:
        - GET /api/v1/departments
      depends_on_stories: [E1-S1]

    - id: E10-S2
      title: 薪酬自动核算
      repository_type: backend
      estimated_complexity: L
      priority: P1
      acceptance_criteria:
        - id: AC-10.2.1
          given: 王师傅工资账套：基本 5000 + 岗位 2000 + 全勤 500 + 绩效 1000 - 社保 800 - 个税 200
          when: 月底自动核算
          then: |
            应发 = 5000 + 2000 + 500 + 1000 + 计件工资 = X；
            实发 = X - 800 - 200 = Y。
          business_rules:
            - 工资账套：基本 + 岗位 + 绩效 + 加班 + 全勤 - 社保 - 个税
            - 计件工资自动归集（来自 E9-S2）
            - 个税按 7 级累进
            - 工资条 PDF 推送到员工 APP
        - id: AC-10.2.2
          given: 工资条已生成
          when: 员工打开 APP 查看
          then: |
            显示本月工资条 + 历史 12 个月。
          business_rules:
            - 工资条：PDF 格式
            - 历史可查 12 个月
      provides_apis:
        - POST /api/v1/salary/calculate
        - GET  /api/v1/salary/{employeeId}?month=
        - GET  /api/v1/salary/{employeeId}/payslip
        - GET  /api/v1/salary-sets
        - POST /api/v1/salary-sets
      consumes_apis:
        - GET /api/v1/employees
        - GET /api/v1/attendance
      depends_on_stories: [E10-S1, E9-S2]

    - id: E10-S3
      title: 绩效与招聘
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-10.3.1
          given: HR 配置绩效方案
          when: 项目=产量 40% + 质量 30% + 态度 30%，评分 0-100
          then: |
            月底自动计算员工绩效分；
            关联到薪酬。
          business_rules:
            - 考核项 + 权重 + 评分
            - 申诉：员工可对评分申诉
        - id: AC-10.3.2
          given: 用人部门提交用人申请
          when: 招聘专员创建招聘计划 → 导入简历 → 安排面试
          then: |
            简历入库人才档案池；
            面试通过后入职创建员工档案。
          business_rules:
            - 招聘流程：用人申请 → 招聘计划 → 简历 → 面试 → 录用
            - 简历批量导入
            - 面试评价记录
      provides_apis:
        - POST /api/v1/performance
        - GET  /api/v1/performance/{employeeId}?month=
        - POST /api/v1/recruitment/plans
        - POST /api/v1/recruitment/resumes
      consumes_apis:
        - GET /api/v1/employees
      depends_on_stories: [E10-S1]
```

#### E11: 报表与看板（完整 YAML）

```yaml
epic:
  id: E11
  title: 报表与看板
  version: V1.1
  target_platform: [backend, web]
  why_exist: |
    WHY：老板要"一个界面看全公司"。V1.1 让老板/生管/业务员/HR
         都能在各自的工作台上一眼看到自己关心的指标。
  what_it_is:
    - 生产工作台（一屏总览）
    - 多维度看板（委外/自加工/逾期）
    - 销售排行 + 老板驾驶舱
  what_it_is_not:
    - 不是 BI 平台（不做自定义报表/数据可视化编辑器）
    - 不是大屏可视化（V1.1 简版，V1.2+ 考虑专业大屏）
  reuse_analysis:
    can_reuse:
      - ECharts 做图表
      - WebSocket 做实时推送
    must_build:
      - 多角色工作台
      - 实时数据刷新
  depends_on_epics: [E2, E5, E6, E7, E9]
  stories_count: 5   # V1.3.4 从 3 升至 5（新增 S4 委外面板 / S5 料号价格成本面板）
  stories:
    - id: E11-S1
      title: 生产工作台
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-11.1.1
          given: 生管员登录后进入"生产工作台"
          when: 打开页面
          then: |
            一屏展示：待办（5）/ 订单进度（10）/ 派工（3）/ 设备（8 空闲/2 加工中）/ 人员出勤（18/20）；
            关键数据 5 秒自动刷新。
          business_rules:
            - 待办：审批/逾期/缺料/待检
            - 订单进度：销售订单 × 工单状态
            - 派工：当天/未来 3 天
            - 设备：实时状态
            - 人员：今日出勤
      provides_apis:
        - GET /api/v1/dashboard/production
        - WS  /api/v1/ws/dashboard
      consumes_apis:
        - GET /api/v1/workorders
        - GET /api/v1/machines
        - GET /api/v1/employees/attendance
      depends_on_stories: [E5-S1, E5-S5, E10-S1]

    - id: E11-S2
      title: 多维度看板
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-11.2.1
          given: 老板/生管查看"委外看板"
          when: 选择时间范围=近 30 天
          then: |
            展示：委外产值（折线）/ 按期交货率（环形）/ 品质合格率（柱状）/ 厂商排行。
          business_rules:
            - 时间范围：今日/7天/30天/自定义
            - 维度切换：按厂商/工序/订单
            - 数据导出 Excel
        - id: AC-11.2.2
          given: 业务员/生管查看"逾期看板"
          when: 打开页面
          then: |
            列表展示逾期工单/订单，按逾期天数排序；
            3 天内 / 5 天内 / 已逾期。
          business_rules:
            - 逾期 3 天：黄色
            - 逾期 5 天：橙色
            - 逾期 7 天：红色
        - id: AC-11.2.3
          given: 生管员需要回复客户"我们订单做到哪了 / 啥时候交货"
          when: 在"交付期检索"页输入客户名 + 选状态多选（已完成 / 待交货）
          then: |
            一屏返回符合条件的订单 + 工单进度 + 预计交期 + 实际交期；
            支持"已完成"/"待交货"/"已逾期"三态过滤；
            可一键生成"交付进度反馈"文案，复制粘贴回客户。
          business_rules:
            - 过滤维度：客户 / 状态（多选）/ 交期范围 / 业务员
            - "待交货"= 已入库未发货 + 计划交期 ≤ 7 天
            - 文案模板：客户名 + 订单号 + 当前工序 + 预计交期，可在系统参数自定义
      provides_apis:
        - GET /api/v1/dashboard/outsource
        - GET /api/v1/dashboard/internal
        - GET /api/v1/dashboard/overdue
        - GET /api/v1/dashboard/delivery?customerId=&status=&deliveryFrom=&deliveryTo=
        - POST /api/v1/dashboard/delivery/template     # 生成客户反馈文案
      consumes_apis:
        - GET /api/v1/outsource
        - GET /api/v1/workorders
      depends_on_stories: [E5-S1, E6-S1]

    - id: E11-S3
      title: 销售排行与统计
      repository_type: backend+web
      estimated_complexity: M
      priority: P1
      acceptance_criteria:
        - id: AC-11.3.1
          given: 老板查看"销售龙虎榜"
          when: 选择时间范围=本月
          then: |
            业务员排行：销售金额/订单数/回款额/客户数；
            前 3 名标金色，第 4-10 名银色，其余白色。
          business_rules:
            - 排行维度：金额/订单数/回款额
            - 时间范围：今日/本周/本月/本季/本年
            - 提成规则：可配置
        - id: AC-11.3.2
          given: 老板查看"客户利润汇总"
          when: 选择时间范围=本年
          then: |
            客户 × 营收 × 成本 × 利润 × 利润率；
            利润率 < 5% 标红。
          business_rules:
            - 客户维度
            - 利润 = 营收 - 成本
            - 利润率 < 5% 标红
      provides_apis:
        - GET /api/v1/dashboard/sales-ranking
        - GET /api/v1/dashboard/customer-profit
      consumes_apis:
        - GET /api/v1/orders
        - GET /api/v1/profit/customers
      depends_on_stories: [E2-S3, E9-S4]

    - id: E11-S4
      title: PC 端委外面板（V1.3.4 新增）
      repository_type: backend+web
      estimated_complexity: M
      priority: P0
      why_first: |
        WHY：客户原话："管理层 / 生管可一屏查看 哪些委外订单已送货、哪些未送、哪些在返修、即将逾期"。
             原 PRD 委外看板只有"产值/按期率/品质"统计，无法回答"现在到底哪几张单卡住了"。
             本 Story 用"列表 + 高亮"范式，把所有进行中委外订单一屏铺开，
             让管理层 5 秒抓出问题单。
             傻瓜测试：老板早上 9 点打开浏览器，红色块在哪，问题就在哪。
      acceptance_criteria:
        - id: AC-11.4.1
          given: 老板 / 生管员登录 PC 端 → "委外面板"
          when: 打开页面
          then: |
            一屏列表展示所有未关闭委外订单（按 7 状态分组折叠 / 展开）；
            关键列：委外单号 / 厂商 / 工序 / 数量 / 状态 / 计划完成日 / 实际送货日 / 剩余天数 / 返修次数 / 操作；
            高亮规则：
              · 即将逾期（剩余天数 ≤ 1）→ 整行红色背景
              · 返修次数 ≥ 2 → 返修次数列深红 + 闪烁
              · 待检（PENDING_INSPECTION）超过 24h 未排质检 → 状态列黄色
              · 已逾期（剩余天数 < 0）→ 整行深红 + 顶部预警条统计
            支持 5 秒自动刷新（WebSocket）。
          business_rules:
            - 数据范围：所有 status != STORED && status != 已关闭 的委外单
            - 默认排序：剩余天数升序（最紧急在最上）
            - 分组折叠：默认全部展开；可保存用户偏好（折叠 / 展开 / 仅显示某些状态）
            - 一行点击 → 弹抽屉显示委外单详情 + 7 状态机时间线 + 返修历史
        - id: AC-11.4.2
          given: 生管员需要快速找"A 表处厂 + 未送货"
          when: 顶部筛选：厂商 = A 表处厂，状态 = PENDING_SHIP + SHIPPING
          then: |
            列表实时过滤；
            底部出现"已筛选 3 张单，总金额 12000"；
            可一键导出 Excel（含当前筛选）。
          business_rules:
            - 筛选维度：状态（多选）/ 厂商（多选）/ 时间范围 / 返修次数区间
            - 筛选条件可保存为"我的视图"
            - 导出 Excel 包含筛选条件元数据
        - id: AC-11.4.3
          given: 委外单 WW202606040001 返修次数从 1 升到 2
          when: 系统触发预警（来自 E6-S6 AC-6.6.2）
          then: |
            面板该行立刻变深红 + 闪烁；
            顶部出现红点提醒"+1 高返修单"；
            点红点直达该单详情。
          business_rules:
            - 实时推送：WebSocket 通道 `ws://.../dashboard/outsource`
            - 预警声效：可在用户偏好关闭
            - 闪烁持续：直到该单被高层 ack（E6-S6 ack API）
      provides_apis:
        - GET /api/v1/dashboard/outsource/board?status=&vendorId=&dueFrom=&dueTo=&reworkMin=
        - GET /api/v1/dashboard/outsource/board/export?...
        - WS  /api/v1/ws/dashboard/outsource
      consumes_apis:
        - GET /api/v1/outsource
        - GET /api/v1/outsource/rework-alerts
      depends_on_stories: [E6-S5, E6-S6, E11-S2]

    - id: E11-S5
      title: 料号价格/成本检索面板（V1.3.4 新增）
      repository_type: backend+web+android
      estimated_complexity: L
      priority: P0
      why_first: |
        WHY：客户原话："客户想通过直接搜料号，查特定料号的价格和成本明细，材料成本整个明细"。
             原 PRD 没有"料号维度"的检索入口，业务员问报价、财务问成本、老板问利润都各看各的。
             本 Story 是 G10「料号维度数据洞察」的前端落地，5 个 Tab 一屏看完一个料号生命周期。
             权限隔离贴合 G7：管理层看全量、一线只见自己工序工时。
             傻瓜测试：老板 Ctrl+K 输入"YL45D80" → 3 秒内看到完整成本结构。
      acceptance_criteria:
        - id: AC-11.5.1
          given: 业务员 / 工程师 / 财务 / 管理层在 PC 端任意页面
          when: 按下 Ctrl+K 全局搜索 → 输入"YL45D80" → 选"料号: YL45D80"
          then: |
            进入"料号成本面板 - YL45D80"页；
            顶部展示：料号 / 名称 / 规格 / 当前库存 / 时间范围筛选（默认近 12 个月）/ 客户筛选（可选）；
            5 个 Tab：
              · 价格 Tab：12 个月销售单价折线 + 报价均价 + 历史均价 + 最高 / 最低
              · 材料成本 Tab：BOM 展开树（料号 × 尺寸 × 单价 × 损耗系数 = 单件成本）
              · 工时成本 Tab：按工序展示平均工时 × 工时费率 = 工时成本，含曲线
              · 外协成本 Tab：关联委外单列表 + 各厂商分摊 + 平均单价趋势
              · 总成本 Tab：材料 + 工时 + 表处 + 外协 + 管理费分摊 = 单件总成本，环比 / 同比
            首次加载 ≤ 3 秒（命中缓存 ≤ 800ms）。
          business_rules:
            - 入口：PC Ctrl+K 全局搜索 / 顶部菜单"数据 → 料号成本" / APP 首页搜索框
            - 时间范围：默认近 12 个月；可拖拽自定义；时间窗 > 12 个月时面板顶部黄色提示"数据较多，加载可能较慢"
            - 客户筛选：仅"业务员 / 财务 / 管理层"可见
            - Tab 切换：URL 路由保留 Tab 状态，可分享链接
            - 图表交互：折线点 hover 显示订单详情（hyperlink 跳转）
        - id: AC-11.5.2
          given: 操作工角色（一线）打开料号成本面板
          when: 查看任意料号
          then: |
            价格 Tab / 材料 / 外协 / 总成本 Tab 全部置灰，显示"无权限查看，请联系管理层"；
            工时 Tab 仅显示该用户所属工序的工时（来自后端 X-Permission-Limited 标记）；
            顶部红色提示条"您是一线角色，仅可见自己工序的工时成本"。
          business_rules:
            - 权限判定：复用 E9-S5 AC-9.5.2 同款规则（@CostScope 注解）
            - 前端不依赖隐藏：所有数据从后端获取，后端返回 null 即前端置灰
            - 一线角色查询日志记录到审计（防止越权探测）
        - id: AC-11.5.3
          given: 财务 / 管理层在总成本 Tab
          when: 点"导出 Excel" / "导出 PDF"
          then: |
            生成下载文件，含：
              · 元数据（料号 / 时间范围 / 导出人 / 导出时间）
              · 5 个 Tab 全部数据明细
              · 总成本结构饼图（PDF 含图片）
              · 水印"机密-禁止外传"（PDF）
            异步生成（> 50 条数据走后端任务），完成后消息中心通知下载链接。
          business_rules:
            - Excel 格式：xlsx，按 Tab 分 Sheet
            - PDF 格式：A4 横向，含 logo + 水印
            - 导出操作写入审计日志
            - 一线角色导出按钮置灰
            - 异步导出任务 TTL 24h，过期自动清理
        - id: AC-11.5.4
          given: 业务员在 APP 端
          when: 首页搜索栏输入"YL45D80"
          then: |
            进入 APP 简版料号面板（横向 5 Tab，移动端优化）；
            价格 / 材料 / 工时 / 外协 / 总成本 同 PC，但图表自适应小屏；
            支持下拉刷新；
            支持"分享到企业微信"（生成卡片消息含核心数据快照）。
          business_rules:
            - APP 端权限同 PC（基于角色）
            - 分享卡片仅含汇总（无明细），含"详情请登录 PC 端"提示
            - APP 端不支持 PDF 导出（仅 PC 端）
      provides_apis:
        - GET /api/v1/cost/by-material/panel?materialNo=&tab=price|material|labor|outsource|total
        - POST /api/v1/cost/by-material/export-task     # 异步导出
        - GET /api/v1/cost/by-material/export-task/{id}/status
        - GET /api/v1/cost/by-material/export-task/{id}/download
        - POST /api/v1/cost/by-material/share           # 生成分享卡片
      consumes_apis:
        - GET /api/v1/cost/by-material                  # E9-S5 后端引擎
        - GET /api/v1/materials/{id}
      depends_on_stories: [E9-S5, E1-S4]
```

#### E12: 委外到货与品质领料协同（V1.3.5 客户调整 · 原"供应商协同"改版）

> **V1.3.5 重要变更**：客户 2026-06-08 反馈"取消厂商送货员角色，由仓管代替厂商送货员操作到货"。原 E12（V1.3.3 引入）整套"供应商 APP 账号 + 送货员 + 线上对账"全部下线。E12 重新定义为**仓管到货扫码 + 品质去仓库领料**的厂内协同故事。
> **保留项**：E6-S1 月度对账、E6-S6 返修预警等"我方视角"的委外深化不变——这些不需要供应商介入。
> **删除项**：E12-S1 供应商账号与权限、E12-S2 送货员扫码到货、E12-S3 线上对账——3 个 Story 全部下线。

```yaml
epic:
  id: E12
  title: 委外到货与品质领料协同
  version: V1.1   # 与 E6 委外深化配套交付；V1.3.5 改版
  target_platform: [backend, android]
  why_exist: |
    WHY：V1.3.4 引入"送货员扫码到货"后，客户 2026-06-08 反馈实操更顺畅的做法是——
         让厂内**仓库管理员**直接到货扫码，**品质员**去仓库领料后质检。
         理由三：
         (1) 厂外送货员带 APP 账号 / 设备 / 培训成本高，仓管是厂内正式员工；
         (2) 仓管扫码时即知道实际数量 / 重量 / 破损情况，比"送货员代录"更准；
         (3) 品质去仓库"凭通知单找仓管领料"是行业最自然流程，省掉"自动入待检区再排队等"的硬环节。
         客户原话："委外厂商送货回来，**大部分都还要流转**（继续下一道工序），
                    仓库增加一个**到货扫码**，扫**货物所属的委外订单二维码**，
                    **生管品质那边就可以收到通知**，然后再由**品质去仓库领料**进行质检。"
         这是"一码到底"灵魂的厂内化收口——从"我方单据一码贯穿"升级到"我方 4 类岗位一码贯穿"。
  what_it_is:
    - 仓管 APP"到货扫码"功能（与"入库扫码"并列）
    - 扫**委外订单二维码**（不是物料码 / 工单码 / 流转码）触发 SHIPPING → PENDING_INSPECTION
    - 通知生管 + 品质 + 仓管三方可见
    - 品质去仓库领料后质检（PENDING_INSPECTION → INSPECTING）
  what_it_is_not:
    - ~~不是供应商 APP 协同~~（V1.3.5 已下线）
    - ~~不是厂外人员对账工具~~（V1.3.5 已下线；线上对账保留在 E6-S1 我方财务视角）
    - 不是 WMS RF 手持端全功能（不做 RF 策略 / 拣货路径优化）
    - 不替代 E6-S5 委外 7 状态机（E12 仅为状态机的"前置动作"赋能）
  reuse_analysis:
    can_reuse:
      - E1-S4 APP 扫码壳（识别 WW- 委外订单码前缀）
      - E4-S2 APP 扫码出入库（入口 / UI 范式 / 离线策略）
      - E6-S5 委外 7 状态机（状态转换由本 Epic 触发）
      - E1-S1 用户/角色（"仓管员" / "品质员" 角色已存在）
    must_build:
      - 委外订单二维码 WW- 在委外下单时同步生成并随货同行
      - 仓管"到货扫码"专用入口（与"入库扫码"并列，识别后路由不同）
      - 品质"领料扫码"动作（轻量：扫委外单码 → 记录领料人 + 时间）
  depends_on_epics: [E1, E4, E6]
  stories_count: 3   # V1.3.5 重置：S1 仓管到货扫码权限 / S2 仓管到货扫码 AC / S3 品质去仓库领料
  stories:
    - id: E12-S1
      title: 仓管到货扫码权限（V1.3.5 改版）
      repository_type: backend+web
      estimated_complexity: S
      priority: P0
      why_first: |
        WHY：V1.3.5 客户原话"由仓库管理员来代替厂商送货员操作到货"。
             权限是把"仓管可以扫到货"这件事做扎实的底座——不需新建角色（仓管员 E1-S1 已存在），
             只需在角色权限上挂"到货扫码"功能码 + 在数据权限上设"我被分配的委外单"。
             善良设计：不让任何仓管都能扫所有到货单，避免"串单"风险。
      acceptance_criteria:
        - id: AC-12.1.1
          given: 委外订单 WW202606040001 由「A 表处厂」承做，状态 = SHIPPING
          when: 管理员在「委外订单详情」为该单分配仓管 = 「王仓管」（主仓管）+ 「李仓管」（副仓管）
          then: |
            王仓管 / 李仓管 APP 首页出现"待到货"红点提醒；
            其他仓管员看不到该单的到货入口；
            王仓管可在 APP"到货扫码"功能中看到该单。
          business_rules:
            - 仓管角色 = E1-S1 已有的"仓管员"内置角色；不新建
            - 委外单的"收货仓管"由生管在创建委外单时指派（V1.3.5 默认 = 委外下单人所在班组的仓管）
            - 主仓管 + 副仓管：副仓管为备份（主仓管休假时代扫码）
            - 数据权限：仓管只能扫被自己为"主/副仓管"的委外单；扫描其他单 → 403
            - 取消"送货员"角色（V1.3.5）：E1-S1 角色清单中删除"送货员"角色条目
          data_validation:
            - 主仓管：必填，引用用户表（必须角色=仓管员）
            - 副仓管：可选，引用用户表
          error_handling:
            - 仓管被禁用 / 调岗 → 自动从所有委外单"主/副仓管"中移除，系统提示管理员重派
            - 扫描非自己管辖的委外单 → "您不是该单收货仓管，请联系生管" + 写审计日志
      provides_apis:
        - POST /api/v1/outsource/{id}/assign-warehouse-keeper   # 生管分配仓管
        - GET  /api/v1/app/outsource/pending-arrival           # 仓管 APP 待到货列表
        - GET  /api/v1/warehouses/keepers                      # 仓管候选列表（生管用）
      consumes_apis:
        - GET /api/v1/outsource/{id}
        - GET /api/v1/users?role=仓管员
      depends_on_stories: [E1-S1, E6-S5]

    - id: E12-S2
      title: 仓管到货扫码 AC（V1.3.5 新增）
      repository_type: android+backend
      estimated_complexity: M
      priority: P0
      why_first: |
        WHY：客户原话"仓库增加一个到货扫码，扫货物所属的委外订单二维码"。
             这条 Story 是 V1.3.5 最核心的实操入口。
             关键设计：扫的是**委外订单二维码（WW- 前缀）**，不是物料码、不是工单码、不是流转码——
             因为一个委外单对应一张到货通知，物料码会"一码多到货"造成歧义。
             傻瓜测试：仓管在仓库门口接过送货单 → 打开 APP 扫送货单上的二维码 → 1 秒识别 → 弹"已到达" → 按确认。
      acceptance_criteria:
        - id: AC-12.2.1
          given: 仓管员「王仓管」在仓库入口，收到「A 表处厂」送货员送来的委外单 WW202606040001
          when: 打开 APP → "仓储"模块 → "到货扫码" → 扫送货单上的委外订单二维码
          then: |
            1 秒内识别为"委外订单码 WW-..."；
            弹出确认框："已到达一号仓，委外单 WW202606040001（A 表处厂，料号 P001，数量 100），是否确认到货？"；
            点"确认" → 委外单状态 SHIPPING → PENDING_INSPECTION；
            自动通知生管（APP + 企业微信）+ 品质（APP + 企业微信），仓管自己在"已到货"列表可见。
          business_rules:
            - 入口：APP 首页 → 仓储 → **"到货扫码"**（与"入库扫码"并列，两个 Tab）
            - 识别码前缀：WW- = 委外订单码（V1.3.5 强约束）
            - 扫码权限：王仓管必须 = 该委外单的"主仓管"或"副仓管"（AC-12.1.1）
            - 触发状态机：SHIPPING → PENDING_INSPECTION（E6-S5）
            - 时间记录：扫码时间 = 到货时间 `arrived_at`，用于供应商交期统计
            - 一码多次扫描：同一 WW- 同一仓管 5 分钟内重复扫码视为同一次（去重）
            - 离线：复用 E1-S4 离线策略（SQLite 500 条 + 30s 同步）
          data_validation:
            - 委外单码：必填，WW+YYYYMMDD+4 位流水（生成时机：委外下单时 E5-S4）
            - 实收数量：> 0，> 委外单数量 110% 弹差异原因（少送/超送/破损）
            - 重量：可选，> 0
            - 照片：≤ 5 张，每张 ≤ 5MB（仓管可拍送货单 / 货物外观）
          error_handling:
            - 扫错码（识别为物料码/工单码/流转码）→ "这不是委外单二维码，请扫送货单上的 WW- 码"
            - 仓管非本单管辖 → "您不是该单收货仓管，请联系生管"
            - 委外单状态非 SHIPPING → "该单不在送货中态，当前状态 = {state}，无法扫码到货"
          examples:
            - valid: { outsourceId: "WW202606040001", warehouseKeeperId: "U-W001", receivedQty: 100 }
            - invalid: { outsourceId: "GD202606040001" }   # 扫成工单码
        - id: AC-12.2.2
          given: 仓管扫码后发现实收 = 95，委外单数量 = 100（差 5 件，≤ 5% 阈值）
          when: 点"确认"前勾选"少送 5 件"差异说明
          then: |
            状态仍正常 PENDING_INSPECTION；
            委外单附加"少送 5 件"差异记录；
            生管 APP 收到提醒（不强制审批）。
          business_rules:
            - 差异阈值：默认 5%（可在系统参数调），≤ 阈值自动接受
            - > 5%：弹"差异原因"必填（少送/超送/破损/其他）+ 升级生管员审批
            - 差异记录归档，写入供应商绩效数据（E8 P2 范围）
        - id: AC-12.2.3
          given: 仓管到货扫码时网络不稳定
          when: 扫了委外单码 + 点确认，提交失败
          then: |
            本地 SQLite 缓存（同 E1-S4 离线策略）；
            网络恢复 30s 内自动同步；
            重复提交时由服务端按"业务幂等键 = 委外单号 + 扫码时间戳"去重。
          business_rules:
            - 离线缓存上限：复用 500 条总限额（不单独限制）
            - 幂等键：outsourceId + warehouseKeeperId + scanTimestamp
      provides_apis:
        - POST /api/v1/app/outsource/arrival-scan       # 仓管到货扫码
        - POST /api/v1/app/outsource/arrival-scan/photos
        - GET  /api/v1/app/outsource/arrivals?status=    # 仓管"已到货"列表
        - GET  /api/v1/outsource/{id}/arrival-record     # PC 端查看到货记录
      consumes_apis:
        - GET  /api/v1/outsource/{id}
        - POST /api/v1/app/messages                       # 通知生管/品质
        - POST /api/v1/outsource/{id}/transit             # 触发状态机转换
      depends_on_stories: [E12-S1, E1-S4, E6-S5, E4-S2]

    - id: E12-S3
      title: 品质去仓库领料后质检（V1.3.5 新增）
      repository_type: backend+web+android
      estimated_complexity: S
      priority: P0
      why_first: |
        WHY：客户原话"生管品质那边就可以收到通知，然后再由品质去仓库领料进行质检"。
             V1.3.4 让"扫码到货 → 自动入待检区"看似智能，但实操需要"自动找货位 + 等品管来提"，
             反而引入了"待检区谁负责""找货时间"等新问题。
             V1.3.5 把流程"返璞归真"：品质员收到通知后，**人走到仓库找仓管**，仓管凭通知发货给品质。
             状态机触发：PENDING_INSPECTION → INSPECTING。
             傻瓜测试：品质员收到 APP 红点 → 走到仓库 → 出示通知单 → 仓管发货 → 品质员扫委外单码领料 → 开始质检。
      acceptance_criteria:
        - id: AC-12.3.1
          given: 委外单 WW202606040001 状态 = PENDING_INSPECTION（仓管已扫码到货）
          when: 品质员「赵品管」在 APP 收到推送"委外单 WW202606040001 已到货，请去仓库领料"
          then: |
            推送内容含：委外单号 / 厂商 / 料号 / 数量 / 仓管 / 仓库入口位置 / 建议领料时间；
            品质员点击推送 → 进入委外单详情 → 点"我已去仓库领料"按钮；
            系统记录领料时间 `picked_at` + 领料人 = 赵品管；
            状态 PENDING_INSPECTION → INSPECTING。
          business_rules:
            - 推送通道：APP 推送（必）+ 企业微信（必）+ PC 端"待检"红点（必）
            - 推送对象：品质员（角色=品检）+ 生管（CC，仅看）
            - "我已去仓库领料"按钮：必须在 PENDING_INSPECTION 状态才可点
            - 领料时效：PENDING_INSPECTION 超过 24h 未领料 → 自动升级生管员 APP 提醒（橙色）
            - 不再"自动入待检区"：物料物理位置由仓管员口头告知品质员，无需系统记录
          data_validation:
            - 领料时间：必填 = 系统当前时间
            - 领料人：必填 = 当前登录品质员
            - 关联原扫码记录：自动关联 E12-S2 AC-12.2.1 的到货扫码记录
          error_handling:
            - 品质员尝试领料时状态非 PENDING_INSPECTION → "该单已被其他人领料或已开始质检"
            - 同一品质员重复点 → 自动忽略（去重）
        - id: AC-12.3.2
          given: 品质员已领料，状态 = INSPECTING
          when: 录入质检结果（合格 / 不合格 / 让步接收 / 数量短缺等）
          then: |
            状态 INSPECTING → QUALIFIED_STORAGE（合格） / REPAIR_REQUESTED（不合格 + 退回维修）/ STORED 让步接收；
            触发 E6-S5 状态机的下一跳（同 V1.3.4）。
          business_rules:
            - 领料后才允许录入质检结果（前置校验）
            - 领料后 24h 内未录入结果 → APP 提醒品质员 + 生管员
            - 与 E6-S4 委外质检联动：自动生成"复检"标记（若 is_rework_reinspection=true）
      provides_apis:
        - POST /api/v1/app/outsource/{id}/pickup         # 品质员领料
        - GET  /api/v1/app/outsource/pending-pickup      # 待领料列表
        - POST /api/v1/app/outsource/{id}/quality-result # 录入质检结果（领料后强制）
      consumes_apis:
        - GET  /api/v1/outsource/{id}
        - POST /api/v1/outsource/{id}/transit             # 状态机转换
        - POST /api/v1/app/messages
      depends_on_stories: [E12-S2, E6-S5, E7-S1]
```

---


---
