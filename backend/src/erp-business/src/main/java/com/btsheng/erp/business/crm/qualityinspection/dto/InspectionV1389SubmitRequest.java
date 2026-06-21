package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.9 · APP 品检提交（录入实测 + 判定结论）
 */
@Data
@Schema(description = "检验单提交请求（手机端主力 · PC 辅助提交）")
public class InspectionV1389SubmitRequest {

    @Schema(description = "PASS / FAIL", required = true)
    private String conclusion;

    @Schema(description = "检验项实测与判定", required = true)
    private List<ItemSubmitDto> items;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "不合格原因（conclusion=FAIL 时）")
    private String rejectReason;

    @Schema(description = "PASS / FAIL / CONDITIONAL")
    private String overallResult;

    @Schema(description = "RETURN / REWORK / SCRAP")
    private String disposition;

    private Integer defectQty;

    private String conditionalReason;

    @Data
    public static class ItemSubmitDto {
        private Long id;
        private String itemName;
        private String measuredValue;
        @Schema(description = "PASS / FAIL")
        private String result;
        @Schema(description = "INFO/WARN/ERROR/CRITICAL")
        private String severity;
        private String defectDesc;
    }
}
