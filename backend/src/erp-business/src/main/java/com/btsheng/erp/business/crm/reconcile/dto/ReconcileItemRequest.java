package com.btsheng.erp.business.crm.reconcile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "对账明细请求")
public class ReconcileItemRequest {
    @Schema(description = "委外单 ID", required = true)
    private Long outsourceOrderId;
    @Schema(description = "委外单号", required = true)
    private String outsourceOrderNo;
    @Schema(description = "项目名称", required = true)
    private String itemName;
    @Schema(description = "数量", required = true)
    private Integer quantity;
    @Schema(description = "单价", required = true)
    private BigDecimal unitPrice;
}
