# Figma Library 升级清单 + 5 个新页面 JSON Mock-up

> **V1.3.7 升级日期**：2026-06-09
> **生成人**：UX Expert agent（orchestrix）
> **用途**：通知设计组把 8 个新组件 + 5 个新页面落入 Figma Library；前端据此实现 Vue 组件
> **配套**：`docs/ux-handoff.md` V1.1

---

## 〇、Figma Library 升级清单

### 1. 新增组件（3 个）—— 在 Figma 中创建 + 落入 Library

| 组件 | Figma 路径 | 描述 |
|------|-----------|------|
| **ReworkBadge** | Library/Components/V1.3.4/ReworkBadge | 返修次数徽章；< 2 灰色；≥ 2 深红（#82071e）；支持 PC + APP 双尺寸 |
| **EmailConfigForm** | Library/Forms/V1.3.7/EmailConfigForm | 163 邮箱配置表单；含 SMTP/重试/额度/告警/日志 5 个 Section |
| **SignedScanUpload** | Library/Upload/V1.3.6/SignedScanUpload | 签字扫描件上传组件；含加密提示 + 下载限 3 角色提示 |

### 2. 升级组件（5 个）

| 组件 | 升级点 |
|------|--------|
| `<StatusTag>` | V1.3.4 升级：7 状态机色板（蓝/青/黄/紫/橙/绿/红）+ NOTIFIED_REPAIR 衍生态 |
| `<TableFilter>` | V1.3.4 升级：委外面板支持按 7 状态分组 + 返修高亮 |
| `<FileUpload>` | V1.3.6 升级：bucket 类型新增 `signed_scan`，上传时提示"AES-256-GCM 加密 · 5 年保留 · 下载限 3 角色" |
| `<ScanButton>` | V1.3.5 升级：新增"扫 WW- 到货"入口图标（仓管视图） |
| `<PermissionButton>` | V1.3.7 升级：新增"代选厂商"、"代改工序"等 5 条红线按钮的视觉禁（仅设计稿中"被禁用"样式） |

### 3. 5 个新页面（按本文件 §1-§5 结构实现）

---

## §1. 工序分配页（生管视图 · V1.3.7 新增）

