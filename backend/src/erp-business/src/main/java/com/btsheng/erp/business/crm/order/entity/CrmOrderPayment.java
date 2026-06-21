package com.btsheng.erp.business.crm.order.entity;

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
 * V1.3.7 · Story 1.6 · AC-2.3.4 · 订单回款（SETTLED 状态联动）
 */
@Data
@Schema(description = "订单回款（crm_order_payment）")
@TableName("crm_order_payment")
public class CrmOrderPayment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id") private Long orderId;
    @TableField("payment_no") private String paymentNo;
    @TableField("amount") private BigDecimal amount;
    @TableField("payment_date") private LocalDate paymentDate;
    @TableField("payment_method") private String paymentMethod = "BANK";
    @TableField("status") private String status = "PENDING";
    @TableField("comment") private String comment;
    @TableField("created_at") private LocalDateTime createdAt;
}
