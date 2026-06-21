package com.btsheng.erp.business.crm.quote;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteHistory;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.quote.service.OrderConversionService;
import com.btsheng.erp.business.crm.quote.service.PdfExportService;
import com.btsheng.erp.business.crm.quote.service.QuoteService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** V1.3.7 Story 1.5 · AC-2.2.3 · Export 集成测例 (4 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExportIntegrationTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;

    private CrmQuote approvedQuote() {
        CrmQuote q = new CrmQuote();
        q.setId(1L);
        q.setQuoteNo("BJ20260611-0001");
        q.setCustomerName("X");
        q.setTotalAmount(new BigDecimal("50000"));
        q.setCurrency("CNY");
        q.setStatus("APPROVED");
        return q;
    }

    @Test void export_pdf_with_approval_sign() {
        CrmQuote q = approvedQuote();
        when(quoteMapper.selectById(1L)).thenReturn(q);
        when(itemMapper.selectByQuoteId(1L)).thenReturn(List.of());
        PdfExportService svc = new PdfExportService(quoteMapper, itemMapper, historyMapper);
        Result<byte[]> r = svc.exportPdf(1L, 1L);
        assertEquals(0, r.getCode());
        String pdf = new String(r.getData());
        assertTrue(pdf.contains("审批签字"));
        verify(historyMapper).insert(any(CrmQuoteHistory.class));
    }

    @Test void export_excel_3_sheets() {
        CrmQuote q = approvedQuote();
        when(quoteMapper.selectById(1L)).thenReturn(q);
        CrmQuoteItem item = new CrmQuoteItem();
        item.setDrawingNo("DWG-001");
        item.setMaterial("STEEL");
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("100"));
        item.setAmount(new BigDecimal("1000"));
        when(itemMapper.selectByQuoteId(1L)).thenReturn(List.of(item));
        PdfExportService svc = new PdfExportService(quoteMapper, itemMapper, historyMapper);
        Result<byte[]> r = svc.exportExcel(1L, 1L);
        assertEquals(0, r.getCode());
        String excel = new String(r.getData());
        assertTrue(excel.contains("Sheet1"));
        assertTrue(excel.contains("Sheet2"));
        assertTrue(excel.contains("DWG-001"));
    }

    @Test void export_pdf_1h_cache_hit() {
        CrmQuote q = approvedQuote();
        when(quoteMapper.selectById(1L)).thenReturn(q);
        when(itemMapper.selectByQuoteId(1L)).thenReturn(List.of());
        PdfExportService svc = new PdfExportService(quoteMapper, itemMapper, historyMapper);
        // 1st call: fresh
            Result<byte[]> r1 = svc.exportPdf(1L, 1L);
        assertEquals(0, r1.getCode());
        // 2nd call within 1h: cache hit
            Result<byte[]> r2 = svc.exportPdf(1L, 1L);
        assertEquals(0, r2.getCode());
        // 2 次 history insert (FRESH + CACHE_HIT)
            verify(historyMapper, times(2)).insert(any(CrmQuoteHistory.class));
    }

    @Test void convert_quote_to_order() {
        CrmQuote q = approvedQuote();
        when(quoteMapper.selectById(1L)).thenReturn(q);
        when(itemMapper.selectByQuoteId(1L)).thenReturn(List.of());
        OrderConversionService svc = new OrderConversionService(quoteMapper, itemMapper, mock(QuoteService.class));
        Result<Map<String, Object>> r = svc.convertToOrder(1L, 1L);
        assertEquals(0, r.getCode());
        assertNotNull(r.getData().get("orderNo"));
        assertTrue(r.getData().get("orderNo").toString().startsWith("XS"));
        assertEquals("CONVERTED", q.getStatus());
    }
}
