package com.btsheng.erp.business.crm.reconcile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "月度对账单创建请求（AC-6.1.1 · RC{yyyyMM}{seq:4}）")
public class ReconcileCreateRequest {
    @Schema(description = "供应商 ID", required = true)
    private Long vendorId;
    @Schema(description = "供应商名称", required = true)
    private String vendorName;
    @Schema(description = "对账年", required = true)
    private Integer periodYear;
    @Schema(description = "对账月 1-12", required = true)
    private Integer periodMonth;
    @Schema(description = "对账明细")
    private List<ReconcileItemRequest> items;
}
