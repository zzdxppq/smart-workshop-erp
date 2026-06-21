package com.btsheng.erp.business.crm.conversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.2 工程转化请求
 */
@Data
@Schema(description = "工程转化请求（POST /drawings/{id}/convert）")
public class ConversionRequest {

    @Schema(description = "BOM 类型：STANDARD/FA/PROTOTYPE", example = "STANDARD", allowableValues = {"STANDARD", "FA", "PROTOTYPE"})
    private String bomType = "STANDARD";

    @Schema(description = "目标数量（正整数 · P1 修补）", example = "100", minimum = "1")
    private Integer targetQty = 1;

    @Schema(description = "工程师姓名（PDF 水印用 · V1.3.7 P2 修补 3）", example = "张工程师")
    private String engineerName;

    @Schema(description = "备注", example = "FA 件紧急转化")
    private String comment;
}
