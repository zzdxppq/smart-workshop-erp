package com.btsheng.erp.production.outsource.incoming.entity;

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
 * V1.3.7 · Story 1.25 · 委外来料质检单（crm_outsource_incoming_inspection · FR-6-5）
 */
@Data
@Schema(description = "委外来料质检单（crm_outsource_incoming_inspection · Story 1.25 FR-6-5）")
@TableName("crm_outsource_incoming_inspection")
public class CrmOutsourceIncomingInspection implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_no")   private String inspectionNo;
    @TableField("outsource_id")    private Long outsourceId;
    @TableField("outsource_no")    private String outsourceNo;
    @TableField("supplier_id")     private Long supplierId;
    @TableField("supplier_name")   private String supplierName;
    @TableField("material_code")   private String materialCode;
    @TableField("inspect_qty")     private Integer inspectQty = 0;
    @TableField("passed_qty")      private Integer passedQty = 0;
    @TableField("failed_qty")      private Integer failedQty = 0;
    @TableField("defect_rate")     private BigDecimal defectRate;
    @TableField("result")          private String result = "DRAFT";
    @TableField("inspector_user_id") private Long inspectorUserId;
    @TableField("inspected_at")    private LocalDateTime inspectedAt;
    @TableField("notify_email")    private String notifyEmail;
    @TableField("remark")          private String remark;
    @TableField("created_by")      private Long createdBy;
    @TableField("created_at")      private LocalDateTime createdAt;
    @TableField("updated_at")      private LocalDateTime updatedAt;
}
