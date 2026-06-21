package com.btsheng.erp.business.crm.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** V1.3.7 Story 1.6 · 订单确认请求（DRAFT → CONFIRMED） */
@Data
@Schema(description = "订单确认请求")
public class OrderConfirmRequest {
    @Schema(description = "二次密码（> 20万 必填）")
    private String secondPassword;
    @Schema(description = "备注")
    private String comment;
}