```json
{
  "pageId": "ux.process-allocation.scheduler",
  "name": "工序分配（生管视图）",
  "version": "V1.3.7",
  "frame": {
    "width": 1440,
    "height": 900,
    "background": "#FFFFFF",
    "padding": 24
  },
  "components": [
    {
      "id": "header",
      "type": "PageHeader",
      "props": {
        "back": true,
        "title": "工序分配（生管视图）",
        "subtitle": "GD-20260609-0001",
        "actions": [
          { "label": "保存", "type": "primary", "action": "save-allocation" }
        ]
      }
    },
    {
      "id": "workorder-summary",
      "type": "Card",
      "layout": "horizontal",
      "items": [
        { "label": "工单", "value": "GD-20260609-0001" },
        { "label": "客户", "value": "佰泰胜" },
        { "label": "数量", "value": "50 件" }
      ]
    },
    {
      "id": "route-info",
      "type": "TextBlock",
      "content": "工艺路线：下料 → 车 → CNC → 铣 → 钳 → 表处 → 质检 → 包装"
    },
    {
      "id": "allocation-table",
      "type": "Table",
      "columns": [
        { "key": "seq",        "title": "工序",         "width": 80 },
        { "key": "name",       "title": "工序名",       "width": 120 },
        { "key": "decision",   "title": "自制/委外",    "width": 200 },
        { "key": "qty",        "title": "数量",         "width": 100 },
        { "key": "status",     "title": "状态",         "width": 100 },
        { "key": "remark",     "title": "备注",         "width": 200 }
      ],
      "rows": [
        { "seq": "P01", "name": "下料",   "decision": "INHOUSE",  "qty": 50, "status": "PENDING" },
        { "seq": "P02", "name": "车床",   "decision": "OUTSOURCE","qty": 50, "status": "PENDING" },
        { "seq": "P03", "name": "CNC",    "decision": "INHOUSE",  "qty": 50, "status": "PENDING" },
        { "seq": "P04", "name": "铣",     "decision": "INHOUSE",  "qty": 50, "status": "PENDING" },
        { "seq": "P05", "name": "钳",     "decision": "INHOUSE",  "qty": 50, "status": "PENDING" },
        { "seq": "P06", "name": "表处",   "decision": "OUTSOURCE","qty": 50, "status": "PENDING" },
        { "seq": "P07", "name": "质检",   "decision": "INHOUSE",  "qty": 50, "status": "PENDING" },
        { "seq": "P08", "name": "包装",   "decision": "INHOUSE",  "qty": 50, "status": "PENDING" }
      ]
    },
    {
      "id": "warning-banner",
      "type": "Alert",
      "severity": "warning",
      "content": "已勾委外 2 道（P02/P06），提交后将推送给采购选厂商。",
      "rules": ["V1.3.7 红线"]
    },
    {
      "id": "redline-banner",
      "type": "Alert",
      "severity": "error",
      "content": "严禁：生管在此页选厂商（V1.3.7 红线）",
      "rules": ["V1.3.7 红线"]
    },
    {
      "id": "actions",
      "type": "ButtonGroup",
      "items": [
        { "label": "保存工序划分", "type": "primary", "action": "save-allocation" },
        { "label": "取消",          "type": "default", "action": "cancel" }
      ]
    }
  ],
  "redlines": {
    "no_vendor_dropdown": true,
    "comment": "决策列只有'自制/委外'单选按钮；勾委外后无厂商下拉框"
  }
}
```

---

## §2. 委外下单页（采购视图 · V1.3.7 改版）

```json
{
  "pageId": "ux.outsub-order.creator",
  "name": "委外下单（采购视图）",
  "version": "V1.3.7",
  "frame": {
    "width": 1440,
    "height": 900,
    "background": "#FFFFFF",
    "padding": 24
  },
  "components": [
    {
      "id": "header",
      "type": "PageHeader",
      "props": {
        "back": true,
        "title": "委外下单（采购视图）",
        "subtitle": "GD-20260609-0001",
        "actions": [
          { "label": "创建 WW- 单", "type": "primary", "action": "create-ww-order" }
        ]
      }
    },
    {
      "id": "source-info",
      "type": "Card",
      "items": [
        { "label": "工单",   "value": "GD-20260609-0001" },
        { "label": "来源",   "value": "生管 潘主管 推送（2026-06-09 14:23）" }
      ]
    },
    {
      "id": "pending-list",
      "type": "Card",
      "title": "待委外工序清单（2 道）",
      "items": [
        "工序 P02 - 车床加工 - 50 件",
        "工序 P06 - 表面处理 - 50 件"
      ]
    },
    {
      "id": "p02-vendor-table",
      "type": "Table",
      "title": "P02 厂商选择",
      "columns": [
        { "key": "vendor",        "title": "厂商",       "width": 200 },
        { "key": "historyPrice",  "title": "历史价",     "width": 120 },
        { "key": "historyEta",    "title": "历史交期",   "width": 120 },
        { "key": "lastDeal",      "title": "上次合作",   "width": 120 },
        { "key": "selected",      "title": "选择",       "width": 100 }
      ],
      "rows": [
        { "vendor": "张家港A厂", "historyPrice": "¥8.50/件", "historyEta": "5 天", "lastDeal": "2026-05", "selected": true  },
        { "vendor": "苏州B厂",   "historyPrice": "¥9.20/件", "historyEta": "7 天", "lastDeal": "2026-04", "selected": false },
        { "vendor": "昆山C厂",   "historyPrice": "¥7.80/件", "historyEta": "4 天", "lastDeal": "2026-03", "selected": false }
      ],
      "footer": "历史价：最近 3 次中位数 = ¥8.50（V1.3.4 自动）\n历史交期预估：4-7 天（50%/80%/100% 分位）"
    },
    {
      "id": "p06-vendor-table",
      "type": "Table",
      "title": "P06 厂商选择",
      "note": "结构同上"
    },
    {
      "id": "redline-banner",
      "type": "Alert",
      "severity": "error",
      "content": "严禁：采购在此页改工序归属（V1.3.7 红线）"
    },
    {
      "id": "create-action",
      "type": "Button",
      "label": "创建 WW- 单 → 自动发 163 邮箱通知厂商",
      "type": "primary"
    }
  ],
  "redlines": {
    "no_process_decision_toggle": true,
    "comment": "无'自制/委外'切换按钮；创建后自动触发 163 邮箱推送"
  }
}
```

