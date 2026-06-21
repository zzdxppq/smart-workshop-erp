package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "检验单提交判定请求")
public class InspectionFinalizeRequest {

    @Schema(description = "PASS / FAIL / CONDITIONAL", required = true)
    private String overallResult;

    @Schema(description = "RETURN / REWORK / SCRAP（FAIL 时必填）")
    private String disposition;

    @Schema(description = "不良数量（FAIL 时必填）")
    private Integer defectQty;

    @Schema(description = "让步原因（CONDITIONAL 时必填）")
    private String conditionalReason;

    @Schema(description = "图号")
    private String drawingNo;

    private String remark;

    @Schema(description = "检验项（可选，用于更新草稿）")
    private List<InspectionV1389CreateRequest.InspectionV1389ItemDto> inspectItems;
}
