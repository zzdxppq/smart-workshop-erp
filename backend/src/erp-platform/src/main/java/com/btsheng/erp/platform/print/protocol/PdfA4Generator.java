package com.btsheng.erp.platform.print.protocol;

import com.btsheng.erp.platform.font.FontProvider;
import com.btsheng.erp.platform.print.dto.LabelData;
import com.btsheng.erp.platform.print.dto.PdfA4PrintRequest;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * A4 PDF 标签排版生成器（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.2 · TC-12.4.2.1/2.2/2.4 · Sprint 13 Story 13.2 思源黑体嵌入）
 *
 * <p>OpenPDF 1.3.34 · 27 标签/页（3 列 × 9 行 = 50mm×30mm × 9 = 270mm 高度 · A4 297mm 可用）
 * <p>30 项输入 → 自动 2 页（第 1 页 27 + 第 2 页 3 + 24 空 cell 留白）
 * <p>PDF metadata 含 customer + log_no + 厂名 · 离线追溯
 *
 * <p>Sprint 13 升级（Story 13.2）：
 * <ul>
 *   <li>字体从 PDFBox/内置 Helvetica 切换到思源黑体（SourceHanSansCN-Normal）via {@link FontProvider}</li>
 *   <li>EMBEDDED 嵌入字体到 PDF · 接收端（Windows/macOS/Linux/Android）跨 OS 100% 一致</li>
 *   <li>FontProvider 加载失败时降级到 Helvetica（保底）</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component
public class PdfA4Generator {

    private static final Logger log = LoggerFactory.getLogger(PdfA4Generator.class);

    public static final int COLS = 3;
    public static final int ROWS = 9;
    public static final int LABELS_PER_PAGE = COLS * ROWS;  // 27 标签/页

    /** Sprint 13 思源黑体支持的字号（与 12.4 既有 7pt/6pt 保持兼容） */
    private static final float TEMPLATE_CODE_FONT_SIZE = 7f;
    private static final float LINE_FONT_SIZE = 6f;

    /** 思源黑体字体提供者（注入） */
    private final FontProvider fontProvider;

    @Autowired
    public PdfA4Generator(FontProvider fontProvider) {
        this.fontProvider = fontProvider;
        log.info("[PdfA4Generator] 初始化完成 · FontProvider 注入: {}",
                fontProvider != null && fontProvider.isLoaded() ? "思源黑体 OK" : "fallback(Helvetica)");
    }

    /**
     * 生成 A4 PDF 字节流（含 metadata + 多页支持）
     *
     * @param items 标签项
     * @param logNo sys_print_log.logNo（用于 PDF metadata）
     * @param customer 客户名（用于 PDF metadata）
     * @param companyName 厂名（用于 PDF metadata）
     * @return PDF 字节流
     */
    public byte[] generate(List<PdfA4PrintRequest.LabelItem> items, String logNo, String customer, String companyName) {
        long start = System.currentTimeMillis();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // A4 尺寸（595 × 842 pt）
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            // ===== PDF metadata =====
            doc.addTitle((customer != null ? customer : "customer") + "-" + (logNo != null ? logNo : "log"));
            doc.addAuthor(companyName != null ? companyName : "Smart Workshop ERP");
            doc.addSubject(companyName != null ? companyName : "标签打印");
            doc.addCreator("Smart Workshop ERP V1.3.9 · Sprint 12 Story 12.4 + Sprint 13 Story 13.2");
            doc.addKeywords("label,print,GD,LZ,SB,WW,WL,Noto,SourceHanSans");

            doc.open();

            int total = items.size();
            int pages = (total + LABELS_PER_PAGE - 1) / LABELS_PER_PAGE;
            for (int p = 0; p < pages; p++) {
                if (p > 0) {
                    doc.newPage();
                }
                int from = p * LABELS_PER_PAGE;
                int to = Math.min(from + LABELS_PER_PAGE, total);
                renderPage(doc, items.subList(from, to));
            }

            doc.close();
            writer.close();

            byte[] result = baos.toByteArray();
            log.info("[PdfA4Generator] generated pages={} labels={} bytes={} latencyMs={} fontEmbedded={}",
                    pages, total, result.length, System.currentTimeMillis() - start,
                    fontProvider != null && fontProvider.isLoaded());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 渲染单页（27 标签/页 · 不足补空 cell 留白）
     */
    private void renderPage(Document doc, List<PdfA4PrintRequest.LabelItem> items) throws Exception {
        PdfPTable table = new PdfPTable(COLS);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1});
        table.setSpacingAfter(0);

