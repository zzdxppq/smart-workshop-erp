package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.1 · 修改图纸请求（DRAFT 状态）
 */
@Data
@Schema(description = "修改图纸请求")
public class DrawingUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "标题")
    private String title;

    @Pattern(regexp = "^WL-\\d{4}$", message = "物料编码格式 WL-XXXX")
    @Schema(description = "物料编码", example = "WL-1006")
    private String materialCode;

    @Schema(description = "工艺路线 JSON")
    private String processRoute;

    @Schema(description = "PDF 路径")
    private String pdfPath;

    @Schema(description = "签字扫描件路径")
    private String signatureScanPath;

    @Schema(description = "备注")
    private String comment;
}
