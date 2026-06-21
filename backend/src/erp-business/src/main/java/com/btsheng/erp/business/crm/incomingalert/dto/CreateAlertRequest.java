package com.btsheng.erp.business.crm.incomingalert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.34 · 创建到货提醒请求
 */
@Data
@Schema(description = "创建到货提醒请求（Story 1.34 FR-8-3）")
public class CreateAlertRequest {

    @Schema(description = "采购单 ID", example = "1", required = true)
    private Long poId;

    @Schema(description = "采购单号", example = "PO20260401-0001", required = true)
    private String poNo;

    @Schema(description = "厂商 ID")
    private Long vendorId;

    @Schema(description = "厂商名称")
    private String vendorName;

    @Schema(description = "物料 ID", example = "1001", required = true)
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "数量", example = "100", required = true)
    private BigDecimal qty;

    @Schema(description = "单位", example = "PCS")
    private String unit;

    @Schema(description = "预估到货日 · P1 修补 1 必填", example = "2026-07-01", required = true)
    private LocalDate expectedDate;
}
