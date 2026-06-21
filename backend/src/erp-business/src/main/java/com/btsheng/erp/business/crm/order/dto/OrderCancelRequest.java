package com.btsheng.erp.business.crm.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** V1.3.7 Story 1.6 · 订单取消请求（含 reason 必填） */
@Data
@Schema(description = "订单取消请求")
public class OrderCancelRequest {
    @Schema(description = "取消原因（必填）", required = true)
    private String reason;
}
