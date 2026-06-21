package com.btsheng.erp.platform.auth.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.BaseDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录（V1.3.7 · Story 1.2 · P1 修补 ④ 新表）
 *
 * <p>对应 {@code cnc_platform.sys_approval_record} 表（V3__approval_record.sql 迁移创建）。
 * 同一业务单号（{@code bizType+bizId}）可有多轮审批单（每个节点一条），体现审批轨迹。
 *
 * <p>状态机：{@code PENDING} → {@code APPROVED} / {@code REJECTED} / {@code SKIPPED}；
 * 禁止 {@code PENDING → PENDING} 自循环（→ 40904）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批记录（sys_approval_record · P1 修补 ④）")
@TableName("sys_approval_record")
public class ApprovalRecord extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务类型（QUOTE/ORDER/PURCHASE/PAYMENT/OTHER）", example = "QUOTE")
    @TableField("biz_type")
    private String bizType;

    @Schema(description = "业务单号（≤50 字符）", example = "BJ202606100001")
    @TableField("biz_id")
    private String bizId;

    @Schema(description = "工作流编码（sys_workflow.workflow_code）", example = "QUOTE_FLOW")
    @TableField("workflow_code")
    private String workflowCode;

    @Schema(description = "当前节点序号", example = "2")
    @TableField("current_node_index")
    private Integer currentNodeIndex;

    @Schema(description = "当前审批人 user_id（首次分配确定性：candidates[0]）", example = "10010")
    @TableField("current_approver_user_id")
    private Long currentApproverUserId;

    @Schema(description = "OR 会签候选人列表（JSON 数组：[10010, 10011]）")
    @TableField("candidates")
    private String candidates;

    @Schema(description = "是否 OR 会签（V1.3.7 P1 修补）", example = "false")
    @TableField("or_sign_required")
    private Boolean orSignRequired;

    @Schema(description = "审批状态：PENDING/APPROVED/REJECTED/SKIPPED/WAITING", example = "PENDING")
    @TableField("status")
    private String status;

    @Schema(description = "跳过原因：ON_LEAVE/ON_TRIP/DISABLED/RESIGNED")
    @TableField("skip_reason")
    private String skipReason;

    @Schema(description = "跳过时间")
    @TableField("skipped_at")
    private LocalDateTime skippedAt;

    @Schema(description = "审批意见（同意时）")
    @TableField("comment")
    private String comment;

    @Schema(description = "驳回原因（驳回时必填）")
    @TableField("reason")
    private String reason;

    @Schema(description = "审批通过时间")
    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @Schema(description = "超时阈值（= created_at + timeout_hours）")
    @TableField("timeout_at")
    private LocalDateTime timeoutAt;

    @Schema(description = "是否已超时（V1.3.7 升级：24h 推送后置 true）", example = "false")
    @TableField("is_overdue")
    private Boolean isOverdue;

    @Schema(description = "首次超时时间")
    @TableField("overdue_at")
    private LocalDateTime overdueAt;

    @Schema(description = "本节点是否被全员 SKIPPED 跳过（→ 自动推进下一节点）", example = "false")
    @TableField("node_skipped")
    private Boolean nodeSkipped;
}
