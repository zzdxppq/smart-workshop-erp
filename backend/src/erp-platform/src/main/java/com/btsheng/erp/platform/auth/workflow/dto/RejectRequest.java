package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批驳回请求（V1.3.7 · Story 1.2 · T2.2 · AC-1.2.3）
 */
@Data
@Schema(description = "审批驳回请求")
public class RejectRequest {

    @Schema(description = "审批人 user_id", example = "10010")
    private Long approverUserId;

    @Schema(description = "驳回原因（必填，1..500 字符）", example = "价格超出预算")
    private String reason;
}
