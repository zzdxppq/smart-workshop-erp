package com.btsheng.erp.business.crm.inventoryalert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "安全库存配置请求（AC-4.4.1）")
public class SafetyStockRequest {
    @Schema(description = "物料编码", required = true)
    private String materialCode;
    @Schema(description = "物料名称")
    private String materialName;
    @Schema(description = "安全库存下限 min_qty", required = true)
    private Integer minQty;
    @Schema(description = "安全库存上限 max_qty", required = true)
    private Integer maxQty;
    @Schema(description = "补货量 reorder_qty", required = true)
    private Integer reorderQty;
    @Schema(description = "单位")
    private String unit;
}
