# 条码前缀规范（全仓统一）

> 与 `docs/front-end-spec.md` 一码到底定义一致，**仅使用下列前缀**，不再使用 WN-/WR-/WD-。

| 类型 | 前缀 | 示例 | 用途 |
|------|------|------|------|
| 工单 | **GD-** | `GD-20260615-0001` | 开工 / 报工 |
| 流转 | **LZ-** | `LZ-GD001-P01` | 工序过站 |
| 设备 | **SB-** | `SB-CNC-001` | 机台绑定 |
| 物料 | **WL-** | `WL-STEEL-001` | 仓储 / 来料 |
| 委外 | **WW-** | `WW-20260615-0001` | 委外到货 |
| 客户 | **KH-** | `KH-001` | CRM（Web） |

## 代码落点

- Android：`QrCodeParser.kt`
- 后端：`BarcodePrefixUtil.java`、`AppService.parseCode()`
- Web：`ScanTrigger.vue` placeholder
- OpenAPI：`/app/workorders/{barcode}` 示例均为 GD-

## 联调口令

```
GD-20260615-0001 → LZ-GD001-P01 → SB-CNC-001
```
