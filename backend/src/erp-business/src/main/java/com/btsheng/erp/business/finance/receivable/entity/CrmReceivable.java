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
 * V1.3.7 · Story 1.36 · 财务·应收账款（crm_receivable · FR-9-1）
 */
@Data
@Schema(description = "应收账款（Story 1.36 FR-9-1）")
@TableName("crm_receivable")
public class CrmReceivable implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("receivable_no")   private String receivableNo;
    @TableField("customer_id")     private Long customerId;
    @TableField("customer_name")   private String customerName;
    @TableField("order_id")        private Long orderId;
    @TableField("order_no")        private String orderNo;
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
