package com.btsheng.erp.production.scan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码报工请求（AC-5.2.2 · 完成数量 + 实际工时 + 异常标记）")
public class ScanReportRequest {
    @Schema(description = "工单号", required = true)
    private String workorderNo;
    @Schema(description = "工序号", required = true)
    private Integer stepNo;
    @Schema(description = "报工数量", required = true)
    private Integer reportedQty;
    @Schema(description = "实际工时（分钟）")
    private Integer actualMinutes;
    @Schema(description = "是否异常")
    private Integer isAbnormal = 0;
    @Schema(description = "异常类型 QUALITY/EQUIPMENT/MATERIAL")
    private String abnormalType;
    @Schema(description = "异常备注")
    private String abnormalNote;
    @Schema(description = "客户端 ID")
    private String clientId;
}
