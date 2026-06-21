package com.btsheng.erp.production.outsource.incoming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.7 · Story 1.25 · 来料质检创建请求
 */
@Data
@Schema(description = "来料质检创建请求（Story 1.25 FR-6-5）")
public class IncomingInspectionRequest {

    @Schema(description = "委外单 ID", example = "1", required = true)
    private Long outsourceId;

    @Schema(description = "送检数量", example = "10", required = true)
    private Integer inspectQty;

    @Schema(description = "通知邮箱（V1.3.7 AD-3 · 必须 163 邮箱）", example = "qa@btsheng-163.com")
    private String notifyEmail;

    @Schema(description = "备注", example = "首次送检")
    private String remark;

    @Schema(description = "检验项目列表（必填 · 至少 1 项）")
    private List<IncomingItemDto> items;

    @Data
    public static class IncomingItemDto {
        @Schema(description = "检验项目名称（必填）", example = "外观", required = true)
        private String itemName;
        @Schema(description = "检验标准", example = "无明显划伤")
        private String standard;
        @Schema(description = "实测值", example = "无划伤")
        private String measuredValue;
        @Schema(description = "是否通过 0/1", example = "1")
        private Integer passed = 0;
    }
}
