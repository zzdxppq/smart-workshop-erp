package com.btsheng.erp.production.outsource.incoming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.25 · 添加不良项请求
 */
@Data
@Schema(description = "添加不良项请求（Story 1.25 FR-6-5）")
public class AddDefectRequest {

    @Schema(description = "质检单 ID", example = "1", required = true)
    private Long inspectionId;

    @Schema(description = "关联检验项 ID（可选）", example = "1")
    private Long itemId;

    @Schema(description = "不良类型", example = "尺寸超差", required = true)
    private String defectType;

    @Schema(description = "严重度 MINOR/MAJOR/CRITICAL（必填）", example = "MAJOR", required = true)
    private String severity;

    @Schema(description = "不良数量", example = "1", required = true)
    private Integer qty = 1;

    @Schema(description = "描述", example = "孔径偏差 0.08 mm")
    private String description;
}
