package com.btsheng.erp.business.crm.qualityfa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.7 · Story 1.29 · FA 首件创建请求
 */
@Data
@Schema(description = "FA 首件创建请求（Story 1.29 FR-7-2）")
public class FaCreateRequest {

    @Schema(description = "工单 ID（必填）", example = "1", required = true)
    private Long workOrderId;

    @Schema(description = "工单号", example = "GD20260608-0001", required = true)
    private String workOrderNo;

    @Schema(description = "工序 ID（必填）", example = "10", required = true)
    private Long processId;

    @Schema(description = "工序名称", example = "粗车", required = true)
    private String processName;

    @Schema(description = "操作工 ID", example = "201")
    private Long operatorUserId;

    @Schema(description = "首件数量（默认 1）", example = "1")
    private Integer inspectQty = 1;

    @Schema(description = "PDF 报告路径", example = "/reports/fa/QF20260612-0001.pdf")
    private String pdfUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "8 维度检验项目（必填 · 至少 1 项）")
    private List<FaItemDto> items;

    @Data
    public static class FaItemDto {
        @Schema(description = "尺寸/形位/粗糙度/硬度/材质/外观/装配/性能 8 维度", example = "尺寸", required = true)
        private String dimension;
        @Schema(description = "项目名称（必填）", example = "外径 φ50", required = true)
        private String itemName;
        @Schema(description = "标准", example = "50±0.05")
        private String standard;
        @Schema(description = "实测值", example = "50.01")
        private String measuredValue;
        @Schema(description = "允差 ±mm", example = "±0.05")
        private String tolerance;
        @Schema(description = "0/1", example = "1")
        private Integer passed = 0;
    }
}