---

## §3. 月度对账页（采购视图 · V1.3.6 改版 · 不含"线下"）

```json
{
  "pageId": "ux.outsub-reconcile.detail",
  "name": "月度对账（不含'线下'动作）",
  "version": "V1.3.7",
  "frame": {
    "width": 1440,
    "height": 900,
    "background": "#FFFFFF",
    "padding": 24
  },
  "components": [
    {
      "id": "header",
      "type": "PageHeader",
      "props": {
        "back": true,
        "title": "月度对账（采购）",
        "subtitle": "2026 年 5 月 · 厂商：张家港A厂"
      }
    },
    {
      "id": "status-card",
      "type": "Card",
      "items": [
        { "label": "状态",     "value": "⚠ 待对账（DRAFT）" },
        { "label": "汇总",     "value": "8 个 WW- 单 · 金额合计 ¥12,450.00" }
      ]
    },
    {
      "id": "reconcile-lines",
      "type": "Table",
      "columns": [
        { "key": "wwNo",        "title": "WW-单号",    "width": 160 },
        { "key": "process",     "title": "工序",       "width": 80 },
        { "key": "qty",         "title": "数量",       "width": 80 },
        { "key": "unitPrice",   "title": "单价",       "width": 100 },
        { "key": "amount",      "title": "金额",       "width": 100 },
        { "key": "planEnd",     "title": "计划完成",   "width": 120 }
      ],
      "rows": [
        { "wwNo": "WW-001", "process": "车", "qty": 50, "unitPrice": "¥8.50",  "amount": "¥425.00", "planEnd": "05-15" },
        { "wwNo": "WW-002", "process": "钳", "qty": 30, "unitPrice": "¥12.00", "amount": "¥360.00", "planEnd": "05-20" }
      ],
      "footer": {
        "summary": [
          { "label": "合计",   "value": "¥12,450.00" },
          { "label": "运杂费", "value": "¥200.00"   },
          { "label": "应付合计", "value": "¥12,650.00" }
        ]
      }
    },
    {
      "id": "four-step-actions",
      "type": "Stepper",
      "steps": [
        {
          "step": 1,
          "label": "生成对账单 PDF",
          "status": "completed",
          "action": "generate-pdf",
          "result": "已生成：recon-202605-001.pdf"
        },
        {
          "step": 2,
          "label": "发送对账单邮件",
          "status": "completed",
          "action": "send-email",
          "result": "163 邮箱推送：success"
        },
        {
          "step": 3,
          "label": "上传厂商签字扫描件",
          "status": "pending",
          "action": "upload-scan",
          "note": "PDF / JPG / PNG · ≤ 10MB"
        },
        {
          "step": 4,
          "label": "对账已确认 → 触发付款",
          "status": "disabled",
          "action": "confirm",
          "note": "需先完成 1/2/3"
        }
      ]
    },
    {
      "id": "redline-banner",
      "type": "Alert",
      "severity": "error",
      "content": "严禁：此页不提供'采购带纸去厂商处'、'采购线下核对'等按钮",
      "rules": ["V1.3.7 红线"]
    },
    {
      "id": "security-info",
      "type": "Alert",
      "severity": "info",
      "content": "厂商签字扫描件 AES-256-GCM 加密 · 5 年保留 · 下载限 3 角色"
    }
  ],
  "redlines": {
    "no_offline_actions": true,
    "no_paper_printing_button": true,
    "comment": "V1.3.7 客户原话：'线下流程不去涉及'"
  }
}
```

