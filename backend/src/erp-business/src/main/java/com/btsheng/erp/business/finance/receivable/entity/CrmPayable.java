package com.btsheng.erp.business.finance.receivable.entity;

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
 * V1.3.7 · Story 1.36 · 财务·应付账款（crm_payable · FR-9-1）
 */
@Data
@Schema(description = "应付账款（Story 1.36 FR-9-1）")
@TableName("crm_payable")
public class CrmPayable implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("payable_no")      private String payableNo;
    @TableField("vendor_id")       private Long vendorId;
    @TableField("vendor_name")     private String vendorName;
    @TableField("po_id")           private Long poId;
    @TableField("po_no")           private String poNo;
    @TableField("total_amount")    private BigDecimal totalAmount;
    @TableField("paid_amount")     private BigDecimal paidAmount;
    @TableField("unpaid_amount")   private BigDecimal unpaidAmount;
    @TableField("due_date")        private LocalDate dueDate;
    @TableField("aging_days")      private Integer agingDays;
    @TableField("aging_bucket")    private String agingBucket = "CURRENT";
    @TableField("status")          private String status = "OPEN";
    @TableField("created_at")      private LocalDateTime createdAt;
    @TableField("updated_at")      private LocalDateTime updatedAt;
}
