package com.btsheng.erp.production.outsource.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.7 · Story 1.27 · 委外工序质检创建请求
 */
@Data
@Schema(description = "委外工序质检创建请求（Story 1.27 FR-6-7）")
public class QualityCreateRequest {

    @Schema(description = "委外单 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "工序名称", example = "调质", required = true)
    private String processName;

    @Schema(description = "FA（首件）/CMM（三次元）", example = "FA", required = true)
    private String inspectType = "FA";

    @Schema(description = "送检数量", example = "1", required = true)
    private Integer inspectQty = 1;

    @Schema(description = "备注", example = "首次工序质检")
    private String remark;

    @Schema(description = "检验项目列表（必填 · 至少 1 项）")
    private List<QualityItemDto> items;

    @Data
    public static class QualityItemDto {
        @Schema(description = "FA/CMM", example = "FA", required = true)
        private String itemType = "FA";
        @Schema(description = "检验项目名称（必填）", example = "外观", required = true)
        private String itemName;
        @Schema(description = "标准", example = "无氧化色")
        private String standard;
        @Schema(description = "实测值", example = "无氧化")
        private String measuredValue;
        @Schema(description = "CMM 专用允差 ±mm", example = "±0.05")
        private String tolerance;
        @Schema(description = "0/1", example = "1")
        private Integer passed = 0;
    }
}
