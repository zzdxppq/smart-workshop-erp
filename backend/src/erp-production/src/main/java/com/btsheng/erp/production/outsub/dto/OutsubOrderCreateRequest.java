package com.btsheng.erp.production.outsub.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "【采购】委外下单请求（V1.3.7 · allocationId + vendorId · V1.3.8 + drawingId）")
public class OutsubOrderCreateRequest {

    @Schema(description = "工序分配 ID（来自 GET /production/allocations/pending）", required = true)
    private Long allocationId;

    @Schema(description = "厂商 ID", required = true)
    private Long vendorId;

    @Schema(description = "单价", required = true)
    private BigDecimal unitPrice;

    @Schema(description = "交期", required = true)
    private LocalDate deliveryDate;

    @Schema(description = "加工图纸 ID（Epic 3 · 下单前确认）", required = true)
    private Long drawingId;
}
