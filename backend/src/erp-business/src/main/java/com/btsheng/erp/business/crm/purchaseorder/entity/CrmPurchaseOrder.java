package com.btsheng.erp.business.crm.purchaseorder.entity;

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
 * V1.3.8 Sprint 7 · Story 4.1 · 采购订单主表（crm_purchase_order）
 *
 * <p>V1.3.7 RFQ 流程只在 crm_rfq.purchase_order_no 维护字符串字段，
 * V1.3.8 由 V49__batch.sql 首次创建独立物理表。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "采购订单主表（Story 4.1 · V1.3.8 Sprint 7）")
@TableName("crm_purchase_order")
public class CrmPurchaseOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("po_no")
    private String poNo;

    @TableField("rfq_id")
    private Long rfqId;

    @TableField("pr_id")
    private Long prId;

    @TableField("pr_no")
    private String prNo;

    @TableField("workorder_no")
    private String workorderNo;

    @TableField("mrp_run_id")
    private Long mrpRunId;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("status")
    private String status;

    @TableField("source_type")
    private String sourceType;

    @TableField("purchase_reason")
    private String purchaseReason;

    @TableField("approval_route")
    private String approvalRoute;

    @TableField("approval_status")
    private String approvalStatus;

    @TableField("remark")
    private String remark;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}