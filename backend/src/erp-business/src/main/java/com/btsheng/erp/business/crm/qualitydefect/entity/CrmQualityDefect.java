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
 * V1.3.7 · Story 1.31 · 不良品单（crm_quality_defect · FR-7-4 · 8D 报告）
 */
@Data
@Schema(description = "不良品单（crm_quality_defect · Story 1.31 FR-7-4）")
@TableName("crm_quality_defect")
public class CrmQualityDefect implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("defect_no")         private String defectNo;
    @TableField("source_type")       private String sourceType;
    @TableField("source_id")         private Long sourceId;
    @TableField("source_no")         private String sourceNo;
    @TableField("defect_type")       private String defectType;
    @TableField("severity")          private String severity = "MAJOR";
    @TableField("qty")               private Integer qty = 1;
    @TableField("material_id")       private Long materialId;
    @TableField("material_code")     private String materialCode;
    @TableField("work_order_id")     private Long workOrderId;
    @TableField("work_order_no")     private String workOrderNo;
    @TableField("d1_team")           private String d1Team;
    @TableField("d4_root_cause")     private String d4RootCause;
    @TableField("d5_action")         private String d5Action;
    @TableField("d8_closure")        private String d8Closure;
    @TableField("defect_rate_ppm")   private BigDecimal defectRatePpm;
    @TableField("total_qty")         private Integer totalQty;
    @TableField("status")            private String status = "OPEN";
    @TableField("result")            private String result;
    @TableField("disposition_status") private String dispositionStatus = "PENDING";
    @TableField("cause_category")    private String causeCategory;
    @TableField("responsible_dept")  private String responsibleDept;
    @TableField("cost_amount")       private BigDecimal costAmount;
    @TableField("rework_work_order_no") private String reworkWorkOrderNo;
    @TableField("rework_count")      private Integer reworkCount = 0;
    @TableField("scrap_inventory_deducted") private Integer scrapInventoryDeducted = 0;
    @TableField("concession_approver_id") private Long concessionApproverId;
    @TableField("concession_approved_at") private LocalDateTime concessionApprovedAt;
    @TableField("created_by")        private Long createdBy;
    @TableField("created_at")        private LocalDateTime createdAt;
    @TableField("updated_at")        private LocalDateTime updatedAt;
}
