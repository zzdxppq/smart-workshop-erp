package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.9 Sprint 13.1 · OpenAPI InspectionResponse 契约
 */
@Data
@Schema(description = "V1.3.9 检验单创建响应")
public class InspectionV1389Response {
    private Long inspectionId;
    private String inspectionNo;
    private String status;
    @Schema(description = "最终状态中文标签")
    private String statusLabel;
    private String createdAt;
    private String downstreamOrderNo;
}
