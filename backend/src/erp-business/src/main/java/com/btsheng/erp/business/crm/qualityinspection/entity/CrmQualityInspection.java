package com.btsheng.erp.business.crm.qualityinspection.entity;

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
 * V1.3.7 · Story 1.28 · 来料/过程/成品检单（crm_quality_inspection · FR-7-1）
 */
@Data
@Schema(description = "来料/过程/成品检单（crm_quality_inspection · Story 1.28 FR-7-1）")
@TableName("crm_quality_inspection")
public class CrmQualityInspection implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_no")     private String inspectionNo;
    @TableField("inspect_type")      private String inspectType;
    @TableField("material_id")       private Long materialId;
    @TableField("material_code")     private String materialCode;
    @TableField("material_name")     private String materialName;
    @TableField("work_order_id")     private Long workOrderId;
    @TableField("work_order_no")     private String workOrderNo;
    @TableField("process_name")      private String processName;
    @TableField("batch_no")          private String batchNo;
    @TableField("lot_size")          private Integer lotSize = 0;
    @TableField("sample_size")       private Integer sampleSize = 0;
    @TableField("sample_rule")       private String sampleRule = "AQL-1.0";
    @TableField("aql_level")         private String aqlLevel = "1.0";
    @TableField("inspect_qty")       private Integer inspectQty = 0;
    @TableField("passed_qty")        private Integer passedQty = 0;
    @TableField("failed_qty")        private Integer failedQty = 0;
    @TableField("defect_rate")       private BigDecimal defectRate;
    @TableField("result")            private String result = "DRAFT";
    @TableField("disposition")      private String disposition;
    @TableField("approval_status")   private String approvalStatus;
    @TableField("defect_disposition_qty") private Integer defectDispositionQty;
    @TableField("drawing_no")       private String drawingNo;
    @TableField("source_ref")       private String sourceRef;
    @TableField("inspector_user_id") private Long inspectorUserId;
    @TableField("inspected_at")      private LocalDateTime inspectedAt;
    @TableField("max_severity")      private String maxSeverity;
    @TableField("trigger_rework")    private Integer triggerRework = 0;
    @TableField("trigger_stockin")   private Integer triggerStockin = 0;
    @TableField("remark")            private String remark;
    @TableField("created_by")        private Long createdBy;
    @TableField("created_at")        private LocalDateTime createdAt;
    @TableField("updated_at")        private LocalDateTime updatedAt;
}
