package com.btsheng.erp.business.crm.inventoryalert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "库存预警查询")
public class InventoryAlertQueryRequest {
    @Schema(description = "状态 OPEN/RESOLVED/ARCHIVED")
    private String status;
    @Schema(description = "级别 INFO/WARN/ERROR/CRITICAL")
    private String level;
    @Schema(description = "页码")
    private Integer page = 0;
    @Schema(description = "页大小")
    private Integer size = 20;
}
