package com.btsheng.erp.business.crm.qualitydefect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.31 · 不良品登记请求
 */
@Data
@Schema(description = "不良品登记请求（Story 1.31 FR-7-4）")
public class DefectCreateRequest {

    @Schema(description = "来源类型 INTERNAL/OUTSOURCE", example = "INTERNAL", required = true)
    private String sourceType;

    @Schema(description = "来源单 ID（IQC/IPQC/OQC/FA/CMM 单 ID）", example = "3")
    private Long sourceId;

    @Schema(description = "来源单号", example = "QI20260612-0003")
    private String sourceNo;

    @Schema(description = "不良类型（必填）", example = "Mg 元素低于下限", required = true)
    private String defectType;

    @Schema(description = "严重度 MINOR/MAJOR/CRITICAL", example = "CRITICAL", required = true)
    private String severity = "MAJOR";

    @Schema(description = "不良数量", example = "10", required = true)
    private Integer qty = 1;

    @Schema(description = "物料 ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "工单 ID")
    private Long workOrderId;

    @Schema(description = "工单号")
    private String workOrderNo;

    @Schema(description = "8D D1 团队组建")
    private String d1Team;

    @Schema(description = "8D D4 根本原因")
    private String d4RootCause;

    @Schema(description = "8D D5 永久对策")
    private String d5Action;

    @Schema(description = "8D D8 关闭")
    private String d8Closure;

    @Schema(description = "总生产数量（PPM 计算）", example = "50")
    private Integer totalQty;

    @Schema(description = "责任部门（必填 · P1 修补 2）", example = "QA", required = true)
    private String responsibleDept;

    @Schema(description = "V2.1 原因分类 MATERIAL/PROCESS/EQUIPMENT/HUMAN", example = "MATERIAL")
    private String causeCategory;
}
