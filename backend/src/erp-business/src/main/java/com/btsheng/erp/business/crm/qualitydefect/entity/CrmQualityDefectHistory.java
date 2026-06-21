package com.btsheng.erp.business.crm.qualitydefect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.31 · 不良处理历史
 */
@Data
@Schema(description = "不良处理历史")
@TableName("crm_quality_defect_history")
public class CrmQualityDefectHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("defect_id")         private Long defectId;
    @TableField("from_status")       private String fromStatus;
    @TableField("to_status")         private String toStatus;
    @TableField("operator_user_id")  private Long operatorUserId;
    @TableField("operator_name")     private String operatorName;
    @TableField("comment")           private String comment;
    @TableField("created_at")        private LocalDateTime createdAt;
}
