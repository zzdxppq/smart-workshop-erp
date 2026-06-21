package com.btsheng.erp.business.crm.qualityfa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.29 · FA 首件单（crm_quality_fa · FR-7-2 · 开工前必检）
 */
@Data
@Schema(description = "FA 首件单（crm_quality_fa · Story 1.29 FR-7-2）")
@TableName("crm_quality_fa")
public class CrmQualityFa implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("fa_no")             private String faNo;
    @TableField("work_order_id")     private Long workOrderId;
    @TableField("work_order_no")     private String workOrderNo;
    @TableField("process_id")        private Long processId;
    @TableField("process_name")      private String processName;
    @TableField("operator_user_id")  private Long operatorUserId;
    @TableField("inspect_qty")       private Integer inspectQty = 1;
    @TableField("result")            private String result = "DRAFT";
    @TableField("status")            private String status = "PENDING_INSPECT";
    @TableField("locked")            private Integer locked = 0;
    @TableField("locked_work_order_no") private String lockedWorkOrderNo;
    @TableField("pdf_url")           private String pdfUrl;
    @TableField("inspector_user_id") private Long inspectorUserId;
    @TableField("inspector_signed_at") private LocalDateTime inspectorSignedAt;
    @TableField("inspected_at")      private LocalDateTime inspectedAt;
    @TableField("engineer_user_id")  private Long engineerUserId;
    @TableField("engineer_signed_at") private LocalDateTime engineerSignedAt;
    @TableField("reject_reason")     private String rejectReason;
    @TableField("rework_count")      private Integer reworkCount = 0;
    @TableField("remark")            private String remark;
    @TableField("created_by")        private Long createdBy;
    @TableField("created_at")        private LocalDateTime createdAt;
    @TableField("updated_at")        private LocalDateTime updatedAt;
}
