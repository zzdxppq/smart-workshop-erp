package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "OpenAPI 扫码开工（/app/workorders/{barcode}/start）")
public class AppBarcodeStartRequest {
    @Schema(description = "设备码 SB-")
    private String machineBarcode;
    @Schema(description = "机台 ID")
    private Long equipmentId;
    @Schema(description = "操作员 ID")
    private Long operatorId;
    @Schema(description = "工序号，默认 1")
    private Integer stepNo;
}
