package com.btsheng.erp.business.crm.warehousescan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码历史查询")
public class ScanQueryRequest {
    @Schema(description = "扫码类型 INBOUND/OUTBOUND")
    private String scanType;
    @Schema(description = "同步状态 SYNCED/PENDING/FAILED")
    private String syncStatus;
    @Schema(description = "条码号")
    private String barcodeNo;
    @Schema(description = "页码")
    private Integer page = 0;
    @Schema(description = "页大小")
    private Integer size = 20;
}
