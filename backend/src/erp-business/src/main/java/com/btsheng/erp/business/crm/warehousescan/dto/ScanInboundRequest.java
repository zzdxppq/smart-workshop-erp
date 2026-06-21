package com.btsheng.erp.business.crm.warehousescan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码入库请求（AC-4.2.1）")
public class ScanInboundRequest {
    @Schema(description = "条码号 BC{yyyyMMdd}{seq:4}", required = true)
    private String barcodeNo;
    @Schema(description = "库位编码", required = true)
    private String locationCode;
    @Schema(description = "数量", required = true)
    private Integer qty;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "客户端 ID（离线同步用）")
    private String clientId;
    @Schema(description = "客户端扫码时间（毫秒）")
    private Long clientScannedAt;
}
