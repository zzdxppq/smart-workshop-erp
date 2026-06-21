package com.btsheng.erp.business.finance.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.38 · 财务·回款告警（crm_payment_alert · FR-9-3）
 */
@Data
@Schema(description = "回款告警（Story 1.38 FR-9-3）")
@TableName("crm_payment_alert")
public class CrmPaymentAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("plan_id")           private Long planId;
    @TableField("alert_level")       private String alertLevel;
    @TableField("alert_message")     private String alertMessage;
    @TableField("days_to_due")       private Integer daysToDue;
    @TableField("notified_at")       private LocalDateTime notifiedAt;
    @TableField("notified_channel")  private String notifiedChannel;
    @TableField("acknowledged")      private Integer acknowledged;
    @TableField("acknowledged_by")   private Long acknowledgedBy;
    @TableField("acknowledged_at")   private LocalDateTime acknowledgedAt;
    @TableField("created_at")        private LocalDateTime createdAt;
}
