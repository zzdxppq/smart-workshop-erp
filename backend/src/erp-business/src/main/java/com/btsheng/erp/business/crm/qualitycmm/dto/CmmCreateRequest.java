package com.btsheng.erp.business.crm.qualitycmm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * V1.3.7 · Story 1.30 · CMM 三次元创建请求
 */
@Data
@Schema(description = "CMM 三次元创建请求（Story 1.30 FR-7-3）")
public class CmmCreateRequest {

    @Schema(description = "工单 ID", example = "1")
    private Long workOrderId;

    @Schema(description = "工单号", example = "GD20260608-0001")
    private String workOrderNo;

    @Schema(description = "图号", example = "DWG-001-Rev2")
    private String drawingNo;

    @Schema(description = "零件名称", example = "法兰盘")
    private String partName;

    @Schema(description = "PDF 报告路径")
    private String pdfUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "测点列表（必填 · P1 修补 1 至少 3 个）")
    private List<CmmPointDto> points;

    @Data
    public static class CmmPointDto {
        @Schema(description = "测点编号", example = "P1", required = true)
        private String pointNo;
        @Schema(description = "X/Y/Z", example = "X")
        private String axis = "X";
        @Schema(description = "标称值", example = "50.0000", required = true)
        private BigDecimal nominalValue;
        @Schema(description = "实测值", example = "50.0050", required = true)
        private BigDecimal measuredValue;
        @Schema(description = "上偏差", example = "0.0500")
        private BigDecimal toleranceUpper;
        @Schema(description = "下偏差", example = "-0.0500")
        private BigDecimal toleranceLower;
    }
}
