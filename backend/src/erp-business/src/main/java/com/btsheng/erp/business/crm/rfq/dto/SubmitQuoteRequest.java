package com.btsheng.erp.business.crm.rfq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.32 · 厂商报价请求
 */
@Data
@Schema(description = "厂商报价请求（Story 1.32 FR-8-1）")
public class SubmitQuoteRequest {

    @Schema(description = "厂商 ID", example = "901", required = true)
    private Long vendorId;

    @Schema(description = "单价 · P1 修补 2 必填", example = "450.00", required = true)
    private BigDecimal unitPrice;

    @Schema(description = "总报价", example = "45000.00", required = true)
    private BigDecimal totalAmount;

    @Schema(description = "交货周期（天）", example = "7")
    private Integer leadTimeDays;

    @Schema(description = "报价有效期")
    private LocalDate validUntil;

    @Schema(description = "付款条件")
    private String paymentTerms;

    @Schema(description = "质量评分 0-5", example = "4.2")
    private BigDecimal qualityScore;

    @Schema(description = "交付评分 0-5", example = "4.5")
    private BigDecimal deliveryScore;

    @Schema(description = "服务评分 0-5", example = "4.0")
    private BigDecimal serviceScore;

    @Schema(description = "备注")
    private String remark;
}
