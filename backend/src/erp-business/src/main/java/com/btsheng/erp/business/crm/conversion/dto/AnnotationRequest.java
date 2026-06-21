package com.btsheng.erp.business.crm.conversion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.1 标注请求
 *
 * 字段校验：version 必填 + content 至少 1 字符
 */
@Data
@Schema(description = "图纸标注请求（POST /drawings/{id}/annotations）")
public class AnnotationRequest {

    @Schema(description = "挂载版本（P1 修补：必填 · 防 v1→v2 标注丢失）", example = "v1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;

    @Schema(description = "标注类型：DIMENSION/TOLERANCE/PROCESS_REQ/TECH_NOTE", example = "DIMENSION", allowableValues = {"DIMENSION", "TOLERANCE", "PROCESS_REQ", "TECH_NOTE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "标注内容（至少 1 字符）", example = "法兰外径 φ100±0.05", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "颜色：RED/YELLOW/BLUE/GREEN", example = "RED", allowableValues = {"RED", "YELLOW", "BLUE", "GREEN"})
    private String color = "RED";

    @Schema(description = "PDF 视口 X 坐标", example = "100.50")
    private BigDecimal x = BigDecimal.ZERO;

    @Schema(description = "PDF 视口 Y 坐标", example = "200.30")
    private BigDecimal y = BigDecimal.ZERO;

    @Schema(description = "标注框宽", example = "80.00")
    private BigDecimal width = BigDecimal.ZERO;

    @Schema(description = "标注框高", example = "30.00")
    private BigDecimal height = BigDecimal.ZERO;

    @Schema(description = "优先级 1-10（10 最高）", example = "9", minimum = "1", maximum = "10")
    private Integer priority = 5;

    @Schema(description = "SVG 嵌入数据（P2 修补 · 部署阶段）")
    private String svgData;
}
