package com.btsheng.erp.business.crm.qualitycmm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.30 · 追加 CMM 测点请求
 */
@Data
@Schema(description = "追加 CMM 测点请求（Story 1.30 FR-7-3）")
public class AddCmmPointRequest {

    @Schema(description = "CMM 单 ID", example = "1", required = true)
    private Long cmmId;

    @Schema(description = "测点编号", example = "P6", required = true)
    private String pointNo;

    @Schema(description = "X/Y/Z", example = "Z")
    private String axis = "X";

    @Schema(description = "标称值", example = "20.0000", required = true)
    private BigDecimal nominalValue;

    @Schema(description = "实测值", example = "20.0150", required = true)
    private BigDecimal measuredValue;

    @Schema(description = "上偏差", example = "0.0500")
    private BigDecimal toleranceUpper;

    @Schema(description = "下偏差", example = "-0.0500")
    private BigDecimal toleranceLower;
}
