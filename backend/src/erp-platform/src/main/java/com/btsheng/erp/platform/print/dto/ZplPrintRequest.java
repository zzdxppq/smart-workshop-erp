package com.btsheng.erp.platform.print.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 模式一 ZPL/TSPL 打印请求（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "模式一 ZPL/TSPL 直连打印请求")
public class ZplPrintRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "模板代号 · GD/LZ/SB/WW/WL", example = "GD")
    private String templateCode;

    @NotBlank
    @Schema(description = "QR 内容", example = "GD-260614-001")
    private String qrContent;

    @NotNull
    @Size(max = 6)
    @Schema(description = "行文本 · max 6")
    private List<String> lines;

    @NotNull
    @Schema(description = "sys_printer.id", example = "5")
    private Long printerId;

    @Min(1) @Max(100)
    @Schema(description = "份数 · 1-100", example = "1")
    private Integer count = 1;

    @Schema(description = "色条 HEX（默认 #1E40AF）", example = "#1E40AF")
    private String colorBarHex;

    @Size(max = 200)
    @Schema(description = "备注")
    private String remark;
}
