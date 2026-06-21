package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码开工请求（AC-5.2.1）")
public class ScanStartRequest {
    @Schema(description = "工单号", required = true)
    private String workorderNo;
    @Schema(description = "机台 ID")
    private Long equipmentId;
    @Schema(description = "设备码 SB-（与 equipmentId 二选一）")
    private String machineBarcode;
    @Schema(description = "工序号", required = true)
    private Integer stepNo;
    @Schema(description = "客户端 ID")
    private String clientId;
}
