package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.4 · 图纸查询 6 维过滤
 */
@Data
@Schema(description = "图纸查询请求（6 维过滤）")
public class DrawingQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字（图号/标题/物料）", example = "")
    private String keyword;

    @Schema(description = "版本过滤", example = "v1")
    private String version;

    @Schema(description = "类别过滤", example = "")
    private String category;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "FA 件过滤 0/1", example = "")
    private Integer isFa;

    @Schema(description = "状态过滤 DRAFT/RELEASED/ARCHIVED/OBSOLETE/CONVERTED", example = "")
    private String status;

    @Schema(description = "仅返回已绑定料号的图纸（material_code 非空）", example = "true")
    private Boolean hasMaterialCode;

    @Schema(description = "页码", example = "0")
    private int page = 0;

    @Schema(description = "每页大小", example = "20")
    private int size = 20;
}
