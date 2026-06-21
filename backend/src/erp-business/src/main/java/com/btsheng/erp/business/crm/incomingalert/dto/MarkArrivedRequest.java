package com.btsheng.erp.business.crm.incomingalert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.34 · 标记到货请求
 */
@Data
@Schema(description = "标记到货请求（Story 1.34 FR-8-3）")
public class MarkArrivedRequest {

    @Schema(description = "到货数量", example = "100", required = true)
    private BigDecimal arrivedQty;

    @Schema(description = "扫码批次号（关联 1.12）", example = "B20260612-0001")
    private String scanBatchNo;

    @Schema(description = "备注")
    private String remark;
}
