package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批通过请求（V1.3.7 · Story 1.2 · T2.2 · AC-1.2.3）
 */
@Data
@Schema(description = "审批通过请求")
public class ApproveRequest {

    @Schema(description = "审批人 user_id", example = "10010")
    private Long approverUserId;

    @Schema(description = "审批意见", example = "同意")
    private String comment;
}
