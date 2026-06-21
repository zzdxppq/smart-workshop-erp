package com.btsheng.erp.business.crm.noorderpurchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * V1.3.8 · Story 4.1 · 无订单采购创建请求 DTO
 *
 * <p>Story 4.1 AC-4.1.1：采购员主动创建 PO，source_type=NO_ORDER，purchase_reason 必填。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "无订单采购创建请求（Story 4.1 AC-4.1.1）")
public class NoOrderPurchaseRequest {

    @Schema(description = "采购理由（NO_ORDER 模式必填）", example = "URGENT_REPLENISH", required = true)
    @NotNull
    private String purchaseReason;

    @Schema(description = "供应商 ID", example = "2001", required = true)
    @NotNull
    private Long supplierId;

    @Schema(description = "物料列表（每项至少 1 行）", required = true)
    @NotEmpty
    @Valid
    private List<Item> items;

    @Schema(description = "备注（可选）", example = "生产中途 M1 损坏 5 件，紧急补料")
    private String remark;

    @Data
    public static class Item {
        @Schema(description = "物料 ID", example = "5001", required = true)
        @NotNull
        private Long materialId;

        @Schema(description = "采购数量", example = "50", required = true)
        @NotNull
        private Integer quantity;

        @Schema(description = "预估单价", example = "80.0", required = true)
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal estimatedPrice;
    }
}