package com.btsheng.erp.production.outsource.quality.entity;

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
 * V1.3.7 · Story 1.27 · 委外工序质检单（crm_outsource_quality · FR-6-7）
 */
@Data
@Schema(description = "委外工序质检单（crm_outsource_quality · Story 1.27 FR-6-7）")
@TableName("crm_outsource_quality")
public class CrmOutsourceQuality implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("quality_no")        private String qualityNo;
    @TableField("outsource_id")      private Long outsourceId;
    @TableField("outsource_no")      private String outsourceNo;
    @TableField("process_name")      private String processName;
    @TableField("supplier_id")       private Long supplierId;
    @TableField("supplier_name")     private String supplierName;
    @TableField("inspect_type")      private String inspectType = "FA";
    @TableField("inspect_qty")       private Integer inspectQty = 1;
    @TableField("passed_qty")        private Integer passedQty = 0;
    @TableField("failed_qty")        private Integer failedQty = 0;
    @TableField("defect_rate")       private BigDecimal defectRate;
    @TableField("alerted")           private Integer alerted = 0;
    @TableField("result")            private String result = "DRAFT";
    @TableField("inspector_user_id") private Long inspectorUserId;
    @TableField("inspected_at")      private LocalDateTime inspectedAt;
    @TableField("remark")            private String remark;
    @TableField("created_by")        private Long createdBy;
    @TableField("created_at")        private LocalDateTime createdAt;
    @TableField("updated_at")        private LocalDateTime updatedAt;
}
