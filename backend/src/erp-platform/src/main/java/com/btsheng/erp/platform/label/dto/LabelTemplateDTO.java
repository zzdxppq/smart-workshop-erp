package com.btsheng.erp.platform.label.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签模板对外 DTO（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.1）
 *
 * <p>字段对应 OpenAPI LabelTemplate schema
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "标签模板元数据")
public class LabelTemplateDTO {
    @Schema(description = "模板类型", example = "GD", allowableValues = {"GD", "LZ", "SB", "WW", "WL"})
    private String type;

    @Schema(description = "显示名", example = "工单码")
    private String name;

    @Schema(description = "前缀", example = "GD-")
    private String prefix;

    @Schema(description = "色条 HEX", example = "#1E40AF")
    private String colorStrip;

    @Schema(description = "复用基础模板 · SB 复用 GD · 其他 null")
    private String reuseFrom;

    @Schema(description = "布局参数")
    private LabelLayout layout;

    @Schema(description = "DPI", example = "300", allowableValues = {"203", "300"})
    private Integer dpi;

    @Schema(description = "启停", example = "true")
    private Boolean enabled;

    @Schema(description = "厂名（标签顶栏）", example = "昆山佰泰胜精密加工")
    private String factoryName;

    @Schema(description = "QR 内容示例", example = "GD-260614-001")
    private String qrExample;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;
}