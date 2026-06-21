package com.btsheng.erp.production.outsource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.22 · 委外状态机推进请求（FR-6-2）
 */
@Data
@Schema(description = "委外状态机推进请求")
public class OutsourceStateAdvanceRequest {

    @Schema(description = "委外单主键 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "目标状态 DRAFT/SENT/ACCEPTED/IN_PRODUCTION/INSPECTED/COMPLETED/CLOSED/REWORK/REJECTED", example = "ACCEPTED", required = true)
    private String targetState;

    @Schema(description = "操作员角色（V1.3.7 AD-1 生管/采购/品检/财务）", example = "采购")
    private String operatorRole;

    @Schema(description = "推进原因", example = "供应商接单")
    private String reason;
}
