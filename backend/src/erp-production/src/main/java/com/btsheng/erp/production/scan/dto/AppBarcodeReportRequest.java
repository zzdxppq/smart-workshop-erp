package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OpenAPI 扫码报工（/app/workorders/{barcode}/report）")
public class AppBarcodeReportRequest {
    private Integer qtyDone;
    private Integer qtyOk;
    private Integer qtyScrap;
    @Schema(description = "工序号，默认 1")
    private Integer stepNo;
}
