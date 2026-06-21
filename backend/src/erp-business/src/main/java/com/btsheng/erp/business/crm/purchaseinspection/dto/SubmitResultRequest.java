package com.btsheng.erp.business.crm.purchaseinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.35 · 提交质检结论请求（PASS/REJECT）
 */
@Data
@Schema(description = "提交质检结论请求（Story 1.35 FR-8-4）")
public class SubmitResultRequest {

    @Schema(description = "结论 PASS/REJECT", example = "PASS", required = true)
    private String result;
    @Schema(description = "抽样合格数", example = "50")
    private Integer samplePass;
    @Schema(description = "抽样不合格数", example = "0")
    private Integer sampleFail;
    @Schema(description = "备注")
    private String remark;
}
