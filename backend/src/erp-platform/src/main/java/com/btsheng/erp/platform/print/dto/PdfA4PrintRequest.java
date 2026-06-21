package com.btsheng.erp.platform.print.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 模式二 A4 PDF 打印请求（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.2）
 *
 * <p>单次最多 30 标签/请求（V1.3.9 客户规模 · 30+ 需多次请求）
 * <p>PDF 排版 27 标签/页（3×9 = 50mm×30mm × 9 行 = 270mm 高度 · A4 297mm 可用）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "模式二 A4 PDF 打印请求（3×9=27 标签/页）")
public class PdfA4PrintRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    @NotEmpty
    @Size(min = 1, max = 30)
    @Valid
    @Schema(description = "标签项 · 1-30 项/请求")
    private List<LabelItem> items;

    @Size(max = 200)
    @Schema(description = "备注")
    private String remark;

    @Data
    @Schema(description = "标签项")
    public static class LabelItem implements Serializable {
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

        @Schema(description = "色条 HEX（默认 #1E40AF）", example = "#1E40AF")
        private String colorBarHex;
    }
}
