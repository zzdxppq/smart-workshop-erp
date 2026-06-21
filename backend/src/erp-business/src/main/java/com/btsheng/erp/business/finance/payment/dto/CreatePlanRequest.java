package com.btsheng.erp.business.finance.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.38 · 创建回款计划请求
 */
@Data
@Schema(description = "创建回款计划请求（Story 1.38 FR-9-3）")
public class CreatePlanRequest {
    @Schema(description = "客户 ID", required = true) private Long customerId;
    @Schema(description = "客户名称") private String customerName;
    @Schema(description = "订单 ID", required = true) private Long orderId;
    @Schema(description = "订单号", required = true) private String orderNo;
    @Schema(description = "关联 1.36 应收 ID") private Long receivableId;
    @Schema(description = "关联 1.36 应收单号") private String receivableNo;
    @Schema(description = "订单金额", required = true) private BigDecimal totalAmount;
    @Schema(description = "计划回款金额 · P1 修补 1 ≤ 订单金额", required = true) private BigDecimal plannedAmount;
    @Schema(description = "计划回款日", required = true) private LocalDate plannedDate;
    @Schema(description = "备注") private String remark;
}
