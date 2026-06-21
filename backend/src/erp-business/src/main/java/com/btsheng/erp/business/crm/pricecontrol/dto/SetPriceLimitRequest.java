package com.btsheng.erp.business.crm.pricecontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.33 · 设置限价请求
 */
@Data
@Schema(description = "设置物料限价请求（Story 1.33 FR-8-2）")
public class SetPriceLimitRequest {

    @Schema(description = "物料 ID", example = "1001", required = true)
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "厂商 ID（NULL = 通用限价）", example = "901")
    private Long vendorId;

    @Schema(description = "厂商名称")
    private String vendorName;

    @Schema(description = "采购价上限 · P1 修补 1 非负", example = "480.00", required = true)
    private BigDecimal priceLimit;

    @Schema(description = "币种", example = "CNY")
    private String currency = "CNY";

    @Schema(description = "生效日", example = "2026-01-01", required = true)
    private LocalDate effectiveDate;

    @Schema(description = "失效日", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    private String remark;
}
