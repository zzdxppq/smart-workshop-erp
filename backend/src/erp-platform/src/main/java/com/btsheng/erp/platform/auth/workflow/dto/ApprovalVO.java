package com.btsheng.erp.platform.auth.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批单 VO（V1.3.7 · Story 1.2 · T2.2）
 */
@Data
@Schema(description = "审批单视图")
public class ApprovalVO {

    private Long id;
    private String bizType;
    private String bizId;
    private String workflowCode;
    private Integer currentNodeIndex;
    private Long currentApproverUserId;
    private List<Long> candidates;
    private Boolean orSignRequired;
    private String status;
    private String skipReason;
    private LocalDateTime skippedAt;
    private String comment;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime timeoutAt;
    private Boolean isOverdue;
    private LocalDateTime overdueAt;
    private Boolean nodeSkipped;
}
