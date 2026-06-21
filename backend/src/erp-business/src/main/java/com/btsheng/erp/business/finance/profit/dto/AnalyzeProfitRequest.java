package com.btsheng.erp.business.finance.profit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.39 · 利润分析请求
 */
@Data
@Schema(description = "利润分析请求（Story 1.39 FR-9-4）")
public class AnalyzeProfitRequest {
    @Schema(description = "订单 ID", required = true) private Long orderId;
    @Schema(description = "订单号", required = true)   private String orderNo;
    @Schema(description = "客户 ID", required = true)  private Long customerId;
    @Schema(description = "客户名称", required = true)  private String customerName;
    @Schema(description = "产品 ID")                    private Long productId;
    @Schema(description = "产品编码")                   private String productCode;
    @Schema(description = "产品名称")                   private String productName;
    @Schema(description = "订单收入（不含税）", required = true) private BigDecimal revenue;
    @Schema(description = "SETTLED 日期", required = true) private LocalDate settledDate;
}
