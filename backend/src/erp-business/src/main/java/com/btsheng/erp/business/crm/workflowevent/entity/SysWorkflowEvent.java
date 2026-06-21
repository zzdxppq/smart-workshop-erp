package com.btsheng.erp.business.crm.workflowevent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.8 Sprint 8 Story 8.3 · sys_workflow_event 审批事件实体
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@Data
@Schema(description = "审批事件（V1.3.8 Sprint 8）")
@TableName("sys_workflow_event")
public class SysWorkflowEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_no")
    private String eventNo;

    @TableField("workflow_code")
    private String workflowCode;

    @TableField("biz_id")
    private Long bizId;

    @TableField("biz_no")
    private String bizNo;

    @TableField("event_type")
    private String eventType;

    @TableField("approver_role")
    private String approverRole;

    @TableField("approver_user_id")
    private Long approverUserId;

    @TableField("approver_user_name")
    private String approverUserName;

    @TableField("comment")
    private String comment;

    @TableField("matched_node_index")
    private Integer matchedNodeIndex;

    @TableField("matched_threshold")
    private String matchedThreshold;

    @TableField("created_at")
    private LocalDateTime createdAt;
}