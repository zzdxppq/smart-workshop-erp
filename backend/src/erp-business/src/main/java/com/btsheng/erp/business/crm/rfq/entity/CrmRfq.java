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
 * V1.3.7 · Story 1.32 · 询价单（crm_rfq · FR-8-1）
 */
@Data
@Schema(description = "询价单 RFQ（Story 1.32 FR-8-1）")
@TableName("crm_rfq")
public class CrmRfq implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rfq_no")                 private String rfqNo;
    @TableField("title")                  private String title;
    @TableField("material_id")            private Long materialId;
    @TableField("material_code")          private String materialCode;
    @TableField("material_name")          private String materialName;
    @TableField("qty")                    private BigDecimal qty;
    @TableField("unit")                   private String unit;
    @TableField("budget_amount")          private BigDecimal budgetAmount;
    @TableField("required_date")          private LocalDate requiredDate;
    @TableField("status")                 private String status = "DRAFT";
    @TableField("awarded_vendor_id")      private Long awardedVendorId;
    @TableField("awarded_vendor_name")    private String awardedVendorName;
    @TableField("awarded_quote_id")       private Long awardedQuoteId;
    @TableField("awarded_amount")         private BigDecimal awardedAmount;
    @TableField("purchase_order_id")      private Long purchaseOrderId;
    @TableField("purchase_order_no")      private String purchaseOrderNo;
    @TableField("winner_mode")            private String winnerMode = "LOWEST";
    @TableField("inquiry_source_type")    private String inquirySourceType;
    @TableField("pr_id")                  private Long prId;
    @TableField("pr_no")                  private String prNo;
    @TableField("workorder_no")           private String workorderNo;
    @TableField("process_step_no")        private Integer processStepNo;
    @TableField("allocation_id")          private Long allocationId;
    @TableField("convert_status")         private String convertStatus = "NOT_CONVERTED";
    @TableField("converted_order_no")     private String convertedOrderNo;
    @TableField("created_by")             private Long createdBy;
    @TableField("created_at")             private LocalDateTime createdAt;
    @TableField("updated_at")             private LocalDateTime updatedAt;
}
