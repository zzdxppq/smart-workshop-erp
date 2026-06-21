package com.btsheng.erp.business.crm.qualitycmm.entity;

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
 * V1.3.7 · Story 1.30 · CMM 三次元测量单（crm_quality_cmm · FR-7-3）
 */
@Data
@Schema(description = "CMM 三次元测量单（crm_quality_cmm · Story 1.30 FR-7-3）")
@TableName("crm_quality_cmm")
public class CrmQualityCmm implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("cmm_no")           private String cmmNo;
    @TableField("work_order_id")    private Long workOrderId;
    @TableField("work_order_no")    private String workOrderNo;
    @TableField("drawing_no")       private String drawingNo;
    @TableField("part_name")        private String partName;
    @TableField("point_count")      private Integer pointCount = 0;
    @TableField("cpk")              private BigDecimal cpk;
    @TableField("pp")               private BigDecimal pp;
    @TableField("ppk")              private BigDecimal ppk;
    @TableField("cp")               private BigDecimal cp;
    @TableField("max_deviation")    private BigDecimal maxDeviation;
    @TableField("deviation_alert")  private Integer deviationAlert = 0;
    @TableField("result")           private String result = "DRAFT";
    @TableField("pdf_url")          private String pdfUrl;
    @TableField("inspector_user_id") private Long inspectorUserId;
    @TableField("inspected_at")     private LocalDateTime inspectedAt;
    @TableField("remark")           private String remark;
    @TableField("created_by")       private Long createdBy;
    @TableField("created_at")       private LocalDateTime createdAt;
    @TableField("updated_at")       private LocalDateTime updatedAt;
}
