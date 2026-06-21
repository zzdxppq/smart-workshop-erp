package com.btsheng.erp.business.crm.pricecontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.33 · 价格校验请求
 */
@Data
@Schema(description = "价格校验请求（Story 1.33 FR-8-2）")
public class CheckPriceRequest {

    @Schema(description = "物料 ID", example = "1001", required = true)
    private Long materialId;

    @Schema(description = "厂商 ID", example = "901", required = true)
    private Long vendorId;

    @Schema(description = "拟采购单价", example = "550.00", required = true)
    private BigDecimal unitPrice;
}
