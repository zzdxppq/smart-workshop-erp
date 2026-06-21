package com.btsheng.erp.platform.print.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 标签数据 DTO（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1/12.4.2）
 *
 * <p>统一入参：templateCode + qrContent + lines + colorBarHex
 * <p>协议无关：ZplProtocol / TsplProtocol / PdfA4Generator 各自渲染
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "标签数据 · 协议无关统一 DTO")
public class LabelData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模板代号 · GD/LZ/SB/WW/WL", example = "GD")
    private String templateCode;

    @Schema(description = "QR 内容", example = "GD-260614-001")
    private String qrContent;

    @Schema(description = "行文本 · max 6")
    private List<String> lines;

    @Schema(description = "色条 HEX（默认 #1E40AF）", example = "#1E40AF")
    private String colorBarHex;
}
