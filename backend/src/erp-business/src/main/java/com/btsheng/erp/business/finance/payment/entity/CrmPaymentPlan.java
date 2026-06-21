package com.btsheng.erp.business.finance.payment.entity;

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
 * V1.3.7 · Story 1.38 · 财务·回款计划（crm_payment_plan · FR-9-3）
 */
@Data
@Schema(description = "回款计划（Story 1.38 FR-9-3）")
@TableName("crm_payment_plan")
public class CrmPaymentPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("plan_no")         private String planNo;
    @TableField("customer_id")     private Long customerId;
    @TableField("customer_name")   private String customerName;
    @TableField("order_id")        private Long orderId;
    @TableField("order_no")        private String orderNo;
    @TableField("receivable_id")   private Long receivableId;
    @TableField("receivable_no")   private String receivableNo;
    @TableField("total_amount")    private BigDecimal totalAmount;
    @TableField("planned_amount")  private BigDecimal plannedAmount;
    @TableField("paid_amount")     private BigDecimal paidAmount;
    @TableField("planned_date")    private LocalDate plannedDate;
    @TableField("alert_level")     private String alertLevel = "PENDING";
    @TableField("paid_at")         private LocalDateTime paidAt;
    @TableField("paid_by")         private Long paidBy;
    @TableField("remark")          private String remark;
    @TableField("created_at")      private LocalDateTime createdAt;
    @TableField("updated_at")      private LocalDateTime updatedAt;
}
