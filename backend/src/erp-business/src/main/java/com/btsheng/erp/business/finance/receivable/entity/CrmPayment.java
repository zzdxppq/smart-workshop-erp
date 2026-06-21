package com.btsheng.erp.business.finance.receivable.entity;

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
 * V1.3.7 · Story 1.36 · 财务·收付款记录（crm_payment · FR-9-1）
 */
@Data
@Schema(description = "收付款记录（Story 1.36 FR-9-1）")
@TableName("crm_payment")
public class CrmPayment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("payment_no")  private String paymentNo;
    @TableField("type")        private String type;
    @TableField("ref_id")      private Long refId;
    @TableField("ref_no")      private String refNo;
    @TableField("amount")      private BigDecimal amount;
    @TableField("method")      private String method = "BANK";
    @TableField("paid_by")     private Long paidBy;
    @TableField("paid_at")     private LocalDateTime paidAt;
    @TableField("remark")      private String remark;
    @TableField("created_at")  private LocalDateTime createdAt;
}
