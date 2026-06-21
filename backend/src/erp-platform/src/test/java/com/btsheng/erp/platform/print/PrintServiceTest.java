package com.btsheng.erp.platform.print;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.print.dto.PdfA4PrintRequest;
import com.btsheng.erp.platform.print.dto.PrintLogResponse;
import com.btsheng.erp.platform.print.dto.PrintStatisticsBucket;
import com.btsheng.erp.platform.print.dto.ReprintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintRequest;
import com.btsheng.erp.platform.print.dto.ZplPrintResult;
import com.btsheng.erp.platform.print.entity.SysPrintLog;
import com.btsheng.erp.platform.printer.entity.SysPrinter;
import com.btsheng.erp.platform.print.mapper.SysPrintLogMapper;
import com.btsheng.erp.platform.printer.mapper.SysPrinterMapper;
import com.btsheng.erp.platform.print.protocol.PdfA4Generator;
import com.btsheng.erp.platform.print.protocol.ProtocolFactory;
import com.btsheng.erp.platform.print.protocol.TsplProtocol;
import com.btsheng.erp.platform.print.protocol.ZplProtocol;
import com.btsheng.erp.platform.print.service.PrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PrintService 测例（V1.3.9 Sprint 12 · Story 12.4 · 32 测例中 12 项）
 *
 * <p>覆盖：
 * <ul>
 *   <li>TC-12.4.1.5 count 超限 / 协议不匹配预检</li>
 *   <li>TC-12.4.2.3 items 超限 422</li>
 *   <li>TC-12.4.3.1/3.2/3.3 多维过滤分页</li>
 *   <li>TC-12.4.3.4 单条详情</li>
 *   <li>TC-12.4.3.5/3.6 补打正向 + 防递归</li>
 *   <li>TC-12.4.4.1/4.2 统计聚合</li>
 *   <li>TC-12.4.6.1 50201 OFFLINE</li>
 *   <li>TC-12.4.6.2 50202 PROTOCOL_UNSUPPORTED</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@DisplayName("Story 12.4 · PrintService 测例（12 项 · 业务逻辑）")
class PrintServiceTest {

    private SysPrintLogMapper printLogMapper;
    private SysPrinterMapper printerMapper;
    private ProtocolFactory protocolFactory;
    private PdfA4Generator pdfA4Generator;
    private PrintService service;

    @BeforeEach
    void setup() {
        printLogMapper = mock(SysPrintLogMapper.class);
        printerMapper = mock(SysPrinterMapper.class);
        protocolFactory = new ProtocolFactory(Arrays.asList(new ZplProtocol(), new TsplProtocol()));
        pdfA4Generator = mock(PdfA4Generator.class);
        service = new PrintService(printLogMapper, printerMapper, protocolFactory, pdfA4Generator);
        when(printLogMapper.nextLogNoSeq(anyLong(), any())).thenReturn(1L);
    }

    // ========== TC-12.4.1.5 count 超限 422 ==========
            @Test
    @DisplayName("TC-12.4.1.5 ZPL count=101 → 422 40003")
    void TC_12_4_1_5_count_over_limit() {
        ZplPrintRequest req = new ZplPrintRequest();
        req.setTemplateCode("GD");
        req.setQrContent("GD-001");
        req.setLines(Arrays.asList("L1"));
        req.setPrinterId(5L);
        req.setCount(101);
        Result<ZplPrintResult> r = service.printZpl(req, 1L, "操作员", 1L);
        assertEquals(Result.CODE_PARAM_BOUND, r.getCode());
        assertTrue(r.getMessage().contains("1-100"));
        verify(printLogMapper, never()).insert(any(SysPrintLog.class));
    }

    // ========== TC-12.4.1.5.b 必填参数校验 ==========
            @Test
    @DisplayName("TC-12.4.1.5.b printerId 缺失 → 40001")
    void TC_12_4_1_5_b_printer_id_required() {
        ZplPrintRequest req = new ZplPrintRequest();
        req.setTemplateCode("GD");
        req.setQrContent("GD-001");
        req.setLines(Arrays.asList("L1"));
        req.setCount(1);
        Result<ZplPrintResult> r = service.printZpl(req, 1L, "操作员", 1L);
        assertEquals(Result.CODE_PARAM_MISSING, r.getCode());
    }

