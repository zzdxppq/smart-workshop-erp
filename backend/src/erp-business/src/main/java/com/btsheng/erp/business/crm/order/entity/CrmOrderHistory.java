package com.btsheng.erp.business.crm.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.6 · AC-2.3.1 · 订单变更历史（红线 5 变更留痕）
 *
 * <p>14 个 operation 枚举：CREATE/UPDATE/CONFIRM/APPROVE/REJECT/CONVERT_PROD/CONVERT_OUTSUB/SHIP/PARTIAL_SHIP/SETTLE/CLOSE/CANCEL/CREDIT_CHECK/PDF_DOWNLOAD/EXCEL_DOWNLOAD/PROFIT_ANALYSIS
 */
@Data
@Schema(description = "订单变更历史（crm_order_history）")
@TableName("crm_order_history")
public class CrmOrderHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id") private Long orderId;
    @TableField("operation") private String operation;
    @TableField("before_json") private String beforeJson;
    @TableField("after_json") private String afterJson;
    @TableField("changed_by") private Long changedBy;
    @TableField("changed_at") private LocalDateTime changedAt;
}
