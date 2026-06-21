package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.9 Sprint 13.1 · OpenAPI InspectionCreateRequest 契约
 */
@Data
@Schema(description = "V1.3.9 创建检验单请求")
public class InspectionV1389CreateRequest {

    @Schema(description = "物料编码", required = true)
    private String materialCode;

    @Schema(description = "INCOMING/IN_PROCESS/OUTGOING/FA/CMM", required = true)
    private String inspectionType;

    @Schema(description = "PASS/FAIL/PENDING/REWORK")
    private String qualityStatus;

    @Schema(description = "PASS / FAIL / CONDITIONAL · 提交时立即判定")
    private String overallResult;

    @Schema(description = "RETURN / REWORK / SCRAP（FAIL 时必填）")
    private String disposition;

    @Schema(description = "不良数量（FAIL 时必填）")
    private Integer defectQty;

    @Schema(description = "让步原因（CONDITIONAL 时必填）")
    private String conditionalReason;

    @Schema(description = "图号")
    private String drawingNo;

    @Schema(description = "编辑已有待检单 ID")
    private Long inspectionId;

    @Schema(description = "检验项", required = true)
    private List<InspectionV1389ItemDto> inspectItems;

    private String remark;

    @Schema(description = "工单号（IPQC/OQC · APP 扫码 GD-）")
    private String workOrderNo;

    @Schema(description = "工序名称（IPQC）")
    private String processName;

    @Data
    public static class InspectionV1389ItemDto {
        private String itemName;
        private String standard;
        private String measuredValue;
        private String result;
        private Integer sortOrder;
    }
}
