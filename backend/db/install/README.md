# 数据库安装与演示数据

## 空库一次性导入（推荐）

只需执行 **`backend/db/init.sql`**，已包含：

| 内容 | 说明 |
|------|------|
| 三库 DDL | `cnc_platform` / `cnc_business` / `cnc_production` 全部表 |
| 增量迁移 | V2/V3 工作流 + `migrations/V3~V66` + **V60a/V60b（cnc_production）** |
| 基线种子 | admin、角色、字典、工作流模板、工序库 |
| 演示 Mock | 扫码三码工单、看板、报价/订单、物料码 |

### 含完整 Mock 演示数据（推荐联调 / UAT）

执行 **`backend/db/init_data.sql`**（= `init.sql` 结构 + 演示种子 + **50 在途订单 + 50 员工** 全链路数据）：

| 内容 | 说明 |
|------|------|
| 同上全部 DDL/迁移 | 含 V65 工单关联销售订单 · V66 委外单 drawing_id |
| demo-flow-seed | 扫码三码 · 演示账号 · 订单 7001 |
| demo-finance-rfq-seed | 成本核算 · RFQ · 利润率参数 |
| mock-bulk-seed | 50 订单 `XS-MOCK-0001~0050`、工单、委外、PO、质检 |
| 人事 Mock | 50 员工 `EM-MOCK-0001~0050`、考勤、6 月薪酬、绩效 |

```powershell
# Windows · 完整 Mock（约 550KB）
mysql -h 10.100.4.10 -P 3306 -u xm_admin -p --default-character-set=utf8mb4 < backend\db\init_data.sql

# 重新生成 init_data.sql（改 mock 脚本后）
cd backend\db
powershell -ExecutionPolicy Bypass -File .\build-init-data.ps1
```

**执行后末尾应显示 `cnc_production` 约 33 张表。** 若 GUI 工具遇错中断，请用 `import-all.ps1` 或补跑 `sync-cnc-production.ps1`。

```powershell
# Windows
mysql -h 10.100.4.10 -P 3306 -u xm_admin -p --default-character-set=utf8mb4 < backend\db\init.sql

# 或使用脚本（等价）
cd backend\db\install
.\import-all.ps1
```

## 维护 init.sql / init_data.sql

修改基线结构请编辑 `backend/db/init.baseline.sql`，修改增量表请编辑 `backend/db/migrations/`，然后：

```powershell
cd backend\db
.\build-init.ps1          # 生成 init.sql
.\build-init-data.ps1     # 生成 init_data.sql（含 50 订单/50 员工 Mock）
```

批量 Mock 逻辑在 `backend/db/install/generate-mock-bulk-data.py`，可调整 `ORDER_COUNT` / `EMPLOYEE_COUNT` 后重跑 `build-init-data.ps1`。

## 演示账号（每角色 1 人 · 密码均为 **123456**）

| 用户名 | 角色编码 | 姓名 |
|--------|----------|------|
| admin | SYS_ADMIN | 系统管理员 |
| sales | SALES | 业务员 |
| sales_mgr | SALES_MGR | 销售经理 |
| gm | GM | 总经理 |
| prod_mgr | PROD_MGR | 生管 |
| engineer | ENGINEER | 工程师 |
| warehouse | WAREHOUSE | 仓管 |
| qc | QC | 品检 |
| buyer | BUYER | 采购 |
| finance | FINANCE | 财务 |
| hr | HR | 人事 |
| procurement_manager | PROCUREMENT_MANAGER | 采购主管 |
| operator | OPERATOR | 操作工 |

已有库仅改密码可执行：`backend/db/install/fix-demo-passwords.sql`

## 全链路体验

完整测试步骤见 **[docs/demo-e2e-flow-test.md](../../docs/demo-e2e-flow-test.md)**（下单 → 生管 → 采购 → 仓库 → 质检）。

1. Web 登录 → `https://cnc.51xiaoping.com/`（本地：`http://localhost:5173`）
2. API → `https://bts.51xiaoping.com/erp-{service}/...`（例：登录 `POST /erp-platform/auth/login`）
3. 扫码三码：`GD-20260615-0001` → `LZ-GD001-P01` → `SB-CNC-001`
4. 仓储物料码：`WL-DEMO-STEEL-001`

条码规范见 `web-impl/docs/barcode-prefix.md`。

## 生产 MySQL

- Host: `10.100.4.10:3306`
- User: `xm_admin`
- 库名: `cnc_platform` / `cnc_business` / `cnc_production`

## cnc_production 为 0 表时补建

**架构说明（不是表建错库）：**

| 库 | 作用 |
|----|------|
| `cnc_business` | 业务源库 · 迁移脚本在此建 `crm_workorder` 等全量表 |
| `cnc_production` | `erp-production` 读写库 · V60 **LIKE 复制** business 中的生产域表 + 数据 |

生产表**同时存在于** `cnc_business`（源）和 `cnc_production`（副本），并非只迁到 production。`init.sql` 若在 V60a（约第 5928 行）之前报错中断，则 production 库为空。

`init.sql` 在 V62 之后建表（V60a）、demo 之后同步数据（V60b）。若 GUI 工具未跑完末尾段，可单独补：

```powershell
cd backend\db\install
.\sync-cnc-production.ps1
```

若 V54 报 `crm_drawing_link_backup doesn't exist`（表误建在 cnc_platform），先执行：

```bash
mysql ... < backend/db/install/fix-drawing-link-schema.sql
```

然后再跑 `sync-cnc-production.ps1` 或继续 `init.sql` 剩余段。

## 注意

- **仅适用于空库**；`init.sql` 含 `DROP TABLE`，勿在已有生产数据上重跑。
- 推荐用 **mysql 命令行** 或 `import-all.ps1` 执行 `init.sql`；Navicat 等 GUI 遇 `# include:` 可能跳过末尾 V60 段。
- 若只需补演示数据，可单独执行 `demo-flow-seed.sql`（需表已存在）。
