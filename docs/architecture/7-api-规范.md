# 7. API 规范

## 7.1 URL 命名（V1.3.7 新增 3 个端点类）

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

## 7.2 响应格式（不变）

成功：`{ "code": 0, "message": "ok", "data": {...}, "traceId": "..." }`
失败：`{ "code": 40001, "message": "...", "data": null, "traceId": "..." }`
分页：`{ "code": 0, "data": { "records": [...], "total": 100, "pageNum": 1, "pageSize": 20, "pages": 5 } }`

## 7.3 错误码体系（V1.3.7 删 90002 短信网关错误）

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

## 7.4 关键 API 端点（V1.3.7 新增 12 个，共 67+）

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
