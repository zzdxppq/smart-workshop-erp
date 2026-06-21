package com.btsheng.erp.business.crm.drawing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.1 · 创建图纸请求
 */
@Data
@Schema(description = "创建图纸请求")
public class DrawingCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "图号必填")
    @Pattern(regexp = "^DWG-\\d{8}-\\d{4}$", message = "图号格式 DWG-YYYYMMDD-NNNN")
    @Schema(description = "图号（自动生成）", example = "DWG-20260612-0006")
    private String drawingNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "客户图号（PDF 文件名，V2.1）")
    private String customerDrawingNo;

    @Schema(description = "单件重量 kg（V2.1）")
    private java.math.BigDecimal unitWeight;

    @Schema(description = "材质（上传图纸必填，工程转化前无料号）")
    private String materialGrade;

    @Schema(description = "规格尺寸（上传图纸必填）")
    private String specSize;

    @Schema(description = "物料编码（工程转化后绑定，上传时可空）", example = "WL-A001")
    private String materialCode;

    @NotBlank(message = "工艺路线必填")
    @Schema(description = "工艺路线 JSON（5 段成本）", example = "[{\"step\":1,\"name\":\"车削\",\"cost\":100.00}]")
    private String processRoute;

    @Schema(description = "FA 件（> 20万 二次密码）", example = "0")
    private Integer isFa = 0;

    @Schema(description = "新品", example = "1")
    private Integer isNew = 0;

    @Schema(description = "PDF 路径（MinIO / 本地）")
    private String pdfPath;

    @Schema(description = "签字扫描件路径（AES-256-GCM 加密）")
    private String signatureScanPath;

    @Schema(description = "备注")
    private String comment;
}
