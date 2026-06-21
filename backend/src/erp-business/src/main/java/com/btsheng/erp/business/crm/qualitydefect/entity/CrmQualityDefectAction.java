package com.btsheng.erp.business.crm.qualitydefect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.31 · 不良处理动作
 */
@Data
@Schema(description = "不良处理动作（3 选 1：返工/报废/让步接收）")
@TableName("crm_quality_defect_action")
public class CrmQualityDefectAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("defect_id")          private Long defectId;
    @TableField("action_type")        private String actionType;
    @TableField("qty")                private Integer qty = 1;
    @TableField("responsible_dept")   private String responsibleDept;
    @TableField("cost_amount")        private BigDecimal costAmount;
    @TableField("executed_at")        private LocalDateTime executedAt;
    @TableField("executor_user_id")   private Long executorUserId;
    @TableField("remark")             private String remark;
    @TableField("created_at")         private LocalDateTime createdAt;
}
