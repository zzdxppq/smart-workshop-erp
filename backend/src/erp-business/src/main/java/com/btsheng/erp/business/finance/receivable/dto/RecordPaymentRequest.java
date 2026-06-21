package com.btsheng.erp.business.finance.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.36 · 收付款请求
 */
@Data
@Schema(description = "收付款请求（Story 1.36 FR-9-1）")
public class RecordPaymentRequest {
    @Schema(description = "类型 RECEIPT/PAYMENT", required = true) private String type;
    @Schema(description = "应收/应付 ID", required = true) private Long refId;
    @Schema(description = "本次金额 · P1 修补 2 ≤ 未收/未付", required = true) private BigDecimal amount;
    @Schema(description = "方式 BANK/CASH/CHECK/WECHAT/ALIPAY") private String method = "BANK";
    @Schema(description = "收付操作人 user_id", required = true) private Long paidBy;
    @Schema(description = "备注") private String remark;
    @Schema(description = "收款/付款日期 YYYY-MM-DD，默认当天") private String paidDate;
}