        // 渲染真实项
            for (PdfA4PrintRequest.LabelItem item : items) {
            table.addCell(renderCell(item));
        }
        // 不足 27 补空 cell 留白（防止缩印）
            for (int i = items.size(); i < LABELS_PER_PAGE; i++) {
            table.addCell(emptyCell());
        }

        doc.add(table);
    }

    /**
     * 渲染单个标签 cell
     */
    private PdfPCell renderCell(PdfA4PrintRequest.LabelItem item) throws Exception {
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(85f);  // 30mm ≈ 85pt
            cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4f);

        LabelData data = new LabelData();
        data.setTemplateCode(item.getTemplateCode());
        data.setQrContent(item.getQrContent());
        data.setLines(item.getLines());
        data.setColorBarHex(item.getColorBarHex());

        // 色条（顶部）
            PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        PdfPCell barCell = new PdfPCell();
        barCell.setFixedHeight(10f);
        barCell.setBackgroundColor(parseColor(item.getColorBarHex()));
        barCell.setBorder(Rectangle.NO_BORDER);
        inner.addCell(barCell);

        // 模板代号 + QR · Sprint 13：思源黑体
            PdfPCell bodyCell = new PdfPCell();
        bodyCell.setBorder(Rectangle.NO_BORDER);
        bodyCell.setPadding(2f);
        bodyCell.addElement(new Paragraph("[" + safe(item.getTemplateCode()) + "] " + safe(item.getQrContent()),
                cjkFont(TEMPLATE_CODE_FONT_SIZE, Font.NORMAL)));
        // QR 图（缩略 60×60 pt）
            if (item.getQrContent() != null) {
            try {
                Image qrImage = renderQrImage(item.getQrContent(), 60, 60);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                bodyCell.addElement(qrImage);
            } catch (Exception ignored) {
                // QR 渲染失败 → 仅文字 · 不阻塞
            }
        }
        // 文本行 · Sprint 13：思源黑体
            if (item.getLines() != null) {
            for (String line : item.getLines()) {
                if (line == null) continue;
                bodyCell.addElement(new Phrase(safe(line),
                        cjkFont(LINE_FONT_SIZE, Font.NORMAL)));
            }
        }
        inner.addCell(bodyCell);
        cell.addElement(inner);

        return cell;
    }

    /**
     * 空 cell（留白用）
     */
    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(85f);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    /**
     * QR 渲染
     */
    private Image renderQrImage(String content, int width, int height) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
        return Image.getInstance(pngOut.toByteArray());
    }

    /**
     * Sprint 13 · 思源黑体 Com.lowagie.text.Font 工厂方法
     *
     * <p>使用 FontProvider.getPdfBaseFont()（IDENTITY_H + EMBEDDED）·
     * 加载失败时降级到 Helvetica（保底 · 不阻塞 PDF 生成）
     */
    private Font cjkFont(float size, int style) {
        if (fontProvider != null && fontProvider.getPdfBaseFont() != null) {
            return new Font(fontProvider.getPdfBaseFont(), size, style);
        }
        // Fallback: Helvetica（不支持中文 · 仅保底）
            return new Font(Font.HELVETICA, size, style);
    }

    /**
     * HEX 颜色解析（#1E40AF → blue）
     */
    private static java.awt.Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) return new java.awt.Color(0x1E, 0x40, 0xAF);
        try {
            String s = hex.startsWith("#") ? hex.substring(1) : hex;
            if (s.length() == 6) {
                return new java.awt.Color(
                        Integer.parseInt(s.substring(0, 2), 16),
                        Integer.parseInt(s.substring(2, 4), 16),
                        Integer.parseInt(s.substring(4, 6), 16));
            }
        } catch (Exception ignored) {
        }
        return new java.awt.Color(0x1E, 0x40, 0xAF);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}