package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "自动推送待检任务请求")
public class PendingInspectionRequest {

    @Schema(description = "IQC/IPQC/OQC")
    private String inspectType;

    private String materialCode;
    private String materialName;
    private Long materialId;
    private String workOrderNo;
    private Long workOrderId;
    private String processName;
    private String batchNo;
    private Integer qty;
    @Schema(description = "幂等键，如 SCAN:SC20260620-0001")
    private String sourceRef;
    private String remark;
}
