package com.btsheng.erp.platform.print;

import com.btsheng.erp.platform.font.FontProvider;
import com.btsheng.erp.platform.print.dto.PdfA4PrintRequest;
import com.btsheng.erp.platform.print.protocol.PdfA4Generator;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A4 PDF 生成器测例（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.2 · TC-12.4.2.1/2.2/2.4/2.5/2.6）
 *
 * <p>PDFBox 3.x 解析 PDF · 验证 27 标签/页 + metadata + 文件大小
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@DisplayName("Story 12.4 · A4 PDF 排版 PDFBox 解析（TC-12.4.2.1/2.2/2.4/2.6）+ Sprint 13 思源黑体（fallback Helvetica）")
class PdfA4GeneratorTest {

    private PdfA4Generator gen;

    @BeforeEach
    void setup() {
        // Sprint 13 · 测例用 null FontProvider 触发 fallback Helvetica（不依赖真实字体文件）
            gen = new PdfA4Generator(null);
    }

    private List<PdfA4PrintRequest.LabelItem> items(int n) {
        List<PdfA4PrintRequest.LabelItem> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            PdfA4PrintRequest.LabelItem it = new PdfA4PrintRequest.LabelItem();
            it.setTemplateCode("GD");
            it.setQrContent("GD-260614-" + String.format("%03d", i + 1));
            it.setLines(java.util.Arrays.asList("工单:WO-001", "工序:OP-10", "材料:Q235"));
            list.add(it);
        }
        return list;
    }

    // ========== TC-12.4.2.1 27 标签/页（30 项 → 2 页）==========
            @Test
    @DisplayName("TC-12.4.2.1 30 项 → 2 页（PDFBox 解析 · 27 标签/页 · 3×9=27 不是 30）")
    void TC_12_4_2_1_27_labels_per_page() throws Exception {
        byte[] pdf = gen.generate(items(30), "PR-20260614-001", "客户A", "Smart Workshop ERP");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(2, doc.getNumberOfPages(), "30 项 → 2 页（27 + 3）");
            PDPage page1 = doc.getPage(0);
            assertEquals(org.apache.pdfbox.pdmodel.common.PDRectangle.A4, page1.getMediaBox(),
                    "A4 尺寸");
        }
    }

    // ========== TC-12.4.2.1.b PDF 大小 < 1MB（27 标签）==========
            @Test
    @DisplayName("TC-12.4.2.6 PDF 大小 < 1MB（27 标签）")
    void TC_12_4_2_6_pdf_size_under_1mb() throws Exception {
        byte[] pdf = gen.generate(items(27), "PR-20260614-001", "客户A", "Smart Workshop ERP");
        assertTrue(pdf.length < 1024 * 1024, "PDF < 1MB · 实际 " + pdf.length + " bytes");
    }

    // ========== TC-12.4.2.2 5 标签不满页（仍按 27 栅格）==========
            @Test
    @DisplayName("TC-12.4.2.2 5 标签不满页 → 单页 + 22 空 cell 留白")
    void TC_12_4_2_2_5_items_one_page() throws Exception {
        byte[] pdf = gen.generate(items(5), "PR-20260614-001", "客户A", "Smart Workshop ERP");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages(), "5 项 → 1 页");
        }
    }

    // ========== TC-12.4.2.4 metadata ==========
            @Test
    @DisplayName("TC-12.4.2.4 PDF metadata 含 Title=logNo + Author=厂名")
    void TC_12_4_2_4_metadata() throws Exception {
        byte[] pdf = gen.generate(items(3), "PR-20260614-001", "客户A", "Smart Workshop ERP");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDDocumentInformation info = doc.getDocumentInformation();
            assertNotNull(info.getTitle(), "Title 必须存在");
            assertTrue(info.getTitle().contains("PR-20260614-001"), "Title 含 logNo");
            assertEquals("Smart Workshop ERP", info.getAuthor(), "Author = 厂名");
        }
    }

    // ========== TC-12.4.2.1.c 27 标签 1 页（精确 27）==========
            @Test
    @DisplayName("TC-12.4.2.1.c 27 项 → 1 页（满页）")
    void TC_12_4_2_1_c_27_items_exact_one_page() throws Exception {
        byte[] pdf = gen.generate(items(27), "PR-20260614-001", "客户A", "Smart Workshop ERP");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages(), "27 项恰好 1 页");
        }
    }

    // ========== 边界 · 1 标签 ==========
            @Test
    @DisplayName("边界 1 标签 → 1 页 + 26 空 cell")
    void boundary_1_item() throws Exception {
        byte[] pdf = gen.generate(items(1), "PR-20260614-001", "客户A", "Smart Workshop ERP");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages(), "1 项 → 1 页");
        }
    }
}