    // ========== TC-12.4.6.1 50201 PRINTER_OFFLINE ==========
            @Test
    @DisplayName("TC-12.4.6.1 sys_printer.status=OFFLINE → 50201")
    void TC_12_4_6_1_printer_offline() {
        SysPrinter p = new SysPrinter();
        p.setId(5L);
        p.setName("Zebra-1");
        p.setStatus("OFFLINE");
        p.setProtocol("ZPL");
        when(printerMapper.selectById(5L)).thenReturn(p);
        ZplPrintRequest req = new ZplPrintRequest();
        req.setTemplateCode("GD");
        req.setQrContent("GD-001");
        req.setLines(Arrays.asList("L1"));
        req.setPrinterId(5L);
        req.setCount(1);
        Result<ZplPrintResult> r = service.printZpl(req, 1L, "操作员", 1L);
        assertEquals(50201, r.getCode());
        assertTrue(r.getMessage().contains("PRINTER_OFFLINE"));
        assertTrue(r.getMessage().contains("Zebra-1"));
    }

    // ========== TC-12.4.6.2 50202 PROTOCOL_UNSUPPORTED ==========
            @Test
    @DisplayName("TC-12.4.6.2 sys_printer.protocol=INVALID → 50202")
    void TC_12_4_6_2_protocol_unsupported() {
        SysPrinter p = new SysPrinter();
        p.setId(5L);
        p.setName("BadPrinter");
        p.setStatus("ONLINE");
        p.setProtocol("INVALID");
        when(printerMapper.selectById(5L)).thenReturn(p);
        ZplPrintRequest req = new ZplPrintRequest();
        req.setTemplateCode("GD");
        req.setQrContent("GD-001");
        req.setLines(Arrays.asList("L1"));
        req.setPrinterId(5L);
        req.setCount(1);
        Result<ZplPrintResult> r = service.printZpl(req, 1L, "操作员", 1L);
        assertEquals(50202, r.getCode());
        assertTrue(r.getMessage().contains("PROTOCOL_UNSUPPORTED"));
    }

