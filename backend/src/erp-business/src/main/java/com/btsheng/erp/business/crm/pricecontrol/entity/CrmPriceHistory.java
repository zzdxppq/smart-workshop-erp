package com.btsheng.erp.business.crm.pricecontrol.entity;

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
 * V1.3.7 · Story 1.33 · 采购历史价（crm_price_history · FR-8-2）
 */
@Data
@Schema(description = "采购历史价（Story 1.33 FR-8-2）")
@TableName("crm_price_history")
public class CrmPriceHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_id")     private Long materialId;
    @TableField("material_code")   private String materialCode;
    @TableField("vendor_id")       private Long vendorId;
    @TableField("vendor_name")     private String vendorName;
    @TableField("unit_price")      private BigDecimal unitPrice;
    @TableField("qty")             private BigDecimal qty;
    @TableField("total_amount")    private BigDecimal totalAmount;
    @TableField("source_type")     private String sourceType = "PO";
    @TableField("source_no")       private String sourceNo;
    @TableField("purchased_at")    private LocalDate purchasedAt;
    @TableField("created_by")      private Long createdBy;
    @TableField("created_at")      private LocalDateTime createdAt;
}
