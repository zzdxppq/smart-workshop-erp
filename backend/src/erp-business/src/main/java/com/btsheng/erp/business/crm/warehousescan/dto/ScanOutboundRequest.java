package com.btsheng.erp.business.crm.warehousescan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码出库请求（AC-4.2.2）")
public class ScanOutboundRequest {
    @Schema(description = "条码号", required = true)
    private String barcodeNo;
    @Schema(description = "工单号", required = true)
    private String workorderNo;
    @Schema(description = "数量", required = true)
    private Integer qty;
    @Schema(description = "库位编码")
    private String locationCode;
    @Schema(description = "客户端 ID")
    private String clientId;
}
