package com.btsheng.erp.business.crm.reconcile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "厂商对账确认请求（AC-6.1.2 · 40905 金额校验）")
public class ReconcileVendorConfirmRequest {
    @Schema(description = "厂商确认金额明细（itemId → vendorAmount）", required = true)
    private List<VendorAmountItem> vendorAmounts;

    @Data
    public static class VendorAmountItem {
        @Schema(description = "对账明细 ID", required = true)
        private Long itemId;
        @Schema(description = "厂商确认金额", required = true)
        private BigDecimal vendorAmount;
    }
}
