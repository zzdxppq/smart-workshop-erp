package com.btsheng.erp.business.crm.order.dto;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** V1.3.7 Story 1.6 · 订单创建请求 */
@Data
@Schema(description = "订单创建请求")
public class OrderCreateRequest {
    @Schema(description = "订单主单")
    private CrmOrder order;
    @Schema(description = "订单明细（≥ 1）")
    private List<CrmOrderItem> items;
}
