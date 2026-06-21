package com.btsheng.erp.business.crm.order.dto;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** V1.3.7 Story 1.6 · 订单更新请求（仅 DRAFT） */
@Data
@Schema(description = "订单更新请求（仅 DRAFT 状态可改）")
public class OrderUpdateRequest {
    @Schema(description = "订单主单字段")
    private CrmOrder order;
    @Schema(description = "订单明细（替换模式）")
    private List<CrmOrderItem> items;
}
