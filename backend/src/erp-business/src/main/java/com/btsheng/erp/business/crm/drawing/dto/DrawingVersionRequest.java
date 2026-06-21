package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.2 · 新增版本请求（v1 → v2 → v3 严格递增）
 */
@Data
@Schema(description = "新增图纸版本请求")
public class DrawingVersionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "版本号必填")
    @Pattern(regexp = "^v\\d+$", message = "版本号格式 v\\d+ (v1 < v2 < v3)")
    @Schema(description = "新版本号", example = "v2")
    private String version;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "新 PDF 路径")
    private String pdfPath;

    @Schema(description = "新签字扫描件路径")
    private String signatureScanPath;
}
