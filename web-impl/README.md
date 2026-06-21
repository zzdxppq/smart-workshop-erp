# erp-web · 昆山佰泰胜专属 ERP V1.3.7

> **Frontend Web Implementation Repository** (multi-repo)
> **角色**: frontend
> **产品仓**: `../smart-workshop-erp`
> **架构依据**: 产品仓 `docs/architect-handoff.md` V1.1
> **配套 backend**: `../backend-impl`

## 快速开始

```bash
# 1. 安装依赖
npm install

# 2. 启动 dev server（Vite 5）
npm run dev
# 默认 http://localhost:5173

# 3. 类型检查
npm run type-check

# 4. 单元测试（Vitest）
npm run test

# 5. E2E 测试（Playwright）
npm run test:e2e

# 6. 构建生产包
npm run build
# 输出到 dist/

# 7. 自动生成 API 客户端（来自产品仓 OpenAPI 契约）
npm run api:gen
```

## 技术栈

| 维度 | 选型 | 版本 |
|------|------|------|
| 框架 | Vue | 3.4 (Composition API + `<script setup>`) |
| 构建 | Vite | 5.2 |
| 语言 | TypeScript | 5.4 |
| UI | Element Plus | 2.7 |
| 状态 | Pinia | 2.1 |
| 路由 | Vue Router | 4.3 |
| HTTP | Axios | 1.7 |
| 图表 | ECharts | 5.5 |
| PDF | pdfjs-dist | 4.0 |
| 测试 | Vitest + Playwright | 1.6 / 1.44 |
| Mock | MSW | 2.3 |
| API 客户端生成 | openapi-typescript-codegen | 0.25 |

## 7 大顶级菜单

按 `docs/ux-handoff.md` V1.1 §3.1 落地：

1. **工作台**（Dashboard）
2. **销售**（CRM / 报价 / 订单 / 合同回款）
3. **生产**（工单 / **工序分配（V1.3.7 生管）** / **委外下单（V1.3.7 采购）** / 委外面板 / 返修单 / 设备）
4. **物料**（图纸 / BOM / 工艺 / 库存 / **料号成本 5 Tab（V1.3.4）**）
5. **品质**（检验 / FA / 三次元 / 不良品）
6. **采购**（询比价 / 采购单 / 到货 / **月度对账 4 步（V1.3.6/7 不含"线下"）** / 厂商资料）
7. **财务**（应收/应付 / 账龄 / 成本 / 付款 / 利润）
8. **管理**（用户/角色 / 工作流 / 字典 / **邮件配置 163（V1.3.7）** / 人事）

## V1.3.7 关键约束（前端必须遵守）

### 5 条红线（不可触碰）

| # | 红线 | 验证方法 |
|---|------|----------|
| 1 | 生管页面 = 没有"代选厂商"按钮 | `grep 'VendorSelect' src/views/production/Allocation.vue` → 必须为 null |
| 2 | 采购页面 = 没有"改工序归属"按钮 | `grep 'ProcessDecisionToggle' src/views/production/OutsubOrder.vue` → 必须为 null |
| 3 | 对账页面 = 没有"采购带纸去厂商处"按钮 | `grep 'OfflineAction' src/views/sourcing/Reconcile.vue` → 必须为 null |
| 4 | 厂商资料"通知偏好"下拉 = 只有"163 邮箱" | `grep 'notifyChannel' src/views/sourcing/Vendors.vue` → options 长度必须 = 1 |
| 5 | 消息中心 = 没有"短信发送"按钮 | `grep 'SmsSend' src/views/MessageCenter.vue` → 必须为 null |

### 数据安全（V1.3.6/7）

- 字段级 AES-256-GCM 透明加解密（后端 MyBatis TypeHandler 处理，前端只显示）
- 签字扫描件（PDF/JPG ≤ 10MB）走 `SignedScanUpload` 组件，含加密提示
- 签字件下载走 `GET /platform/files/{id}/download` 限 3 角色（总经理/财务总监/采购员）

## 目录结构

```
web-impl/
├── .orchestrix-core/core-config.yaml
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── README.md
├── docs/
│   ├── architecture.md            # 🆕 由 architect *create-frontend-architecture 生成
│   ├── stories/                   # SM *draft 生成的 Story
│   ├── qa/
│   └── dev/logs/
├── public/
│   └── mockServiceWorker.js       # MSW
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/
    │   ├── client.ts              # Axios 实例 + JWT 拦截器
    │   └── generated/             # 🆕 openapi-typescript-codegen 输出
    ├── stores/
    │   ├── auth.ts
    │   └── user.ts
    ├── router/index.ts
    ├── layouts/
    │   ├── MainLayout.vue
    │   └── MenuLayout.vue
    ├── components/                # 🆕 8 个自研组件
    │   ├── ScanButton.vue
    │   ├── StatusTag.vue
    │   ├── MoneyInput.vue
    │   ├── FileUpload.vue
    │   ├── TableFilter.vue
    │   ├── PermissionButton.vue
    │   ├── ApprovalTimeline.vue
    │   ├── ReworkBadge.vue        # V1.3.4
    │   ├── EmailConfigForm.vue    # V1.3.7
    │   └── SignedScanUpload.vue   # V1.3.6
    ├── views/                     # 30+ 页面
    │   ├── auth/Login.vue
    │   ├── dashboard/Index.vue
    │   ├── sales/...
    │   ├── production/Allocation.vue  # V1.3.7 关键
    │   ├── production/OutsubOrder.vue # V1.3.7 关键
    │   ├── sourcing/Reconcile.vue    # V1.3.6 关键（不含"线下"）
    │   ├── material/CostAggregator.vue # V1.3.4 关键
    │   └── admin/EmailConfig.vue     # V1.3.7 关键
    ├── styles/
    │   └── index.scss
    ├── utils/
    └── test/
        └── setup.ts
```

## 关联

- **产品仓**：`../smart-workshop-erp`（PRD / 架构 / Epic YAML / OpenAPI 契约 / 合同）
- **后端仓**：`../backend-impl`（Spring Cloud Alibaba）
- **移动端仓**：`../android-impl`（Kotlin + Jetpack）

## 下一步

- SM 萧何 `*draft` 生成所有前端 stories（1.1 / 1.2 / 2.x / ...）
- Dev agent 实现 Story 1.1（用户/角色页）
- architect 鲁班 `*create-frontend-architecture` 生成详细前端架构
