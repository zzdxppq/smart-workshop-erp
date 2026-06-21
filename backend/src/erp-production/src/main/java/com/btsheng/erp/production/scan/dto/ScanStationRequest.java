package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码过站请求（AC-5.2.3）")
public class ScanStationRequest {
    @Schema(description = "工单号", required = true)
    private String workorderNo;
    @Schema(description = "源工序号", required = true)
    private Integer fromStepNo;
    @Schema(description = "目标工序号", required = true)
    private Integer toStepNo;
    @Schema(description = "源机台 ID")
    private Long fromEquipmentId;
    @Schema(description = "目标机台 ID", required = true)
    private Long toEquipmentId;
    @Schema(description = "客户端 ID")
    private String clientId;
}
