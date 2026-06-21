package com.btsheng.erp.business.finance.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.36 · 创建应收请求
 */
@Data
@Schema(description = "创建应收请求（Story 1.36 FR-9-1）")
public class CreateReceivableRequest {
    @Schema(description = "客户 ID", required = true) private Long customerId;
    @Schema(description = "客户名称") private String customerName;
    @Schema(description = "订单 ID", required = true) private Long orderId;
    @Schema(description = "订单号", required = true) private String orderNo;
    @Schema(description = "订单金额 · P1 修补 1 非负", required = true) private BigDecimal totalAmount;
    @Schema(description = "到期日", required = true) private LocalDate dueDate;
}
