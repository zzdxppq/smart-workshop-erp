package com.btsheng.erp.platform.label.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签预览响应（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.2）
 *
 * <p>base64 PNG 默认输出（与 OpenAPI LabelPreviewResponse 对齐）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "标签预览响应（base64 PNG）")
public class LabelPreviewResponse {
    @Schema(description = "模板类型", example = "GD")
    private String type;

    @Schema(description = "格式", example = "PNG")
    private String format;

    @Schema(description = "base64 数据 · data:image/png;base64,...", example = "data:image/png;base64,iVBORw0KG...")
    private String base64;

    @Schema(description = "Content-Type", example = "image/png")
    private String contentType;

    @Schema(description = "字节数", example = "4096")
    private Integer sizeBytes;

    @Schema(description = "渲染时间")
    private String renderedAt;
}