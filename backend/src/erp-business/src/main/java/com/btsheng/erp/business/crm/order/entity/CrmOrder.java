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
import java.util.List;

/**
 * V1.3.7 · Story 1.6 · AC-2.3.1 · 订单主表
 *
 * <p>V2.1 改造：状态机扩展
 * <p>8 状态机：DRAFT → APPROVED → PROCESSING → PENDING_PRODUCTION → IN_PRODUCTION → PARTIAL_SHIPPED → SHIPPED → SETTLED → CLOSED
 * <br>驳回回退：PROCESSING → APPROVED（工程转化可退回）
 * <br>取消：DRAFT/APPROVED → CANCELLED
 */
@Data
@Schema(description = "订单主表（crm_order）")
@TableName("crm_order")
public class CrmOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_no") private String orderNo;             // XS+YYYYMMDD+NNNN
            @TableField("quote_id") private Long quoteId;               // 1.5 转单来源
    @TableField("source_quotation_id") private Long sourceQuotationId;  // V2.1 来源报价单ID
    @TableField("customer_id") private Long customerId;
    @TableField("customer_name") private String customerName;
    @TableField("owner_user_id") private Long ownerUserId;
    @TableField("dept_id") private Long deptId;
    @TableField("currency") private String currency = "CNY";
    @TableField("total_amount") private BigDecimal totalAmount;
    @TableField("delivery_date") private LocalDate deliveryDate;
    @TableField("is_fa") private Integer isFa = 0;
    @TableField("is_new") private Integer isNew = 0;
    @TableField("is_urgent") private Integer isUrgent = 0;
    @TableField("status") private String status = "DRAFT";
    @TableField("current_node") private Integer currentNode = 1;
    @TableField("comment") private String comment;
    @TableField("detailed_process") private String detailedProcess;  // V2.1 工程师细化的详细工艺(JSON)
    @TableField("bom_data") private String bomData;  // V2.1 工程师编制的BOM数据(JSON)
    @TableField("production_order_no") private String productionOrderNo;  // GD+...
            @TableField("outsource_order_no") private String outsourceOrderNo;     // WW+...
    @TableField("credit_limit_check") private Integer creditLimitCheck = 0;
    @TableField("is_deleted") private Integer isDeleted = 0;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;

    /** 非持久化：订单明细（列表接口填充） */
    @TableField(exist = false)
    private List<CrmOrderItem> items;
}