    // ========== TC-12.4.2.3 items 超限 422 ==========
            @Test
    @DisplayName("TC-12.4.2.3 PDF items=31 → 422 40003")
    void TC_12_4_2_3_pdf_items_over_limit() {
        PdfA4PrintRequest req = new PdfA4PrintRequest();
        List<PdfA4PrintRequest.LabelItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < 31; i++) {
            PdfA4PrintRequest.LabelItem it = new PdfA4PrintRequest.LabelItem();
            it.setTemplateCode("GD");
            it.setQrContent("GD-" + i);
            it.setLines(Arrays.asList("L1"));
            items.add(it);
        }
        req.setItems(items);
        Result<Map<String, Object>> r = service.printPdfA4(req, 1L, "操作员", 1L);
        assertEquals(Result.CODE_PARAM_BOUND, r.getCode());
        assertTrue(r.getMessage().contains("30"));
    }

    // ========== TC-12.4.2.1/2.2 PDF 成功生成（含 logId）==========
            @Test
    @DisplayName("TC-12.4.2.1 PDF 模式二 27 标签 → 写 SUCCESS log + 返回 base64 + logId")
    void TC_12_4_2_1_pdf_a4_success() {
        PdfA4PrintRequest req = new PdfA4PrintRequest();
        List<PdfA4PrintRequest.LabelItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < 27; i++) {
            PdfA4PrintRequest.LabelItem it = new PdfA4PrintRequest.LabelItem();
            it.setTemplateCode("GD");
            it.setQrContent("GD-260614-" + String.format("%03d", i + 1));
            it.setLines(Arrays.asList("L1"));
            items.add(it);
        }
        req.setItems(items);
        when(pdfA4Generator.generate(any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3, 4, 5});

        Result<Map<String, Object>> r = service.printPdfA4(req, 1L, "操作员A", 1L);
        assertEquals(0, r.getCode());
        Map<String, Object> data = r.getData();
        assertNotNull(data.get("printLogId"));
        assertNotNull(data.get("pdfBase64"));
        assertEquals("application/pdf", data.get("contentType"));
        ArgumentCaptor<SysPrintLog> captor = ArgumentCaptor.forClass(SysPrintLog.class);
        verify(printLogMapper).insert(captor.capture());
        SysPrintLog saved = captor.getValue();
        assertEquals("PDF_BROWSER", saved.getPrintMode());
        assertEquals("普通浏览器", saved.getPrinterNameSnapshot());
        assertEquals("SUCCESS", saved.getStatus());
        assertNull(saved.getPrinterId());
    }

    // ========== TC-12.4.3.1 多维过滤分页 ==========
            @Test
    @DisplayName("TC-12.4.3.1 多维过滤分页 · codeType=GD + operatorId=1")
    void TC_12_4_3_1_multi_filter_paged() {
        SysPrintLog r1 = new SysPrintLog();
        r1.setId(1L);
        r1.setLogNo("PR-20260614-001");
        r1.setCodeType("GD");
        r1.setCodeValue("GD-001");
        r1.setPrintMode("ZPL_DIRECT");
        r1.setStatus("SUCCESS");
        r1.setCopies(1);
        r1.setOperatorName("操作员A");
        when(printLogMapper.countByFilters(eq("GD"), any(), any(), eq(1L), any(), any(), any(), eq(1L))).thenReturn(10L);
        when(printLogMapper.selectByFilters(eq("GD"), any(), any(), eq(1L), any(), any(), any(), eq(20), eq(0), eq(1L)))
                .thenReturn(Arrays.asList(r1));
        Result<PageResponse<PrintLogResponse>> r = service.listPrintLogs(
                "GD", null, null, 1L, null, null, null, 1, 20, 1L);
        assertEquals(0, r.getCode());
        assertEquals(10L, r.getData().getTotal());
        assertEquals(1, r.getData().getRecords().size());
        assertEquals("PR-20260614-001", r.getData().getRecords().get(0).getLogNo());
    }

    // ========== TC-12.4.3.4 单条详情 ==========
            @Test
    @DisplayName("TC-12.4.3.4 GET /logs/{id} 单条详情 · 含 reference 链")
    void TC_12_4_3_4_get_one_detail() {
        SysPrintLog r1 = new SysPrintLog();
        r1.setId(2L);
        r1.setLogNo("PR-20260614-002");
        r1.setCodeType("GD");
        r1.setCodeValue("GD-002");
        r1.setPrintMode("ZPL_DIRECT");
        r1.setStatus("SUCCESS");
        r1.setCopies(1);
        r1.setReferenceLogId(99L);
        r1.setPrinterNameSnapshot("Zebra-1");
        r1.setTenantId(1L);
        when(printLogMapper.selectById(2L)).thenReturn(r1);
        Result<PrintLogResponse> r = service.getPrintLog(2L, 1L);
        assertEquals(0, r.getCode());
        assertEquals(99L, r.getData().getReferenceLogId());
        assertEquals("Zebra-1", r.getData().getPrinterNameSnapshot());
    }

    // ========== TC-12.4.3.5 补打同模式 ==========
            @Test
    @DisplayName("TC-12.4.3.5 补打同模式（targetMode=SAME）· 新 log reference_log_id=sourceId")
    void TC_12_4_3_5_replay_same_mode() {
        SysPrintLog source = new SysPrintLog();
        source.setId(1L);
        source.setLogNo("PR-20260614-001");
        source.setCodeType("GD");
        source.setCodeValue("GD-001");
        source.setPrintMode("ZPL_DIRECT");
        source.setStatus("SUCCESS");
        source.setCopies(1);
        source.setReferenceLogId(null);
        source.setTenantId(1L);
        source.setPrinterId(5L);
        when(printLogMapper.selectById(1L)).thenReturn(source);

        SysPrinter p = new SysPrinter();
        p.setId(5L);
        p.setName("Zebra-1");
        p.setStatus("ONLINE");
        p.setProtocol("ZPL");
        p.setIp("192.168.1.100");
        p.setPort(9100);
        when(printerMapper.selectById(5L)).thenReturn(p);

        ReprintRequest req = new ReprintRequest();
        req.setTargetMode("SAME");

        Result<Map<String, Object>> r = service.replay(1L, req, 2L, "操作员B", 1L);
        assertEquals(0, r.getCode());
        assertEquals(1L, r.getData().get("referenceLogId"));
        assertEquals("ZPL_DIRECT", r.getData().get("mode"));
    }

    // ========== TC-12.4.3.6 补打链防递归 ==========
            @Test
    @DisplayName("TC-12.4.3.6 补打链防递归 · source.referenceLogId != null → 40954")
    void TC_12_4_3_6_replay_forbidden_recursion() {
        SysPrintLog b = new SysPrintLog();
        b.setId(2L);
        b.setLogNo("PR-20260614-002");
        b.setCodeType("GD");
        b.setCodeValue("GD-002");
        b.setPrintMode("PDF_BROWSER");
        b.setStatus("SUCCESS");
        b.setReferenceLogId(1L);  // 已经是补打记录
            b.setTenantId(1L);
        when(printLogMapper.selectById(2L)).thenReturn(b);

        BizException ex = assertThrows(BizException.class,
                () -> service.replay(2L, new ReprintRequest(), 3L, "操作员C", 1L));
        assertEquals(40954, ex.getCode());
        assertTrue(ex.getMessage().contains("PRINT_REPLAY_FORBIDDEN"));
        assertTrue(ex.getMessage().contains("递归"));
    }

    // ========== TC-12.4.3.6.b FAILED 不可补打 ==========
            @Test
    @DisplayName("TC-12.4.3.6.b FAILED 状态不可补打 → 40954")
    void TC_12_4_3_6_b_replay_forbidden_failed() {
        SysPrintLog f = new SysPrintLog();
        f.setId(3L);
        f.setStatus("FAILED");
        f.setCodeType("GD");
        f.setCodeValue("GD-003");
        f.setPrintMode("ZPL_DIRECT");
        f.setTenantId(1L);
        when(printLogMapper.selectById(3L)).thenReturn(f);

        BizException ex = assertThrows(BizException.class,
                () -> service.replay(3L, new ReprintRequest(), 4L, "操作员D", 1L));
        assertEquals(40954, ex.getCode());
        assertTrue(ex.getMessage().contains("SUCCESS"));
    }

    // ========== TC-12.4.4.1/4.2 统计聚合 ==========
            @Test
    @DisplayName("TC-12.4.4.1 groupBy=operator_id 聚合 · 3 人各 100 条 80 SUCCESS / 20 FAILED")
    void TC_12_4_4_1_statistics_group_by_operator() {
        List<Map<String, Object>> rows = Arrays.asList(
                row("1001", "SUCCESS", 80L, 80L),
                row("1001", "FAILED", 20L, 20L),
                row("1002", "SUCCESS", 80L, 80L),
                row("1002", "FAILED", 20L, 20L),
                row("1003", "SUCCESS", 80L, 80L),
                row("1003", "FAILED", 20L, 20L));
        when(printLogMapper.aggregateByGroupAndStatus(eq("operator_id"), any(), any(), eq(1L)))
                .thenReturn(rows);

        Result<List<PrintStatisticsBucket>> r = service.getStatistics("operator_id", null, null, 1L);
        assertEquals(0, r.getCode());
        assertEquals(3, r.getData().size());
        for (PrintStatisticsBucket b : r.getData()) {
            assertEquals(100L, b.getTotalCount());
            assertEquals(80L, b.getSuccessCount());
            assertEquals(20L, b.getFailedCount());
        }
    }

    private static Map<String, Object> row(String key, String status, long cnt, long copies) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("bucket_key", key);
        m.put("status", status);
        m.put("cnt", cnt);
        m.put("total_copies", copies);
        return m;
    }
}
