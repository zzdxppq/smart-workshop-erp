package com.btsheng.erp.business.crm.materialbarcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "批量物料条码生成请求（P1 修补 3 · 100 并发不重复）")
public class BarcodeBatchGenerateRequest {
    @Schema(description = "BOM ID（1.9 联动）", required = true)
    private Long bomId;
    @Schema(description = "目标数量（缩放比例）", required = true)
    private Integer targetQty;
    @Schema(description = "批次号")
    private String batchNo;
}
