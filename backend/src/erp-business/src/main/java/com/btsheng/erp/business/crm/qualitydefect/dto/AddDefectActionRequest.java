package com.btsheng.erp.business.crm.qualitydefect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.31 · 不良处理动作请求
 */
@Data
@Schema(description = "不良处理动作请求（Story 1.31 FR-7-4）")
public class AddDefectActionRequest {

    @Schema(description = "不良品单 ID", example = "1", required = true)
    private Long defectId;

    @Schema(description = "动作 REWORK/SCRAP/CONCESSION（必填 · P1 修补 1）", example = "REWORK", required = true)
    private String actionType;

    @Schema(description = "处理数量", example = "10", required = true)
    private Integer qty = 1;

    @Schema(description = "责任部门（必填 · P1 修补 2）", example = "QA", required = true)
    private String responsibleDept;

    @Schema(description = "成本（非负 · P1 修补 3）", example = "5000.00")
    private BigDecimal costAmount;

    @Schema(description = "备注")
    private String remark;
}
