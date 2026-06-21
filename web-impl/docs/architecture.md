# CNC ERP V1.3.7 · 前端 Web 详细架构

> **项目**：昆山佰泰胜专属 ERP 系统（Web 前端）
> **合同**：XP-ZPF202606082405
> **版本**：V1.3.7（2026-06-10）
> **作者**：Orchestrix Architect agent（鲁班）
> **输入依据**：
> - `docs/architect-handoff.md` V1.1（产品仓）
> - `docs/ux-handoff.md` V1.1（产品仓）
> - `docs/architecture/` 18 shard（产品仓）
> - `backend/spec/openapi.yaml`（产品仓 · API 契约）
> - `design/figma-library-upgrade.md`（产品仓 · Figma 资产）
> - `docs/contract-appendix-a-prd.md`（产品仓 · PRD 摘录）
> - `web-impl/.orchestrix-core/core-config.yaml`（仓配置）
> **目标读者**：前端 Web 工程师 / Storybook 维护者 / QA 工程师 / SRE

---

## 目录

1. [Introduction](#1-introduction)
2. [Template / Framework Selection](#2-template--framework-selection)
3. [System Architecture Context](#3-system-architecture-context)
4. [Tech Stack](#4-tech-stack)
5. [Source Tree](#5-source-tree)
6. [Component Standards](#6-component-standards)
7. [State Management](#7-state-management)
8. [API Integration](#8-api-integration)
9. [Routing](#9-routing)
10. [Styling Guidelines](#10-styling-guidelines)
11. [Testing Strategy](#11-testing-strategy)
12. [Environment Configuration](#12-environment-configuration)
13. [Coding Standards](#13-coding-standards)
14. [UI/UX 5 条红线实现](#14-uiux-5-条红线实现)
15. [Performance & Optimization](#15-performance--optimization)
16. [Accessibility](#16-accessibility)
17. [i18n](#17-i18n)
18. [Risks & Mitigations](#18-risks--mitigations)
19. [Deliverables Checklist](#19-deliverables-checklist)

---

## 1. Introduction

### 1.1 项目背景

本系统是面向 **昆山佰泰胜精密机械有限公司**（CNC 加工厂）的专属 ERP 系统，V1.3.7 版本。系统的产品精神是 **"一码到底，一数到底"**：

- **一码到底**：物料/工单/委外/库位全生命周期使用统一编码前缀（GD-/WL-/WW-/LZ- 等）；
- **一数到底**：单一可信数据源 + 状态机驱动 UI，避免多端数据漂移。

V1.3.7 在 V1.3.6 基础上落地 3 项客户原话 + 5 条 UI 红线，并新增"邮件配置后台"、"料号成本聚合 5 Tab"、"委外面板 7 状态机"等关键模块。

### 1.2 范围

**本架构文档范围**：`web-impl` 仓的前端 Web 实现，对应产品仓 `smart-workshop-erp` 的 frontend 角色。

**In-Scope**：
- Vue 3 + Vite + TypeScript 单页应用（SPA）
- 7 大顶级菜单（工作台 / 销售 / 生产 / 物料 / 品质 / 采购 / 财务 / 管理）
- Element Plus 2.7 桌面端 UI
- 30+ 故事 Story 1.1 ~ 11.5 的 Web 实现
- V1.3.7 关键 3 页 + 3 新组件 + 5 条红线 grep 验证

**Out-of-Scope**：
- Android APP（由 `app-impl` 仓实现，本仓仅提供类型/接口契约）
- 后端 Spring Boot 实现（由 `backend-impl` 仓实现）
- 移动端 H5（V1.3.7 暂未规划）

### 1.3 读者

- 前端工程师：实现 Story / 修复 Bug / 写测试
- Storybook 维护者：组件库迭代
- QA 工程师：依据本文档写 E2E 用例
- SRE：理解打包产物 / 监控接入
- Architect / Tech Lead：评审架构变更

### 1.4 关键术语

| 术语 | 含义 |
|------|------|
| **WW- 单** | 委外订单前缀（WorkOut 外协） |
| **GD- 单** | 工单前缀（GoDown 生产） |
| **WL- 单** | 物料码前缀（Ware Location） |
| **LZ- 单** | 过站扫码前缀 |
| **7 状态机** | 委外订单 PENDING_SHIP / SHIPPING / PENDING_INSPECTION / INSPECTING / QUALIFIED_STORAGE / STORED / REPAIR_REQUESTED + NOTIFIED_REPAIR 衍生态 |
| **红线按钮** | 严禁出现在 UI 上的按钮（V1.3.7 共 5 类） |
| **HANDOFF** | 跨角色任务交接指令 |

---

## 2. Template / Framework Selection

### 2.1 候选评估

| 维度 | Vue 3.4 | React 18 | Angular 17 |
|------|---------|----------|------------|
| 中文社区/Element Plus | 强 | 中 | 弱 |
| 学习曲线 | 平缓 | 中 | 陡峭 |
| 体积（运行时） | ~33KB gz | ~45KB gz | ~130KB gz |
| TypeScript 友好度 | 优秀（Volar） | 优秀 | 优秀 |
| Element Plus 生态 | 原生 | 间接（antd） | 间接（ng-zorro） |
| 团队既有经验 | 高 | 中 | 低 |
| 移动端 H5 复用 | 可（同一框架） | 可 | 困难 |

### 2.2 选型理由（Vue 3.4 + Vite 5.2）

1. **Element Plus 2.7** 是 163 邮箱配置 / 委外面板 / 料号成本聚合 5 Tab 等高密度表格的最佳选择，原生集成；
2. **Vite 5.2** HMR < 50ms，符合"操作工 30 秒学会扫码"的快速迭代；
3. **Composition API + `<script setup>`** 适合 V1.3.7 的细粒度红线按钮封装（`<PermissionButton>` 等）；
4. **Pinia 2.1** 替代 Vuex 4，TS 推断更友好；
5. **团队既有栈**降低 Sprint 0 启动成本。

### 2.3 不选 React 的原因

- 团队 React 经验少，Sprint 0 培训成本高；
- Element Plus → Antd 迁移将丢失约 30% 业务组件，需重写。

### 2.4 模板脚手架

- 基础模板：`vite@5.2 create-vite` + `vue-ts` preset
- 目录结构按 §5 执行（feature-based）
- 路径别名 `@/` → `src/`

---

## 3. System Architecture Context

### 3.1 4 实现仓位置图

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       orchestrix multi-repo 拓扑                          │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌──────────────────┐         ┌──────────────────┐                      │
│   │   product repo   │  prd    │   web-impl       │  story 1.1-11.5      │
│   │ smart-workshop-erp│ ◄────── │ (frontend)       │  30+ stories         │
│   │   - docs/        │  arch   │  Vue 3 + Vite    │  E2E + Vitest        │
│   │   - prd.md       │ ux-hf   │  Element Plus    │                      │
│   │   - architecture │  figma  │                  │                      │
│   └────────┬─────────┘         └────────┬─────────┘                      │
│            │ openapi.yaml              │                                 │
│            │ backend/spec             │ REST + WebSocket                │
│            ▼                          ▼                                 │
│   ┌──────────────────┐         ┌──────────────────┐                      │
│   │  backend-impl    │         │   app-impl       │                      │
│   │  Spring Boot 3.2 │  ───►   │  Android RN+TS   │                      │
│   │  PostgreSQL 16   │  API    │  （V1.3.5+）     │                      │
│   │  Redis 7 + Nacos │         │                  │                      │
│   └──────────────────┘         └──────────────────┘                      │
│                                                                          │
│   ┌──────────────────┐                                                   │
│   │  infra-impl      │  K8s + Prometheus + Nacos + XXL-JOB               │
│   │  (sre)           │  GitLab CI + Harbor                               │
│   └──────────────────┘                                                   │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.2 API 边界

- **Web 端 ↔ Backend**：HTTPS REST（Axios 1.7）+ WebSocket（用于返修预警推送、扫码回执）
- **Web 端 ↔ 第三方**：Nacos（配置中心）、Sentry（前端错误监控）、自建埋点（神策 SDK 占位）
- **Web 端内部**：Pinia store 跨组件共享，组件 → API 客户端 → 后端

### 3.3 责任划分

| 仓 | 角色 | 责任 |
|----|------|------|
| `smart-workshop-erp` | product | PRD/architecture/UX/Figma 资产/契约 |
| `web-impl` | frontend | Vue 3 Web 实现 + Storybook + E2E |
| `backend-impl` | backend | Spring Boot 实现 + 持久化 + 集成 |
| `app-impl` | mobile | Android RN 实现（V1.3.5+） |
| `infra-impl` | sre | K8s/监控/CI |

---

## 4. Tech Stack

### 4.1 核心依赖（生产）

| 包 | 版本 | 用途 | 关键约束 |
|---|------|------|----------|
| vue | 3.4.21 | 框架核心 | `<script setup>` + Composition API |
| vue-router | 4.3.0 | 路由 | 7 大顶级菜单 + 鉴权守卫 + 红线校验钩子 |
| pinia | 2.1.7 | 状态管理 | 4 store：auth/user/app/workflow |
| element-plus | 2.7.0 | UI 组件库 | 全量 + 按需（unplugin-vue-components） |
| @element-plus/icons-vue | 2.3.1 | 图标 | 按需 |
| axios | 1.7.2 | HTTP 客户端 | JWT 拦截 + 错误码处理 + 取消请求 |
| echarts | 5.5.0 | 图表 | 料号成本趋势图 + 利润分析图 |
| vue-echarts | 6.7.0 | Vue 封装 | 按需注册 |
| pdfjs-dist | 4.0.379 | PDF 在线预览 | 月度对账 PDF + 报价单 |
| openapi-typescript-codegen | 0.25.0 | 类型生成 | 从 `backend/spec/openapi.yaml` 生成 |
| dayjs | 1.11.10 | 日期 | V1.3.4 料号成本时间范围 |
| decimal.js | 10.4.3 | 金额精度 | 配合 `<MoneyInput>` 严格 BigDecimal |
| vue-i18n | 9.10.0 | 国际化 | V1.3.7 简中为主 + 预留 |
| pinia-plugin-persistedstate | 3.2.1 | 持久化 | 替代 SSR 兼容的 cookie 方案 |

### 4.2 开发依赖

| 包 | 版本 | 用途 |
|---|------|------|
| vite | 5.2.0 | 构建 |
| @vitejs/plugin-vue | 5.0.4 | Vue SFC 编译 |
| typescript | 5.4.5 | 类型 |
| vue-tsc | 2.0.6 | 类型检查 |
| eslint | 8.57.0 | 代码检查 |
| @typescript-eslint/parser | 7.7.0 | TS 解析 |
| eslint-plugin-vue | 9.25.0 | Vue 规则 |
| prettier | 3.2.5 | 格式化 |
| vitest | 1.5.0 | 单元测试 |
| @vue/test-utils | 2.4.6 | 组件测试 |
| @playwright/test | 1.43.0 | E2E |
| msw | 2.2.13 | API Mock |
| unplugin-vue-components | 0.26.0 | Element Plus 按需 |
| unplugin-auto-import | 0.17.5 | API 按需 |
| @rollup/plugin-visualizer | 5.12.0 | Bundle 分析 |
| storybook | 7.6.17 | 组件文档 |
| @storybook/vue3-vite | 7.6.17 | Vue 3 适配 |
| husky | 9.0.11 | Git hooks |
| lint-staged | 15.2.2 | 暂存区检查 |
| stylelint | 16.3.1 | SCSS 检查 |
| sass | 1.77.0 | SCSS 编译 |

### 4.3 关键依赖理由

- **openapi-typescript-codegen** 而非手写 DTO：与 `backend/spec/openapi.yaml` 单一可信源对齐
- **decimal.js** 而非 number：金额精度符合 `<MoneyInput>` 强约束
- **pdfjs-dist** 而非 iframe：月度对账需禁止下载/打印按钮（V1.3.6 红线）
- **MSW** 而非 mockjs：E2E 与单测同源 Mock，便于拦截 5 类错误码

---

## 5. Source Tree

### 5.1 目录结构

```
web-impl/
├── .orchestrix-core/
│   └── core-config.yaml
├── docs/
│   ├── architecture.md                ← 本文件
│   ├── architecture/
│   │   ├── coding-standards.md
│   │   ├── tech-stack.md
│   │   ├── source-tree.md
│   │   └── story-reviews/
│   └── stories/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                           # openapi-typescript-codegen 输出
│   │   ├── index.ts                   # 入口：导出全部
│   │   ├── models/                    # DTO 类型
│   │   └── services/                  # 业务接口
│   ├── assets/                        # 静态资源
│   │   ├── images/
│   │   ├── icons/
│   │   └── fonts/
│   ├── components/                    # 自研组件
│   │   ├── base/                      # 基础原子
│   │   │   ├── BaseButton.vue
│   │   │   ├── BaseIcon.vue
│   │   │   └── BaseDialog.vue
│   │   ├── business/                  # 业务组件
│   │   │   ├── ScanButton.vue
│   │   │   ├── StatusTag.vue
│   │   │   ├── MoneyInput.vue
│   │   │   ├── FileUpload.vue
│   │   │   ├── TableFilter.vue
│   │   │   ├── PermissionButton.vue
│   │   │   ├── ApprovalTimeline.vue
│   │   │   ├── ReworkBadge.vue        # V1.3.4 新增
│   │   │   ├── EmailConfigForm.vue    # V1.3.7 新增
│   │   │   └── SignedScanUpload.vue   # V1.3.6 新增
│   │   └── README.md                  # Storybook 索引
│   ├── composables/                   # 组合式函数
│   │   ├── useAuth.ts
│   │   ├── usePermission.ts
│   │   ├── useTablePagination.ts
│   │   └── useDebounce.ts
│   ├── directives/                    # 自定义指令
│   │   ├── permission.ts              # v-permission
│   │   └── copy.ts                    # v-copy
│   ├── layouts/                       # 布局
│   │   ├── DefaultLayout.vue
│   │   ├── AuthLayout.vue
│   │   └── components/
│   │       ├── SidebarMenu.vue
│   │       ├── HeaderBar.vue
│   │       └── Breadcrumb.vue
│   ├── router/
│   │   ├── index.ts
│   │   ├── routes.ts                  # 7 大顶级菜单路由
│   │   ├── guards.ts                  # 鉴权 + 红线钩子
│   │   └── redlines.ts                # V1.3.7 红线 grep 校验
│   ├── stores/                        # Pinia 4 store
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   ├── app.ts
│   │   └── workflow.ts
│   ├── styles/                        # SCSS 7-1 架构
│   │   ├── abstracts/
│   │   │   ├── _variables.scss
│   │   │   ├── _functions.scss
│   │   │   ├── _mixins.scss
│   │   │   └── _placeholders.scss
│   │   ├── base/
│   │   │   ├── _reset.scss
│   │   │   ├── _typography.scss
│   │   │   └── _animations.scss
│   │   ├── components/
│   │   │   ├── _button.scss
│   │   │   ├── _table.scss
│   │   │   └── _form.scss
│   │   ├── layout/
│   │   │   ├── _header.scss
│   │   │   ├── _sidebar.scss
│   │   │   └── _grid.scss
│   │   ├── pages/
│   │   │   ├── _login.scss
│   │   │   ├── _dashboard.scss
│   │   │   └── _reconcile.scss
│   │   ├── themes/
│   │   │   ├── _element-overrides.scss
│   │   │   └── _v137-palette.scss     # V1.3.7 7 状态机色板
│   │   ├── vendors/
│   │   │   └── _element-plus.scss
│   │   └── main.scss
│   ├── types/                         # 全局类型
│   │   ├── env.d.ts
│   │   ├── api.d.ts
│   │   ├── redline.d.ts               # V1.3.7 红线常量
│   │   └── global.d.ts
│   ├── utils/                         # 工具
│   │   ├── request.ts                 # Axios 实例
│   │   ├── auth.ts                    # token 管理
│   │   ├── error-code.ts              # 4 类错误码
│   │   ├── format.ts                  # 格式化
│   │   ├── validate.ts                # 校验
│   │   └── redline-grep.ts            # 5 条红线 grep
│   ├── views/                         # 页面（feature-based）
│   │   ├── dashboard/                 # 工作台
│   │   │   └── ProductionDashboard.vue
│   │   ├── sales/                     # 销售（E2）
│   │   │   ├── customer/
│   │   │   ├── quotation/
│   │   │   ├── order/
│   │   │   └── contract/
│   │   ├── production/                # 生产（E5）
│   │   │   ├── workorder/
│   │   │   ├── process-allocation/    # V1.3.7 关键
│   │   │   │   └── ProcessAllocation.vue
│   │   │   ├── outsub-order/          # V1.3.7 关键
│   │   │   │   └── OutsubOrderCreate.vue
│   │   │   ├── outsub-panel/          # E11-S4
│   │   │   ├── rework/                # E6-S6
│   │   │   └── equipment/
│   │   ├── material/                  # 物料（E3+E4+E9-S5）
│   │   │   ├── drawing/
│   │   │   ├── bom/
│   │   │   ├── process/
│   │   │   ├── inventory/
│   │   │   └── cost-aggregator/       # V1.3.4 关键
│   │   ├── quality/                   # 品质（E7）
│   │   ├── sourcing/                  # 采购（E8+E6）
│   │   │   ├── inquiry/
│   │   │   ├── purchase/
│   │   │   ├── arrive/
│   │   │   ├── reconcile/             # V1.3.6 关键
│   │   │   │   └── ReconcileDetail.vue
│   │   │   └── vendor/
│   │   ├── finance/                   # 财务（E9）
│   │   └── admin/                     # 管理（E1）
│   │       ├── user/
│   │       ├── workflow/
│   │       ├── dict/
│   │       ├── email-config/          # V1.3.7 关键
│   │       │   └── EmailConfig.vue
│   │       └── personnel/
│   ├── App.vue
│   ├── main.ts
│   └── env/
│       ├── .env.development
│       ├── .env.staging
│       ├── .env.production
│       └── .env.local
├── tests/
│   ├── unit/
│   ├── component/
│   └── e2e/
│       ├── fixtures/
│       └── specs/
├── scripts/
│   ├── openapi-gen.sh                 # openapi-typescript-codegen 脚本
│   ├── redline-grep.sh                # 5 条红线 grep 验证
│   └── verify-v137.sh                 # V1.3.7 一键验证
├── .storybook/
│   ├── main.ts
│   └── preview.ts
├── .eslintrc.cjs
├── .prettierrc.json
├── .stylelintrc.cjs
├── tsconfig.json
├── vite.config.ts
├── package.json
└── README.md
```

### 5.2 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 目录 | kebab-case | `process-allocation/` |
| Vue SFC | PascalCase | `ProcessAllocation.vue` |
| 普通 .ts | camelCase | `useAuth.ts` |
| 类型/接口 | PascalCase | `UserInfo` |
| 枚举 | PascalCase + UPPER 值 | `OutsubStatus.PENDING_SHIP` |
| 常量 | UPPER_SNAKE | `MAX_FILE_SIZE_MB = 10` |
| CSS 类（SCSS 变量） | kebab-case + 前缀 | `$v137-status-pending-ship` |
| 路径别名 | `@/` → `src/` | `@/components/business/StatusTag.vue` |

### 5.3 路径别名（tsconfig.json + vite.config.ts）

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@components/*": ["src/components/*"],
      "@views/*": ["src/views/*"],
      "@stores/*": ["src/stores/*"],
      "@utils/*": ["src/utils/*"],
      "@api/*": ["src/api/*"],
      "@styles/*": ["src/styles/*"],
      "@types/*": ["src/types/*"]
    }
  }
}
```

---

## 6. Component Standards

### 6.1 自研组件（10 个 · V1.3.7 升级）

| 组件 | 用途 | 关键 props | Story |
|------|------|-----------|-------|
| `<ScanButton>` | APP 扫码壳（PC 端降级为二维码显示） | `codeType: 'GD' \| 'WL' \| 'WW' \| 'LZ' \| 'SB'` | 1.x |
| `<StatusTag>` | 7 状态机状态徽章（带 V1.3.4 色板） | `status: OutsubStatus`、`size` | 5.x |
| `<MoneyInput>` | 金额输入（BigDecimal 严格） | `value`、`precision: number`、`currency: 'CNY'` | 9.x |
| `<FileUpload>` | 文件上传（含 AES-256-GCM 提示） | `bucket: 'common' \| 'signed_scan' \| 'avatar'`、`maxSize` | 6.x |
| `<TableFilter>` | 高密度表格 + 过滤 + 列可隐藏 | `columns`、`data`、`filterable`、`virtualized` | 5.x |
| `<PermissionButton>` | 权限按钮 + V1.3.7 红线按钮"被禁用"样式 | `permission: string`、`redline?: RedlineKey` | 1.x |
| `<ApprovalTimeline>` | 审批时间线 | `steps: ApprovalStep[]` | 1.x |
| `<ReworkBadge>` | 返修次数徽章（≥2 深红 #82071e） | `count: number`、`threshold: number` | 6.x |
| `<EmailConfigForm>` | 163 邮箱配置表单（5 Section） | `modelValue: EmailConfig` | 1.x |
| `<SignedScanUpload>` | 签字扫描件上传（AES-256-GCM + 下载限 3 角色提示） | `value`、`readonly` | 6.x |

### 6.2 第三方库（Element Plus · 9 类）

| 组件 | 用途 |
|------|------|
| `el-table` / `el-table-v2` | 表格（v2 用于 1 万行虚拟滚动） |
| `el-form` / `el-form-item` | 表单 |
| `el-dialog` / `el-drawer` | 弹窗与抽屉 |
| `el-cascader` | BOM 多级 / 工艺路线 |
| `el-date-picker` | 日期范围 |
| `el-tree` | 工艺树 / 库位树 |
| `el-select` / `el-cascader-panel` | 下拉 |
| `el-upload` | 上传（基座，包装为 `<FileUpload>`） |
| `el-tabs` / `el-steps` | Tab 与步骤条（5 Tab 料号成本） |

### 6.3 命名与 props 规范

- **命名 PascalCase**，文件名 `kebab-case`；
- **props 必填**：使用 TypeScript interface 显式声明；
- **emit 事件**：使用 `defineEmits<{ (e: 'update:modelValue', v: T): void }>()` 形式；
- **插槽**：用命名插槽 `header` / `footer` / `actions` / `<slot name="cell-{key}">`；
- **v-model**：统一 `modelValue` + `update:modelValue`；
- **样式穿透**：使用 `:deep()` 限定根作用域，避免污染 Element Plus。

```vue
<!-- 标准组件示例：StatusTag.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { OutsubStatus } from '@/api/models/OutsubStatus'

interface Props {
  status: OutsubStatus
  size?: 'small' | 'default' | 'large'
}
const props = withDefaults(defineProps<Props>(), { size: 'default' })

const colorMap: Record<OutsubStatus, string> = {
  [OutsubStatus.PENDING_SHIP]:       'v137-status-pending-ship',     // 蓝
  [OutsubStatus.SHIPPING]:           'v137-status-shipping',          // 青
  [OutsubStatus.PENDING_INSPECTION]: 'v137-status-pending-inspection',// 黄
  [OutsubStatus.INSPECTING]:         'v137-status-inspecting',       // 紫
  [OutsubStatus.QUALIFIED_STORAGE]:  'v137-status-qualified-storage',// 橙
  [OutsubStatus.STORED]:             'v137-status-stored',            // 绿
  [OutsubStatus.REPAIR_REQUESTED]:   'v137-status-repair-requested', // 红
  [OutsubStatus.NOTIFIED_REPAIR]:    'v137-status-notified-repair'    // 深红
}

const cls = computed(() => colorMap[props.status])
</script>
<template>
  <span :class="['status-tag', cls, `size-${size}`]">
    <slot>{{ status }}</slot>
  </span>
</template>
```

---

## 7. State Management

### 7.1 Pinia 4 Store 总览

| Store | 状态 | 持久化 | 备注 |
|-------|------|--------|------|
| `auth` | `token`、`refreshToken`、`isAuthenticated` | localStorage（加密） | JWT 管理 |
| `user` | `profile`、`permissions[]`、`roles[]`、`redlineFlags` | sessionStorage | 含 5 条红线角色标记 |
| `app` | `sidebarCollapsed`、`theme`、`locale`、`nacosConfig` | localStorage | 全局 UI 状态 |
| `workflow` | `pendingApprovals`、`currentApproval`、`approvalHistory` | 不持久化 | 审批工作流 |

### 7.2 Store 示例（auth）

```ts
// src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { encrypt, decrypt } from '@/utils/crypto'

export const useAuthStore = defineStore(
  'auth',
  () => {
    const token = ref<string>('')
    const refreshToken = ref<string>('')

    const isAuthenticated = computed(() => !!token.value)

    function setToken(t: string, rt: string) {
      token.value = t
      refreshToken.value = rt
    }

    function clearAuth() {
      token.value = ''
      refreshToken.value = ''
    }

    return { token, refreshToken, isAuthenticated, setToken, clearAuth }
  },
  {
    persist: {
      key: 'erp.auth',
      storage: localStorage,
      serializer: { serialize: encrypt, deserialize: decrypt }
    }
  }
)
```

### 7.3 持久化策略

- **auth**：localStorage + AES 加密（KMS 注入 key，前端读不到明文）
- **user**：sessionStorage（关闭标签自动失效，避免越权）
- **app**：localStorage（用户偏好保留）
- **workflow**：不持久化（每次进审批重新拉取）

### 7.4 SSR 兼容

- 所有 Store 使用 `setup` 风格，无 `this` 依赖；
- `pinia-plugin-persistedstate` 默认在浏览器环境生效，SSR 需通过 `pinia.use(...)` 注入；
- 本项目为纯 SPA，不启用 SSR，但保留兼容写法（参考 Nuxt 3 移植路径）。

---

## 8. API Integration

### 8.1 Axios 实例

```ts
// src/utils/request.ts
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { ErrorCodeHandler } from './error-code'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截：注入 JWT
service.interceptors.request.use((cfg) => {
  const auth = useAuthStore()
  if (auth.token) cfg.headers.Authorization = `Bearer ${auth.token}`
  return cfg
})

// 响应拦截：4 类错误码处理
service.interceptors.response.use(
  (resp) => resp.data,
  async (err) => {
    const { response, code, message } = err
    if (response) {
      const { status, data } = response
      await ErrorCodeHandler.handle(status, data?.code, data?.message)
    } else if (code === 'ECONNABORTED') {
      ElMessage.error('请求超时')
    } else {
      ElMessage.error(message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default service
```

### 8.2 openapi-typescript-codegen

- **生成命令**：`./scripts/openapi-gen.sh`
- **输入**：`../smart-workshop-erp/backend/spec/openapi.yaml`
- **输出**：`src/api/{models,services}/`
- **关键约束**：
  - 每次 backend PR merge 必须重新生成；
  - 提交 CI 检查 `git diff --exit-code src/api/`；
  - 不允许手改 `src/api/` 下任何文件。

```bash
#!/usr/bin/env bash
# scripts/openapi-gen.sh
set -e
npx openapi-typescript-codegen \
  --input ../smart-workshop-erp/backend/spec/openapi.yaml \
  --output src/api \
  --client axios \
  --useUnionTypes
```

### 8.3 错误码 4 类处理

| HTTP 状态 | 业务码 | 处理 |
|-----------|--------|------|
| 401 | UNAUTHORIZED | 跳转登录 + 清空 auth store |
| 403 | FORBIDDEN | ElMessageBox：'权限不足' |
| 4xx | VALIDATION_ERROR | 表单字段红字提示 |
| 5xx | SERVER_ERROR | ElMessage.error + Sentry 上报 |
| 业务 | RATE_LIMIT | 倒计时提示 |

### 8.4 WebSocket（返修预警推送）

- 端点：`/ws/repair-alert`（握手时携带 JWT）
- 重连：指数退避（1s/2s/4s/8s/16s）
- 接收消息：dispatch 到 `workflow` store，触发 `<ReworkBadge>` 刷新

---

## 9. Routing

### 9.1 7 大顶级菜单路由结构

```ts
// src/router/routes.ts
import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/ProductionDashboard.vue'),
        meta: { title: '工作台', icon: 'Dashboard' }
      },
      // ... 7 大顶级菜单
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { layout: 'auth' }
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/NotFound.vue')
  }
]
```

### 9.2 7 大顶级菜单

| # | path | 标题 | 角色 |
|---|------|------|------|
| 1 | `/dashboard` | 工作台 | 全员 |
| 2 | `/sales/*` | 销售 | 业务员、经理 |
| 3 | `/production/*` | 生产 | 生管、操作工（PC 端辅助） |
| 4 | `/material/*` | 物料 | 工程师、仓管、采购 |
| 5 | `/quality/*` | 品质 | 品检、生管 |
| 6 | `/sourcing/*` | 采购 | 采购 |
| 7 | `/finance/*` | 财务 | 财务、总经理 |
| 8 | `/admin/*` | 管理 | 管理员 |

### 9.3 鉴权守卫

```ts
// src/router/guards.ts
import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { runRedlineCheck } from './redlines'

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const auth = useAuthStore()
    const user = useUserStore()

    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    if (to.meta.permission && !user.hasPermission(to.meta.permission as string)) {
      ElMessage.error('权限不足')
      return next(false)
    }

    // V1.3.7 红线校验（生管页禁厂商、采购页禁工序、对账页禁线下等）
    const redlineResult = await runRedlineCheck(to)
    if (!redlineResult.passed) {
      ElMessage.error(`V1.3.7 红线违反：${redlineResult.violations.join('、')}`)
      return next(false)
    }

    next()
  })
}
```

### 9.4 V1.3.7 红线路由钩子

| 路由 | 红线校验 |
|------|---------|
| `/production/process-allocation` | 无 `VendorSelect` |
| `/sourcing/outsub-order/create` | 无 `ProcessDecisionToggle` |
| `/sourcing/reconcile/*` | 无 `OfflineAction` / `PaperPrint` |
| `/sourcing/vendor` | `notifyChannel.options.length === 1` |
| `/admin/message-center` | 无 `SmsSend` 按钮 |

---

## 10. Styling Guidelines

### 10.1 SCSS 7-1 架构

```
styles/
├── abstracts/   # 变量 / 函数 / mixin（不输出 CSS）
├── base/        # 重置 / 排版 / 动效
├── components/  # 组件级样式
├── layout/      # 布局样式
├── pages/       # 页面级样式
├── themes/      # 主题与覆盖
├── vendors/     # 第三方覆盖
└── main.scss    # 入口
```

### 10.2 V1.3.7 7 状态机色板

```scss
// src/styles/themes/_v137-palette.scss

// 7 主状态
$v137-status-pending-ship:       #0969da; // 蓝
$v137-status-shipping:           #0891b2; // 青
$v137-status-pending-inspection: #d97706; // 黄
$v137-status-inspecting:         #7c3aed; // 紫
$v137-status-qualified-storage:  #ea580c; // 橙
$v137-status-stored:             #1a7f37; // 绿
$v137-status-repair-requested:   #cf222e; // 红
$v137-status-notified-repair:    #82071e; // 深红（衍生态）

// 警告与危险
$v137-warn:  #d97706;
$v137-danger: #cf222e;
$v137-deep-red: #82071e; // 返修 ≥ 2 / 数据泄露
```

### 10.3 Element Plus 主题覆盖

```scss
// src/styles/themes/_element-overrides.scss
@forward 'v137-palette';

:root {
  --el-color-primary: #0969da;
  --el-color-success: #1a7f37;
  --el-color-warning: #d97706;
  --el-color-danger: #cf222e;
  --el-color-error: #cf222e;
  --el-color-info: #6b7280;

  --el-border-radius-base: 4px;
  --el-font-size-base: 14px;
}
```

### 10.4 响应式断点

| 断点 | 宽度 | 设备 |
|------|------|------|
| xs | < 640px | 手机 |
| sm | 640-1024px | 平板 |
| md | 1024-1280px | 桌面（小） |
| lg | 1280-1536px | 桌面（中） |
| xl | ≥ 1536px | 桌面（大） |

Web 端以 lg 为默认；APP 端（适配预留）以 xs/sm 为主。

---

## 11. Testing Strategy

### 11.1 测试金字塔

```
                ┌──────────────┐
                │   E2E 5%     │  Playwright · 关键 5 流程
                ├──────────────┤
                │  组件 25%    │  @vue/test-utils · 10 业务组件
                ├──────────────┤
                │  单元 70%    │  Vitest · utils/store/composables
                └──────────────┘
```

### 11.2 Vitest 单元测试

- 范围：`utils/`、`composables/`、`stores/`、`utils/error-code.ts`、`utils/redline-grep.ts`
- 覆盖率：≥ 70%（行/分支/函数）
- 必测：5 条红线 grep 函数、红线常量、`<MoneyInput>` 精度、Axios 拦截器

### 11.3 @vue/test-utils 组件测试

- 范围：10 业务组件（`<StatusTag>`、`<ReworkBadge>`、`<EmailConfigForm>` 等）
- 用例：props 渲染、emit 事件、v-model 双向、插槽穿透
- MSW 拦截 5 类错误码（401/403/VALIDATION/SERVER/RATE_LIMIT）

### 11.4 Playwright E2E

5 关键流程：
1. 登录 → 工作台 → 工序分配（生管）
2. 登录 → 待委外清单 → 委外下单 → 创建 WW- 单（采购）
3. 登录 → 月度对账 → 4 步操作（PDF→邮件→签字→确认）
4. 登录 → 料号成本 → Ctrl+K 搜料号 → 5 Tab 切换
5. 登录 → 邮件配置 → 测发邮件（163 SMTP）

每流程 ≤ 30 步；P95 完成时间 ≤ 30s。

### 11.5 覆盖率与质量门

- 行/分支覆盖率 ≥ 70%
- E2E 通过率 100%
- 任意红线 grep 失败 = build 失败

---

## 12. Environment Configuration

### 12.1 Vite env 5 类

| 文件 | 用途 |
|------|------|
| `.env.development` | 本地开发（mock API + 假 JWT） |
| `.env.staging` | 预发（连后端预发） |
| `.env.production` | 生产 |
| `.env.local` | 个人覆盖（不提交） |
| `.env.test` | Vitest 单元测试（MSW 拦截） |

### 12.2 关键变量

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=佰泰胜 ERP (Dev)
VITE_USE_MOCK=true
VITE_SENTRY_DSN=
VITE_NACOS_NAMESPACE=dev

# .env.production
VITE_API_BASE_URL=https://erp.yourcompany.com/api
VITE_APP_TITLE=佰泰胜 ERP
VITE_USE_MOCK=false
VITE_SENTRY_DSN=https://xxx@sentry.io/123
VITE_NACOS_NAMESPACE=prod
```

### 12.3 Nacos 配置热更新

- 后端通过 Nacos 推送运行时配置（如：返修阈值、告警阈值、3 角色白名单）；
- 前端通过 `app` store 缓存 `nacosConfig`；
- WebSocket 接收 `nacos-update` 事件 → 刷新 store → 触发相关组件 watch；
- 前端 TypeScript 强类型覆盖 Nacos 字段（`src/types/nacos.d.ts`）。

---

## 13. Coding Standards

### 13.1 ESLint + Prettier + TypeScript

- `strict: true`（TS）
- `noImplicitAny`、`strictNullChecks`、`noUncheckedIndexedAccess` 开启
- ESLint 规则继承 `@vue/eslint-config-typescript` + `@vue/eslint-config-prettier`
- Prettier：2 空格 / 单引号 / 无分号 / 100 字符

### 13.2 Vue 3 风格指南

- `<script setup lang="ts">` 优先
- 组件名 ≥ 2 词（避免与 HTML 标签冲突）
- props 显式类型 + withDefaults
- 事件 `defineEmits` 强类型
- 避免在 `setup` 外的全局副作用
- 组合式函数命名 `useXxx`

### 13.3 V1.3.7 红线代码级 grep 验证

详见 §14，包含 5 条 grep 脚本。

### 13.4 提交规范（commitlint）

- `feat:` / `fix:` / `chore:` / `docs:` / `refactor:` / `test:` / `style:` / `perf:`
- scope 必填（如 `feat(production): add ProcessAllocation view`）
- header ≤ 72 字符
- body 关联 story ID

### 13.5 Git hooks（husky + lint-staged）

- pre-commit：`eslint --fix` + `prettier --write` + `redline-grep.sh`
- commit-msg：commitlint 检查
- pre-push：`vue-tsc --noEmit` + `vitest run`

---

## 14. UI/UX 5 条红线实现

### 14.1 红线清单（V1.3.7 客户原话）

| # | 红线 | 实现位置 | 验证方法 |
|---|------|---------|---------|
| 1 | 生管页面无 `VendorSelect` 组件 | `views/production/process-allocation/` | `grep -r 'VendorSelect' src/views/production/process-allocation/` |
| 2 | 采购页面无 `ProcessDecisionToggle` 组件 | `views/sourcing/outsub-order/` | `grep -r 'ProcessDecisionToggle' src/views/sourcing/outsub-order/` |
| 3 | 对账页面无 `OfflineAction` 组件 | `views/sourcing/reconcile/` | `grep -rE 'OfflineAction\|PaperPrint' src/views/sourcing/reconcile/` |
| 4 | 厂商资料 `notifyChannel` options 长度 = 1 | `views/sourcing/vendor/` | 单测断言 + grep 固定常量 |
| 5 | 消息中心无 `SmsSend` 按钮 | `views/admin/message-center/` | `grep -r 'SmsSend' src/views/admin/message-center/` |

### 14.2 grep 脚本（scripts/redline-grep.sh）

```bash
#!/usr/bin/env bash
# scripts/redline-grep.sh
# V1.3.7 UI 红线 grep 验证 — 任意一条不通过 = build 失败
set -e
cd "$(dirname "$0")/.."
RED=0

# 红线 1：生管页面无 VendorSelect
if grep -rE "VendorSelect|vendor-dropdown" src/views/production/process-allocation/ ; then
  echo "[FAIL] 红线 1：生管页面出现 VendorSelect / vendor-dropdown"
  RED=1
else
  echo "[PASS] 红线 1：生管页面无 VendorSelect"
fi

# 红线 2：采购页面无 ProcessDecisionToggle
if grep -rE "ProcessDecisionToggle|process-decision" src/views/sourcing/outsub-order/ ; then
  echo "[FAIL] 红线 2：采购页面出现 ProcessDecisionToggle"
  RED=1
else
  echo "[PASS] 红线 2：采购页面无 ProcessDecisionToggle"
fi

# 红线 3：对账页面无 OfflineAction
if grep -rE "OfflineAction|PaperPrint|paper-print|offline-action" src/views/sourcing/reconcile/ ; then
  echo "[FAIL] 红线 3：对账页面出现 OfflineAction / PaperPrint"
  RED=1
else
  echo "[PASS] 红线 3：对账页面无 OfflineAction"
fi

# 红线 4：厂商资料 notifyChannel options = 1
if ! grep -E "NOTIFY_CHANNEL_163_ONLY\s*=\s*\[\s*'163_EMAIL'\s*\]" src/views/sourcing/vendor/constants.ts ; then
  echo "[FAIL] 红线 4：notifyChannel options ≠ 1"
  RED=1
else
  echo "[PASS] 红线 4：notifyChannel options = 1"
fi

# 红线 5：消息中心无 SmsSend
if grep -rE "SmsSend|sms-send|sms-template" src/views/admin/message-center/ ; then
  echo "[FAIL] 红线 5：消息中心出现 SmsSend"
  RED=1
else
  echo "[PASS] 红线 5：消息中心无 SmsSend"
fi

exit $RED
```

### 14.3 单元测试（redline-grep.test.ts）

```ts
// tests/unit/utils/redline-grep.test.ts
import { describe, it, expect } from 'vitest'
import { runRedlineCheck, REDLINE_BUTTONS } from '@/utils/redline-grep'
import { NOTIFY_CHANNEL_163_ONLY } from '@/views/sourcing/vendor/constants'

describe('V1.3.7 UI 红线', () => {
  it('红线 4：notifyChannel options = 1', () => {
    expect(NOTIFY_CHANNEL_163_ONLY).toEqual(['163_EMAIL'])
  })

  it('红线 1-3 + 5：禁用按钮常量在黑名单中', () => {
    expect(REDLINE_BUTTONS).toContain('VendorSelect')
    expect(REDLINE_BUTTONS).toContain('ProcessDecisionToggle')
    expect(REDLINE_BUTTONS).toContain('OfflineAction')
    expect(REDLINE_BUTTONS).toContain('SmsSend')
  })

  it('5 条红线全部通过', async () => {
    const result = await runRedlineCheck()
    expect(result.passed).toBe(true)
  })
})
```

### 14.4 路由层兜底（src/router/redlines.ts）

```ts
// src/router/redlines.ts
import type { RouteLocationNormalized } from 'vue-router'

export interface RedlineResult {
  passed: boolean
  violations: string[]
}

export async function runRedlineCheck(to: RouteLocationNormalized): Promise<RedlineResult> {
  const violations: string[] = []
  const path = to.path

  if (path.startsWith('/production/process-allocation')) {
    // 动态 import 此路由的 Vue 组件并扫描（开发模式）
    if (import.meta.env.DEV) {
      const mod = await import('@/views/production/process-allocation/ProcessAllocation.vue')
      const source = mod.default.__file || ''
      if (/VendorSelect/.test(source)) violations.push('生管页出现 VendorSelect')
    }
  }

  // ... 其他 4 条同理（生产构建时仅依赖 grep 脚本）

  return { passed: violations.length === 0, violations }
}
```

### 14.5 红线 Playwright 验证（e2e/redlines.spec.ts）

```ts
// tests/e2e/specs/redlines.spec.ts
import { test, expect } from '@playwright/test'

test.describe('V1.3.7 5 条 UI 红线', () => {
  test.beforeEach(async ({ page }) => {
    // 登录生管 / 采购 / 管理员
    await page.goto('/login')
    // ... 登录流程
  })

  test('红线 1：生管页无厂商下拉', async ({ page }) => {
    await page.goto('/production/process-allocation')
    await expect(page.locator('[data-test="vendor-select"]')).toHaveCount(0)
  })

  test('红线 2：采购页无工序切换', async ({ page }) => {
    await page.goto('/sourcing/outsub-order/create')
    await expect(page.locator('[data-test="process-decision-toggle"]')).toHaveCount(0)
  })

  test('红线 3：对账页无线下动作', async ({ page }) => {
    await page.goto('/sourcing/reconcile/202605-001')
    await expect(page.locator('[data-test="offline-action"]')).toHaveCount(0)
  })

  test('红线 4：厂商资料 notifyChannel = 1', async ({ page }) => {
    await page.goto('/sourcing/vendor/1')
    await expect(page.locator('[data-test="notify-channel"] option')).toHaveCount(1)
  })

  test('红线 5：消息中心无 SmsSend', async ({ page }) => {
    await page.goto('/admin/message-center')
    await expect(page.locator('[data-test="sms-send-btn"]')).toHaveCount(0)
  })
})
```

---

## 15. Performance & Optimization

### 15.1 路由懒加载

```ts
{
  path: 'production/process-allocation',
  component: () => import('@/views/production/process-allocation/ProcessAllocation.vue')
}
```

- 全部 50+ 路由懒加载；
- 首屏仅加载 `DefaultLayout` + 当前路由组件。

### 15.2 虚拟滚动

- `el-table-v2` 用于 1 万行表格（料号成本 / 委外面板）；
- 自研 `useVirtualList` composable 用于消息中心列表。

### 15.3 ECharts 按需

```ts
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])
```

- 不引入完整 ECharts（节省 ~700KB）；
- 图表按页面 import。

### 15.4 Vite 构建优化

- `build.target: 'es2020'`（兼容现代浏览器）
- `build.rollupOptions.output.manualChunks`：拆 `vue`、`element-plus`、`echarts`、`pdfjs`
- `vite-plugin-compression`：gzip + brotli
- `vite-plugin-imagemin`：PNG/JPG/WebP 压缩

### 15.5 Bundle 分析

- `@rollup/plugin-visualizer` 生成 `dist/stats.html`
- 预算：主包 ≤ 300KB gz / 全包 ≤ 1.2MB gz
- CI 检查：超出预算 = build 失败

### 15.6 缓存策略

- Axios 响应：`Cache-Control` 走 Nacos 配置（默认 5 分钟）
- Vue keep-alive：工作台 + 7 大顶级菜单路由
- 静态资源：CDN 强缓存 1 年 + 文件名 hash

---

## 16. Accessibility

### 16.1 WCAG 2.1 AA 目标

- 颜色对比度 ≥ 4.5:1（正文）/ 3:1（大字号）
- 全部交互元素键盘可达
- 屏幕阅读器（NVDA / JAWS）友好

### 16.2 键盘导航

- `Tab` 序：从主到次
- `Esc` 关闭弹窗/抽屉
- `Enter` 提交表单
- `Ctrl+S` 保存（V1.3.7）
- `Ctrl+K` 全局搜索
- `Ctrl+Shift+M` 生成对账单（V1.3.7）
- `Ctrl+Shift+E` 测发邮件（V1.3.7）

### 16.3 ARIA

- `<el-button>` 包装 `<PermissionButton>` 自动加 `aria-disabled`
- 自研组件 `role` + `aria-label` 完整
- `<table>` 含 `<caption>` + `<th scope="col">`

### 16.4 焦点管理

- 路由切换时焦点移到 `<h1>`
- 弹窗打开时焦点锁内
- 删除成功后焦点回到触发按钮

---

## 17. i18n

### 17.1 当前范围（V1.3.7）

- **主语言**：简体中文（zh-CN）
- **预留**：英文（en-US）框架已就位

### 17.2 vue-i18n 接入

```ts
// src/main.ts
import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

export const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: { 'zh-CN': zhCN, 'en-US': enUS }
})
```

### 17.3 文件命名

- `src/locales/{namespace}.zh-CN.ts`
- `src/locales/{namespace}.en-US.ts`
- 命名空间按模块：`common`、`production`、`sourcing`、`finance`、`admin`、`redline`（V1.3.7 红线中文固定）

### 17.4 红线文字不翻译

- "严禁：生管在此页选厂商（V1.3.7 红线）" 等固定为中文，不进 i18n；
- 通过 `<RedlineBanner>` 组件硬编码。

---

## 18. Risks & Mitigations

| # | 风险 | 影响 | 缓解 |
|---|------|------|------|
| 1 | 5 条红线被无意引入（如生管页加厂商下拉） | 灵魂一致性 0 分 | grep 脚本 + 路由钩子 + Playwright |
| 2 | Element Plus 按需未生效导致包体积大 | 首屏慢 | unplugin-vue-components + visualizer CI |
| 3 | openapi 重新生成后类型错乱 | 编译错误 | CI 锁文件 + `git diff --exit-code` |
| 4 | Pinia 持久化 token 泄露 | 安全 | AES 加密 + KMS 注入 key |
| 5 | 委外面板 1 万行卡顿 | 用户体验 | el-table-v2 虚拟滚动 |
| 6 | ECharts 全量引入 | 体积 +700KB | 按需 use() |
| 7 | 163 邮箱发送失败 | 业务中断 | XXL-JOB 1h/6h/24h 三档重试 + 兜底推送 |

---

## 19. Deliverables Checklist

### 19.1 12 项必交付

- [ ] 1. `docs/architecture.md`（本文件）
- [ ] 2. `src/` 完整代码（50+ 路由 + 10 业务组件）
- [ ] 3. `src/api/` openapi 生成产物
- [ ] 4. `.storybook/` 10 业务组件 Storybook
- [ ] 5. `tests/unit/` Vitest 单元测试（覆盖率 ≥ 70%）
- [ ] 6. `tests/component/` @vue/test-utils 组件测试
- [ ] 7. `tests/e2e/` Playwright 5 关键流程 + 5 红线验证
- [ ] 8. `scripts/redline-grep.sh` 5 条红线 grep
- [ ] 9. `scripts/openapi-gen.sh` 自动生成
- [ ] 10. `vite.config.ts` 按需 + 拆包 + 压缩
- [ ] 11. `package.json` scripts：`dev / build / test / e2e / storybook / redline-grep / verify-v137`
- [ ] 12. `README.md` 启动 / 部署 / 验证说明

### 19.2 V1.3.7 验证 8 条

- [ ] **V1.3.7-V1**：生管页 `ProcessAllocation.vue` 不引用 `VendorSelect`
- [ ] **V1.3.7-V2**：采购页 `OutsubOrderCreate.vue` 不引用 `ProcessDecisionToggle`
- [ ] **V1.3.7-V3**：对账页 `ReconcileDetail.vue` 不引用 `OfflineAction` / `PaperPrint`
- [ ] **V1.3.7-V4**：厂商资料 `notifyChannel` options = `['163_EMAIL']`
- [ ] **V1.3.7-V5**：消息中心无 `SmsSend` 按钮
- [ ] **V1.3.7-V6**：7 状态机色板在 `<StatusTag>` 全量覆盖 8 状态
- [ ] **V1.3.7-V7**：`<ReworkBadge>` count ≥ 2 时使用 `$v137-deep-red` (#82071e)
- [ ] **V1.3.7-V8**：Playwright `redlines.spec.ts` 5 用例全通过

### 19.3 一键验证脚本

```bash
#!/usr/bin/env bash
# scripts/verify-v137.sh
set -e

echo "[1/5] 类型检查..."
pnpm vue-tsc --noEmit

echo "[2/5] ESLint..."
pnpm eslint --max-warnings 0 "src/**/*.{ts,vue}"

echo "[3/5] 单元测试..."
pnpm vitest run --coverage

echo "[4/5] 5 条红线 grep..."
./scripts/redline-grep.sh

echo "[5/5] V1.3.7 验证..."
./scripts/verify-v137-redlines.sh

echo "ALL PASS — V1.3.7 verify OK"
```

---

## 附录 A：组件依赖图

```
┌──────────────────────────────────────────────────────────────┐
│                    App.vue (DefaultLayout)                    │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────────┐    │
│  │ Sidebar  │  │  Header  │  │ <router-view>           │    │
│  │ Menu     │  │  Bar     │  │   └─ Feature Views     │    │
│  └──────────┘  └──────────┘  │      ├─ ProcessAlloc   │    │
│                              │      ├─ OutsubOrder    │    │
│                              │      ├─ ReconcileDet   │    │
│                              │      └─ EmailConfig    │    │
│                              └────────────────────────┘    │
│                                                              │
│   Pinia Stores: auth · user · app · workflow                 │
│         ↓                                                     │
│   Axios ──→ openapi-typescript-codegen ──→ backend           │
│   WebSocket ──→ /ws/repair-alert                              │
└──────────────────────────────────────────────────────────────┘
```

## 附录 B：V1.3.7 关键数据流

```
工序分配（生管）
   │
   │ POST /api/v1/outsub-allocations   ──→ backend 写入 outsub_allocation
   │
   │ WebSocket /ws/allocation-push
   ▼
待委外清单（采购）
   │
   │ POST /api/v1/outsub-orders        ──→ 创建 WW- 单
   │
   │ 163 邮箱 SMTP（XXL-JOB 兜底）     ──→ 厂商收邮件
   ▼
WW- 单流转（7 状态机）
   │
   │ PENDING_SHIP → SHIPPING → PENDING_INSPECTION → INSPECTING
   │   → QUALIFIED_STORAGE → STORED
   │   或 → REPAIR_REQUESTED → NOTIFIED_REPAIR（≥2 预警）
   ▼
月度对账（采购，4 步）
   │
   │ 1. 生成 PDF（local report + 后端 PDF）
   │ 2. 发 163 邮件
   │ 3. 上传签字扫描件（AES-256-GCM）
   │ 4. 对账已确认 → 触发付款
   ▼
付款管理（财务）
```

## 附录 C：V1.3.7 引用清单

| 来源 | 路径 | 引用章节 |
|------|------|---------|
| 产品仓 PRD | `docs/prd.md` V1.3.7 | §13 Epic / 60+ Story / 200+ AC |
| 产品仓 UX | `docs/ux-handoff.md` V1.1 | §3 IA / §4 关键页 / §5 五段式 |
| 产品仓 架构 | `docs/architect-handoff.md` V1.1 | §2 边界 / §3 模块 |
| 产品仓 Figma | `design/figma-library-upgrade.md` | §1-§5 5 页面 JSON / §〇 8 组件 |
| 产品仓 API | `backend/spec/openapi.yaml` | 全部 |
| 产品仓 合同 | `docs/contract-appendix-a-prd.md` | PRD 摘录 |
| 本仓配置 | `web-impl/.orchestrix-core/core-config.yaml` | 30+ stories 分配 |
| V1.3.6/7 changelog | `docs/prd-v1.3.6-changelog.md` / `docs/prd-v1.3.7-changelog.md` | 升级依据 |

---

**文档版本**：V1.3.7-frontend-architecture v1.0
**生成时间**：2026-06-10
**生成人**：Orchestrix Architect agent（鲁班）
**配套**：`docs/architecture/coding-standards.md` / `docs/architecture/tech-stack.md` / `docs/architecture/source-tree.md`
**下一步**：dev agent 启动 `*develop-story 1.1`

🎯 HANDOFF TO dev: *develop-story 1.1