---

## §4. 料号成本聚合页（V1.3.4 新增）

```json
{
  "pageId": "ux.cost-aggregator.viewer",
  "name": "料号成本聚合（5 Tab）",
  "version": "V1.3.4",
  "frame": {
    "width": 1440,
    "height": 900,
    "background": "#FFFFFF",
    "padding": 24
  },
  "components": [
    {
      "id": "search-bar",
      "type": "SearchBar",
      "placeholder": "Ctrl+K 搜料号",
      "defaultValue": "YL-45-D80",
      "actions": [
        { "label": "搜索", "type": "primary" },
        { "label": "导出", "type": "default" }
      ]
    },
    {
      "id": "tabs",
      "type": "Tabs",
      "activeTab": "price",
      "items": [
        { "key": "price",      "label": "价格" },
        { "key": "material",   "label": "材料成本" },
        { "key": "labor",      "label": "工时成本" },
        { "key": "outsource",  "label": "外协成本" },
        { "key": "total",      "label": "总成本" }
      ]
    },
    {
      "id": "price-tab",
      "type": "Table",
      "columns": [
        { "key": "month",     "title": "月份",       "width": 120 },
        { "key": "unitPrice", "title": "单价",       "width": 120 },
        { "key": "qty",       "title": "数量",       "width": 100 },
        { "key": "customer",  "title": "客户",       "width": 200 },
        { "key": "trend",     "title": "趋势",       "width": 100 }
      ],
      "rows": [
        { "month": "2026-05", "unitPrice": "¥85.00", "qty": 100, "customer": "佰泰胜",   "trend": "↑ +5%" },
        { "month": "2026-04", "unitPrice": "¥80.95", "qty": 80,  "customer": "苏州A",     "trend": "↓ -2%" },
        { "month": "2026-03", "unitPrice": "¥82.50", "qty": 120, "customer": "佰泰胜",   "trend": "→" },
        { "month": "2026-02", "unitPrice": "¥82.50", "qty": 60,  "customer": "昆山B",     "trend": "↑ +3%" },
        { "month": "2026-01", "unitPrice": "¥80.00", "qty": 50,  "customer": "佰泰胜",   "trend": "↓ -1%" }
      ]
    },
    {
      "id": "permission-info",
      "type": "Alert",
      "severity": "info",
      "content": "权限隔离：管理层看全量；业务员只看自己客户；一线只看自己工序工时"
    },
    {
      "id": "cache-info",
      "type": "TextBlock",
      "content": "缓存策略：Redis TTL 3600s + Stream cost-invalidate 失效驱动（V1.3.4：委外单完成 / 工时录入 / 物料价格变更 → 失效重算）"
    }
  ]
}
```

---

## §5. 邮件配置后台（V1.3.7 新增）

