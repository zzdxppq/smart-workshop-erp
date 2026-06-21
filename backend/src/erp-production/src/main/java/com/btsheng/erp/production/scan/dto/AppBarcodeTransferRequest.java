package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OpenAPI 扫码过站（/app/transfer/{barcode}/next）")
public class AppBarcodeTransferRequest {
    private String workorderNo;
    private String toProcessCode;
    private Integer qty;
    @Schema(description = "源工序，默认 1")
    private Integer fromStepNo;
    @Schema(description = "目标工序，默认 2")
    private Integer toStepNo;
}
