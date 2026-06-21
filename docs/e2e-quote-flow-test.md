# 报价流程 V2.1 · E2E 联调指南

> 业务员建单 → 提交工程师 → 定义工艺/计算 → 提交审批 → 多级审批 → 导出 PDF → 发客户邮箱

## 环境要求

| 组件 | 端口 | 说明 |
|------|------|------|
| MySQL `cnc_business` | 3306 | 已执行 `backend/db/init.sql`（含 V95/V96） |
| erp-business | 9082 | 报价/客户/导出 API |
| erp-platform | 9080 | 登录 + 邮件发送（步骤 8 可选） |

## 自动化测试

### 1. 单元 / Mock 测试

**Node 轻量验证（推荐，无需 DB）：**

```powershell
node backend/scripts/test-quote-flow-v21.mjs
# 完整 Live API：先启动 erp-business 后
node backend/scripts/test-quote-flow-v21.mjs --live
```

**Maven Mock 测试（仅 quote 包相关类）：**

```powershell
cd backend
mvn -pl src/erp-business -am test "-Dtest=QuoteApprovalServiceTest,QuoteApprovalIntegrationTest,QuoteApprovalRouterTest,PdfExportServiceTest" -DfailIfNoTests=false
```

> 注：若 erp-business 其他历史测例编译失败，可先跑 Node 脚本验证 Router/PDF 逻辑；Live 链路用 `--live` 或 PowerShell 脚本。

覆盖：

| 测试类 | 验证点 |
|--------|--------|
| `QuoteApprovalRouterTest` | 5万/20万阈值路由 |
| `QuoteApprovalServiceTest` | 工程师提交、双签两节点 |
| `QuoteApprovalIntegrationTest` | DRAFT→PENDING_ENG→PENDING_APPROVAL→APPROVED |
| `PdfExportServiceTest` | PDF/Excel 含客户图号 |

### 2. Live API 脚本

```powershell
cd backend\scripts
.\e2e-quote-flow-v21.ps1 -BusinessUrl http://127.0.0.1:9082 -PlatformUrl http://127.0.0.1:9080
# 跳过发邮件: -SkipEmail
# 指定客户: -CustomerId 1101
```

脚本步骤：

1. 更新客户联系邮箱  
2. `POST /quotes` 创建报价（含 `customerDrawingNo`）  
3. `POST /quotes/{id}/submit-to-engineer`  
4. `POST /quotes/items/{itemId}/process` + `calculate`  
5. `POST /quotes/{id}/submit`  
6. `POST /quotes/{id}/approve`  
7. `GET /quotes/export/{id}?format=pdf` — 断言含客户图号  
8. `POST /quotes/{id}/send-email`（SMTP 未配则 SKIP）  
9. `GET /quote-cost-items` — 断言种子数据  

## 手工 Web 验收

| # | 页面 | 操作 | 预期 |
|---|------|------|------|
| 1 | 客户档案 | 编辑联系人/邮箱 | 保存成功 |
| 2 | 新建报价 | 选客户、加明细、提交工程师 | → PENDING_ENG |
| 3 | 报价工艺定义 | 定义工艺、计算 | 单价更新 |
| 4 | 报价详情 | 提交审批 | → PENDING_APPROVAL |
| 5 | 报价审批 | 通过（>20万需两次） | → APPROVED |
| 6 | 报价详情 | 导出 PDF | 下载含客户图号 |
| 7 | 报价详情 | 发送客户邮箱 | 客户收到附件（需 SMTP） |

## 大额双签验证

金额 > 20 万时，第一次 `approve` 后 `current_node=2`、状态仍为 `PENDING_APPROVAL`；第二次 `approve` 后 `APPROVED`。

可在 E2E 脚本创建报价后手动改 DB `total_amount=250000` 再跑审批步骤验证。
