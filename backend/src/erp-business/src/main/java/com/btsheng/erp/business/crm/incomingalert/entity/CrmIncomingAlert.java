package com.btsheng.erp.business.crm.incomingalert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.34 · 到货提醒（crm_incoming_alert · FR-8-3）
 */
@Data
@Schema(description = "到货提醒（Story 1.34 FR-8-3）")
@TableName("crm_incoming_alert")
public class CrmIncomingAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("alert_no")         private String alertNo;
    @TableField("po_id")            private Long poId;
    @TableField("po_no")            private String poNo;
    @TableField("vendor_id")        private Long vendorId;
    @TableField("vendor_name")      private String vendorName;
    @TableField("material_id")      private Long materialId;
    @TableField("material_code")    private String materialCode;
    @TableField("material_name")    private String materialName;
    @TableField("qty")              private BigDecimal qty;
    @TableField("unit")             private String unit;
    @TableField("expected_date")    private LocalDate expectedDate;
    @TableField("alert_level")      private String alertLevel = "PENDING";
    @TableField("alert_message")    private String alertMessage;
    @TableField("arrived_qty")      private BigDecimal arrivedQty;
    @TableField("arrived_at")       private LocalDateTime arrivedAt;
    @TableField("arrived_by")       private Long arrivedBy;
    @TableField("reminded_at")      private LocalDateTime remindedAt;
    @TableField("reminded_count")   private Integer remindedCount = 0;
    @TableField("created_at")       private LocalDateTime createdAt;
    @TableField("updated_at")       private LocalDateTime updatedAt;
}
