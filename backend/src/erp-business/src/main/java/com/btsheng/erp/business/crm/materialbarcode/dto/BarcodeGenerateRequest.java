package com.btsheng.erp.business.crm.materialbarcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "物料条码生成请求")
public class BarcodeGenerateRequest {
    @Schema(description = "物料编码 WL-XXXX", required = true)
    private String materialCode;
    @Schema(description = "批次号（1.13 联动）")
    private String batchNo;
    @Schema(description = "数量（默认 1）")
    private Integer qty = 1;
    @Schema(description = "工艺 ID")
    private Long processId;
}