```json
{
  "pageId": "ux.email-config.editor",
  "name": "邮件配置（163 邮箱 · 单一渠道）",
  "version": "V1.3.7",
  "frame": {
    "width": 1440,
    "height": 900,
    "background": "#FFFFFF",
    "padding": 24
  },
  "components": [
    {
      "id": "page-title",
      "type": "Title",
      "content": "邮件配置（163 邮箱 · 单一渠道）"
    },
    {
      "id": "smtp-section",
      "type": "FormSection",
      "title": "SMTP 配置",
      "fields": [
        { "key": "smtpHost",     "label": "服务器地址", "type": "input",   "defaultValue": "smtp.163.com" },
        { "key": "smtpPort",     "label": "端口",        "type": "number",  "defaultValue": 465 },
        { "key": "useSsl",       "label": "SSL/TLS",     "type": "switch",  "defaultValue": true },
        { "key": "fromAddress",  "label": "发件人地址", "type": "input",   "defaultValue": "noreply@yourcompany.com" },
        { "key": "authCode",     "label": "授权码",      "type": "password", "placeholder": "KMS 注入 · 不写死" }
      ]
    },
    {
      "id": "retry-section",
      "type": "FormSection",
      "title": "重试策略（V1.3.7 三档重试）",
      "fields": [
        { "key": "retry1",  "label": "第一档", "type": "select", "defaultValue": "1h",  "options": ["1h", "6h", "24h"], "note": "XXL-JOB job-11" },
        { "key": "retry2",  "label": "第二档", "type": "select", "defaultValue": "6h",  "options": ["1h", "6h", "24h"], "note": "XXL-JOB job-12" },
        { "key": "retry3",  "label": "第三档", "type": "select", "defaultValue": "24h", "options": ["1h", "6h", "24h"], "note": "最终失败" }
      ],
      "footer": "失败兜底：APP 推送 + PC 端红点"
    },
    {
      "id": "quota-section",
      "type": "FormSection",
      "title": "额度监控",
      "fields": [
        { "key": "dailyQuota",      "label": "日额度",          "type": "number",   "defaultValue": 5000, "note": "企业版" },
        { "key": "warnThreshold",   "label": "告警阈值",        "type": "number",   "defaultValue": 0.80, "note": "达 80% 触发企业微信告警" },
        { "key": "todaySentCount",  "label": "今日已发",        "type": "readonly", "defaultValue": "1234 封（24.7%）" }
      ]
    },
    {
      "id": "log-section",
      "type": "FormSection",
      "title": "发送日志",
      "fields": [
        { "key": "logRetentionDays",   "label": "保留天数", "type": "number", "defaultValue": 90 },
        { "key": "attachmentMaxSizeMb","label": "附件上限", "type": "number", "defaultValue": 10 }
      ],
      "footer": "清理任务：XXL-JOB job-14（每日 03:00）\n记录字段：时间 / 收件人 / 主题 / 附件 hash / SMTP 响应 / 投递状态"
    },
    {
      "id": "actions",
      "type": "ButtonGroup",
      "items": [
        { "label": "保存",         "type": "primary",   "action": "save-config" },
        { "label": "测试发送",     "type": "default",   "action": "test-send" },
        { "label": "查看日志",     "type": "default",   "action": "view-logs" }
      ]
    },
    {
      "id": "redline-banners",
      "type": "AlertGroup",
      "items": [
        { "severity": "warning", "content": "渠道收敛为单 163 邮箱（V1.3.7 删短信）" },
        { "severity": "warning", "content": "授权码由 KMS 注入，配置文件分文件存储，不入 Nacos" }
      ]
    }
  ]
}
```

---

## §6. APP 端：到货扫码页（V1.3.5 新增 · 仓管视图）

