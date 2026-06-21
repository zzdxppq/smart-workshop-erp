package com.btsheng.erp.business.crm.qualityinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.7 · Story 1.28 · 创建来料/过程/成品检单请求
 */
@Data
@Schema(description = "创建来料/过程/成品检单请求（Story 1.28 FR-7-1）")
public class InspectionCreateRequest {

    @Schema(description = "IQC（来料）/IPQC（过程）/OQC（成品）", example = "IQC", required = true)
    private String inspectType;

    @Schema(description = "物料 ID（IQC 必填）", example = "1001")
    private Long materialId;

    @Schema(description = "物料编码", example = "M-Q235-01")
    private String materialCode;

    @Schema(description = "物料名称", example = "Q235 钢板 10mm")
    private String materialName;

    @Schema(description = "工单 ID（IPQC 必填）", example = "1")
    private Long workOrderId;

    @Schema(description = "工单号", example = "GD20260608-0001")
    private String workOrderNo;

    @Schema(description = "工序名称（IPQC 必填）", example = "粗车")
    private String processName;

    @Schema(description = "批次号", example = "B20260610-0001")
    private String batchNo;

    @Schema(description = "批量", example = "500")
    private Integer lotSize = 0;

    @Schema(description = "抽样量", example = "50")
    private Integer sampleSize = 0;

    @Schema(description = "AQL 等级 0.65/1.0/1.5/2.5/4.0", example = "1.0")
    private String aqlLevel = "1.0";

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "检验项目列表（必填 · 至少 1 项 · P1 修补 2）")
    private List<InspectionItemDto> items;

    @Schema(description = "抽样记录（可选 · P1 修补 1）")
    private List<SampleDto> samples;

    @Data
    public static class InspectionItemDto {
        @Schema(description = "检验项目名称（必填）", example = "外观", required = true)
        private String itemName;
        @Schema(description = "判定标准", example = "无锈蚀/无划伤")
        private String standard;
        @Schema(description = "实测值", example = "合格")
        private String measuredValue;
        @Schema(description = "严重度 INFO/WARN/ERROR/CRITICAL（必填）", example = "INFO", required = true)
        private String severity = "INFO";
        @Schema(description = "0/1", example = "1")
        private Integer passed = 0;
        @Schema(description = "不良描述")
        private String defectDesc;
    }

    @Data
    public static class SampleDto {
        @Schema(description = "样本编号", example = "S001")
        private String sampleNo;
        @Schema(description = "抽样量", example = "50")
        private Integer sampleQty = 1;
        @Schema(description = "不良数量", example = "0")
        private Integer defectQty = 0;
        @Schema(description = "0/1", example = "1")
        private Integer aqlPassed = 0;
        @Schema(description = "备注")
        private String remark;
    }
}
