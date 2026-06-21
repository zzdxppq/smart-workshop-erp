package com.btsheng.erp.business.crm.purchaseinspection.entity;

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
 * V1.3.7 · Story 1.35 · 采购·来料质检单（crm_purchase_incoming_inspection · FR-8-4）
 */
@Data
@Schema(description = "采购来料质检单（Story 1.35 FR-8-4）")
@TableName("crm_purchase_incoming_inspection")
public class CrmPurchaseIncomingInspection implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("inspection_no")   private String inspectionNo;
    @TableField("incoming_id")     private Long incomingId;
    @TableField("po_id")           private Long poId;
    @TableField("po_no")           private String poNo;
    @TableField("vendor_id")       private Long vendorId;
    @TableField("vendor_name")     private String vendorName;
    @TableField("material_id")     private Long materialId;
    @TableField("material_code")   private String materialCode;
    @TableField("material_name")   private String materialName;
    @TableField("batch_no")        private String batchNo;
    @TableField("inspector_id")    private Long inspectorId;
    @TableField("inspector_name")  private String inspectorName;
    @TableField("sample_size")     private Integer sampleSize;
    @TableField("sample_pass")     private Integer samplePass;
    @TableField("sample_fail")     private Integer sampleFail;
    @TableField("defect_rate")     private BigDecimal defectRate;
    @TableField("aql_level")       private String aqlLevel = "II";
    @TableField("result")          private String result = "PENDING";
    @TableField("notify_email")    private String notifyEmail;
    @TableField("inspected_at")    private LocalDateTime inspectedAt;
    @TableField("remark")          private String remark;
    @TableField("created_at")      private LocalDateTime createdAt;
    @TableField("updated_at")      private LocalDateTime updatedAt;
}