```json
{
  "pageId": "ux.app.warehouse-arrive",
  "name": "APP - 到货扫码（仓管视图）",
  "version": "V1.3.5",
  "frame": {
    "width": 360,
    "height": 720,
    "background": "#FFFFFF"
  },
  "device": "Android APP",
  "components": [
    {
      "id": "header",
      "type": "AppHeader",
      "title": "仓管 · 到货扫码",
      "back": true
    },
    {
      "id": "scan-area",
      "type": "ScanBox",
      "size": "80%",
      "hint": "扫 WW- 委外订单二维码"
    },
    {
      "id": "post-scan-form",
      "type": "Form",
      "fields": [
        { "key": "wwNo",         "label": "WW-单号",         "value": "WW-20260609-0001", "readonly": true },
        { "key": "vendor",       "label": "厂商",             "value": "张家港A厂",        "readonly": true },
        { "key": "process",      "label": "工序",             "value": "车床加工",         "readonly": true },
        { "key": "planQty",      "label": "计划数量",         "value": "50 件",            "readonly": true },
        { "key": "deliveryDate", "label": "约定交期",         "value": "2026-06-15",       "readonly": true },
        { "key": "actualQty",    "label": "实收数量",         "type": "number" },
        { "key": "actualWeight", "label": "重量（kg）",       "type": "number" },
        { "key": "photoUrls",    "label": "外观照片",         "type": "upload" }
      ]
    },
    {
      "id": "confirm-button",
      "type": "Button",
      "label": "确认到货",
      "type": "primary",
      "size": "large"
    },
    {
      "id": "info-banner",
      "type": "Alert",
      "severity": "info",
      "content": "⚠ 到货后状态自动置 PENDING_INSPECTION\n通知：生管 + 品质（APP + PC 红点）"
    }
  ]
}
```

---

## 一、设计组 Action Items

| # | 任务 | 优先级 | 截止 |
|---|------|--------|------|
| 1 | 把 3 个新组件（ReworkBadge / EmailConfigForm / SignedScanUpload）落入 Library | P0 | 6/10 |
| 2 | 升级 5 个老组件（StatusTag 7 状态机色板等） | P0 | 6/10 |
| 3 | 落入 5 个新页面 Frame（按本文件 §1-§6 JSON 结构） | P0 | 6/10 |
| 4 | 红线视觉禁：5 条 V1.3.7 红线的"被禁用"样式 demo | P0 | 6/10 |
| 5 | 导出 Figma Library v1.3.7 链接给前端 | P0 | 6/11 |

---

## 二、前端 Action Items

| # | 任务 | 截止 |
|---|------|------|
| 1 | 按 §1-§6 JSON 结构生成 Vue 3 + TypeScript 组件 | 6/12 |
| 2 | 在 `packages/web/src/views/production/` 新增 `ProcessAllocation.vue` | 6/12 |
| 3 | 在 `packages/web/src/views/sourcing/` 新增 `OutsubOrderCreate.vue` | 6/12 |
| 4 | 在 `packages/web/src/views/finance/` 新增 `ReconcileDetail.vue` | 6/12 |
| 5 | 在 `packages/web/src/views/material/` 新增 `CostAggregator.vue` | 6/12 |
| 6 | 在 `packages/web/src/views/admin/` 新增 `EmailConfig.vue` | 6/12 |
| 7 | 在 `packages/app/src/screens/warehouse/ArriveScan.tsx`（V1.3.5 新增）| 6/12 |

---

## 三、5 条 V1.3.7 红线 UI 验证

| # | 红线 | 验证方法 |
|---|------|----------|
| 1 | 生管页面 = 没有"代选厂商"按钮 | grep `vendor-dropdown` / `VendorSelect` 在生管页面 → 必须为 null |
| 2 | 采购页面 = 没有"改工序归属"按钮 | grep `process-decision-toggle` 在采购页面 → 必须为 null |
| 3 | 对账页面 = 没有"采购带纸去厂商处"按钮 | grep `offline-action` / `paper-print` 在对账页面 → 必须为 null |
| 4 | 厂商资料"通知偏好"下拉 = 只有"163 邮箱" | grep `notify-channel` options → 必须只有 1 项 |
| 5 | 消息中心 = 没有"短信发送"按钮 | grep `sms-send` / `sms-template` 在消息中心 → 必须为 null |

> **红线 0 容忍**。任何 1 条不通过 = 灵魂一致性 0 分，立即回退。

---

**文档版本**：V1.1（基于 V1.0 升级）
**生成时间**：2026-06-09
**生成人**：UX Expert agent（orchestrix）
**下一步**：设计组在 Figma 中执行 Library 升级；前端按 JSON 结构实现组件。
