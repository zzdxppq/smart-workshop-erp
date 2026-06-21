package com.btsheng.erp.business.crm.qualityinspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crm_quality_concession_approval")
public class CrmQualityConcessionApproval implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_id") private Long inspectionId;
    @TableField("approver_role") private String approverRole;
    @TableField("approval_status") private String approvalStatus;
    @TableField("approver_user_id") private Long approverUserId;
    @TableField("approved_at") private LocalDateTime approvedAt;
    @TableField("comment") private String comment;
    @TableField("created_at") private LocalDateTime createdAt;
}
