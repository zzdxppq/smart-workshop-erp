package com.btsheng.erp.business.crm.noorderpurchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.8 · Story 4.1 · 无订单采购创建响应 DTO
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "无订单采购创建响应（Story 4.1 AC-4.1.1）")
public class NoOrderPurchaseResponse {

    private Long poId;

    @Schema(description = "PO 单号")
    private String poNo;

    @Schema(description = "FROM_ORDER / FROM_MRP / NO_ORDER")
    private String sourceType;

    private String purchaseReason;

    @Schema(description = "PROCUREMENT_MANAGER / DEPT_MANAGER / GM / GM+PROCUREMENT_MANAGER")
    private String approvalRoute;

    @Schema(description = "预估总金额")
    private BigDecimal estimatedTotal;
}