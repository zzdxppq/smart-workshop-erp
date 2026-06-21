package com.btsheng.erp.business.crm.incomingalert.entity;

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
 * V1.3.7 · Story 1.34 · 实际到货（crm_incoming · FR-8-3）
 */
@Data
@Schema(description = "实际到货（Story 1.34 FR-8-3）")
@TableName("crm_incoming")
public class CrmIncoming implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("incoming_no")     private String incomingNo;
    @TableField("alert_id")        private Long alertId;
    @TableField("po_id")           private Long poId;
    @TableField("po_no")           private String poNo;
    @TableField("vendor_id")       private Long vendorId;
    @TableField("vendor_name")     private String vendorName;
    @TableField("material_id")     private Long materialId;
    @TableField("material_code")   private String materialCode;
    @TableField("material_name")   private String materialName;
    @TableField("arrived_qty")     private BigDecimal arrivedQty;
    @TableField("expected_qty")    private BigDecimal expectedQty;
    @TableField("unit")            private String unit;
    @TableField("arrived_at")      private LocalDateTime arrivedAt;
    @TableField("arrived_by")      private Long arrivedBy;
    @TableField("quality_status")  private String qualityStatus = "PENDING";
    @TableField("scan_batch_no")   private String scanBatchNo;
    @TableField("remark")          private String remark;
    @TableField("created_at")      private LocalDateTime createdAt;
}
