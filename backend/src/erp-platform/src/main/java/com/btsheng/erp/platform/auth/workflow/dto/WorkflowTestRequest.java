package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工作流试跑请求（V1.3.7 · Story 1.2 · T2.1 · AC-1.2.1 (d)）
 */
@Data
@Schema(description = "工作流试跑请求（不落库）")
public class WorkflowTestRequest {

    @Schema(description = "金额", example = "80000")
    private BigDecimal amount;

    @Schema(description = "申请人角色", example = "salesperson")
    private String applicantRole;
}
