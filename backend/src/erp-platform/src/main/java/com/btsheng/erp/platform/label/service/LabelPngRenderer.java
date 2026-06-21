package com.btsheng.erp.platform.label.service;

import com.btsheng.erp.platform.font.FontProvider;
import com.btsheng.erp.platform.label.dto.LabelLayout;
import com.btsheng.erp.platform.label.entity.LabelTemplate;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 标签 PNG 渲染器（V1.3.9 Sprint 12 Story 12.3 · AC-12.3.2 · Sprint 13 Story 13.2 思源黑体嵌入）
 *
 * <p>三区布局：
 * <ul>
 *   <li>顶行：色条（colorStripWidthMm）+ 厂名（居中右侧）</li>
 *   <li>中央：QR 二维码（ZXing 输出嵌入）</li>
 *   <li>下方：明文 lines（≤6 行）· 字号 8pt</li>
 * </ul>
 *
 * <p>Sprint 13 升级（Story 13.2）：
 * <ul>
 *   <li>默认字体从 JDK SansSerif 切换到思源黑体（SourceHanSansCN-Normal）via {@link FontProvider}</li>
 *   <li>保证跨 OS 中文厂名（昆山佰泰胜精密加工 6pt）渲染一致</li>
 *   <li>FontProvider 加载失败时降级到 SansSerif（保底）</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
public class LabelPngRenderer {

    /** 思源黑体厂名字号（顶行）· 与 12.3 既有 14pt 保持一致 */
    private static final float FACTORY_FONT_SIZE = 14f;
    /** 思源黑体明文字号（下方 6 行）· 与 12.3 既有 11pt 保持一致 */
    private static final float TEXT_FONT_SIZE = 11f;

    /** 字体提供者（由 Service 注入；null 时降级 SansSerif） */
    private final FontProvider fontProvider;

    /** 默认无参构造（保持向后兼容 · 老调用方不传 FontProvider → 自动 fallback） */
    public LabelPngRenderer() {
        this(null);
    }

    /** 注入 FontProvider 的构造（Sprint 13 推荐使用） */
    public LabelPngRenderer(FontProvider fontProvider) {
        this.fontProvider = fontProvider;
    }

    /**
     * 获取厂名字体（思源黑体 Bold / SansSerif Bold fallback）
     */
    private Font factoryFont() {
        if (fontProvider != null) {
            return fontProvider.getBold(FACTORY_FONT_SIZE);
        }
        return new Font("SansSerif", Font.BOLD, (int) FACTORY_FONT_SIZE);
    }

    /**
     * 获取明文字体（思源黑体 Regular / SansSerif Plain fallback）
     */
    private Font textFont() {
        if (fontProvider != null) {
            return fontProvider.getRegular(TEXT_FONT_SIZE);
        }
        return new Font("SansSerif", Font.PLAIN, (int) TEXT_FONT_SIZE);
    }

    /** 物理 mm → 像素转换（基于 DPI） */
    public static int mmToPx(double mm, int dpi) {
        return (int) Math.round(mm * dpi / 25.4);
    }

    /**
     * 渲染 PNG 字节数组
     *
     * @param template     标签模板（含 colorStrip / factoryName / layout / dpi）
     * @param qrPngBytes   ZXing 渲染的 QR PNG 字节
     * @param lines        下方明文（≤6 行 · null/空时使用空行）
     * @return PNG 字节数组
     */
    public byte[] render(LabelTemplate template, byte[] qrPngBytes, List<String> lines)
            throws IOException {
        LabelLayout layout = parseLayout(template.getLayoutJson());
        int dpi = template.getDpi() == null ? 300 : template.getDpi();
        int imgW = mmToPx(layout.resolveWidthMm(), dpi);
        int imgH = mmToPx(layout.resolveHeightMm(), dpi);

        BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 背景白
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgW, imgH);

            // 顶行高度
            int topBarPx = mmToPx(layout.resolveTopBarH(), dpi);
            int stripPx = mmToPx(layout.resolveColorStripWidthMm(), dpi);
            // 色条
            Color stripColor = parseHex(template.getColorStrip());
            g.setColor(stripColor);
            g.fillRect(0, 0, stripPx, topBarPx);

            // 厂名（顶行右侧 · 居中）· Sprint 13：思源黑体 Bold
            String factoryName = template.getFactoryName();
            g.setColor(Color.BLACK);
            g.setFont(factoryFont());
            FontMetrics fm = g.getFontMetrics();
            int nameX = stripPx + mmToPx(1.0, dpi); // 1mm 间隙
            int nameY = topBarPx / 2 + fm.getAscent() / 2 - 2;
            g.drawString(factoryName, nameX, nameY);

            // QR 区
            int qrAreaY = topBarPx;
            int qrAreaH = mmToPx(layout.resolveQrAreaH(), dpi);
            int qrBoxPx = mmToPx(layout.resolveQrSizeMm(), dpi);
            int qrX = (imgW - qrBoxPx) / 2;
            int qrY = qrAreaY + (qrAreaH - qrBoxPx) / 2;
            drawQr(g, qrPngBytes, qrX, qrY, qrBoxPx);

            // 下方明文 · Sprint 13：思源黑体 Regular
            int textY = qrAreaY + qrAreaH + mmToPx(0.5, dpi);
            g.setColor(Color.BLACK);
            g.setFont(textFont());
            int lineHeight = mmToPx(1.1, dpi);
            int maxLines = Math.min(6, lines == null ? 0 : lines.size());
            for (int i = 0; i < maxLines; i++) {
                g.drawString(lines.get(i), mmToPx(1.0, dpi), textY + i * lineHeight);
            }

            // 边框
            g.setColor(new Color(200, 200, 200));
            g.setStroke(new BasicStroke(0.5f));
            g.drawRect(0, 0, imgW - 1, imgH - 1);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }

    private void drawQr(Graphics2D g, byte[] qrPngBytes, int x, int y, int size) throws IOException {
        BufferedImage qr = ImageIO.read(new ByteArrayInputStream(qrPngBytes));
        if (qr == null) {
            // 兜底：绘制占位灰块
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, y, size, size);
            return;
        }
        g.drawImage(qr, x, y, size, size, null);
    }

    /**
     * 解析 #RRGGBB HEX 字符串为 Color · 失败兜底黑色
     */
    static Color parseHex(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return Color.BLACK;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int gr = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return new Color(r, gr, b);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /**
     * 解析 layout_json · 缺字段时返回默认值（architect §6.1 TC-12.3.5.1）
     */
    public static LabelLayout parseLayout(String json) {
        if (json == null || json.isEmpty()) {
            return new LabelLayout();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(json, LabelLayout.class);
        } catch (Exception e) {
            return new LabelLayout();
        }
    }
}