package com.btsheng.erp.platform.label.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 标签预览请求（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.2）
 *
 * <p>入参：type (GD/LZ/SB/WW/WL) + data {qr_content, lines, factory_name} + format (PNG/PDF, 默认 PNG)
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "标签预览请求")
public class LabelPreviewRequest {

    @NotBlank
    @Pattern(regexp = "^(GD|LZ|SB|WW|WL)$", message = "type 不支持")
    @Schema(description = "模板类型", example = "GD", allowableValues = {"GD", "LZ", "SB", "WW", "WL"})
    private String type;

    @NotNull
    @Valid
    @Schema(description = "渲染数据载荷")
    private DataPayload data;

    @Schema(description = "输出格式", example = "PNG", allowableValues = {"PNG", "PDF"}, defaultValue = "PNG")
    private String format;

    @Data
    @Schema(description = "渲染数据")
    public static class DataPayload {
        @NotBlank
        @Size(max = 200, message = "qr_content ≤ 200 字符（QR v3-M 上限）")
        @Schema(description = "二维码内容（纯文本 · APP 扫码壳按前缀路由）", example = "GD-260614-001")
        private String qrContent;

        @Size(max = 6, message = "lines 最多 6 行")
        @Schema(description = "下方明文 · 最多 6 行 · 单行 ≤ 50 字符",
                example = "[\"GD-260614-001\", \"工单：WO20260614001\", \"工序：P03\", \"数量：50\", \"日期：2026-06-14\"]")
        private List<@Size(max = 50, message = "单行 ≤ 50 字符") String> lines;

        @Size(max = 20, message = "factory_name ≤ 20 字符")
        @Schema(description = "厂名 · 默认昆山佰泰胜精密加工", example = "昆山佰泰胜精密加工")
        private String factoryName;
    }
}