# 工程师核心链路 · E2E 联调指南

> 图纸 → CAD 附件 → 工程转化 → BOM → 工艺路线 → 报价预览

## 环境要求

| 组件 | 端口 | 说明 |
|------|------|------|
| MySQL `cnc_business` | 3306 | 已执行 `init.sql` / 迁移 |
| erp-business | 9082 | `mvn spring-boot:run` |
| erp-production | 9083 | 工艺路线发布/预览（可选） |
| MinIO | 9000 | 可选；未启动时 dev 配置自动 **本地回退** `./data/drawing-files` |

### Dev MinIO 配置（`application.yml`）

```yaml
app.minio:
  endpoint: http://127.0.0.1:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-drawing: erp-drawing
  use-local-fallback: true   # MinIO 不可用时写本地目录
  local-fallback-dir: ./data/drawing-files
```

## 自动化

### 1. Mock 集成测试（无需 DB）

```powershell
cd backend\src\erp-business
mvn compile
# 类：EngineeringWorkflowE2ETest · DrawingAttachmentServiceTest · ConversionServiceTest
```

覆盖步骤：CAD 上传/下载 → 工程转化(WL+BOM) → BOM save-tree → RELEASED 工艺过滤。

### 2.  live API 脚本

```powershell
cd backend\scripts
.\e2e-engineer-workflow.ps1 -BaseUrl http://127.0.0.1:9082
# 指定图纸：-DrawingId 1
```

脚本步骤：
1. 列出 RELEASED 图纸
2. 上传 `.dxf` CAD 附件 → `crm_drawing_attachment`
3. `POST /drawings/{id}/convert` → WL 料号 + BOM
4. `POST /boms/save-tree` 挂载子件
5. `GET /materials/{id}/process-route` 验证发布过滤

## 手工 Web 验收

| # | 页面 | 操作 | 预期 |
|---|------|------|------|
| 1 | 图纸 → 工程转化 | 选 RELEASED 图 → 上传 CAD → 转化 | 弹窗显示 WL- 料号 + bomId |
| 2 | 立即去维护 BOM | 点击按钮 | 跳转 `/material/boms/edit?bomId=` |
| 3 | BOM 编辑 | + 添加子件 → 保存 → 发布 | Toast 成功，状态 RELEASED |
| 4 | 工艺路线维护 | 选已转化图号 | 显示 WL 料号（非「待工程转化」） |
| 5 | 编辑工艺路线 | 排序 → 保存草稿 → 发布 | routeStatus=RELEASED |
| 6 | 销售报价 | 选图号 → 工艺预览 | 显示已发布工序 |

## CAD/CAM 附件（FR-3-2-2）

- **表**：`crm_drawing_attachment`（V71 迁移）
- **API**：
  - `GET /drawings/{id}/attachments` 列表
  - `POST /drawings/{id}/attachments` 上传（dxf/step/stp/nc/dwg/pdf，≤50MB）
  - `GET /drawings/attachments/{id}/download` 下载
- **存储**：MinIO `erp-drawing/cad/{drawingId}/…` 或本地 `local://…` 回退路径
- **前端**：工程转化向导 Step 2 · 图纸详情 · `DrawingCadAttachments.vue`
