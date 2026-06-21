package com.btsheng.erp.business.crm.bom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.9 · AC-3.3.4 BOM 转生产请求
 */
@Data
@Schema(description = "BOM 转生产请求（POST /boms/{id}/convert-to-production）")
public class ConvertToProductionRequest {

    @Schema(description = "生产数量（正整数）", example = "100", minimum = "1")
    private Integer produceQty = 1;

    @Schema(description = "计划开工日期", example = "2026-06-15")
    private String plannedStartDate;

    @Schema(description = "备注")
    private String comment;
}
