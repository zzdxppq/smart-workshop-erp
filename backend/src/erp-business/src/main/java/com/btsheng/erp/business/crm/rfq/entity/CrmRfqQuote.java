package com.btsheng.erp.business.crm.rfq.entity;

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
 * V1.3.7 · Story 1.32 · 厂商报价（crm_rfq_quote · FR-8-1）
 */
@Data
@Schema(description = "厂商报价（Story 1.32 FR-8-1）")
@TableName("crm_rfq_quote")
public class CrmRfqQuote implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rfq_id")             private Long rfqId;
    @TableField("rfq_vendor_id")      private Long rfqVendorId;
    @TableField("vendor_id")          private Long vendorId;
    @TableField("unit_price")         private BigDecimal unitPrice;
    @TableField("total_amount")       private BigDecimal totalAmount;
    @TableField("lead_time_days")     private Integer leadTimeDays;
    @TableField("valid_until")        private LocalDate validUntil;
    @TableField("payment_terms")      private String paymentTerms;
    @TableField("quality_score")      private BigDecimal qualityScore;
    @TableField("delivery_score")     private BigDecimal deliveryScore;
    @TableField("service_score")      private BigDecimal serviceScore;
    @TableField("weighted_score")     private BigDecimal weightedScore;
    @TableField("is_awarded")         private Integer isAwarded = 0;
    @TableField("remark")             private String remark;
    @TableField("submitted_at")       private LocalDateTime submittedAt;
    @TableField("submitted_by")       private Long submittedBy;
}
