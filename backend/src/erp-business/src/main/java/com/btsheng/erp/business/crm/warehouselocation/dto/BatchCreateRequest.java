package com.btsheng.erp.business.crm.warehouselocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建批次请求（AC-4.3.2 · BATCH{yyyyMMdd}{seq:4}）")
public class BatchCreateRequest {
    @Schema(description = "物料编码", required = true)
    private String materialCode;
    @Schema(description = "供应商 ID", required = true)
    private Long supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "数量", required = true)
    private Integer qty;
    @Schema(description = "库位编码")
    private String locationCode;
    @Schema(description = "FEFO 顺序（先入先出）")
    private Integer fefoOrder;
}
