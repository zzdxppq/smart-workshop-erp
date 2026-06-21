package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.28 · 追加检验项目请求
 */
@Data
@Schema(description = "追加检验项目请求（Story 1.28 FR-7-1）")
public class AddInspectionItemRequest {

    @Schema(description = "检验单 ID", example = "1", required = true)
    private Long inspectionId;

    @Schema(description = "检验项目名称（必填）", example = "硬度", required = true)
    private String itemName;

    @Schema(description = "判定标准", example = "HRC 28-32")
    private String standard;

    @Schema(description = "实测值", example = "HRC 30")
    private String measuredValue;

    @Schema(description = "严重度 INFO/WARN/ERROR/CRITICAL（必填）", example = "INFO", required = true)
    private String severity = "INFO";

    @Schema(description = "0/1", example = "1")
    private Integer passed = 0;

    @Schema(description = "不良描述", example = "尺寸超差 0.04mm")
    private String defectDesc;
}
