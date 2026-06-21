package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建审批单请求（V1.3.7 · Story 1.2 · T2.2）
 */
@Data
@Schema(description = "创建审批单请求")
public class ApprovalCreateRequest {

    @Schema(description = "业务类型（QUOTE/ORDER/PURCHASE/PAYMENT/OTHER）", example = "QUOTE")
    private String bizType;

    @Schema(description = "业务单号（≤50 字符）", example = "BJ202606100002")
    private String bizId;

    @Schema(description = "金额", example = "60000.00")
    private BigDecimal amount;

    @Schema(description = "申请人 user_id", example = "10086")
    private Long applicantUserId;

    @Schema(description = "工作流编码（可选，默认按 bizType 选）")
    private String workflowCode;

    @Schema(description = "申请备注")
    private String comment;
}
