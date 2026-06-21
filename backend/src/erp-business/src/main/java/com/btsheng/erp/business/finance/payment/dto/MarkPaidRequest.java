package com.btsheng.erp.business.finance.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.38 · 标记已回款请求
 */
@Data
@Schema(description = "标记已回款请求（Story 1.38 FR-9-3）")
public class MarkPaidRequest {
    @Schema(description = "本次回款金额 · P1 修补 1 ≤ 计划未回款", required = true) private BigDecimal paidAmount;
    @Schema(description = "收款人 user_id", required = true) private Long paidBy;
    @Schema(description = "备注") private String remark;
}
