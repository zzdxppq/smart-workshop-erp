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
 * V1.3.7 · Story 1.33 · 物料采购限价（crm_price_control · FR-8-2）
 */
@Data
@Schema(description = "物料采购限价（Story 1.33 FR-8-2）")
@TableName("crm_price_control")
public class CrmPriceControl implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("control_no")      private String controlNo;
    @TableField("material_id")     private Long materialId;
    @TableField("material_code")   private String materialCode;
    @TableField("material_name")   private String materialName;
    @TableField("vendor_id")       private Long vendorId;
    @TableField("vendor_name")     private String vendorName;
    @TableField("price_limit")     private BigDecimal priceLimit;
    @TableField("currency")        private String currency = "CNY";
    @TableField("effective_date")  private LocalDate effectiveDate;
    @TableField("expiry_date")     private LocalDate expiryDate;
    @TableField("status")          private String status = "ACTIVE";
    @TableField("set_by")          private Long setBy;
    @TableField("set_by_name")     private String setByName;
    @TableField("remark")          private String remark;
    @TableField("created_at")      private LocalDateTime createdAt;
    @TableField("updated_at")      private LocalDateTime updatedAt;
}
