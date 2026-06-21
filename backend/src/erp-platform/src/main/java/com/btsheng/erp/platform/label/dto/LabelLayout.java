package com.btsheng.erp.platform.label.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签布局参数（V1.3.9 Sprint 12 · Story 12.3 · 解析 label_template.layout_json）
 *
 * <p>字段全部带默认值兜底（architect §6.1 测例 TC-12.3.5.1）· 缺字段 fallback 默认值
 * <p>三区坐标（mm）+ 字号（pt）+ QR 像素
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelLayout {
    /** 顶行色条 + 厂名高度（mm）· 默认 5 */
    private Double topBarH;
    /** 中央 QR 区高度（mm）· 默认 18 */
    private Double qrAreaH;
    /** 下方明文区高度（mm）· 默认 7 */
    private Double textAreaH;
    /** 明文字号（pt）· 默认 8 */
    private Double fontSize;
    /** QR 像素尺寸（px）· 默认 300 */
    private Integer qrSizePx;
    /** QR 物理尺寸（mm）· 默认 12 */
    private Double qrSizeMm;
    /** 标签宽度（mm）· 默认 50 */
    private Double widthMm;
    /** 标签高度（mm）· 默认 30 */
    private Double heightMm;
    /** 色条宽度（mm）· 默认 8 */
    private Double colorStripWidthMm;

    public double resolveTopBarH() {
        return topBarH == null ? 5.0 : topBarH;
    }

    public double resolveQrAreaH() {
        return qrAreaH == null ? 18.0 : qrAreaH;
    }

    public double resolveTextAreaH() {
        return textAreaH == null ? 7.0 : textAreaH;
    }

    public double resolveFontSize() {
        return fontSize == null ? 8.0 : fontSize;
    }

    public int resolveQrSizePx() {
        return qrSizePx == null ? 300 : qrSizePx;
    }

    public double resolveQrSizeMm() {
        return qrSizeMm == null ? 12.0 : qrSizeMm;
    }

    public double resolveWidthMm() {
        return widthMm == null ? 50.0 : widthMm;
    }

    public double resolveHeightMm() {
        return heightMm == null ? 30.0 : heightMm;
    }

    public double resolveColorStripWidthMm() {
        return colorStripWidthMm == null ? 8.0 : colorStripWidthMm;
    }
}